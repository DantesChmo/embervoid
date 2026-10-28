package com.embervoid.realm;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmLayoutTest {
	private static final double START = Math.toRadians(-90);
	private static final double WARP = Math.toRadians(20);
	/** Three open zones of 120° from north, no final sector. */
	private static final RealmLayout.Sectors EQUAL = RealmLayout.Sectors.equal(3, START);
	/** The Ashen Reach as its data files say. */
	private static final RealmLayout.Sectors REACH = ReachLayoutFiles.sectors();

	@Test
	void angleIsNormalized() {
		assertEquals(0, RealmLayout.angle(10, 0), 1e-9);
		assertEquals(Math.PI / 2, RealmLayout.angle(0, 10), 1e-9);
		assertEquals(Math.PI * 1.5, RealmLayout.angle(0, -10), 1e-9);
	}

	@Test
	void equalSectorsCoverTheCircle() {
		// Start at -90° (north), three sectors of 120°.
		assertEquals(0, zone(EQUAL, -60, 0));
		assertEquals(1, zone(EQUAL, 90, 0));
		assertEquals(2, zone(EQUAL, 200, 0));
		assertEquals(0, zone(EQUAL, -90, 0));
		assertEquals(2, zone(EQUAL, -90.0001, 0));
	}

	@Test
	void sectorsMustCoverTheCircle() {
		assertThrows(IllegalArgumentException.class, () -> new RealmLayout.Sectors(0, 3, Math.toRadians(100), Math.toRadians(50)));
	}

	@Test
	void reachHasFourSectors() {
		// DESIGN §4.1: three open zones of 100°, the final zone's 60°, radius 1300.
		assertEquals(3, REACH.openCount());
		assertEquals(100, Math.toDegrees(REACH.openWidth()), 1e-9);
		assertEquals(60, Math.toDegrees(REACH.finalWidth()), 1e-9);
		assertEquals(4, REACH.count());
		assertEquals(1300, ReachLayoutFiles.radius(), 1e-9);
		// The final sector faces south (+Z), where the Citadel Gate stands on the plaza; the open zones follow it.
		assertEquals(90, Math.toDegrees(REACH.finalCenter()), 1e-9);
		assertEquals(120, Math.toDegrees(REACH.openStart(0)), 1e-9);
		assertEquals(220, Math.toDegrees(REACH.openStart(1)), 1e-9);
		assertEquals(320, Math.toDegrees(REACH.openStart(2)), 1e-9);
		assertEquals(60, Math.toDegrees(REACH.finalStart()), 1e-9);
		assertEquals(0, zone(REACH, 170, 0));
		assertEquals(1, zone(REACH, 270, 0));
		assertEquals(2, zone(REACH, 10, 0));
		assertEquals(RealmLayout.FINAL, zone(REACH, 90, 0));
	}

	@Test
	void noiseSettingsMatchTheBiomeSource() {
		JsonObject source = ReachLayoutFiles.biomeSource();
		List<JsonObject> blends = ReachLayoutFiles.densityFunctions("embervoid:zone_blend");
		assertEquals(2, blends.size());
		for (JsonObject blend : blends) {
			assertEquals(source.get("start_angle").getAsDouble(), blend.get("start_angle").getAsDouble(), 1e-9);
			assertEquals(source.get("final_width").getAsDouble(), blend.get("final_width").getAsDouble(), 1e-9);
			assertEquals(source.get("warp").getAsDouble(), blend.get("warp_degrees").getAsDouble(), 1e-9);
			assertEquals(source.get("landing_radius").getAsDouble(), blend.get("landing_radius").getAsDouble(), 1e-9);
			assertEquals(source.getAsJsonArray("zones").size(), blend.getAsJsonArray("zones").size());
			assertTrue(blend.has("final_zone"), "no final zone terrain");
		}
		for (JsonObject falloff : ReachLayoutFiles.densityFunctions("embervoid:radial_falloff")) {
			assertEquals(ReachLayoutFiles.radius(), falloff.get("outer").getAsDouble(), 1e-9, "the land must end at the radius");
		}
		for (String zone : ReachLayoutFiles.ZONES) {
			JsonObject placement = ReachLayoutFiles.arenaPlacement(zone);
			assertEquals(source.get("start_angle").getAsDouble(), placement.get("start_angle").getAsDouble(), 1e-9, zone);
			assertEquals(Math.toDegrees(REACH.openWidth()), placement.get("sector_width").getAsDouble(), 1e-9, zone);
		}
	}

	@Test
	void landingAndAbyss() {
		assertEquals(RealmLayout.LANDING, RealmLayout.zoneAt(10, 10, EQUAL, 48, 1200, 0, WARP));
		assertEquals(RealmLayout.ABYSS, RealmLayout.zoneAt(1300, 0, EQUAL, 48, 1200, 0, WARP));
		// In the final sector too: the landing disc and the abyss come first.
		assertEquals(RealmLayout.LANDING, RealmLayout.zoneAt(0, 30, REACH, 48, 1300, 0, WARP));
		assertEquals(RealmLayout.FINAL, RealmLayout.zoneAt(0, 1299, REACH, 48, 1300, 0, WARP));
		assertEquals(RealmLayout.ABYSS, RealmLayout.zoneAt(0, 1301, REACH, 48, 1300, 0, WARP));
	}

	@Test
	void warpMovesBordersBetweenOpenZones() {
		// 1° past the border between sector 0 and 1 (at 30°): warped back by -20° it belongs to sector 0.
		assertEquals(1, zone(EQUAL, 31, 0));
		assertEquals(0, zone(EQUAL, 31, -1));
		// Out-of-range warp is clamped.
		assertEquals(0, zone(EQUAL, 31, -5));
		// The Reach: the Fields–Woods border at 220° moves the full 20° either way.
		assertEquals(1, zone(REACH, 221, 0));
		assertEquals(0, zone(REACH, 221, -1));
		assertEquals(0, zone(REACH, 239, -1));
		assertEquals(1, zone(REACH, 201, 1));
		assertEquals(0, zone(REACH, 199, 0));
	}

	@Test
	void finalBordersAreStraight() {
		double eps = 0.05;
		for (double warp = -1; warp <= 1; warp += 0.25) {
			// The Necropolis–final border at 60° and the final–Fields border at 120° do not move with the noise.
			assertEquals(2, zone(REACH, 60 - eps, warp), "warp " + warp);
			assertEquals(RealmLayout.FINAL, zone(REACH, 60 + eps, warp), "warp " + warp);
			assertEquals(RealmLayout.FINAL, zone(REACH, 120 - eps, warp), "warp " + warp);
			assertEquals(0, zone(REACH, 120 + eps, warp), "warp " + warp);
			for (int distance = 60; distance < 1300; distance += 37) {
				for (double border : new double[]{60, 120}) {
					double inside = Math.toRadians(border == 60 ? border + eps : border - eps);
					assertEquals(RealmLayout.FINAL, RealmLayout.zoneAt(Math.cos(inside) * distance, Math.sin(inside) * distance,
							REACH, 48, 1300, warp, WARP));
				}
			}
		}
	}

	@Test
	void warpedAngleIsMonotonic() {
		for (double warp : new double[]{-1, -0.4, 0, 0.6, 1}) {
			double previous = -1;
			for (double a = 0; a < 360; a += 0.1) {
				double local = REACH.warpedLocal(REACH.startAngle() + Math.toRadians(a), warp, WARP);
				assertTrue(local >= previous - 1e-12, "warp " + warp + " at " + a);
				assertTrue(local >= 0 && local < RealmLayout.TAU, "out of range: " + local);
				previous = local;
			}
		}
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
		double width = Math.toRadians(100);
		int[] a = RealmLayout.featurePosition(12345, 7, START, width, 0.5, 400, 900);
		int[] b = RealmLayout.featurePosition(12345, 7, START, width, 0.5, 400, 900);
		assertArrayEquals(a, b);
		int[] other = RealmLayout.featurePosition(54321, 7, START, width, 0.5, 400, 900);
		assertTrue(a[0] != other[0] || a[1] != other[1]);
	}

	@Test
	void featurePositionAtDistanceZeroIsOrigin() {
		assertArrayEquals(new int[]{0, 0}, RealmLayout.featurePosition(1, 1, 0, RealmLayout.TAU, 1, 0, 0));
	}

	@Test
	void arenasStayInTheirZoneForAnyWarp() {
		// Arena footprint: its site plus snapping to a chunk corner. Whatever the noise does to the borders, all of it
		// stays in the zone.
		for (long seed = 0; seed < 2000; seed++) {
			for (int zone = 0; zone < 3; zone++) {
				String name = ReachLayoutFiles.ZONES[zone];
				int[] pos = ReachLayoutFiles.arenaPosition(seed, name);
				double distance = Math.sqrt((double) pos[0] * pos[0] + (double) pos[1] * pos[1]);
				assertTrue(distance >= 399 && distance <= 901, "distance " + distance);
				int reach = ReachLayoutFiles.siteRadius(name) + 16;
				for (int dx = -reach; dx <= reach; dx += 12) {
					for (int dz = -reach; dz <= reach; dz += 12) {
						if (dx * dx + dz * dz > reach * reach) {
							continue;
						}
						for (double warp : new double[]{-1, 0, 1}) {
							assertEquals(zone, RealmLayout.zoneAt(pos[0] + dx, pos[1] + dz, REACH, ReachLayoutFiles.landingRadius(),
									ReachLayoutFiles.radius(), warp, ReachLayoutFiles.warpRadians()), "seed " + seed + " zone " + zone);
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
		double width = EQUAL.openWidth();
		// Middle of zone 1: all of it.
		double[] middle = RealmLayout.zoneWeights(1.5 * width, EQUAL, 500, 48);
		assertEquals(1, middle[1], 1e-9);
		// On the border between zone 0 and zone 1: half and half.
		double[] border = RealmLayout.zoneWeights(width, EQUAL, 500, 48);
		assertEquals(0.5, border[0], 1e-9);
		assertEquals(0.5, border[1], 1e-9);
		// Across the wrap-around border (zone 2 → zone 0) the weights still sum to 1.
		double[] wrap = RealmLayout.zoneWeights(3 * width - 0.01, EQUAL, 500, 48);
		assertEquals(1, wrap[0] + wrap[2], 1e-9);
		assertTrue(wrap[0] > 0 && wrap[2] > wrap[0]);
		for (double a = 0; a < RealmLayout.TAU; a += 0.01) {
			double[] w = RealmLayout.zoneWeights(a, EQUAL, 300, 48);
			assertEquals(1, w[0] + w[1] + w[2], 1e-9);
		}
	}

	@Test
	void finalZoneTerrainBlendsAtItsStraightBorders() {
		int fin = REACH.index(RealmLayout.FINAL);
		assertEquals(3, fin);
		double open = REACH.openSpan();
		// Middle of the final sector: all final terrain.
		assertEquals(1, RealmLayout.zoneWeights(open + REACH.finalWidth() / 2, REACH, 500, 48)[fin], 1e-9);
		// On its borders: half the last open zone (or the first), half the final zone.
		double[] last = RealmLayout.zoneWeights(open, REACH, 500, 48);
		assertEquals(0.5, last[2], 1e-9);
		assertEquals(0.5, last[fin], 1e-9);
		double[] first = RealmLayout.zoneWeights(0, REACH, 500, 48);
		assertEquals(0.5, first[0], 1e-9);
		assertEquals(0.5, first[fin], 1e-9);
		for (double a = 0; a < RealmLayout.TAU; a += 0.005) {
			for (double distance : new double[]{60, 300, 1200}) {
				double[] w = RealmLayout.zoneWeights(a, REACH, distance, 48);
				assertEquals(1, w[0] + w[1] + w[2] + w[3], 1e-9);
				int mixed = 0;
				for (double v : w) {
					mixed += v > 0 ? 1 : 0;
				}
				assertTrue(mixed <= 2, "more than two zones mix at " + a);
			}
		}
	}

	@Test
	void landingWeightFadesOut() {
		assertEquals(1, RealmLayout.landingWeight(10, 48, 32), 1e-9);
		assertEquals(0.5, RealmLayout.landingWeight(64, 48, 32), 1e-9);
		assertEquals(0, RealmLayout.landingWeight(80, 48, 32), 1e-9);
	}

	/** Zone at an angle in degrees, 500 blocks out. */
	private static int zone(RealmLayout.Sectors sectors, double degrees, double warp) {
		double a = Math.toRadians(degrees);
		return RealmLayout.zoneAt(Math.cos(a) * 500, Math.sin(a) * 500, sectors, 48, 1300, warp, WARP);
	}
}
