package com.embervoid.realm.worldgen;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

/**
 * Where a realm feature's template turns and where its Ember Shrine stands, over plain positions (JUnit-tested);
 * {@link RealmFeaturePiece} places by these.
 */
public final class FeatureFrame {
	/** An arena's shrine stands this many blocks past its fog wall. */
	public static final int SHRINE_DISTANCE = 4;

	/** Rotation that turns a template's north (−Z) side at {@code center} towards the landing, in 90° steps. */
	public static BlockRotation rotationTowardsLanding(BlockPos center) {
		int dx = -center.getX();
		int dz = -center.getZ();
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? BlockRotation.CLOCKWISE_90 : BlockRotation.COUNTERCLOCKWISE_90;
		}
		return dz < 0 ? BlockRotation.NONE : BlockRotation.CLOCKWISE_180;
	}

	/**
	 * The shrine of a feature at {@code center}: one up from the landing's centre, or on an arena's north axis
	 * {@value #SHRINE_DISTANCE} blocks past its fog wall of {@code radius}.
	 */
	public static BlockPos shrinePos(BlockPos center, int radius, boolean landing) {
		if (landing) {
			return center.up();
		}
		return center.add(new BlockPos(0, 1, -(radius + SHRINE_DISTANCE)).rotate(rotationTowardsLanding(center)));
	}

	private FeatureFrame() {}
}
