package com.embervoid.reach.gear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponRulesTest {
	@Test
	void chargeFactorMatchesVanilla() {
		assertEquals(0.2f, WeaponRules.chargeFactor(0), 1e-6);
		assertEquals(1.0f, WeaponRules.chargeFactor(1), 1e-6);
		assertEquals(0.4f, WeaponRules.chargeFactor(0.5f), 1e-6);
		assertTrue(WeaponRules.fullyCharged(0.95f));
		assertFalse(WeaponRules.fullyCharged(0.9f));
	}

	@Test
	void kindlingScalesTheEmberPart() {
		// KIN 20: 10 effective points, 2 % each.
		assertEquals(1.2f, WeaponRules.kindlingMultiplier(10, 0.02f, 1), 1e-6);
		// The Rapier's ember part is 0.3 of the hit.
		assertEquals(1.06f, WeaponRules.kindlingMultiplier(10, 0.02f, 0.3f), 1e-6);
		assertEquals(1.0f, WeaponRules.kindlingMultiplier(0, 0.02f, 1), 1e-6);
	}

	@Test
	void ashWaveIsSixLongAndThreeWide() {
		assertTrue(WeaponRules.inWave(5.9, 1.4, 6, 3));
		assertFalse(WeaponRules.inWave(6.1, 0, 6, 3));
		assertFalse(WeaponRules.inWave(3, 1.6, 6, 3));
		assertFalse(WeaponRules.inWave(-0.5, 0, 6, 3));
	}

	@Test
	void slideSpeedCoversTheDistance() {
		double speed = WeaponRules.slideSpeed(4);
		double covered = 0;
		for (int tick = 0; tick < 200; tick++) {
			covered += speed;
			speed *= 0.546;
		}
		assertEquals(4, covered, 1e-6);
	}

	@Test
	void rapierRampsTenPercentAHitUpToThirty() {
		assertEquals(1.0f, WeaponRules.rampMultiplier(0, 0.1f, 0.3f), 1e-6);
		assertEquals(1.1f, WeaponRules.rampMultiplier(1, 0.1f, 0.3f), 1e-6);
		assertEquals(1.2f, WeaponRules.rampMultiplier(2, 0.1f, 0.3f), 1e-6);
		assertEquals(1.3f, WeaponRules.rampMultiplier(3, 0.1f, 0.3f), 1e-6);
		assertEquals(1.3f, WeaponRules.rampMultiplier(7, 0.1f, 0.3f), 1e-6);
		assertTrue(WeaponRules.keepsRamp(30, 30));
		assertFalse(WeaponRules.keepsRamp(31, 30));
	}

	@Test
	void boltDealsItsDamageAtLaunchSpeed() {
		double base = WeaponRules.boltBaseDamage(16, WeaponRules.CROSSBOW_ARROW_SPEED);
		// Vanilla: ceil(speed × base), the speed a little off by the launch's spread.
		for (double speed : new double[]{3.10, 3.15, 3.20}) {
			assertEquals(16, (int) Math.ceil(speed * base));
		}
	}

	@Test
	void chargeFactorGrowsWithCharge() {
		float previous = WeaponRules.chargeFactor(0);
		for (int step = 1; step <= 20; step++) {
			float factor = WeaponRules.chargeFactor(step / 20f);
			assertTrue(factor > previous, "at " + step / 20f);
			previous = factor;
		}
	}

	@Test
	void kindlingBelowTenWeakensTheEmberPartButNeverBelowZero() {
		assertEquals(0.9f, WeaponRules.kindlingMultiplier(-5, 0.02f, 1), 1e-6);
		assertEquals(0.97f, WeaponRules.kindlingMultiplier(-5, 0.02f, 0.3f), 1e-6);
		assertEquals(0, WeaponRules.kindlingMultiplier(-100, 0.02f, 1), 1e-6);
	}

	@Test
	void waveEdgesAreInside() {
		assertTrue(WeaponRules.inWave(0, 0, 6, 3));
		assertTrue(WeaponRules.inWave(6, 1.5, 6, 3));
		assertTrue(WeaponRules.inWave(6, -1.5, 6, 3));
		assertFalse(WeaponRules.inWave(6, -1.51, 6, 3));
	}

	@Test
	void noSlideNeedsNoSpeed() {
		assertEquals(0, WeaponRules.slideSpeed(0), 1e-12);
		assertEquals(2 * WeaponRules.slideSpeed(3), WeaponRules.slideSpeed(6), 1e-12);
	}

	@Test
	void rampIgnoresNegativeHitCountsAndBackwardGaps() {
		assertEquals(1.0f, WeaponRules.rampMultiplier(-3, 0.1f, 0.3f), 1e-6);
		assertTrue(WeaponRules.keepsRamp(0, 30));
		assertFalse(WeaponRules.keepsRamp(-1, 30));
	}

	@Test
	void boltDamageHoldsThroughTheSpreadForEveryWholeDamageUpToThirty() {
		for (int damage = 1; damage <= 30; damage++) {
			double base = WeaponRules.boltBaseDamage(damage, WeaponRules.CROSSBOW_ARROW_SPEED);
			for (double speed : new double[]{3.10, 3.15, 3.20}) {
				assertEquals(damage, (int) Math.ceil(speed * base), damage + " at speed " + speed);
			}
		}
	}

	@Test
	void boltBaseDamageIsNeverNegative() {
		assertEquals(0, WeaponRules.boltBaseDamage(0, WeaponRules.CROSSBOW_ARROW_SPEED), 1e-12);
		assertEquals(0, WeaponRules.boltBaseDamage(0.25f, WeaponRules.CROSSBOW_ARROW_SPEED), 1e-12);
	}
}
