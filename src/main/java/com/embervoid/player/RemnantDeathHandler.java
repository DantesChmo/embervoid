package com.embervoid.player;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.entity.AshRemnantEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Death flow for Ash Remnants: called from {@code PlayerEntityMixin} when a player's inventory is about to drop.
 * Retires the previous remnant (per {@code remnant.secondDeath}), moves items and/or XP (per {@code remnant.mode})
 * into a new one at the last spot the player stood on solid ground, and tells the player where it is. The mixin
 * skips it under {@code keepInventory}; with mode {@code off} nothing changes either.
 */
public final class RemnantDeathHandler {
	private static final int SEARCH_RADIUS = 8;
	private static final int SAFE_POS_INTERVAL = 10;

	public static void onDeath(ServerPlayerEntity player) {
		EmbervoidConfig.RemnantMode mode = EmbervoidConfig.get().remnant().mode();
		ServerWorld world = player.getServerWorld();
		if (mode == EmbervoidConfig.RemnantMode.OFF) {
			return;
		}

		switch (AshRemnantEntity.retireCurrent(player)) {
			case BURNED -> player.sendMessage(Text.translatable("embervoid.remnant.burned").formatted(Formatting.DARK_RED));
			case XP_BURNED -> player.sendMessage(Text.translatable("embervoid.remnant.xp_burned").formatted(Formatting.DARK_RED));
			case NONE -> {
			}
		}

		boolean takeItems = mode == EmbervoidConfig.RemnantMode.XP_AND_ITEMS && !player.getInventory().isEmpty();
		boolean takeExperience = XpUtil.getPoints(player) > 0;
		if (!takeItems && !takeExperience) {
			return;
		}
		Vec3d pos = findRemnantPos(player);
		AshRemnantEntity remnant = AshRemnantEntity.create(player, pos, takeItems, takeExperience);
		BlockPos block = remnant.getBlockPos();
		Text where = Text.translatable("embervoid.remnant.location", block.getX(), block.getY(), block.getZ());
		if (world != player.getServer().getOverworld()) {
			where = Text.translatable("embervoid.remnant.location_dimension", where, world.getRegistryKey().getValue().getPath());
		}
		player.sendMessage(Text.translatable("embervoid.remnant.left", where).formatted(Formatting.GOLD));
	}

	/**
	 * A reachable spot for the remnant: where the player last stood on solid ground in this dimension, tracked while
	 * alive, so deaths in lava, the void, mid-fall or in a wall still leave it somewhere walkable. If that spot is
	 * gone (built over, flooded) the closest safe spot around it; with no tracked spot in this dimension (died right
	 * after a teleport or arriving), the closest safe spot around the death, or the ground below it when the death
	 * was high in the air; then the spot from before the last teleport; else the world spawn.
	 */
	private static Vec3d findRemnantPos(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		PlayerData data = PlayerDataManager.get(player);
		BlockPos found = null;
		if (data.lastSafePos != null && world.getRegistryKey().equals(data.lastSafeWorld)) {
			found = isSafe(world, data.lastSafePos) ? data.lastSafePos : searchNear(world, data.lastSafePos);
		}
		if (found == null) {
			BlockPos death = player.getBlockPos();
			BlockPos clamped = new BlockPos(death.getX(),
					MathHelper.clamp(death.getY(), world.getBottomY() + 1, world.getTopY() - 2), death.getZ());
			found = searchNear(world, clamped);
			if (found == null) {
				found = searchBelow(world, clamped);
			}
		}
		if (found == null && data.preTeleportSafePos != null && world.getRegistryKey().equals(data.preTeleportSafeWorld)) {
			found = isSafe(world, data.preTeleportSafePos) ? data.preTeleportSafePos : searchNear(world, data.preTeleportSafePos);
		}
		if (found == null) {
			found = world.getSpawnPos();
		}
		return Vec3d.ofBottomCenter(found);
	}

	/** Closest safe spot within {@link #SEARCH_RADIUS}, checking rings outward and nearer heights first. */
	@Nullable
	private static BlockPos searchNear(ServerWorld world, BlockPos center) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
			for (int dy = 0; dy <= SEARCH_RADIUS * 2; dy++) {
				int y = center.getY() + ((dy & 1) == 0 ? dy / 2 : -(dy + 1) / 2);
				for (int dx = -radius; dx <= radius; dx++) {
					for (int dz = -radius; dz <= radius; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
							continue;
						}
						pos.set(center.getX() + dx, y, center.getZ() + dz);
						if (isSafe(world, pos)) {
							return pos.toImmutable();
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * The first safe ground straight below {@code center}, in the closest of the columns {@link #searchNear} checks
	 * (for deaths high above the ground). A column ends at its first non-passable block: if that is not a safe floor
	 * (lava, fire, a slab) the column gives nothing, and an empty column down to the void gives nothing either.
	 */
	@Nullable
	private static BlockPos searchBelow(ServerWorld world, BlockPos center) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}
					for (int y = center.getY(); y > world.getBottomY(); y--) {
						pos.set(center.getX() + dx, y, center.getZ() + dz);
						if (!isOpen(world, pos)) {
							break;
						}
						if (isSafe(world, pos)) {
							return pos.toImmutable();
						}
					}
				}
			}
		}
		return null;
	}

	/** Room for the remnant (two passable, lava- and fire-free blocks) on top of a solid floor. */
	private static boolean isSafe(ServerWorld world, BlockPos pos) {
		if (!world.isInBuildLimit(pos) || !world.isInBuildLimit(pos.up()) || !world.getWorldBorder().contains(pos)) {
			return false;
		}
		BlockPos below = pos.down();
		BlockState floor = world.getBlockState(below);
		return isOpen(world, pos) && isOpen(world, pos.up())
				&& floor.isSideSolidFullSquare(world, below, Direction.UP)
				&& !floor.getFluidState().isIn(FluidTags.LAVA) && !floor.isIn(BlockTags.FIRE);
	}

	private static boolean isOpen(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.getCollisionShape(world, pos).isEmpty()
				&& !state.getFluidState().isIn(FluidTags.LAVA) && !state.isIn(BlockTags.FIRE);
	}

	/**
	 * Forgets the safe spot when the player is teleported: it is sampled only every {@link #SAFE_POS_INTERVAL}
	 * ticks, so a death right after a teleport would otherwise put the remnant back where they jumped from. Until
	 * the next sample, a death uses the search around the death point, and the old spot only if that finds nothing
	 * (pearled into a lava lake).
	 */
	public static void onTeleport(ServerPlayerEntity player) {
		PlayerData data = PlayerDataManager.get(player);
		if (data.lastSafePos != null) {
			data.preTeleportSafePos = data.lastSafePos;
			data.preTeleportSafeWorld = data.lastSafeWorld;
		}
		data.lastSafePos = null;
		data.lastSafeWorld = null;
	}

	/** Remembers where each player last stood on safe ground, as a fallback spot for void and lava deaths. */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % SAFE_POS_INTERVAL != 0) {
				return;
			}
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (player.isOnGround() && !player.isInLava() && isSafe(player.getServerWorld(), player.getBlockPos())) {
					PlayerData data = PlayerDataManager.get(player);
					data.lastSafePos = player.getBlockPos();
					data.lastSafeWorld = player.getServerWorld().getRegistryKey();
				}
			}
		});
	}

	private RemnantDeathHandler() {}
}
