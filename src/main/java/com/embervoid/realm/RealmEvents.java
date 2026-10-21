package com.embervoid.realm;

import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.realm.api.ShrineHooks;
import com.embervoid.realm.worldgen.RealmFeatures;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Server-side realm rules: respawn, shrines, the void edge and protected landings/arenas. */
public final class RealmEvents {
	/** How far past the realm radius the void starts pushing players back. */
	private static final int VOID_MARGIN = 32;

	public static void register() {
		// Death in a realm dimension: respawn at the last shrine rested at in that dimension, else its landing (the
		// open zones and the final zone each count on their own). The bed spawn stays as it was.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			RegistryKey<World> died = oldPlayer.getWorld().getRegistryKey();
			if (!alive && Realms.byWorld(died) != null && !RealmTeleports.toShrine(newPlayer, died)) {
				RealmTeleports.toLanding(newPlayer, died);
			}
		});
		// Beds work in realms (DESIGN §10.1) but never move the player's spawn point there.
		EntitySleepEvents.ALLOW_SETTING_SPAWN.register((player, sleepingPos) -> !Realms.isRealm(player.getWorld()));

		ShrineHooks.RESTED.register((player, world, pos) -> {
			RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
			if (realm != null) {
				RealmPlayerData.get(player).shrines.put(world.getRegistryKey().getValue(), GlobalPos.create(world.getRegistryKey(), pos));
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
					pushBackFromVoid(world, realm.radiusOf(world.getRegistryKey()) + VOID_MARGIN);
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
		return state.isOf(RealmBlocks.RETURN_OBELISK) || state.isOf(RealmBlocks.SEALED_GATE) || state.isOf(SanctumBlocks.EMBER_SHRINE);
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
