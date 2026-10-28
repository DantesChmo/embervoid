package com.embervoid.realm;

import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.realm.api.ShrineHooks;
import com.embervoid.realm.worldgen.RealmFeatures;
import com.embervoid.realm.worldgen.RealmRoads;
import com.embervoid.sanctum.SanctumBlocks;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/** Server-side realm rules: respawn, shrines, the void edge and protected landings/arenas. */
public final class RealmEvents {
	/** Where a player who died at x, z in world {@code died} comes back, if that is a realm dimension (see {@link #register}). */
	public static void respawnAfterDeath(ServerPlayerEntity player, RegistryKey<World> died, double x, double z) {
		RealmDefinition realm = Realms.byWorld(died);
		if (realm == null) {
			return;
		}
		if (FinalZoneLock.region(realm).contains(x, z)) {
			if (!RealmTeleports.toShrine(player, RealmTeleports.finalShrineKey(died)) && !toCitadelShrine(player, realm)) {
				RealmTeleports.toLanding(player, died);
			}
		} else if (!RealmTeleports.toShrine(player, died)) {
			RealmTeleports.toLanding(player, died);
		}
	}

	/** To the first Citadel shrine beside the gate road's end ({@link RealmRoads#citadelShrine}); false if there is none. */
	private static boolean toCitadelShrine(ServerPlayerEntity player, RealmDefinition realm) {
		int[] column = RealmRoads.citadelShrine(realm);
		ServerWorld world = player.getServer().getWorld(realm.world());
		if (column == null || world == null) {
			return false;
		}
		world.getChunk(column[0] >> 4, column[1] >> 4);
		int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, column[0], column[1]);
		BlockPos.Mutable shrine = new BlockPos.Mutable(column[0], top, column[1]);
		while (!world.getBlockState(shrine).isOf(SanctumBlocks.EMBER_SHRINE)) {
			if (shrine.getY() < top - 4) {
				return false;
			}
			shrine.move(0, -1, 0);
		}
		RealmTeleports.teleport(player, world, RealmTeleports.standingSpot(world, shrine.toImmutable()), player.getYaw());
		return true;
	}

	/** How far past the realm radius the void starts pushing players back. */
	private static final int VOID_MARGIN = 32;

	public static void register() {
		// Death in a realm dimension: respawn at the last shrine rested at in that dimension, else its landing. The open
		// zones and the final zone each count on their own (DESIGN §4.2): a death in the final sector goes to
		// the last shrine rested at there, else the first Citadel shrine just inside the gate. The bed spawn stays.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				respawnAfterDeath(newPlayer, oldPlayer.getWorld().getRegistryKey(), oldPlayer.getX(), oldPlayer.getZ());
			}
		});
		// Beds work in realms (DESIGN §10.1) but never move the player's spawn point there.
		EntitySleepEvents.ALLOW_SETTING_SPAWN.register((player, sleepingPos) -> !Realms.isRealm(player.getWorld()));

		ShrineHooks.RESTED.register((player, world, pos) -> {
			RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
			if (realm != null) {
				Identifier key = FinalZoneLock.region(realm).contains(pos.getX() + 0.5, pos.getZ() + 0.5)
						? RealmTeleports.finalShrineKey(world.getRegistryKey()) : world.getRegistryKey().getValue();
				RealmPlayerData.get(player).shrines.put(key, GlobalPos.create(world.getRegistryKey(), pos));
			}
		});

		// Landings and arenas cannot be broken or built in (DESIGN §10.1); the rest of a realm can.
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
				!(world instanceof ServerWorld server) || !isProtected(server, player, pos));
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world instanceof ServerWorld server && placesSomething(player.getStackInHand(hand))
					&& (player.isSneaking() || !isInteractive(world.getBlockState(hit.getBlockPos())))
					&& isProtected(server, player, hit.getBlockPos().offset(hit.getSide()))) {
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.getTime() % 10 == 0) {
				RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
				if (realm != null) {
					pushBackFromVoid(world, realm.radius() + VOID_MARGIN);
				}
			}
		});
	}

	private static boolean isProtected(ServerWorld world, PlayerEntity player, BlockPos pos) {
		return !player.isCreative() && Realms.isRealm(world) && RealmFeatures.at(world, pos) != null;
	}

	private static boolean placesSomething(ItemStack stack) {
		Item item = stack.getItem();
		return item instanceof BlockItem || item instanceof BucketItem;
	}

	private static boolean isInteractive(BlockState state) {
		return state.isOf(RealmBlocks.RETURN_OBELISK) || state.isOf(RealmBlocks.CITADEL_GATE) || state.isOf(SanctumBlocks.EMBER_SHRINE);
	}

	private static void pushBackFromVoid(ServerWorld world, double maxDistance) {
		for (ServerPlayerEntity player : world.getPlayers()) {
			double distance = Math.sqrt(player.getX() * player.getX() + player.getZ() * player.getZ());
			if (distance > maxDistance && !player.isCreative() && !player.isSpectator()) {
				Vec3d inward = new Vec3d(-player.getX(), 0, -player.getZ()).normalize();
				player.setVelocity(inward.x * 1.5, 0.5, inward.z * 1.5);
				player.velocityModified = true;
				player.sendMessage(Text.translatable("embervoid.realm.void"), true);
			}
		}
	}

	private RealmEvents() {}
}
