package com.embervoid.reach.gear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumableRulesTest {
	@Test
	void aCoatingBurnsForItsTicksAndThenGoesOut() {
		long putOn = 1000;
		long until = ConsumableRules.expiresAt(putOn, 600);
		assertTrue(ConsumableRules.burning(until, putOn));
		assertTrue(ConsumableRules.burning(until, putOn + 599));
		assertFalse(ConsumableRules.burning(until, putOn + 600));
		assertFalse(ConsumableRules.burning(until, putOn + 10_000));
	}

	@Test
	void noCoatingNeverBurns() {
		assertFalse(ConsumableRules.burning(0, 0));
		assertFalse(ConsumableRules.burning(0, 24_000));
	}

	@Test
	void aCoatingPutOnLateInALongWorldStillBurns() {
		long late = 10_000_000_000L;
		assertTrue(ConsumableRules.burning(ConsumableRules.expiresAt(late, 20), late + 19));
	}
}
