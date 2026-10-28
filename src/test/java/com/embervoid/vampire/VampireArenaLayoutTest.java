package com.embervoid.vampire;

import com.embervoid.vampire.VampireArenaLayout.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VampireArenaLayoutTest {
	private static final int R = VampireArenaLayout.RADIUS;

	private static List<int[]> pillarColumns() {
		List<int[]> columns = new ArrayList<>();
		for (int x = -R; x <= R; x++) {
			for (int z = -R; z <= R; z++) {
				if (VampireArenaLayout.isPillar(x, z)) {
					columns.add(new int[]{x, z});
				}
			}
		}
		return columns;
	}

	@Test
	void theCentreOfTheFloorIsTheHeart() {
		assertEquals(Role.HEART, VampireArenaLayout.roleAt(0, 0, 0));
		assertEquals(Role.HEART, VampireArenaLayout.roleAt(1, 0, 1));
	}

	@Test
	void theFloorIsWholeAcrossTheDisc() {
		for (int x = -R; x <= R; x++) {
			for (int z = -R; z <= R; z++) {
				if (x * x + z * z <= R * R) {
					assertNotEquals(Role.AIR, VampireArenaLayout.roleAt(x, 0, z), x + ", " + z);
					assertEquals(Role.SUBFLOOR, VampireArenaLayout.roleAt(x, -1, z));
				}
			}
		}
	}

	@Test
	void nothingBeyondTheDisc() {
		for (int dy = -2; dy <= 10; dy++) {
			assertEquals(Role.AIR, VampireArenaLayout.roleAt(R + 1, dy, 0));
			assertEquals(Role.AIR, VampireArenaLayout.roleAt(17, dy, 17), "past the edge on the diagonal");
		}
	}

	@Test
	void aLowRimRunsRoundTheEdge() {
		assertEquals(Role.FLOOR_EDGE, VampireArenaLayout.roleAt(R, 0, 0));
		assertEquals(Role.RIM, VampireArenaLayout.roleAt(R, 1, 0));
		assertEquals(Role.RIM, VampireArenaLayout.roleAt(0, 1, -R));
		assertEquals(Role.AIR, VampireArenaLayout.roleAt(R, 2, 0), "one block high: a jump clears it");
		assertEquals(Role.AIR, VampireArenaLayout.roleAt(5, 1, 3), "no rim inside the floor");
	}

	@Test
	void theUndersideStopsShortOfTheEdge() {
		assertEquals(Role.UNDERSIDE, VampireArenaLayout.roleAt(0, -2, 0));
		assertEquals(Role.AIR, VampireArenaLayout.roleAt(R, -2, 0));
		assertEquals(Role.AIR, VampireArenaLayout.roleAt(0, -3, 0), "void below");
	}

	@Test
	void eightPillarsOnTheirRing() {
		List<int[]> columns = pillarColumns();
		assertEquals(VampireArenaLayout.PILLARS, columns.size());
		for (int[] column : columns) {
			double d = Math.sqrt(column[0] * column[0] + column[1] * column[1]);
			assertEquals(VampireArenaLayout.PILLAR_RADIUS, d, 0.75);
		}
		assertTrue(VampireArenaLayout.isPillar(VampireArenaLayout.PILLAR_RADIUS, 0));
		assertTrue(VampireArenaLayout.isPillar(0, -VampireArenaLayout.PILLAR_RADIUS));
	}

	@Test
	void eachPillarRisesFromTheFloorToALightOnTop() {
		for (int[] column : pillarColumns()) {
			int x = column[0];
			int z = column[1];
			for (int dy = 1; dy < VampireArenaLayout.PILLAR_HEIGHT; dy++) {
				assertEquals(Role.PILLAR, VampireArenaLayout.roleAt(x, dy, z));
			}
			assertEquals(Role.PILLAR_LIGHT, VampireArenaLayout.roleAt(x, VampireArenaLayout.PILLAR_HEIGHT, z));
			assertEquals(Role.AIR, VampireArenaLayout.roleAt(x, VampireArenaLayout.PILLAR_HEIGHT + 1, z));
		}
	}

	@Test
	void openAirAboveTheFloorBetweenThePillars() {
		for (int x = -R + 2; x <= R - 2; x++) {
			for (int z = -R + 2; z <= R - 2; z++) {
				if (!VampireArenaLayout.isPillar(x, z) && x * x + z * z < (R - 1) * (R - 1)) {
					for (int dy = 1; dy <= 10; dy++) {
						assertEquals(Role.AIR, VampireArenaLayout.roleAt(x, dy, z));
					}
				}
			}
		}
	}

	@Test
	void theRedInlayCrossesTheFloorAndRingsIt() {
		assertEquals(Role.INLAY, VampireArenaLayout.roleAt(0, 0, 4), "the cross along an axis");
		assertEquals(Role.INLAY, VampireArenaLayout.roleAt(-3, 0, 0));
		assertEquals(Role.FLOOR, VampireArenaLayout.roleAt(3, 0, 3), "between the arms of the cross");
		assertEquals(Role.INLAY, VampireArenaLayout.roleAt(6, 0, 0), "the inner ring");
		assertEquals(Role.INLAY, VampireArenaLayout.roleAt(0, 0, -6));
		assertEquals(Role.INLAY, VampireArenaLayout.roleAt(R - 3, 0, 0), "the outer ring");
		assertEquals(Role.FLOOR, VampireArenaLayout.roleAt(12, 0, 0), "between the rings");
	}

	@Test
	void theLayoutIsTheSameEveryQuarterTurn() {
		for (int x = -R; x <= R; x++) {
			for (int z = -R; z <= R; z++) {
				for (int dy = -2; dy <= 7; dy++) {
					assertEquals(VampireArenaLayout.roleAt(x, dy, z), VampireArenaLayout.roleAt(-z, dy, x), x + " " + dy + " " + z);
				}
			}
		}
	}
}
