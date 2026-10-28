package com.embervoid.reach.gear;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;
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
		// The speed is a little off 3.15 by the launch's spread.
		for (double speed : new double[]{3.10, 3.15, 3.20}) {
			assertEquals(16, WeaponRules.boltHitDamage(16, speed));
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
	void aLongerSlideNeedsMoreSpeedAndNoSlideNone() {
		assertEquals(0, WeaponRules.slideSpeed(0), 1e-12);
		double previous = 0;
		for (double blocks = 0.5; blocks <= 12; blocks += 0.5) {
			double speed = WeaponRules.slideSpeed(blocks);
			assertTrue(speed > previous, "at " + blocks);
			previous = speed;
		}
	}

	@Test
	void rampIgnoresNegativeHitCountsAndBackwardGaps() {
		assertEquals(1.0f, WeaponRules.rampMultiplier(-3, 0.1f, 0.3f), 1e-6);
		assertTrue(WeaponRules.keepsRamp(0, 30));
		assertFalse(WeaponRules.keepsRamp(-1, 30));
	}

	@Test
	void aBoltDealsItsConfiguredDamageThroughTheSpreadOverTheWholeConfigRange() {
		for (int damage = 1; damage <= maxConfigBoltDamage(); damage++) {
			for (double speed : new double[]{3.10, 3.15, 3.20}) {
				assertEquals(damage, WeaponRules.boltHitDamage(damage, speed), damage + " at speed " + speed);
			}
		}
	}

	@Test
	void aSlowedBoltDealsLessAndNeverMoreThanItsDamage() {
		for (int damage : new int[]{7, 16, 40, 200}) {
			int previous = 0;
			for (double speed = 0; speed <= 4; speed += 0.05) {
				int dealt = WeaponRules.boltHitDamage(damage, speed);
				assertTrue(dealt >= previous && dealt <= damage, damage + " at speed " + speed + ": " + dealt);
				previous = dealt;
			}
		}
		// Half the launch speed, about half the damage.
		assertEquals(8, WeaponRules.boltHitDamage(16, 1.55), 1);
		assertEquals(0, WeaponRules.boltHitDamage(16, 0));
	}

	/** The highest whole boltDamage the gear config accepts. */
	private static int maxConfigBoltDamage() {
		ConfigSection<GearConfig> section = GearConfig.SECTION;
		int accepted = 0;
		for (int damage = 1; damage <= 100_000; damage++) {
			EmbervoidConfig config = EmbervoidConfig.fromNetworkString("{\"gear\": {\"crossbow\": {\"boltDamage\": " + damage + "}}}");
			if (config.section(section).crossbow().boltDamage() != damage) {
				break;
			}
			accepted = damage;
		}
		assertTrue(accepted > 1, "the config accepts no bolt damage above 1");
		return accepted;
	}

	@Test
	void aBoltWithoutDamageDealsNone() {
		assertEquals(0, WeaponRules.boltHitDamage(0, WeaponRules.CROSSBOW_ARROW_SPEED));
		assertEquals(0, WeaponRules.boltHitDamage(0, -1));
	}
}
