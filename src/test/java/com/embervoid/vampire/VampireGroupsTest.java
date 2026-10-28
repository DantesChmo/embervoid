package com.embervoid.vampire;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VampireGroupsTest {
	private static double[] at(double x, double z) {
		return new double[]{x, 64, z};
	}

	@Test
	void noPlayersNoGroups() {
		assertEquals(List.of(), VampireGroups.cluster(List.of(), 64));
	}

	@Test
	void farApartPlayersGetOwnGroups() {
		assertEquals(List.of(List.of(0), List.of(1)), VampireGroups.cluster(List.of(at(0, 0), at(200, 0)), 64));
	}

	@Test
	void nearPlayersShareAGroup() {
		assertEquals(List.of(List.of(0, 1)), VampireGroups.cluster(List.of(at(0, 0), at(64, 0)), 64));
	}

	@Test
	void groupsChain() {
		// 0-1 and 1-2 are in range, 0-2 is not: still one group.
		assertEquals(List.of(List.of(0, 1, 2), List.of(3)),
				VampireGroups.cluster(List.of(at(0, 0), at(50, 0), at(100, 0), at(500, 500)), 64));
	}

	@Test
	void heightCounts() {
		assertEquals(2, VampireGroups.cluster(List.of(new double[]{0, -50, 0}, new double[]{0, 100, 0}), 64).size());
	}

	@Test
	void distanceIsMeasuredInThreeDimensions() {
		// 40 across and 40 up: 56.6 apart.
		assertEquals(1, VampireGroups.cluster(List.of(new double[]{0, 0, 0}, new double[]{40, 40, 0}), 57).size());
		assertEquals(2, VampireGroups.cluster(List.of(new double[]{0, 0, 0}, new double[]{40, 40, 0}), 56).size());
	}

	@Test
	void groupsAreOrderedByTheirFirstMember() {
		assertEquals(List.of(List.of(0, 2), List.of(1, 3)),
				VampireGroups.cluster(List.of(at(0, 0), at(500, 0), at(10, 0), at(510, 0)), 64));
	}

	@Test
	void chainJoinsGroupsSeenAsSeparateFirst() {
		// 0 and 2 start apart; 3 links them only at the end.
		assertEquals(List.of(List.of(0, 1, 2, 3)),
				VampireGroups.cluster(List.of(at(0, 0), at(30, 0), at(200, 0), at(115, 0)), 90));
	}

	@Test
	void everyPlayerIsInExactlyOneGroup() {
		java.util.Random random = new java.util.Random(7);
		List<double[]> positions = new java.util.ArrayList<>();
		for (int i = 0; i < 40; i++) {
			positions.add(at(random.nextInt(600), random.nextInt(600)));
		}
		List<List<Integer>> groups = VampireGroups.cluster(positions, 64);
		List<Integer> all = groups.stream().flatMap(List::stream).sorted().toList();
		assertEquals(java.util.stream.IntStream.range(0, 40).boxed().toList(), all);
	}
}
