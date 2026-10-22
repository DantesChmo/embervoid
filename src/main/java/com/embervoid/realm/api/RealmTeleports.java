package com.embervoid.realm.api;

import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmLayout;
import com.embervoid.realm.RealmPlayerData;
import com.embervoid.realm.Realms;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
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
	 * @param toLastShrine arrive at the last Ember Shrine rested at in the realm's open zones instead of the landing;
	 *                     falls back to the landing when there is none
	 * @return false if the realm is unknown or still locked for this player (they get a message)
	 */
	public static boolean enterRealm(ServerPlayerEntity player, Identifier realmId, boolean toLastShrine) {
		RealmDefinition realm = Realms.byId(realmId);
		if (!ProgressManager.isRealmUnlocked(player, realmId)) {
			player.sendMessage(Text.translatable("embervoid.realm.locked"), true);
			return false;
		}
		if (realm == null) {
			// Unlocked by the previous realm's boss, but not built yet.
			player.sendMessage(Text.translatable("embervoid.realm.forming"), true);
			return false;
		}
		RealmPlayerData data = RealmPlayerData.get(player);
		if (Realms.byWorld(player.getWorld().getRegistryKey()) == null) {
			data.returnPoint = GlobalPos.create(player.getWorld().getRegistryKey(), player.getBlockPos());
			data.returnYaw = player.getYaw();
		}
		if (toLastShrine && toShrine(player, realm.world())) {
			return true;
		}
		toLanding(player, realm.world());
		return true;
	}

	/** True if the player has a shrine to return to in the realm's open zones (the obelisk's "landing or shrine"). */
	public static boolean hasRestedShrine(ServerPlayerEntity player, Identifier realmId) {
		RealmDefinition realm = Realms.byId(realmId);
		return realm != null && RealmPlayerData.get(player).shrines.containsKey(realm.world().getValue());
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

	/** To the last shrine rested at in that realm dimension. Returns false (and does nothing) if there is none. */
	public static boolean toShrine(ServerPlayerEntity player, RegistryKey<World> dimension) {
		GlobalPos shrine = RealmPlayerData.get(player).shrines.get(dimension.getValue());
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

	/**
	 * A free spot next to a shrine: solid floor and two blocks of air, never the shrine's own block. Rings of radius
	 * 1 to 3 around it are searched, at the shrine's level first, then one up and one down. With no such spot the
	 * player goes on top of the highest block beside the shrine.
	 */
	public static Vec3d standingSpot(ServerWorld world, BlockPos shrine) {
		world.getChunk(shrine);
		for (BlockPos pos : RealmLayout.standingCandidates(shrine.getX(), shrine.getY(), shrine.getZ(), 3)
				.stream().map(c -> new BlockPos(c[0], c[1], c[2])).toList()) {
			if (canStand(world, pos)) {
				return Vec3d.ofBottomCenter(pos);
			}
		}
		BlockPos beside = shrine.east();
		return Vec3d.ofBottomCenter(new BlockPos(beside.getX(), world.getTopY(Heightmap.Type.MOTION_BLOCKING, beside.getX(), beside.getZ()), beside.getZ()));
	}

	private static boolean canStand(ServerWorld world, BlockPos pos) {
		BlockPos floor = pos.down();
		return world.getBlockState(floor).isSideSolidFullSquare(world, floor, Direction.UP)
				&& world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
				&& world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()
				&& world.getFluidState(pos).isEmpty();
	}

	@Nullable
	public static ServerPlayerEntity teleport(ServerPlayerEntity player, ServerWorld world, Vec3d pos, float yaw) {
		player.fallDistance = 0;
		ServerPlayerEntity moved = FabricDimensions.teleport(player, world, new TeleportTarget(pos, Vec3d.ZERO, yaw, 0));
		// During a respawn (AFTER_RESPAWN) the network handler still holds the dead player, so the teleport moved that
		// one and the new player would stay at the overworld spawn's coordinates until the client confirms.
		if (moved != null && moved.networkHandler.player != moved) {
			moved.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0);
			world.getChunkManager().updatePosition(moved);
		}
		return moved;
	}

	private RealmTeleports() {}
}
