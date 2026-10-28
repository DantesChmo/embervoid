package com.embervoid.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatFxTest {
	@Test
	void shakesLastLongerTheStrongerTheyAreUpToACap() {
		assertEquals(10, CombatFx.shakeTicks(0));
		assertEquals(10, CombatFx.shakeTicks(-1));
		assertEquals(20, CombatFx.shakeTicks(1));
		assertEquals(CombatFx.shakeTicks(5), CombatFx.shakeTicks(50));
		for (float strength = 0; strength < 1.5f; strength += 0.05f) {
			assertTrue(CombatFx.shakeTicks(strength + 0.05f) >= CombatFx.shakeTicks(strength));
		}
	}
}
