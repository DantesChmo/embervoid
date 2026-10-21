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
 * Moves items and/or XP (per {@code remnant.mode}) into a remnant at a safe spot near the death, burns the previous
 * one, and tells the player where the new one is. The mixin skips it under {@code keepInventory}; with mode
 * {@code off} nothing changes either.
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

		if (AshRemnantEntity.burnCurrent(player)) {
			player.sendMessage(Text.translatable("embervoid.remnant.burned").formatted(Formatting.DARK_RED));
		}

		boolean takeItems = mode == EmbervoidConfig.RemnantMode.XP_AND_ITEMS && !player.getInventory().isEmpty();
		boolean takeExperience = AshRemnantEntity.totalExperience(player) > 0;
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
	 * A reachable spot for the remnant: near the death if there is one, else where the player last stood on safe
	 * ground in this dimension (deaths in the void or a lava lake), else the world spawn.
	 */
	private static Vec3d findRemnantPos(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		BlockPos death = player.getBlockPos();
		BlockPos clamped = new BlockPos(death.getX(), MathHelper.clamp(death.getY(), world.getBottomY() + 1, world.getTopY() - 2), death.getZ());
		BlockPos found = searchNear(world, clamped);
		if (found == null) {
			PlayerData data = PlayerDataManager.get(player);
			if (data.lastSafePos != null && world.getRegistryKey().equals(data.lastSafeWorld) && isSafe(world, data.lastSafePos)) {
				found = data.lastSafePos;
			}
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
