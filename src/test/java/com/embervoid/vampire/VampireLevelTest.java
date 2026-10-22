package com.embervoid.vampire;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VampireLevelTest {
	@Test
	void freshPlayersMeetTheMinimum() {
		assertEquals(15, VampireLevel.of(List.of(1, 3), 15, 20));
	}

	@Test
	void highestPlayerLevelCounts() {
		assertEquals(18, VampireLevel.of(List.of(4, 18, 12), 15, 30));
	}

	@Test
	void cappedByTheHighestUnlockedRealm() {
		assertEquals(20, VampireLevel.of(List.of(40), 15, 20));
		assertEquals(30, VampireLevel.of(List.of(40), 15, 30));
	}

	@Test
	void capNeverBelowMinimum() {
		assertEquals(15, VampireLevel.of(List.of(40), 15, 10));
	}

	@Test
	void nobodyGivesTheMinimum() {
		assertEquals(15, VampireLevel.of(List.of(), 15, 20));
	}
}
