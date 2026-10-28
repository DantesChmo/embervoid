package com.embervoid.stats;

import com.embervoid.config.ConfigFile;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelingTest {
	private static final StatsConfig.Leveling LEVELING = StatsConfig.Leveling.DEFAULT;

	private static ConfigFile.Values parse(String json) {
		ConfigFile file = new ConfigFile("test.json");
		file.register(StatsConfig.Effects.SECTION);
		file.register(StatsConfig.Leveling.SECTION);
		return file.parse(JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	void eachLevelCostsMoreThanTheLast() {
		for (int level = 1; level < 500; level++) {
			assertTrue(LEVELING.cost(level + 1) > LEVELING.cost(level), "level " + level);
		}
	}

	@Test
	void levelsBelowOneCostAsLevelOne() {
		assertEquals(LEVELING.cost(1), LEVELING.cost(0));
		assertEquals(LEVELING.cost(1), LEVELING.cost(-5));
	}

	@Test
	void totalCostSumsTheSteps() {
		for (int level = 1; level < 60; level++) {
			assertEquals(LEVELING.totalCost(level) + LEVELING.cost(level), LEVELING.totalCost(level + 1), "level " + level);
		}
		assertEquals(0, LEVELING.totalCost(0));
	}

	@Test
	void costIsCappedForHugeCurves() {
		StatsConfig.Leveling steep = new StatsConfig.Leveling(1_000_000, 100_000, 1_000, 999, 0);
		assertEquals(Integer.MAX_VALUE, steep.cost(100_000));
		assertTrue(steep.totalCost(10_000) > Integer.MAX_VALUE);
	}

	@Test
	void costRoundsToWholePoints() {
		assertEquals(10, new StatsConfig.Leveling(10.4f, 0, 0, 99, 0).cost(7));
		assertEquals(11, new StatsConfig.Leveling(10.6f, 0, 0, 99, 0).cost(7));
	}

	@Test
	void statCapCannotGoBelowTheStartingValue() {
		StatsConfig.Leveling leveling = parse("{\"leveling\": {\"statCap\": 5}}").get(StatsConfig.Leveling.SECTION);
		assertEquals(LEVELING.statCap(), leveling.statCap());
		assertEquals(Stat.BASE, parse("{\"leveling\": {\"statCap\": 10}}").get(StatsConfig.Leveling.SECTION).statCap());
	}

	@Test
	void effectsAreReadAndWrittenBack() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(StatsConfig.Effects.SECTION);
		ConfigFile.Values values = file.parse(JsonParser.parseString(
				"{\"stats\": {\"vitalityHealth\": 2, \"fortuneExtraDropCap\": 0.5}}").getAsJsonObject());
		StatsConfig.Effects effects = values.get(StatsConfig.Effects.SECTION);
		assertEquals(2, effects.vitalityHealth());
		assertEquals(0.5f, effects.fortuneExtraDropCap());
		assertEquals(StatsConfig.Effects.DEFAULT.enduranceStamina(), effects.enduranceStamina());
		assertEquals(effects, file.parse(file.toJson(values)).get(StatsConfig.Effects.SECTION));
	}

	@Test
	void sharesAboveOneAreRejected() {
		StatsConfig.Effects effects = parse("{\"stats\": {\"resilienceFireReductionCap\": 1.5, \"fortuneExtraDrop\": 2}}")
				.get(StatsConfig.Effects.SECTION);
		assertEquals(StatsConfig.Effects.DEFAULT.resilienceFireReductionCap(), effects.resilienceFireReductionCap());
		assertEquals(StatsConfig.Effects.DEFAULT.fortuneExtraDrop(), effects.fortuneExtraDrop());
	}
}
