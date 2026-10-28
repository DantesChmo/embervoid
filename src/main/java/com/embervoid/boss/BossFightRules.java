package com.embervoid.boss;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** The world-free rules of a boss fight: phases by health, the arena cylinder, the ring adds spawn on. */
public final class BossFightRules {
	/** How far below the arena floor a position still counts as inside. */
	public static final int BAND_BELOW = 3;
	/** How far above the arena floor a position still counts as inside. */
	public static final int BAND_ABOVE = 20;

	/**
	 * The phase a fight in {@code phase} is in once the boss is down to {@code healthFraction} of its health. The
	 * first {@code scriptedPhases} phases never end by health ({@link ArenaBoss#scriptedPhases}); from there each
	 * threshold the health has reached starts the next phase, several in one go if one blow crossed them. A phase
	 * never goes back.
	 */
	public static int phaseFor(int phase, int scriptedPhases, float[] thresholds, float healthFraction) {
		int next = phase;
		while (next >= scriptedPhases && next - scriptedPhases < thresholds.length && healthFraction <= thresholds[next - scriptedPhases]) {
			next++;
		}
		return next;
	}

	/**
	 * Whether {@code pos} is within {@code radius + margin} blocks of the arena's centre column (horizontally) and
	 * inside its height band. {@code center} is the arena floor block.
	 */
	public static boolean isInside(BlockPos center, double radius, Vec3d pos, double margin) {
		double dx = pos.x - (center.getX() + 0.5);
		double dz = pos.z - (center.getZ() + 0.5);
		double reach = radius + margin;
		return dx * dx + dz * dz <= reach * reach
				&& pos.y > center.getY() - BAND_BELOW && pos.y < center.getY() + BAND_ABOVE;
	}

	/** Spot {@code index} of {@code count} spread evenly on a horizontal circle of {@code radius} around {@code centre}. */
	public static Vec3d ringSpot(Vec3d centre, int index, int count, double radius) {
		double angle = Math.PI * 2 * index / count;
		return centre.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
	}

	private BossFightRules() {}
}
