package com.embervoid.boss;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Where a {@link FloodHazard} puts water and how fast, over a view of the arena's blocks. */
public final class FloodPlan {
	/** What fills a block above the floor. */
	public enum Space { AIR, WATER, BLOCKED }

	/** The arena's blocks as the flood sees them. */
	public interface Ground {
		/** Whether the block at {@code pos} has a full solid top water can stand on. */
		boolean isSolidFloor(BlockPos pos);

		Space space(BlockPos pos);
	}

	/**
	 * Every air block from the floor up to {@code depth} blocks above it, over each solid floor block within
	 * {@code radius} of {@code center} (a floor block, the arena's floor level), stopping at the first block that
	 * stands in the way: raised ground stays dry; water already there is passed over. Shuffled by {@code seed},
	 * then ordered top layer first, so taking from the end fills the lowest layer first and each layer evenly.
	 */
	public static List<BlockPos> columns(BlockPos center, int radius, int depth, Ground ground, long seed) {
		List<BlockPos> result = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) {
					continue;
				}
				BlockPos floor = center.add(dx, 0, dz);
				if (!ground.isSolidFloor(floor)) {
					continue;
				}
				for (int y = 1; y <= depth; y++) {
					BlockPos at = floor.up(y);
					Space space = ground.space(at);
					if (space == Space.WATER) {
						continue;
					}
					if (space == Space.BLOCKED) {
						break;
					}
					result.add(at);
				}
			}
		}
		Collections.shuffle(result, new Random(seed));
		result.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
		return result;
	}

	/** Blocks to fill this tick so that {@code pending} are all filled by the end of {@code ticksLeft} ticks (all at once when no time is left). */
	public static int perTick(int pending, int ticksLeft) {
		return (int) Math.ceil((double) pending / Math.max(1, ticksLeft));
	}

	private FloodPlan() {}
}
