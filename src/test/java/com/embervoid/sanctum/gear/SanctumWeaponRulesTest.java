package com.embervoid.sanctum.gear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanctumWeaponRulesTest {
	@Test
	void wideCleaveArcIsNinetyDegreesInFront() {
		assertTrue(SanctumWeaponRules.inArc(2, 0, 3, 90));
		assertTrue(SanctumWeaponRules.inArc(2, 1, 3, 90));
		assertTrue(SanctumWeaponRules.inArc(2, 1.99, 3, 90));
		assertFalse(SanctumWeaponRules.inArc(2, 2.01, 3, 90));
		assertFalse(SanctumWeaponRules.inArc(0, 2, 3, 90));
		assertFalse(SanctumWeaponRules.inArc(-2, 0, 3, 90));
		assertFalse(SanctumWeaponRules.inArc(3.1, 0, 3, 90));
		assertTrue(SanctumWeaponRules.inArc(0, 0, 3, 90));
		// A full circle takes anything in range.
		assertTrue(SanctumWeaponRules.inArc(-2, 0, 3, 360));
	}

	@Test
	void groundSlamRingIsAllAround() {
		assertTrue(SanctumWeaponRules.inRing(2, 2, 3));
		assertTrue(SanctumWeaponRules.inRing(-3, 0, 3));
		assertFalse(SanctumWeaponRules.inRing(2.2, 2.2, 3));
	}
}
