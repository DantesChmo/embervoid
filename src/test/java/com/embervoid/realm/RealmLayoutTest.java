package com.embervoid.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmLayoutTest {
	private static final double START = Math.toRadians(-90);
	private static final double WARP = Math.toRadians(20);

	@Test
	void angleIsNormalized() {
		assertEquals(0, RealmLayout.angle(10, 0), 1e-9);
		assertEquals(Math.PI / 2, RealmLayout.angle(0, 10), 1e-9);
		assertEquals(Math.PI * 1.5, RealmLayout.angle(0, -10), 1e-9);
	}

	@Test
	void sectorsCoverTheCircle() {
		// Start at -90° (north), three sectors of 120°.
		assertEquals(0, RealmLayout.sectorOf(Math.toRadians(-60), 3, START));
		assertEquals(1, RealmLayout.sectorOf(Math.toRadians(90), 3, START));
		assertEquals(2, RealmLayout.sectorOf(Math.toRadians(200), 3, START));
		assertEquals(0, RealmLayout.sectorOf(Math.toRadians(-90), 3, START));
		assertEquals(2, RealmLayout.sectorOf(Math.toRadians(-90.0001), 3, START));
	}

	@Test
	void landingAndAbyss() {
		assertEquals(RealmLayout.LANDING, RealmLayout.zoneAt(10, 10, 3, START, 48, 1200, 0, WARP));
		assertEquals(RealmLayout.ABYSS, RealmLayout.zoneAt(1300, 0, 3, START, 48, 1200, 0, WARP));
	}

	@Test
	void warpMovesBorders() {
		// 1° past the border between sector 0 and 1 (at 30°): warped back by -20° it belongs to sector 0.
		double a = Math.toRadians(31);
		double x = Math.cos(a) * 500;
		double z = Math.sin(a) * 500;
		assertEquals(1, RealmLayout.zoneAt(x, z, 3, START, 48, 1200, 0, WARP));
		assertEquals(0, RealmLayout.zoneAt(x, z, 3, START, 48, 1200, -1, WARP));
		// Out-of-range warp is clamped.
		assertEquals(0, RealmLayout.zoneAt(x, z, 3, START, 48, 1200, -5, WARP));
	}

	@Test
	void falloffIsSmoothAndBounded() {
		assertEquals(1, RealmLayout.falloff(100, 1100, 1200));
		assertEquals(-1, RealmLayout.falloff(1300, 1100, 1200));
		assertEquals(0, RealmLayout.falloff(1150, 1100, 1200), 1e-9);
		double previous = 1;
		for (int d = 1100; d <= 1200; d += 5) {
			double v = RealmLayout.falloff(d, 1100, 1200);
			assertTrue(v <= previous);
			previous = v;
		}
	}

	@Test
	void featurePositionIsDeterministic() {
		int[] a = RealmLayout.featurePosition(12345, 7, 3, 1, START, 0.5, 400, 900);
		int[] b = RealmLayout.featurePosition(12345, 7, 3, 1, START, 0.5, 400, 900);
		assertArrayEquals(a, b);
		int[] other = RealmLayout.featurePosition(54321, 7, 3, 1, START, 0.5, 400, 900);
		assertTrue(a[0] != other[0] || a[1] != other[1]);
	}

	@Test
	void featurePositionAtDistanceZeroIsOrigin() {
		assertArrayEquals(new int[]{0, 0}, RealmLayout.featurePosition(1, 1, 1, 0, 0, 1, 0, 0));
	}

	@Test
	void arenasStayInTheirZoneForAnyWarp() {
		// Arena footprint: radius 20 plus the shrine outside it plus snapping to a chunk corner, about 48 blocks.
		// Whatever the noise does to the borders, all of it stays in the zone.
		for (long seed = 0; seed < 2000; seed++) {
			for (int zone = 0; zone < 3; zone++) {
				int[] pos = RealmLayout.featurePosition(seed, 100 + zone, 3, zone, START, 0.5, 400, 900);
				double distance = Math.sqrt((double) pos[0] * pos[0] + (double) pos[1] * pos[1]);
				assertTrue(distance >= 399 && distance <= 901, "distance " + distance);
				for (int dx = -48; dx <= 48; dx += 16) {
					for (int dz = -48; dz <= 48; dz += 16) {
						for (double warp : new double[]{-1, 0, 1}) {
							assertEquals(zone, RealmLayout.zoneAt(pos[0] + dx, pos[1] + dz, 3, START, 48, 1200, warp, WARP),
									"seed " + seed + " zone " + zone);
						}
					}
				}
			}
		}
	}

	@Test
	void standingCandidatesNeverIncludeTheShrineAndStartBesideIt() {
		var candidates = RealmLayout.standingCandidates(10, 64, -5, 3);
		assertTrue(candidates.stream().noneMatch(c -> c[0] == 10 && c[2] == -5));
		int[] first = candidates.get(0);
		assertEquals(1, Math.abs(first[0] - 10) + Math.abs(first[2] + 5)); // a side neighbour, not a corner
		assertEquals(64, first[1]);
		// 3 rings of 8, 16 and 24 cells, each at 3 heights.
		assertEquals((8 + 16 + 24) * 3, candidates.size());
	}
	@Test
	void zoneWeightsBlendAcrossBorders() {
		double start = Math.toRadians(-90);
		double width = RealmLayout.sectorWidth(3);
		// Middle of zone 1: all of it.
		double[] middle = RealmLayout.zoneWeights(start + 1.5 * width, 3, start, 500, 48);
		assertEquals(1, middle[1], 1e-9);
		// On the border between zone 0 and zone 1: half and half.
		double[] border = RealmLayout.zoneWeights(start + width, 3, start, 500, 48);
		assertEquals(0.5, border[0], 1e-9);
		assertEquals(0.5, border[1], 1e-9);
		// Across the wrap-around border (zone 2 → zone 0) the weights still sum to 1.
		double[] wrap = RealmLayout.zoneWeights(start + 3 * width - 0.01, 3, start, 500, 48);
		assertEquals(1, wrap[0] + wrap[2], 1e-9);
		assertTrue(wrap[0] > 0 && wrap[2] > wrap[0]);
		for (double a = 0; a < RealmLayout.TAU; a += 0.01) {
			double[] w = RealmLayout.zoneWeights(a, 3, start, 300, 48);
			assertEquals(1, w[0] + w[1] + w[2], 1e-9);
		}
	}

	@Test
	void landingWeightFadesOut() {
		assertEquals(1, RealmLayout.landingWeight(10, 48, 32), 1e-9);
		assertEquals(0.5, RealmLayout.landingWeight(64, 48, 32), 1e-9);
		assertEquals(0, RealmLayout.landingWeight(80, 48, 32), 1e-9);
	}
}
