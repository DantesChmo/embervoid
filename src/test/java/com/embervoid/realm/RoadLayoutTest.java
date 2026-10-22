package com.embervoid.realm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadLayoutTest {
	private static final List<int[]> ARENAS = List.of(new int[]{600, -300}, new int[]{-50, 700}, new int[]{-500, -400});
	private static final List<Integer> SITES = List.of(48, 56, 40);

	@Test
	void roadsRunFromThePlazaToTheSiteEdge() {
		List<RoadLayout.Road> roads = RoadLayout.roads(1234L, ARENAS, SITES, 22, 24);
		assertEquals(3, roads.size());
		for (RoadLayout.Road road : roads) {
			int[] arena = ARENAS.get(road.arena());
			double[] first = road.points().get(0);
			double[] last = road.points().get(road.points().size() - 1);
			assertEquals(22, Math.hypot(first[0], first[1]), 0.5);
			double toArena = Math.hypot(last[0] - arena[0], last[1] - arena[1]);
			assertTrue(toArena > SITES.get(road.arena()) - 2 && toArena < SITES.get(road.arena()) + 4,
					"road ends " + toArena + " from its arena");
			for (int k = 1; k < road.points().size(); k++) {
				double[] a = road.points().get(k - 1);
				double[] b = road.points().get(k);
				assertEquals(RoadLayout.STEP, Math.hypot(b[0] - a[0], b[1] - a[1]), 0.05);
			}
		}
	}

	@Test
	void milestonesEveryHundredBlocksBesideTheRoad() {
		RoadLayout.Road road = RoadLayout.roads(99L, ARENAS, SITES, 22, 24).get(0);
		assertEquals((int) ((road.length() - 30) / RoadLayout.MILESTONE_SPACING), road.milestones().size());
		for (RoadLayout.Milestone milestone : road.milestones()) {
			double d = Math.sqrt(RoadLayout.distanceSquared(road, milestone.x() + 0.5, milestone.z() + 0.5));
			assertTrue(d > 2.5 && d < 5.5, "milestone " + d + " blocks from the road");
		}
	}

	@Test
	void twoOutpostsOnEachRoad() {
		for (RoadLayout.Road road : RoadLayout.roads(7L, ARENAS, SITES, 22, 24)) {
			assertEquals(2, road.outposts().size());
			assertEquals("toll_gate", road.outposts().get(0).kind());
			assertEquals("watchtower", road.outposts().get(1).kind());
			for (RoadLayout.Outpost outpost : road.outposts()) {
				assertTrue(RoadLayout.distanceSquared(road, outpost.x(), outpost.z()) < 2);
				assertEquals(1, Math.hypot(outpost.dirX(), outpost.dirZ()), 1e-6);
			}
		}
	}

	@Test
	void sameSeedSameRoads() {
		RoadLayout.Road a = RoadLayout.roads(5L, ARENAS, SITES, 22, 24).get(1);
		RoadLayout.Road b = RoadLayout.roads(5L, ARENAS, SITES, 22, 24).get(1);
		assertEquals(a.points().size(), b.points().size());
		assertEquals(a.points().get(40)[0], b.points().get(40)[0]);
	}
}
