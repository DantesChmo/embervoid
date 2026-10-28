package com.embervoid.mob;

import com.embervoid.config.ConfigFile;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MobConfigTest {
	private static final MobConfig.Levels DEFAULT_LEVELS = MobConfig.DEFAULT.levels();

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
	}

	@Test
	void addedScalesAreReadAlongsideTheBuiltInOnes() {
		MobConfig.Levels levels = parse("{\"levels\": {\"healthScale\": {\"othermod:beast\": 2.5},"
				+ " \"damageScale\": {\"minecraft:zombie\": 3}}}").levels();
		assertEquals(2.5f, levels.healthScale("othermod:beast"));
		assertEquals(3f, levels.damageScale("minecraft:zombie"));
		assertEquals(DEFAULT_LEVELS.healthScale("minecraft:zombie"), levels.healthScale("minecraft:zombie"));
	}

	@Test
	void invalidBuiltInEntriesResetToTheirDefaults() {
		MobConfig.Levels levels = parse("{\"levels\": {\"baseLevels\": {\"minecraft:zombie\": 0, \"minecraft:creeper\": 2.5},"
				+ " \"dimensionBonus\": {\"minecraft:the_end\": -2}, \"healthScale\": {\"minecraft:witch\": 0}}}").levels();
		assertEquals(DEFAULT_LEVELS.baseLevels().get("minecraft:zombie"), levels.baseLevels().get("minecraft:zombie"));
		assertEquals(DEFAULT_LEVELS.baseLevels().get("minecraft:creeper"), levels.baseLevels().get("minecraft:creeper"));
		assertEquals(DEFAULT_LEVELS.dimensionBonus().get("minecraft:the_end"), levels.dimensionBonus().get("minecraft:the_end"));
		assertEquals(DEFAULT_LEVELS.healthScale("minecraft:witch"), levels.healthScale("minecraft:witch"));
	}

	@Test
	void invalidAddedEntriesResetToWhatAnUnlistedKeyGets() {
		MobConfig.Levels levels = parse("{\"levels\": {\"baseLevels\": {\"othermod:beast\": 0},"
				+ " \"dimensionBonus\": {\"othermod:sky\": -3}, \"healthScale\": {\"othermod:ghost\": 0}}}").levels();
		assertEquals(DEFAULT_LEVELS.defaultBaseLevel(), levels.baseLevels().get("othermod:beast"));
		assertEquals(0, levels.dimensionBonus().get("othermod:sky"));
		assertEquals(1f, levels.healthScale("othermod:ghost"));
	}

	@Test
	void difficultyFactorsOutsideTheirRangeResetToDefaults() {
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
