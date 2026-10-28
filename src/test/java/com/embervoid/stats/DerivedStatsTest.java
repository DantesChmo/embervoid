package com.embervoid.stats;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DerivedStatsTest {
	private static final StatsConfig.Effects EFFECTS = StatsConfig.Effects.DEFAULT;

	private static int[] stats(Stat stat, int value) {
		int[] stats = new int[Stat.COUNT];
		Arrays.fill(stats, Stat.BASE);
		stats[stat.ordinal()] = value;
		return stats;
	}

	private static DerivedStats with(Stat stat, int value) {
		return DerivedStats.of(stats(stat, value), EFFECTS, 100, 1.0f);
	}

	@Test
	void gainDropsPastEachSoftCap() {
		for (int cap : new int[]{20, 40}) {
			double below = StatCurve.effectivePoints(cap) - StatCurve.effectivePoints(cap - 1);
			double above = StatCurve.effectivePoints(cap + 1) - StatCurve.effectivePoints(cap);
			assertTrue(above < below, "gain does not drop past " + cap);
		}
	}

	@Test
	void everyPointIsWorthSomethingButNeverMoreThanTheLast() {
		double lastGain = Double.MAX_VALUE;
		for (int value = Stat.MIN; value < 999; value++) {
			double gain = StatCurve.effectivePoints(value + 1) - StatCurve.effectivePoints(value);
			assertTrue(gain > 0, "no gain at " + value);
			assertTrue(gain <= lastGain + 1e-12, "gain rises at " + value);
			lastGain = gain;
		}
	}

	/** Round per-point amounts, so every expectation is worked by hand: 20 is 10 full points, 40 is 20 (DESIGN §5.2). */
	private static final StatsConfig.Effects ROUND = new StatsConfig.Effects(2, 3, 0.05f, 0.01f, 0.04f, 0.04f, 0.02f,
			0.03f, 0.2f, 0.02f, 0.5f, 0.25f, 0.01f, 0.5f);

	private static DerivedStats round(Stat stat, int value, int baseStamina, float baseRegen) {
		return DerivedStats.of(stats(stat, value), ROUND, baseStamina, baseRegen);
	}

	@Test
	void vitalityAddsHealthPerFullPoint() {
		assertEquals(40, round(Stat.VITALITY, 20, 100, 1).maxHealth(), 1e-4);
		assertEquals(60, round(Stat.VITALITY, 40, 100, 1).maxHealth(), 1e-4);
	}

	@Test
	void enduranceGrowsStaminaRegenAndPoise() {
		DerivedStats d = round(Stat.ENDURANCE, 20, 100, 1);
		assertEquals(130, d.maxStamina(), 1e-4);
		assertEquals(1.5f, d.staminaRegenPerTick(), 1e-5);
		assertEquals(0.1, d.knockbackResistance(), 1e-6);
	}

	@Test
	void staminaPoolAndRegenBuildOnTheConfiguredBase() {
		DerivedStats d = round(Stat.ENDURANCE, 20, 50, 0.5f);
		assertEquals(80, d.maxStamina(), 1e-4);
		assertEquals(0.75f, d.staminaRegenPerTick(), 1e-5);
	}

	@Test
	void kindlingStrengthensTheFlask() {
		assertEquals(1.3f, round(Stat.KINDLING, 20, 100, 1).flaskHealMultiplier(), 1e-5);
		assertEquals(1.6f, round(Stat.KINDLING, 40, 100, 1).flaskHealMultiplier(), 1e-5);
	}

	@Test
	void resilienceGivesToughnessAndFireReduction() {
		DerivedStats d = round(Stat.RESILIENCE, 20, 100, 1);
		assertEquals(2.0, d.armorToughness(), 1e-6);
		assertEquals(0.2f, d.fireDamageReduction(), 1e-6);
	}

	@Test
	void dexterityGivesAttackSpeedAndDexDamage() {
		DerivedStats d = round(Stat.DEXTERITY, 20, 100, 1);
		assertEquals(0.2, d.attackSpeedBonus(), 1e-6);
		assertEquals(0.4f, d.dexterityDamageBonus(), 1e-6);
	}

	@Test
	void fortuneGivesLuckAndExtraDrops() {
		DerivedStats d = round(Stat.FORTUNE, 20, 100, 1);
		assertEquals(2.5, d.luck(), 1e-6);
		assertEquals(0.1f, d.extraDropChance(), 1e-6);
	}

	@Test
	void debugLowStatsNeverBreakThePlayer() {
		int[] lowest = new int[Stat.COUNT];
		Arrays.fill(lowest, Stat.MIN);
		StatsConfig.Effects harsh = new StatsConfig.Effects(100, 1_000, 10, 1, 10, 10, 10, 10, 100, 1, 1, 100, 1, 1);
		DerivedStats d = DerivedStats.of(lowest, harsh, 100, 1.0f);
		assertEquals(1, d.maxHealth());
		assertEquals(1, d.maxStamina());
		assertEquals(0, d.staminaRegenPerTick());
		assertEquals(0, d.flaskHealMultiplier());
		assertEquals(0, d.fireDamageReduction());
		assertEquals(0, d.extraDropChance());
	}

	@Test
	void raisingAStatNeverLowersWhatItGives() {
		for (Stat stat : Stat.values()) {
			DerivedStats lower = with(stat, 30);
			DerivedStats higher = with(stat, 31);
			assertTrue(higher.maxHealth() >= lower.maxHealth(), stat.id());
			assertTrue(higher.maxStamina() >= lower.maxStamina(), stat.id());
			assertTrue(higher.staminaRegenPerTick() >= lower.staminaRegenPerTick(), stat.id());
			assertTrue(higher.flaskHealMultiplier() >= lower.flaskHealMultiplier(), stat.id());
			assertTrue(higher.strengthDamageBonus() >= lower.strengthDamageBonus(), stat.id());
			assertTrue(higher.dexterityDamageBonus() >= lower.dexterityDamageBonus(), stat.id());
			assertTrue(higher.fireDamageReduction() >= lower.fireDamageReduction(), stat.id());
			assertTrue(higher.extraDropChance() >= lower.extraDropChance(), stat.id());
		}
	}

	@Test
	void levelCountsEveryPointAboveTheStart() {
		int[] stats = new int[Stat.COUNT];
		Arrays.fill(stats, Stat.BASE);
		stats[Stat.VITALITY.ordinal()] = 15;
		stats[Stat.FORTUNE.ordinal()] = 12;
		assertEquals(8, PlayerStats.levelOf(stats));
		stats[Stat.STRENGTH.ordinal()] = 9;
		assertEquals(7, PlayerStats.levelOf(stats));
	}
}
