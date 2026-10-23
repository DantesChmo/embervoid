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

	@Test
	void borderWallHasGapsLowAndWholeStretches() {
		for (int border = 0; border < 2; border++) {
			int gaps = 0;
			int whole = 0;
			int segments = 0;
			for (double along = 24; along < 1236; along += EdgeLayout.BORDER_SEGMENT) {
				int h = EdgeLayout.borderWallHeight(7L, border, along);
				assertTrue(h == 0 || h >= 2 && h <= 7, "height " + h);
				assertEquals(h, EdgeLayout.borderWallHeight(7L, border, along + EdgeLayout.BORDER_SEGMENT - 0.01
						- along % EdgeLayout.BORDER_SEGMENT), "a stretch changes height inside");
				gaps += h == 0 ? 1 : 0;
				whole += h >= 5 ? 1 : 0;
				segments++;
			}
			assertTrue(gaps > segments / 10 && gaps < segments / 3, gaps + " of " + segments + " gaps");
			assertTrue(whole > segments / 3, whole + " of " + segments + " whole");
		}
		// The two borders are not the same wall.
		int same = 0;
		for (double along = 24; along < 1236; along += EdgeLayout.BORDER_SEGMENT) {
			same += EdgeLayout.borderWallHeight(7L, 0, along) == EdgeLayout.borderWallHeight(7L, 1, along) ? 1 : 0;
		}
		assertTrue(same < 150, same + " stretches alike");
	}

	@Test
	void borderTowersStandEverySixtyFourBlocks() {
		assertTrue(!EdgeLayout.isBorderTower(0) && !EdgeLayout.isBorderTower(30));
		assertTrue(EdgeLayout.isBorderTower(64) && EdgeLayout.isBorderTower(68.9) && !EdgeLayout.isBorderTower(69.1));
		assertTrue(EdgeLayout.isBorderTower(128.5) && !EdgeLayout.isBorderTower(100));
	}

	@Test
	void borderCoordinatesFollowTheRay() {
		double[] at = EdgeLayout.borderCoordinates(Math.PI / 2, 2, 300);
		assertEquals(300, at[0], 1e-9);
		assertEquals(-2, at[1], 1e-9);
		double a = Math.toRadians(60);
		at = EdgeLayout.borderCoordinates(a, Math.cos(a) * 500 - Math.sin(a) * 1.5, Math.sin(a) * 500 + Math.cos(a) * 1.5);
		assertEquals(500, at[0], 1e-9);
		assertEquals(1.5, at[1], 1e-9);
	}
}
