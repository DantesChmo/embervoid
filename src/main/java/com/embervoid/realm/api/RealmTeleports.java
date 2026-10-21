package com.embervoid.realm.api;

import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmPlayerData;
import com.embervoid.realm.Realms;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Travel into, inside and out of realms. Entry point for the Sanctum's Void Obelisks: {@link #enterRealm}.
 * All methods run on the server thread.
 */
public final class RealmTeleports {
	/**
	 * Takes the player into a realm, remembering where they came from (the Sanctum) so the return obelisk brings them
	 * back there.
	 *
	 * @param toLastShrine arrive at the last Ember Shrine rested at in that realm instead of the landing; falls back to
	 *                     the landing when there is none
	 * @return false if the realm is unknown or still locked for this player (they get a message)
	 */
	public static boolean enterRealm(ServerPlayerEntity player, Identifier realmId, boolean toLastShrine) {
		RealmDefinition realm = Realms.byId(realmId);
		if (realm == null || !ProgressManager.isRealmUnlocked(player, realmId)) {
			player.sendMessage(Text.translatable("embervoid.realm.locked"), true);
			return false;
		}
		RealmPlayerData data = RealmPlayerData.get(player);
		if (Realms.byWorld(player.getWorld().getRegistryKey()) == null) {
			data.returnPoint = GlobalPos.create(player.getWorld().getRegistryKey(), player.getBlockPos());
			data.returnYaw = player.getYaw();
		}
		if (toLastShrine && toShrine(player, realm)) {
			return true;
		}
		toLanding(player, realm.world());
		return true;
	}

	/** True if the player has a shrine to return to in that realm (for the obelisk's "landing or shrine" choice). */
	public static boolean hasRestedShrine(ServerPlayerEntity player, Identifier realmId) {
		return RealmPlayerData.get(player).shrines.containsKey(realmId);
	}

	/** Back to where the player entered the realm from; the overworld spawn if that is unknown. */
	public static void leaveRealm(ServerPlayerEntity player) {
		RealmPlayerData data = RealmPlayerData.get(player);
		ServerWorld world = data.returnPoint == null ? null : player.getServer().getWorld(data.returnPoint.getDimension());
		if (world != null) {
			BlockPos pos = data.returnPoint.getPos();
			teleport(player, world, Vec3d.ofBottomCenter(pos), data.returnYaw);
		} else {
			ServerWorld overworld = player.getServer().getOverworld();
			teleport(player, overworld, Vec3d.ofBottomCenter(overworld.getSpawnPos()), 0);
		}
		data.returnPoint = null;
	}

	/** To the landing of the given realm dimension (open zones or final zone). */
	public static void toLanding(ServerPlayerEntity player, RegistryKey<World> key) {
		ServerWorld world = player.getServer().getWorld(key);
		if (world != null) {
			teleport(player, world, landingSpot(world), 180);
		}
	}

	/** To the last shrine rested at in the realm. Returns false (and does nothing) if there is none. */
	public static boolean toShrine(ServerPlayerEntity player, RealmDefinition realm) {
		GlobalPos shrine = RealmPlayerData.get(player).shrines.get(realm.id());
		ServerWorld world = shrine == null ? null : player.getServer().getWorld(shrine.getDimension());
		if (world == null) {
			return false;
		}
		teleport(player, world, standingSpot(world, shrine.getPos()), player.getYaw());
		return true;
	}

	/** Where a player stands when arriving at a landing: in front of its shrine, facing the return obelisk. */
	public static Vec3d landingSpot(ServerWorld world) {
		world.getChunk(0, 0);
		int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 0, 2);
		if (y <= world.getBottomY() + 1) {
			// No ground under the landing (broken worldgen): do not drop the player into the void.
			y = world.getSeaLevel() + 64;
			for (BlockPos pos : BlockPos.iterate(-1, y - 1, 1, 1, y - 1, 3)) {
				world.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState());
			}
		}
		return new Vec3d(0.5, y, 2.5);
	}

	/** A free spot next to a shrine: beside it if there is room, else on top of it. */
	public static Vec3d standingSpot(ServerWorld world, BlockPos shrine) {
		world.getChunk(shrine);
		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos pos = shrine.offset(direction);
			if (canStand(world, pos)) {
				return Vec3d.ofBottomCenter(pos);
			}
		}
		return Vec3d.ofBottomCenter(shrine.up());
	}

	private static boolean canStand(ServerWorld world, BlockPos pos) {
		BlockState below = world.getBlockState(pos.down());
		return below.isSideSolidFullSquare(world, pos.down(), Direction.UP)
				&& world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
				&& world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty();
	}

	@Nullable
	public static ServerPlayerEntity teleport(ServerPlayerEntity player, ServerWorld world, Vec3d pos, float yaw) {
		player.fallDistance = 0;
		return FabricDimensions.teleport(player, world, new TeleportTarget(pos, Vec3d.ZERO, yaw, 0));
	}

	private RealmTeleports() {}
}
