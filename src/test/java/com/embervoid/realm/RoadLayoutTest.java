package com.embervoid.realm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadLayoutTest {
	private static final List<int[]> ARENAS = List.of(new int[]{600, -300}, new int[]{-50, 700}, new int[]{-500, -400});
	private static final List<Integer> SITES = List.of(48, 56, 40);
	private static final long[] SEEDS = {1L, 7L, 99L, 1234L, -42L, 20261023L};

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
	void milestonesEveryFiftyBlocksBesideTheRoadOffTheOutposts() {
		for (long seed : SEEDS) {
			for (RoadLayout.Road road : RoadLayout.roads(seed, ARENAS, SITES, 22, 24)) {
				int slots = (int) Math.ceil((road.length() - 30) / RoadLayout.MILESTONE_SPACING) - 1;
				// Up to one slot per outpost may fall on its ground.
				assertTrue(road.milestones().size() >= slots - 2 && road.milestones().size() <= slots + 1,
						road.milestones().size() + " milestones on " + road.length() + " blocks");
				for (RoadLayout.Milestone milestone : road.milestones()) {
					double d = Math.sqrt(RoadLayout.distanceSquared(road, milestone.x() + 0.5, milestone.z() + 0.5));
					assertTrue(d > 2.5 && d < 5.5, "milestone " + d + " blocks from the road");
					for (RoadLayout.Outpost outpost : road.outposts()) {
						assertTrue(Math.hypot(milestone.x() - outpost.x(), milestone.z() - outpost.z()) >= RoadLayout.OUTPOST_CLEARANCE,
								"milestone on the " + outpost.kind());
					}
				}
			}
		}
	}

	@Test
	void lanternPostsEveryTwentyBlocksOnAlternatingSides() {
		for (long seed : SEEDS) {
			for (RoadLayout.Road road : RoadLayout.roads(seed, ARENAS, SITES, 22, 24)) {
				List<RoadLayout.Post> posts = road.posts();
				// Only the stretches at the two outposts and the arena's end go without.
				assertTrue(posts.size() >= road.length() / RoadLayout.POST_SPACING - 5, posts.size() + " posts on " + road.length() + " blocks");
				int right = 0;
				for (RoadLayout.Post post : posts) {
					double d = Math.sqrt(RoadLayout.distanceSquared(road, post.x() + 0.5, post.z() + 0.5));
					assertTrue(d >= RoadLayout.POST_ROAD_CLEARANCE && d < 5.5, "post " + d + " blocks from the road");
					right += post.side() == 1 ? 1 : 0;
				}
				assertTrue(right >= posts.size() / 3 && right <= posts.size() * 2 / 3,
						right + " of " + posts.size() + " posts on the right");
				for (int i = 1; i < posts.size(); i++) {
					RoadLayout.Post a = posts.get(i - 1);
					RoadLayout.Post b = posts.get(i);
					double gap = b.along() - a.along();
					if (gap == RoadLayout.POST_SPACING) {
						double straight = Math.hypot(b.x() - a.x(), b.z() - a.z());
						assertTrue(straight >= 14 && straight <= 28, "posts " + straight + " blocks apart");
					} else {
						// A longer gap only where a post was left out at an outpost.
						assertTrue(road.outposts().stream().anyMatch(o -> outpostBetween(road, o, a, b)),
								"posts " + gap + " blocks of road apart with no outpost between");
					}
				}
			}
		}
	}

	@Test
	void lanternPostsStayOffOutpostsArenaSitesThePlazaAndRoads() {
		for (long seed : SEEDS) {
			List<RoadLayout.Road> roads = RoadLayout.roads(seed, ARENAS, SITES, 22, 24);
			for (RoadLayout.Road road : roads) {
				for (RoadLayout.Post post : road.posts()) {
					double x = post.x() + 0.5;
					double z = post.z() + 0.5;
					assertTrue(Math.hypot(x, z) >= 22 + RoadLayout.SITE_CLEARANCE, "post on the plaza");
					for (int i = 0; i < ARENAS.size(); i++) {
						assertTrue(Math.hypot(x - ARENAS.get(i)[0], z - ARENAS.get(i)[1]) >= SITES.get(i) + RoadLayout.SITE_CLEARANCE,
								"post at " + post.x() + ", " + post.z() + " inside arena site " + i);
					}
					for (RoadLayout.Road other : roads) {
						assertTrue(RoadLayout.distanceSquared(other, x, z) >= RoadLayout.POST_ROAD_CLEARANCE * RoadLayout.POST_ROAD_CLEARANCE,
								"post on road " + other.arena());
						for (RoadLayout.Outpost outpost : other.outposts()) {
							assertTrue(Math.hypot(x - outpost.x(), z - outpost.z()) >= RoadLayout.OUTPOST_CLEARANCE,
									"post at " + post.x() + ", " + post.z() + " on the " + outpost.kind());
						}
					}
					for (RoadLayout.Milestone milestone : road.milestones()) {
						assertTrue(Math.hypot(post.x() - milestone.x(), post.z() - milestone.z()) >= 3, "post on a milestone");
					}
				}
			}
		}
	}

	/** Whether an outpost stands on the road between two posts. */
	private static boolean outpostBetween(RoadLayout.Road road, RoadLayout.Outpost outpost, RoadLayout.Post a, RoadLayout.Post b) {
		for (int k = 0; k < road.points().size(); k++) {
			double[] p = road.points().get(k);
			double along = k * RoadLayout.STEP;
			if (along > a.along() && along < b.along() && Math.hypot(p[0] - outpost.x(), p[1] - outpost.z()) < 2) {
				return true;
			}
		}
		return false;
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
