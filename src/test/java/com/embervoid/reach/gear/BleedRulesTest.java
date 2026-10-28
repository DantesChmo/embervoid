package com.embervoid.reach.gear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BleedRulesTest {
	@Test
	void eachHitAddsALevelUpToTheCap() {
		assertEquals(1, BleedRules.nextLevel(0, 1, 3));
		assertEquals(2, BleedRules.nextLevel(1, 1, 3));
		assertEquals(3, BleedRules.nextLevel(2, 1, 3));
		assertEquals(3, BleedRules.nextLevel(3, 1, 3));
		// The rapier's lunge: double bleed.
		assertEquals(2, BleedRules.nextLevel(0, 2, 3));
		assertEquals(3, BleedRules.nextLevel(2, 2, 3));
	}

	@Test
	void damageIsHalfAHeartPointPerLevelPerSecond() {
		assertEquals(0.5f, BleedRules.damagePerTick(1, 0.5f), 1e-6);
		assertEquals(1.0f, BleedRules.damagePerTick(2, 0.5f), 1e-6);
		assertEquals(1.5f, BleedRules.damagePerTick(3, 0.5f), 1e-6);
	}

	@Test
	void ticksOncePerSecondByAge() {
		assertTrue(BleedRules.damagesAt(40));
		assertFalse(BleedRules.damagesAt(41));
		assertFalse(BleedRules.damagesAt(59));
		assertTrue(BleedRules.damagesAt(60));
	}

	@Test
	void levelNeverDropsBelowNotBleeding() {
		assertEquals(0, BleedRules.nextLevel(1, -3, 3));
		assertEquals(0, BleedRules.nextLevel(0, 0, 3));
	}

	@Test
	void noLevelDealsNoDamage() {
		assertEquals(0f, BleedRules.damagePerTick(0, 0.5f), 1e-6);
	}

	@Test
	void ticksExactlyOncePerSecond() {
		int ticks = 0;
		for (int age = 0; age < 200; age++) {
			if (BleedRules.damagesAt(age)) {
				ticks++;
			}
		}
		assertEquals(10, ticks);
		assertTrue(BleedRules.damagesAt(0));
	}
}
