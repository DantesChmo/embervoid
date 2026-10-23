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
 * A realm is cut into angle {@link Sectors}: its open zones, then (since 0.5.0, DESIGN §4.1) the final zone's sector.
 */
public final class RealmLayout {
	public static final double TAU = Math.PI * 2;
	/** {@link #zoneAt} result inside the landing disc. */
	public static final int LANDING = -1;
	/** {@link #zoneAt} result past the realm radius. */
	public static final int ABYSS = -2;
	/** {@link #zoneAt} result in the final zone's sector. */
	public static final int FINAL = -3;

	/**
	 * The angle sectors of a realm (DESIGN §4.1): {@code openCount} open zones of {@code openWidth} each from
	 * {@code startAngle} on, then the final zone's sector of {@code finalWidth} (0 for a realm without one, where the
	 * open zones go all the way round). Radians.
	 *
	 * <p>Borders between open zones are warped by a noise; the final zone's two borders are straight (its wall runs
	 * along them), so the warp fades out towards them over half an open zone.
	 */
	public record Sectors(double startAngle, int openCount, double openWidth, double finalWidth) {
		public Sectors {
			if (openCount < 1 || openWidth <= 0 || finalWidth < 0 || Math.abs(openCount * openWidth + finalWidth - TAU) > 1e-6) {
				throw new IllegalArgumentException("Sectors must cover the circle: " + openCount + " × " + openWidth
						+ " + " + finalWidth);
			}
		}

		/** {@code count} open zones of equal width, no final sector. */
		public static Sectors equal(int count, double startAngle) {
			return new Sectors(startAngle, count, TAU / count, 0);
		}

		/** In degrees: the open zones share what the final sector leaves. */
		public static Sectors degrees(double startAngle, int openCount, double finalWidth) {
			return new Sectors(Math.toRadians(startAngle), openCount, Math.toRadians((360 - finalWidth) / openCount),
					Math.toRadians(finalWidth));
		}

		public boolean hasFinal() {
			return finalWidth > 0;
		}

		/** Sectors in all: the open zones, then the final zone at index {@code openCount}. */
		public int count() {
			return hasFinal() ? openCount + 1 : openCount;
		}

		/** Angle covered by the open zones. */
		public double openSpan() {
			return openCount * openWidth;
		}

		/** Where open zone {@code zone} starts (its border with the previous sector). */
		public double openStart(int zone) {
			return normalize(startAngle + zone * openWidth);
		}

		public double openCenter(int zone) {
			return normalize(startAngle + (zone + 0.5) * openWidth);
		}

		/** Where the final sector starts (its border with the last open zone); it ends at {@link #startAngle}. */
		public double finalStart() {
			return normalize(startAngle + openSpan());
		}

		/** The final sector's middle line. */
		public double finalCenter() {
			return normalize(startAngle + openSpan() + finalWidth / 2);
		}

		/** Whether an angle lies in the final sector (straight borders: the warp does not move them). */
		public boolean inFinal(double angle) {
			return hasFinal() && normalize(angle - startAngle) >= openSpan();
		}

		/**
		 * The angle from {@link #startAngle}, moved by the border warp: in [0, 2π), with the final sector at
		 * [{@link #openSpan}, 2π) unmoved. With a final sector the warp fades out linearly over half an open zone
		 * towards it, so it stays monotonic as long as {@code warpRadians < openWidth / 2}.
		 *
		 * @param warp        border warp at this column, roughly in [-1, 1] (a seeded noise; clamped)
		 * @param warpRadians how far a warp of ±1 moves the borders between open zones
		 */
		public double warpedLocal(double angle, double warp, double warpRadians) {
			double local = normalize(angle - startAngle);
			double offset = Math.max(-1, Math.min(1, warp)) * warpRadians;
			if (!hasFinal()) {
				return normalize(local + offset);
			}
			if (local >= openSpan()) {
				return local;
			}
			double taper = Math.min(1, Math.min(local, openSpan() - local) / (openWidth / 2));
			return local + offset * taper;
		}

