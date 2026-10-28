package com.embervoid.reach.gear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GearDamageRulesTest {
	/** The DESIGN §4.5 / §6.1 numbers, as the default config gives them. */
	private static final GearDamageRules.Factors DESIGN = GearDamageRules.Factors.of(GearConfig.SECTION.defaults());

	private static GearDamageRules.Hit plain() {
		return new GearDamageRules.Hit(false, 0, false, false, 1, false, false);
	}

	private static GearDamageRules.Hit fire(int pieces) {
		return new GearDamageRules.Hit(true, pieces, false, false, 1, false, false);
	}

	private static GearDamageRules.Hit fall() {
		return new GearDamageRules.Hit(false, 0, false, false, 1, false, true);
	}

	@Test
	void aHitWithoutGearIsUnchanged() {
		assertEquals(10, GearDamageRules.incoming(10, plain(), DESIGN), 1e-6);
	}

	@Test
	void eachSmolderingPieceTakesEightPercentOffFire() {
		assertEquals(10, GearDamageRules.incoming(10, fire(0), DESIGN), 1e-5);
		assertEquals(9.2f, GearDamageRules.incoming(10, fire(1), DESIGN), 1e-5);
		assertEquals(6.8f, GearDamageRules.incoming(10, fire(4), DESIGN), 1e-5);
		// The plate does nothing against a blade.
		GearDamageRules.Hit blade = new GearDamageRules.Hit(false, 4, false, false, 1, false, false);
		assertEquals(10, GearDamageRules.incoming(10, blade, DESIGN), 1e-6);
	}

	@Test
	void fireReductionNeverTurnsIntoHealing() {
		GearDamageRules.Factors steep = new GearDamageRules.Factors(0.4f, 0, 0, 1, 0);
		assertEquals(0, GearDamageRules.incoming(10, fire(4), steep), 1e-6);
	}

	@Test
	void huntsmanSetAddsFifteenPercentToProjectiles() {
		GearDamageRules.Hit arrow = new GearDamageRules.Hit(false, 0, true, false, 1, false, false);
		assertEquals(11.5f, GearDamageRules.incoming(10, arrow, DESIGN), 1e-5);
	}

	@Test
	void wardenSetTakesFifteenPercentOffWhileBlocking() {
		GearDamageRules.Hit blocked = new GearDamageRules.Hit(false, 0, false, true, 1, false, false);
		assertEquals(8.5f, GearDamageRules.incoming(10, blocked, DESIGN), 1e-5);
	}

	@Test
	void anOpenerBoltDealsHalfAgain() {
		GearDamageRules.Hit opener = new GearDamageRules.Hit(false, 0, true, false, 1, true, false);
		// A Huntsman in the full Leathers: 16 × 1.15 × 1.5.
		assertEquals(27.6f, GearDamageRules.incoming(16, opener, DESIGN), 1e-4);
	}

	@Test
	void theAttackersMultiplierScalesTheHit() {
		GearDamageRules.Hit tip = new GearDamageRules.Hit(false, 0, false, false, 1.6f, false, false);
		assertEquals(16, GearDamageRules.incoming(10, tip, DESIGN), 1e-5);
	}

	@Test
	void greavesTakeAFallOfSevenBlocksToNothing() {
		// Vanilla fall damage is the height less 3: 7 blocks deal 4, 8 deal 5.
		assertEquals(0, GearDamageRules.incoming(4, fall(), DESIGN), 1e-6);
		assertEquals(1, GearDamageRules.incoming(5, fall(), DESIGN), 1e-6);
		assertEquals(0, GearDamageRules.incoming(1, fall(), DESIGN), 1e-6);
	}

	@Test
	void greavesSubtractAfterTheMultipliers() {
		GearDamageRules.Factors factors = new GearDamageRules.Factors(0, 0, 0.5f, 1, 4);
		GearDamageRules.Hit blockedFall = new GearDamageRules.Hit(false, 0, false, true, 1, false, true);
		// 10 halved to 5, then 4 off: 1 (subtracting first would give 3).
		assertEquals(1, GearDamageRules.incoming(10, blockedFall, factors), 1e-6);
	}

	@Test
	void theFullPlateStopsBurningTicksOnly() {
		assertFalse(GearDamageRules.allows(true, true, false, 1));
		assertTrue(GearDamageRules.allows(true, false, false, 1));
		assertTrue(GearDamageRules.allows(false, true, false, 1));
	}

	@Test
	void aFallTheGreavesTookToNothingDoesNotHurt() {
		assertFalse(GearDamageRules.allows(false, false, true, 0));
		assertTrue(GearDamageRules.allows(false, false, true, 0.5f));
		// Without the Greaves vanilla decides.
		assertTrue(GearDamageRules.allows(false, false, false, 0));
	}
}
