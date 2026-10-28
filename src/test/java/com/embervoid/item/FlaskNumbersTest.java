package com.embervoid.item;

import com.embervoid.config.EmbervoidConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Flask charges and heal (CORE §2.3): base charges plus Flask Shards up to the cap; heal plus upgrades, scaled. */
class FlaskNumbersTest {
	private static final EmbervoidConfig.Flask THREE_TO_EIGHT = new EmbervoidConfig.Flask(3, 8, 8.0f, 32);

	@Test
	void aFreshPlayerHasTheBaseCharges() {
		assertEquals(3, FlaskNumbers.maxCharges(THREE_TO_EIGHT, 0));
	}

	@Test
	void eachShardAddsACharge() {
		assertEquals(4, FlaskNumbers.maxCharges(THREE_TO_EIGHT, 1));
		assertEquals(7, FlaskNumbers.maxCharges(THREE_TO_EIGHT, 4));
	}

	@Test
	void chargesStopAtTheCap() {
		assertEquals(8, FlaskNumbers.maxCharges(THREE_TO_EIGHT, 5));
		assertEquals(8, FlaskNumbers.maxCharges(THREE_TO_EIGHT, 40));
	}

	@Test
	void aCapAtTheBaseAllowsNoUpgrade() {
		assertEquals(5, FlaskNumbers.maxCharges(new EmbervoidConfig.Flask(5, 5, 8.0f, 32), 3));
	}

	@Test
	void drinkableChargesStayWithinZeroAndTheCap() {
		assertEquals(2, FlaskNumbers.charges(2, 5));
		assertEquals(5, FlaskNumbers.charges(9, 5));
		assertEquals(0, FlaskNumbers.charges(-1, 5));
	}

	@Test
	void theHealIsTheConfigAmountWithoutUpgradesOrMultipliers() {
		assertEquals(8.0f, FlaskNumbers.healAmount(THREE_TO_EIGHT, 0, 2.0f, 1.0));
	}

	@Test
	void eachHealUpgradeAddsItsAmount() {
		// 8 + 3 × 2
		assertEquals(14.0f, FlaskNumbers.healAmount(THREE_TO_EIGHT, 3, 2.0f, 1.0), 1e-5);
	}

	@Test
	void theMultiplierScalesTheWholeHealUpgradesIncluded() {
		// (8 + 1 × 2) × 1.2: Kindling raises the upgraded heal too.
		assertEquals(12.0f, FlaskNumbers.healAmount(THREE_TO_EIGHT, 1, 2.0f, 1.2), 1e-5);
	}

	@Test
	void moreUpgradesNeverHealLess() {
		float previous = 0;
		for (int upgrades = 0; upgrades <= 10; upgrades++) {
			float heal = FlaskNumbers.healAmount(THREE_TO_EIGHT, upgrades, 2.0f, 1.1);
			assertTrue(heal > previous, "upgrades " + upgrades);
			previous = heal;
		}
	}
}
