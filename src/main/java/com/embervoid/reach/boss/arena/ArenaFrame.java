package com.embervoid.reach.boss.arena;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Where an arena template stands in the world: its centre (the floor block under the fog wall's centre) and the
 * rotation that turns its north (−Z) side towards the landing at 0, 0 (the arena placement turns it that way). Turns
 * the layout offsets of {@link ReachArenas} into world positions.
 */
public record ArenaFrame(BlockPos center, BlockRotation rotation) {
	public static ArenaFrame of(BlockPos center) {
		return new ArenaFrame(center.toImmutable(), rotationTowardsLanding(center));
	}

	/** Same rule as the arena placement: the template's north faces the landing, in 90° steps. */
	public static BlockRotation rotationTowardsLanding(BlockPos center) {
		int dx = -center.getX();
		int dz = -center.getZ();
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? BlockRotation.CLOCKWISE_90 : BlockRotation.COUNTERCLOCKWISE_90;
		}
		return dz < 0 ? BlockRotation.NONE : BlockRotation.CLOCKWISE_180;
	}

	/** World position of a layout offset (x east, y up from the floor, z south, before rotation). */
	public BlockPos at(int dx, int dy, int dz) {
		return center.add(new BlockPos(dx, dy, dz).rotate(rotation));
	}

	/** Centre of the top face of the block at a layout offset. */
	public Vec3d top(int dx, int dy, int dz) {
		BlockPos pos = at(dx, dy, dz);
		return new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
	}
}
