package com.embervoid.reach.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HollowsTest {
	/** A 48 × 48 plain at 58 with a bowl in the middle: depth 1 per block from its rim. */
	private static int[][] bowl(int radius, int depth) {
		int[][] ground = new int[48][48];
		for (int x = 0; x < 48; x++) {
			for (int z = 0; z < 48; z++) {
				double d = Math.hypot(x - 24, z - 24);
				ground[x][z] = d < radius ? 58 - (int) Math.min(depth, radius - d) : 58;
			}
		}
		return ground;
	}

	@Test
	void closedHollowFillsToTheLevel() {
		int[][] ground = bowl(8, 20);
		List<int[]> pool = Hollows.fill(ground, 24, 24, ground[24][24] + 2, 900);
		assertNotNull(pool);
		for (int[] c : pool) {
			assertEquals(true, ground[c[0]][c[1]] < ground[24][24] + 2);
		}
	}

	@Test
	void hollowOpenToTheEdgeIsLeftDry() {
		int[][] ground = bowl(8, 20);
		// A channel from the bottom out to the window edge.
		for (int x = 24; x < 48; x++) {
			ground[x][24] = ground[24][24];
		}
		assertNull(Hollows.fill(ground, 24, 24, ground[24][24] + 2, 900));
	}
}
