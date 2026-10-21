package com.embervoid.stats;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsTest {
	private static final StatsConfig.Effects EFFECTS = StatsConfig.Effects.DEFAULT;
	private static final StatsConfig.Leveling LEVELING = StatsConfig.Leveling.DEFAULT;

	private static int[] stats(Stat stat, int value) {
		int[] stats = new int[Stat.COUNT];
		Arrays.fill(stats, Stat.BASE);
		stats[stat.ordinal()] = value;
		return stats;
	}

	@Test
	void softCapCurve() {
		assertEquals(0, StatCurve.effectivePoints(10), 1e-9);
		assertEquals(10, StatCurve.effectivePoints(20), 1e-9);
		assertEquals(10.5, StatCurve.effectivePoints(21), 1e-9);
		assertEquals(20, StatCurve.effectivePoints(40), 1e-9);
		assertEquals(20.2, StatCurve.effectivePoints(41), 1e-9);
		assertEquals(31.8, StatCurve.effectivePoints(99), 1e-9);
		assertEquals(-9, StatCurve.effectivePoints(1), 1e-9);
	}

	@Test
	void allTensAreVanilla() {
		DerivedStats d = DerivedStats.of(stats(Stat.VITALITY, 10), EFFECTS, 100, 1.0f);
		assertEquals(20, d.maxHealth());
		assertEquals(100, d.maxStamina());
		assertEquals(1.0f, d.staminaRegenPerTick());
		assertEquals(0, d.knockbackResistance());
		assertEquals(0, d.attackSpeedBonus());
		assertEquals(0, d.armorToughness());
		assertEquals(0, d.luck());
		assertEquals(0, d.fireDamageReduction());
		assertEquals(1, d.flaskHealMultiplier());
		assertEquals(0, d.strengthDamageBonus());
		assertEquals(0, d.dexterityDamageBonus());
	}

	@Test
	void vitalityHealth() {
		assertEquals(30, DerivedStats.of(stats(Stat.VITALITY, 20), EFFECTS, 100, 1).maxHealth(), 1e-4);
		assertEquals(40, DerivedStats.of(stats(Stat.VITALITY, 40), EFFECTS, 100, 1).maxHealth(), 1e-4);
		assertEquals(51.8, DerivedStats.of(stats(Stat.VITALITY, 99), EFFECTS, 100, 1).maxHealth(), 1e-4);
	}

	@Test
	void eachStatOnlyMovesItsOwnValues() {
		DerivedStats dex = DerivedStats.of(stats(Stat.DEXTERITY, 20), EFFECTS, 100, 1);
		assertEquals(0, dex.strengthDamageBonus());
		assertEquals(0.2, dex.dexterityDamageBonus(), 1e-6);
		DerivedStats str = DerivedStats.of(stats(Stat.STRENGTH, 20), EFFECTS, 100, 1);
		assertEquals(0.2, str.strengthDamageBonus(), 1e-6);
		assertEquals(0, str.dexterityDamageBonus());
		assertEquals(0, str.attackSpeedBonus());
	}

	@Test
	void fireReductionIsCapped() {
		StatsConfig.Effects strong = new StatsConfig.Effects(1, 2, 0.02f, 0.005f, 0.02f, 0.02f, 0.005f, 0.02f,
				0.1f, 0.1f, 0.5f, 0.1f);
		assertEquals(0.5f, DerivedStats.of(stats(Stat.RESILIENCE, 99), strong, 100, 1).fireDamageReduction());
	}

	@Test
	void levelFromStats() {
		assertEquals(1, PlayerStats.levelOf(stats(Stat.VITALITY, 10)));
		assertEquals(11, PlayerStats.levelOf(stats(Stat.VITALITY, 20)));
	}

	@Test
	void costMatchesDesignTable() {
		// DESIGN §5.3: cost of the next level.
		assertEquals(26, LEVELING.cost(1));
		assertEquals(95, LEVELING.cost(10));
		assertEquals(200, LEVELING.cost(20));
		assertEquals(335, LEVELING.cost(30));
		assertEquals(695, LEVELING.cost(50));
		assertEquals(2084, LEVELING.cost(99));
		// Totals are given as approximations (~490, ~1 890, ~4 470, ~14 400, ~78 800).
		assertEquals(0, LEVELING.totalCost(1));
		assertEquals(490, LEVELING.totalCost(10), 490 * 0.02);
		assertEquals(1890, LEVELING.totalCost(20), 1890 * 0.02);
		assertEquals(4470, LEVELING.totalCost(30), 4470 * 0.02);
		assertEquals(14400, LEVELING.totalCost(50), 14400 * 0.02);
		assertEquals(78800, LEVELING.totalCost(99), 78800 * 0.02);
	}
}
