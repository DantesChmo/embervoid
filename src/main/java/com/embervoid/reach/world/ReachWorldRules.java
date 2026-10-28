package com.embervoid.reach.world;

import com.embervoid.realm.RealmLayout;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;

/**
 * Placement numbers of the Ashen Reach's roads, outposts, edge wall and border walls over plain values, free of the
 * Feature classes (JUnit-tested).
 */
public final class ReachWorldRules {
	/** The edge wall stands on a ring this far inside the realm radius. */
	public static final int WALL_INSET = 95;

	/** Radius of the edge wall in a realm of {@code realmRadius}. */
	public static double wallRadius(int realmRadius) {
		return realmRadius - WALL_INSET;
	}

	/** The border angles of a realm's final zone: its sector's start, then its end. */
	public static double[] borders(RealmLayout.Sectors sectors) {
		return new double[]{sectors.finalStart(), sectors.startAngle()};
	}

	/** Rotation that turns a template's +Z side along {@code (dirX, dirZ)}, in 90° steps. */
	public static BlockRotation rotationAlong(double dirX, double dirZ) {
		if (Math.abs(dirX) > Math.abs(dirZ)) {
			return dirX > 0 ? BlockRotation.COUNTERCLOCKWISE_90 : BlockRotation.CLOCKWISE_90;
		}
		return dirZ > 0 ? BlockRotation.NONE : BlockRotation.CLOCKWISE_180;
	}

	/** Template of an outpost of {@code kind} on the road to the arena of zone boss {@code bossId}. */
	public static Identifier outpostId(String kind, Identifier bossId) {
		return new Identifier("embervoid", "ashen_reach/outpost/" + kind + "_" + ZoneLooks.zoneName(bossId));
	}

	/**
	 * Whether x, z lies on the footprint of a template {@code sizeX} by {@code sizeZ} centred on ({@code centreX},
	 * {@code centreZ}) and turned along ({@code dirX}, {@code dirZ}).
	 */
	public static boolean onFootprint(int x, int z, int centreX, int centreZ, int sizeX, int sizeZ, double dirX, double dirZ) {
		BlockRotation rotation = rotationAlong(dirX, dirZ);
		boolean turned = rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90;
		int halfX = (turned ? sizeZ : sizeX) / 2;
		int halfZ = (turned ? sizeX : sizeZ) / 2;
		return Math.abs(x - centreX) <= halfX && Math.abs(z - centreZ) <= halfZ;
	}

	private ReachWorldRules() {}
}