		/** Zone at a {@link #warpedLocal} angle: an open zone index or {@link #FINAL}. */
		public int zoneOf(double warpedLocal) {
			if (hasFinal() && warpedLocal >= openSpan()) {
				return FINAL;
			}
			return Math.max(0, Math.min((int) Math.floor(warpedLocal / openWidth), openCount - 1));
		}

		/** Index of a zone in {@link #zoneWeights} order: open zones 0…n-1, the final zone n. */
		public int index(int zone) {
			return zone == FINAL ? openCount : zone;
		}
	}

	/** Angle of the column at x, z around the origin, in [0, 2π). */
	public static double angle(double x, double z) {
		return normalize(Math.atan2(z, x));
	}

	public static double normalize(double angle) {
		double a = angle % TAU;
		return a < 0 ? a + TAU : a;
	}

	/**
	 * Zone of the column at x, z.
	 *
	 * @param warp        border warp at this column, roughly in [-1, 1] (a seeded noise)
	 * @param warpRadians how far a warp of ±1 moves the borders between open zones
	 * @return {@link #LANDING}, {@link #ABYSS}, {@link #FINAL} or an open zone index in [0, openCount)
	 */
	public static int zoneAt(double x, double z, Sectors sectors, double landingRadius, double radius, double warp,
			double warpRadians) {
		double distance = Math.sqrt(x * x + z * z);
		if (distance > radius) {
			return ABYSS;
		}
		if (distance < landingRadius) {
			return LANDING;
		}
		return sectors.zoneOf(sectors.warpedLocal(angle(x, z), warp, warpRadians));
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
	 * Weight of each zone's terrain at a {@link Sectors#warpedLocal} angle, in {@link Sectors#index} order: 1 well
	 * inside a sector, sliding to the neighbour across a border over {@code blendBlocks} on each side (in blocks of arc
	 * at {@code distance}; at most a third of the narrowest sector, so only two zones ever mix). Weights sum to 1.
	 */
	public static double[] zoneWeights(double warpedLocal, Sectors sectors, double distance, double blendBlocks) {
		int count = sectors.count();
		double[] weights = new double[count];
		if (count == 1) {
			weights[0] = 1;
			return weights;
		}
		double narrowest = sectors.hasFinal() ? Math.min(sectors.openWidth(), sectors.finalWidth()) : sectors.openWidth();
		double blend = Math.min(narrowest / 3, blendBlocks / Math.max(1, distance));
		int sector = sectors.index(sectors.zoneOf(warpedLocal));
		double start = sector * sectors.openWidth();
		double width = sector < sectors.openCount() ? sectors.openWidth() : sectors.finalWidth();
		double into = warpedLocal - start;
		if (into < blend) {
			// Near the border with the previous sector: 0.5 / 0.5 on the border itself.
			double t = smoothstep((into + blend) / (2 * blend));
			weights[sector] = t;
			weights[(sector + count - 1) % count] = 1 - t;
		} else if (width - into < blend) {
			double t = smoothstep((width - into + blend) / (2 * blend));
			weights[sector] = t;
			weights[(sector + 1) % count] = 1 - t;
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
	 * @param sectorStart angle where the sector starts
	 * @param angleSpread fraction of the sector half-width the feature may stray from the sector center, in [0, 1].
	 *                    Keep {@code spread · w/2 + footprint} below {@code w/2 − warpRadians} so the feature never
	 *                    lands across a warped border
	 * @return {x, z}; the origin when {@code maxDistance} is 0
	 */
	public static int[] featurePosition(long seed, int salt, double sectorStart, double sectorWidth, double angleSpread,
			int minDistance, int maxDistance) {
		Random random = new Random(seed ^ (salt * 0x9E3779B97F4A7C15L));
		double halfSpan = sectorWidth / 2 * angleSpread;
		double angle = normalize(sectorStart + sectorWidth / 2) + (random.nextDouble() * 2 - 1) * halfSpan;
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
