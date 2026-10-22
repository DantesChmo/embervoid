package com.embervoid.realm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure layout of a realm's roads (PLAN 9.6): one road from the landing plaza's edge to each arena site's edge, with a
 * seeded gentle wobble, a milestone every {@value #MILESTONE_SPACING} blocks and two outposts. No Minecraft types, so
 * it is unit-tested directly; the road feature paints it.
 */
public final class RoadLayout {
	/** Blocks between path points. */
	public static final double STEP = 2;
	public static final int MILESTONE_SPACING = 100;
	/** Milestones stand this far from the road's centre line (the road is {@code 2·halfWidth + 1} wide). */
	public static final int MILESTONE_OFFSET = 4;
	/** Outposts at these fractions of a road's length: a toll gate, then a watchtower. */
	public static final double[] OUTPOST_AT = {0.3, 0.65};
	public static final String[] OUTPOST_KINDS = {"toll_gate", "watchtower"};

	/** A road: its points from the plaza to the arena, and what stands along it. */
	public record Road(int arena, List<double[]> points, List<Milestone> milestones, List<Outpost> outposts) {
		public double length() {
			return (points.size() - 1) * STEP;
		}
	}

	/** A milestone at x, z, facing the road (direction towards the centre line, a unit vector). */
	public record Milestone(int x, int z, double faceX, double faceZ) {}

	/** An outpost centred on the road at x, z; {@code dirX, dirZ} is the road's direction there (towards the arena). */
	public record Outpost(String kind, int x, int z, double dirX, double dirZ) {}

	/**
	 * The roads of a realm.
	 *
	 * @param arenas       arena centres {x, z}
	 * @param siteRadii    each arena's site radius; its road stops 1 block short of it
	 * @param plazaRadius  roads start at this distance from the landing centre
	 */
	public static List<Road> roads(long seed, List<int[]> arenas, List<Integer> siteRadii, double plazaRadius, double wobble) {
		List<Road> roads = new ArrayList<>();
		for (int i = 0; i < arenas.size(); i++) {
			roads.add(road(seed, i, arenas.get(i), siteRadii.get(i), plazaRadius, wobble));
		}
		return roads;
	}

	static Road road(long seed, int index, int[] arena, int siteRadius, double plazaRadius, double wobble) {
		double distance = Math.sqrt((double) arena[0] * arena[0] + (double) arena[1] * arena[1]);
		double dirX = arena[0] / distance;
		double dirZ = arena[1] / distance;
		double startX = dirX * plazaRadius;
		double startZ = dirZ * plazaRadius;
		double endX = arena[0] - dirX * (siteRadius + 1);
		double endZ = arena[1] - dirZ * (siteRadius + 1);
		double straight = Math.max(STEP, Math.sqrt((endX - startX) * (endX - startX) + (endZ - startZ) * (endZ - startZ)));
		Random random = new Random(seed * 31 + index * 0x5DEECE66DL);
		double waves = 1 + random.nextDouble() * 1.5;
		double phase = random.nextDouble() * Math.PI * 2;
		double amplitude = wobble * (0.6 + random.nextDouble() * 0.4);

		// Sample the curve finely, then resample it every STEP blocks of arc length.
		int fine = (int) Math.ceil(straight / 0.5);
		List<double[]> curve = new ArrayList<>(fine + 1);
		for (int k = 0; k <= fine; k++) {
			double t = (double) k / fine;
			double offset = amplitude * Math.sin(Math.PI * t) * Math.sin(2 * Math.PI * waves * t + phase);
			curve.add(new double[]{startX + (endX - startX) * t - dirZ * offset, startZ + (endZ - startZ) * t + dirX * offset});
		}
		List<double[]> points = new ArrayList<>();
		points.add(curve.get(0));
		double carried = 0;
		for (int k = 1; k < curve.size(); k++) {
			double[] a = curve.get(k - 1);
			double[] b = curve.get(k);
			double segment = Math.hypot(b[0] - a[0], b[1] - a[1]);
			double along = STEP - carried;
			while (along <= segment) {
				double f = along / segment;
				points.add(new double[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f});
				along += STEP;
			}
			carried = segment - (along - STEP);
		}

		List<Milestone> milestones = new ArrayList<>();
		for (int s = MILESTONE_SPACING; s < (points.size() - 1) * STEP - 30; s += MILESTONE_SPACING) {
			int k = (int) Math.round(s / STEP);
			double[] dir = direction(points, k);
			// On the right-hand side walking out, facing back to the road.
			double[] p = points.get(k);
			milestones.add(new Milestone((int) Math.floor(p[0] - dir[1] * MILESTONE_OFFSET),
					(int) Math.floor(p[1] + dir[0] * MILESTONE_OFFSET), dir[1], -dir[0]));
		}

		List<Outpost> outposts = new ArrayList<>();
		for (int o = 0; o < OUTPOST_AT.length; o++) {
			int k = (int) Math.round(OUTPOST_AT[o] * (points.size() - 1));
			double[] p = points.get(k);
			double[] dir = direction(points, k);
			outposts.add(new Outpost(OUTPOST_KINDS[o], (int) Math.floor(p[0]), (int) Math.floor(p[1]), dir[0], dir[1]));
		}
		return new Road(index, points, milestones, outposts);
	}

	/** Unit direction of the road at point k (towards the arena). */
	public static double[] direction(List<double[]> points, int k) {
		double[] a = points.get(Math.max(0, k - 2));
		double[] b = points.get(Math.min(points.size() - 1, k + 2));
		double length = Math.max(1e-9, Math.hypot(b[0] - a[0], b[1] - a[1]));
		return new double[]{(b[0] - a[0]) / length, (b[1] - a[1]) / length};
	}

	/** Squared distance from (x, z) to the road's polyline. */
	public static double distanceSquared(Road road, double x, double z) {
		double best = Double.MAX_VALUE;
		List<double[]> points = road.points();
		for (int k = 1; k < points.size(); k++) {
			best = Math.min(best, segmentDistanceSquared(points.get(k - 1), points.get(k), x, z));
		}
		return best;
	}

	/** Squared distance from (x, z) to the segment a–b. */
	public static double segmentDistanceSquared(double[] a, double[] b, double x, double z) {
		double dx = b[0] - a[0];
		double dz = b[1] - a[1];
		double lengthSquared = dx * dx + dz * dz;
		double t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, ((x - a[0]) * dx + (z - a[1]) * dz) / lengthSquared));
		double px = a[0] + dx * t - x;
		double pz = a[1] + dz * t - z;
		return px * px + pz * pz;
	}

	private RoadLayout() {}
}
