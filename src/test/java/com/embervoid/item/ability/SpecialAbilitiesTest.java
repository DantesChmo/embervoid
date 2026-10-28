package com.embervoid.item.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The tooltip's cost and cooldown numbers: whole numbers without a decimal point, the rest as they are. */
class SpecialAbilitiesTest {
	@Test
	void wholeNumbersHaveNoDecimals() {
		assertEquals("30", SpecialAbilities.formatNumber(30));
		assertEquals("2", SpecialAbilities.formatNumber(40 / 20.0f));
		assertEquals("0", SpecialAbilities.formatNumber(0));
	}

	@Test
	void fractionsKeepThem() {
		// Cooldowns in seconds: 30 ticks and 8 ticks.
		assertEquals("1.5", SpecialAbilities.formatNumber(30 / 20.0f));
		assertEquals("0.4", SpecialAbilities.formatNumber(8 / 20.0f));
		assertEquals("12.5", SpecialAbilities.formatNumber(12.5f));
	}
}
