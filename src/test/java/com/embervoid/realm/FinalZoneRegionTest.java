package com.embervoid.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalZoneRegionTest {
	private static final double PLAZA = 22;

	private static FinalZoneRegion reach() {
		return new FinalZoneRegion(ReachLayoutFiles.sectors(), PLAZA);
	}

	private static double[] at(double degrees, double distance) {
		double a = Math.toRadians(degrees);
		return new double[]{Math.cos(a) * distance, Math.sin(a) * distance};
	}

	private static boolean contains(FinalZoneRegion region, double degrees, double distance) {
		double[] p = at(degrees, distance);
		return region.contains(p[0], p[1]);
	}

	@Test
	void theReachRegionIsSixtyDegreesSouthBeyondThePlaza() {
		FinalZoneRegion region = reach();
		// Final sector 60°–120° (centred on +Z, south).
		for (double distance : new double[]{PLAZA + 0.01, 48, 300, 1299, 1300, 1400, 5000}) {
			assertTrue(contains(region, 90, distance), "middle at " + distance);
			assertTrue(contains(region, 60.01, distance), "just past 60° at " + distance);
			assertTrue(contains(region, 119.99, distance), "just short of 120° at " + distance);
			assertFalse(contains(region, 59.99, distance), "just short of 60° at " + distance);
			assertFalse(contains(region, 120.01, distance), "just past 120° at " + distance);
			assertFalse(contains(region, 270, distance), "opposite at " + distance);
		}
		assertFalse(region.contains(0, 0), "the landing");
		assertFalse(region.contains(0, PLAZA - 0.01), "the plaza rim");
		assertTrue(region.contains(0, PLAZA + 0.01), "past the plaza rim");
		// Block columns either side of the southern rays at one block of arc.
		assertFalse(contains(region, 60 - Math.toDegrees(1.0 / 600), 600));
		assertTrue(contains(region, 60 + Math.toDegrees(1.0 / 600), 600));
	}

	@Test
	void aRealmWithoutAFinalSectorHasNoRegion() {
		FinalZoneRegion region = new FinalZoneRegion(RealmLayout.Sectors.equal(3, 0), PLAZA);
		for (double degrees = 0; degrees < 360; degrees += 7.5) {
			assertFalse(contains(region, degrees, 300));
		}
	}

	@Test
	void aSectorAcrossZeroDegreesWraps() {
		// Final sector 330°–30°.
		FinalZoneRegion region = new FinalZoneRegion(RealmLayout.Sectors.degrees(30, 3, 60), PLAZA);
		assertTrue(contains(region, 0, 100));
		assertTrue(contains(region, 345, 100));
		assertTrue(contains(region, 15, 100));
		assertFalse(contains(region, 329.9, 100));
		assertFalse(contains(region, 30.1, 100));
		assertFalse(contains(region, 180, 100));
	}

	@Test
	void exitIsJustAcrossTheNearestBorder() {
		FinalZoneRegion region = reach();
		double margin = 1.5;
		// Near the 60° ray, far out: out across it, the margin past it.
		double[] near60 = at(61, 400);
		double[] out = region.exit(near60[0], near60[1], margin);
		assertFalse(region.contains(out[0], out[1]));
		assertEquals(margin, region.borderDistance(out[0], out[1]), 1e-6);
		assertTrue(RealmLayout.angle(out[0], out[1]) < Math.toRadians(60), "not across the 60° ray");
		assertEquals(400 * Math.sin(Math.toRadians(1)) + margin, Math.hypot(out[0] - near60[0], out[1] - near60[1]), 1e-6);
		// Near the 120° ray.
		double[] near120 = at(118, 400);
		out = region.exit(near120[0], near120[1], margin);
		assertFalse(region.contains(out[0], out[1]));
		assertTrue(RealmLayout.angle(out[0], out[1]) > Math.toRadians(120), "not across the 120° ray");
		// Just past the plaza rim in the middle: back onto the plaza.
		out = region.exit(0, PLAZA + 1, margin);
		assertEquals(0, out[0], 1e-9);
		assertEquals(PLAZA - margin, out[1], 1e-9);
		// Outside: unchanged.
		out = region.exit(-50, 3, margin);
		assertEquals(-50, out[0], 1e-9);
		assertEquals(3, out[1], 1e-9);
	}

	@Test
	void exitAlwaysLeavesTheRegion() {
		FinalZoneRegion region = reach();
		for (double degrees = 60.05; degrees < 120; degrees += 0.5) {
			for (double distance = PLAZA + 0.05; distance < 1500; distance *= 1.3) {
				double[] p = at(degrees, distance);
				double[] out = region.exit(p[0], p[1], 0.5);
				assertFalse(region.contains(out[0], out[1]), "still inside from " + degrees + "° at " + distance);
				double moved = Math.hypot(out[0] - p[0], out[1] - p[1]);
				assertTrue(moved <= region.borderDistance(p[0], p[1]) + 0.5 + 1e-6,
						"moved " + moved + " from " + degrees + "° at " + distance);
			}
		}
	}

	@Test
	void borderDistanceAlongTheRaysAndTheArc() {
		FinalZoneRegion region = reach();
		double[] onRay = at(60, 500);
		assertEquals(0, region.borderDistance(onRay[0], onRay[1]), 1e-9);
		assertEquals(3, region.borderDistance(0, PLAZA + 3), 1e-9);
		assertEquals(500 * Math.sin(Math.toRadians(5)), region.borderDistance(at(65, 500)[0], at(65, 500)[1]), 1e-9);
		assertEquals(500 * Math.sin(Math.toRadians(5)), region.borderDistance(at(55, 500)[0], at(55, 500)[1]), 1e-9);
	}

	@Test
	void bordersNearACurtainViewer() {
		FinalZoneRegion region = reach();
		// Beside the 60° ray, 300 out: only that ray, about 2 × 48 blocks of it, every piece on the ray and near.
		double[] viewer = at(58, 300);
		var pieces = region.bordersNear(viewer[0], viewer[1], 48, 2, 1300, 7);
		assertTrue(pieces.size() >= 40 && pieces.size() <= 50, pieces.size() + " pieces");
		for (double[] piece : pieces) {
			for (int end = 0; end < 4; end += 2) {
				assertEquals(Math.toRadians(60), RealmLayout.angle(piece[end], piece[end + 1]), 1e-9);
				assertTrue(Math.hypot(piece[end] - viewer[0], piece[end + 1] - viewer[1]) < 48 + 2);
			}
		}
		// Far from both rays and the plaza: nothing.
		viewer = at(90, 900);
		assertTrue(region.bordersNear(viewer[0], viewer[1], 48, 2, 1300, 7).isEmpty());
		// On the plaza: the arc either side of the gate, never across it, and the first stretch of both rays.
		pieces = region.bordersNear(0, 0, 48, 2, 1300, 7);
		boolean arc = false;
		boolean ray60 = false;
		boolean ray120 = false;
		for (double[] piece : pieces) {
			double mx = (piece[0] + piece[2]) / 2;
			double mz = (piece[1] + piece[3]) / 2;
			double angle = Math.toDegrees(RealmLayout.angle(mx, mz));
			if (Math.abs(Math.hypot(piece[0], piece[1]) - PLAZA) < 1e-6 && Math.abs(Math.hypot(piece[2], piece[3]) - PLAZA) < 1e-6) {
				arc = true;
				assertTrue(Math.abs(mx) >= 7 - 2, "an arc piece across the gate at " + mx);
				assertTrue(angle > 60 && angle < 120);
			} else {
				ray60 |= Math.abs(angle - 60) < 1e-6;
				ray120 |= Math.abs(angle - 120) < 1e-6;
			}
		}
		assertTrue(arc && ray60 && ray120, "arc " + arc + ", rays " + ray60 + " " + ray120);
		// Nothing past the end of the rays.
		viewer = at(60, 1320);
		for (double[] piece : region.bordersNear(viewer[0], viewer[1], 48, 2, 1300, 7)) {
			assertTrue(Math.hypot(piece[2], piece[3]) <= 1300 + 1e-6);
		}
	}
}
