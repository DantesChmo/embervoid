package com.embervoid.realm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeLayoutTest {
	@Test
	void wallIsPartlyStandingPartlyFallen() {
		int fallen = 0;
		int tall = 0;
		int segments = 0;
		for (double a = 0; a < RealmLayout.TAU; a += EdgeLayout.SEGMENT / 1105) {
			int h = EdgeLayout.wallHeight(42L, a, 1105);
			assertTrue(h >= 0 && h <= 6);
			fallen += h == 0 ? 1 : 0;
			tall += h >= 3 ? 1 : 0;
			segments++;
		}
		assertTrue(fallen > segments / 6 && fallen < segments / 2, fallen + " of " + segments + " fallen");
		assertTrue(tall > segments / 5, tall + " of " + segments + " tall");
	}

	@Test
	void bridgesAreSpreadAroundTheOpenZones() {
		RealmLayout.Sectors sectors = ReachLayoutFiles.sectors();
		double from = ReachLayoutFiles.radius() - 58;
		for (long seed = 0; seed < 200; seed++) {
			List<EdgeLayout.Bridge> bridges = EdgeLayout.bridges(seed, 6, from, sectors);
			assertEquals(6, bridges.size());
			double step = sectors.openSpan() / 6;
			for (int i = 0; i < bridges.size(); i++) {
				EdgeLayout.Bridge a = bridges.get(i);
				assertTrue(a.to() - a.from() >= 25 && a.to() - a.from() <= 45);
				assertEquals(from, a.from(), 1e-9);
				// None in the final zone's sector, nor within its width of a border.
				assertTrue(!sectors.inFinal(a.angle()), "bridge " + i + " in the final sector");
				double local = RealmLayout.normalize(a.angle() - sectors.startAngle());
				assertTrue(Math.min(local, sectors.openSpan() - local) * from > a.width() * 4, "bridge " + i + " at the final border");
				if (i > 0) {
					double gap = RealmLayout.normalize(a.angle() - bridges.get(i - 1).angle());
					assertTrue(gap >= step / 2 - 1e-9, "bridges " + (i - 1) + " and " + i + " too close: " + gap);
				}
			}
		}
	}

	@Test
	void bridgeCoordinatesFollowTheBridge() {
		EdgeLayout.Bridge bridge = new EdgeLayout.Bridge(Math.PI / 2, 1142, 1170, 5);
		double[] at = EdgeLayout.bridgeCoordinates(bridge, 2, 1150);
		assertEquals(1150, at[0], 1e-9);
		assertEquals(-2, at[1], 1e-9);
	}
}
