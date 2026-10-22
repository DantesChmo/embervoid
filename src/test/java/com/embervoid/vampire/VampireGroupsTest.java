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
}
