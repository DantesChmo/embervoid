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
	void bridgesAreSpreadAroundTheEdge() {
		List<EdgeLayout.Bridge> bridges = EdgeLayout.bridges(7L, 6, 1142);
		assertEquals(6, bridges.size());
		for (int i = 0; i < bridges.size(); i++) {
			EdgeLayout.Bridge a = bridges.get(i);
			EdgeLayout.Bridge b = bridges.get((i + 1) % bridges.size());
			double gap = RealmLayout.normalize(b.angle() - a.angle());
			assertTrue(gap > RealmLayout.TAU / 12, "bridges " + i + " and " + (i + 1) + " too close: " + gap);
			assertTrue(a.to() - a.from() >= 25 && a.to() - a.from() <= 45);
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
