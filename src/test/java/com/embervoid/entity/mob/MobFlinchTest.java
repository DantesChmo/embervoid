package com.embervoid.entity.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobFlinchTest {
	/** The mobs start with their last flinch long ago. */
	private static final int NEVER = -100;

	@Test
	void aFreshMobFlinchesOnItsFirstHit() {
		assertTrue(MobAnimations.mayFlinch(true, 3, 0, NEVER));
		assertTrue(MobAnimations.mayFlinch(true, 3, 500, NEVER));
	}

	@Test
	void aBlockedOrHarmlessHitDoesNotFlinch() {
		assertFalse(MobAnimations.mayFlinch(false, 3, 500, NEVER));
		assertFalse(MobAnimations.mayFlinch(true, 0, 500, NEVER));
	}

	@Test
	void noSecondFlinchWithinTheCooldown() {
		int flinched = 200;
		for (int age = flinched; age <= flinched + MobAnimations.HURT_COOLDOWN; age++) {
			assertFalse(MobAnimations.mayFlinch(true, 3, age, flinched), "at " + (age - flinched) + " ticks");
		}
		assertTrue(MobAnimations.mayFlinch(true, 3, flinched + MobAnimations.HURT_COOLDOWN + 1, flinched));
	}

	@Test
	void anyAmountOfDamageCounts() {
		assertTrue(MobAnimations.mayFlinch(true, 0.01f, 50, NEVER));
		assertTrue(MobAnimations.mayFlinch(true, 1000, 50, NEVER));
	}
}
