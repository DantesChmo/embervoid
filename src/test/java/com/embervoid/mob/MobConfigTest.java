package com.embervoid.mob;

import com.embervoid.config.ConfigFile;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MobConfigTest {
	private static ConfigFile file() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(MobConfig.SECTION);
		return file;
	}

	private static MobConfig parse(String mobsJson) {
		return file().parse(JsonParser.parseString("{\"mobs\": " + mobsJson + "}").getAsJsonObject()).get(MobConfig.SECTION);
	}

	@Test
	void emptySectionGivesTheDefaults() {
		assertEquals(MobConfig.DEFAULT, parse("{}"));
	}

	@Test
	void unlistedTypesScaleByOne() {
		MobConfig.Levels levels = MobConfig.DEFAULT.levels();
		assertEquals(1f, levels.healthScale("othermod:beast"));
		assertEquals(1f, levels.damageScale("othermod:beast"));
		assertEquals(0.75f, levels.healthScale("minecraft:zombie"));
	}

	@Test
	void addedScalesAreReadAndInvalidOnesFallBackToOne() {
		MobConfig.Levels levels = parse("{\"levels\": {\"healthScale\": {\"othermod:beast\": 2.5, \"othermod:ghost\": 0},"
				+ " \"damageScale\": {\"minecraft:zombie\": 3}}}").levels();
		assertEquals(2.5f, levels.healthScale("othermod:beast"));
		assertEquals(1f, levels.healthScale("othermod:ghost"));
		assertEquals(3f, levels.damageScale("minecraft:zombie"));
		assertEquals(0.75f, levels.healthScale("minecraft:zombie"));
	}

	@Test
	void invalidBuiltInEntriesKeepTheirDefaults() {
		MobConfig.Levels levels = parse("{\"levels\": {\"baseLevels\": {\"minecraft:zombie\": 0, \"minecraft:creeper\": 2.5},"
				+ " \"dimensionBonus\": {\"minecraft:the_end\": -2, \"minecraft:the_nether\": 0}}}").levels();
		assertEquals(3, levels.baseLevels().get("minecraft:zombie"));
		assertEquals(6, levels.baseLevels().get("minecraft:creeper"));
		assertEquals(8, levels.dimensionBonus().get("minecraft:the_end"));
		assertEquals(0, levels.dimensionBonus().get("minecraft:the_nether"));
	}

	@Test
	void addedDimensionWithANegativeBonusGetsNone() {
		assertEquals(0, parse("{\"levels\": {\"dimensionBonus\": {\"othermod:sky\": -3}}}").levels().dimensionBonus().get("othermod:sky"));
	}

	@Test
	void difficultyFactorsAreReadAndClamped() {
		MobConfig.Difficulty difficulty = parse("{\"difficulty\": {\"easy\": 0.5, \"normal\": 101, \"hard\": -1}}").difficulty();
		assertEquals(0.5f, difficulty.easy());
		assertEquals(MobConfig.DEFAULT.difficulty().normal(), difficulty.normal());
		assertEquals(MobConfig.DEFAULT.difficulty().hard(), difficulty.hard());
	}

	@Test
	void levelsCanBeTurnedOff() {
		assertFalse(parse("{\"levels\": {\"enabled\": false}}").levels().enabled());
	}

	@Test
	void nonObjectLevelsReadAsDefaults() {
		assertEquals(MobConfig.DEFAULT.levels(), parse("{\"levels\": 5}").levels());
	}

	@Test
	void writtenSectionReadsBackTheSame() {
		ConfigFile file = file();
		ConfigFile.Values values = file.parse(JsonParser.parseString("{\"mobs\": {\"levels\": {\"defaultBaseLevel\": 7,"
				+ " \"blocksPerLevel\": 500, \"baseLevels\": {\"othermod:beast\": 30}, \"healthScale\": {\"othermod:beast\": 1.5}},"
				+ " \"difficulty\": {\"hard\": 2}}}").getAsJsonObject());
		MobConfig read = values.get(MobConfig.SECTION);
		assertEquals(30, read.levels().baseLevels().get("othermod:beast"));
		assertEquals(read, file.parse(file.toJson(values)).get(MobConfig.SECTION));
	}
}
