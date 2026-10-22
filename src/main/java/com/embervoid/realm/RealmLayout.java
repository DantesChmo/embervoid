package com.embervoid.realm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Pure layout math of a realm: which zone a column belongs to, where the edge falls off into the abyss, and where
 * a zone's boss arena goes. No Minecraft types, so it is unit-tested directly.
 *
 * <p>Angles are radians from +X towards +Z (east towards south), as {@code atan2(z, x)}, normalized to [0, 2π).
 * Zone {@code i} of {@code n} covers the sector {@code [start + i·w, start + (i+1)·w)}, {@code w = 2π / n}.
 */
public final class RealmLayout {
	public static final double TAU = Math.PI * 2;
	/** {@link #zoneAt} result inside the landing disc. */
	public static final int LANDING = -1;
	/** {@link #zoneAt} result past the realm radius. */
	public static final int ABYSS = -2;

	/** Angle of the column at x, z around the origin, in [0, 2π). */
	public static double angle(double x, double z) {
		return normalize(Math.atan2(z, x));
	}

	public static double normalize(double angle) {
		double a = angle % TAU;
		return a < 0 ? a + TAU : a;
	}

	public static double sectorWidth(int sectorCount) {
		return TAU / sectorCount;
	}

	/** Center angle of sector {@code sector}. */
	public static double sectorCenter(int sectorCount, int sector, double startAngle) {
		return normalize(startAngle + (sector + 0.5) * sectorWidth(sectorCount));
	}

	/** Sector that contains {@code angle}. */
	public static int sectorOf(double angle, int sectorCount, double startAngle) {
		int sector = (int) Math.floor(normalize(angle - startAngle) / sectorWidth(sectorCount));
		return Math.min(sector, sectorCount - 1);
	}

	/**
	 * Zone of the column at x, z.
	 *
	 * @param warp        border warp at this column, roughly in [-1, 1] (a seeded noise)
	 * @param warpRadians how far a warp of ±1 moves the borders
	 * @return {@link #LANDING}, {@link #ABYSS} or a zone index in [0, zoneCount)
	 */
	public static int zoneAt(double x, double z, int zoneCount, double startAngle, double landingRadius, double radius,
			double warp, double warpRadians) {
		double distance = Math.sqrt(x * x + z * z);
		if (distance > radius) {
			return ABYSS;
		}
		if (distance < landingRadius) {
			return LANDING;
		}
		double clamped = Math.max(-1, Math.min(1, warp));
		return sectorOf(angle(x, z) + clamped * warpRadians, zoneCount, startAngle);
	}

	/**
	 * Edge falloff: +1 up to {@code inner}, −1 from {@code outer} on, smooth in between. The terrain density is
	 * capped by this (times a strength), so land ends in cliffs over the void.
	 */
	public static double falloff(double distance, double inner, double outer) {
		if (distance <= inner) {
			return 1;
		}
		if (distance >= outer) {
			return -1;
		}
		double t = (distance - inner) / (outer - inner);
		double smooth = t * t * (3 - 2 * t);
		return 1 - 2 * smooth;
	}

	/** Weight of the landing shape at {@code distance}: 1 inside the disc, fading to 0 over {@code blend} blocks. */
	public static double landingWeight(double distance, double landingRadius, double blend) {
		if (distance <= landingRadius) {
			return 1;
		}
		if (blend <= 0 || distance >= landingRadius + blend) {
			return 0;
		}
		return 1 - smoothstep((distance - landingRadius) / blend);
	}

	/**
	 * Weight of each zone's terrain at a (warped) angle: 1 well inside a sector, sliding to the neighbour across a
	 * border over {@code blendBlocks} on each side (in blocks of arc at {@code distance}; at most a third of the
	 * sector, so only two zones ever mix). Weights sum to 1.
	 */
	public static double[] zoneWeights(double angle, int zoneCount, double startAngle, double distance, double blendBlocks) {
		double[] weights = new double[zoneCount];
		if (zoneCount == 1) {
			weights[0] = 1;
			return weights;
		}
		double width = sectorWidth(zoneCount);
		double blend = Math.min(width / 3, blendBlocks / Math.max(1, distance));
		double local = normalize(angle - startAngle);
		int sector = Math.min((int) Math.floor(local / width), zoneCount - 1);
		double into = local - sector * width;
		if (into < blend) {
			// Near the border with the previous sector: 0.5 / 0.5 on the border itself.
			double t = smoothstep((into + blend) / (2 * blend));
			weights[sector] = t;
			weights[(sector + zoneCount - 1) % zoneCount] = 1 - t;
		} else if (width - into < blend) {
			double t = smoothstep((width - into + blend) / (2 * blend));
			weights[sector] = t;
			weights[(sector + 1) % zoneCount] = 1 - t;
		} else {
			weights[sector] = 1;
		}
		return weights;
	}

	private static double smoothstep(double t) {
		double c = Math.max(0, Math.min(1, t));
		return c * c * (3 - 2 * c);
	}

	/**
	 * Block position (x, z) of a feature placed in one sector, derived from the seed only.
	 *
	 * @param angleSpread fraction of the sector half-width the feature may stray from the sector center, in [0, 1].
	 *                    Keep {@code spread · w/2 + footprint} below {@code w/2 − warpRadians} so the feature never
	 *                    lands across a warped border
	 * @return {x, z}; the origin when {@code maxDistance} is 0
	 */
	public static int[] featurePosition(long seed, int salt, int sectorCount, int sector, double startAngle,
			double angleSpread, int minDistance, int maxDistance) {
		Random random = new Random(seed ^ (salt * 0x9E3779B97F4A7C15L));
		double halfSpan = sectorWidth(sectorCount) / 2 * angleSpread;
		double angle = sectorCenter(sectorCount, sector, startAngle) + (random.nextDouble() * 2 - 1) * halfSpan;
		double distance = minDistance + random.nextDouble() * Math.max(0, maxDistance - minDistance);
		return new int[]{(int) Math.floor(Math.cos(angle) * distance), (int) Math.floor(Math.sin(angle) * distance)};
	}

	/**
	 * Spots to try for a player standing next to a block at x, y, z, nearest first: rings of radius 1 to
	 * {@code maxRadius} (square rings, sides before corners), each at the block's level, then one up, then one down.
	 * The block itself is never included.
	 */
	public static List<int[]> standingCandidates(int x, int y, int z, int maxRadius) {
		List<int[]> result = new ArrayList<>();
		for (int r = 1; r <= maxRadius; r++) {
			List<int[]> ring = new ArrayList<>();
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
						ring.add(new int[]{dx, dz});
					}
				}
			}
			ring.sort(Comparator.comparingInt(d -> d[0] * d[0] + d[1] * d[1]));
			for (int dy : new int[]{0, 1, -1}) {
				for (int[] d : ring) {
					result.add(new int[]{x + d[0], y + dy, z + d[1]});
				}
			}
		}
		return result;
	}

	private RealmLayout() {}
}
