package com.embervoid.entity.mob;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackPlacementTest {
	@Test
	void prefersTheLeadersLevel() {
		assertEquals(OptionalInt.of(64), PackPlacement.findStandingY(64, 2, y -> true));
	}

	@Test
	void findsTheNearestStepUpOrDown() {
		assertEquals(OptionalInt.of(65), PackPlacement.findStandingY(64, 2, y -> y == 65 || y == 63));
		assertEquals(OptionalInt.of(63), PackPlacement.findStandingY(64, 2, y -> y == 63 || y == 62));
		assertEquals(OptionalInt.of(62), PackPlacement.findStandingY(64, 2, y -> y == 62));
	}

	@Test
	void givesUpOutsideTheStep() {
		assertEquals(OptionalInt.empty(), PackPlacement.findStandingY(64, 2, y -> y == 67 || y == 61));
	}

	@Test
	void checksEachLevelOnceInOrder() {
		List<Integer> checked = new ArrayList<>();
		PackPlacement.findStandingY(64, 2, y -> checked.add(y) && false);
		assertEquals(List.of(64, 65, 63, 66, 62), checked);
	}
}
