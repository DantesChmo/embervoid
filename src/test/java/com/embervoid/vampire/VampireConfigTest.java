package com.embervoid.vampire;

import com.embervoid.config.ConfigFile;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VampireConfigTest {
	private static ConfigFile file() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(VampireConfig.SECTION);
		return file;
	}

	private static VampireConfig parse(String vampireJson) {
		return file().parse(JsonParser.parseString("{\"vampire\": " + vampireJson + "}").getAsJsonObject()).get(VampireConfig.SECTION);
	}

	@Test
	void emptySectionGivesTheDefaults() {
		assertEquals(VampireConfig.SECTION.defaults(), parse("{}"));
	}

	@Test
	void validValuesAreRead() {
		assertEquals(new VampireConfig(2.5f, 100, 2, 5, 20, 30, 4, 0.25f), parse("{\"provokeMinutes\": 2.5, \"groupRadius\": 100,"
				+ " \"groupMin\": 2, \"groupMax\": 5, \"bossMinLevel\": 20, \"bossCapNoRealm\": 30, \"firstKillShards\": 4,"
				+ " \"repeatShardChance\": 0.25}"));
	}

	@Test
	void groupMaxIsNeverBelowGroupMin() {
		VampireConfig config = parse("{\"groupMin\": 4, \"groupMax\": 2}");
		assertEquals(4, config.groupMin());
		assertEquals(4, config.groupMax());
	}

	@Test
	void outOfRangeValuesFallBackToDefaults() {
		assertEquals(VampireConfig.SECTION.defaults(), parse("{\"provokeMinutes\": 0, \"groupRadius\": 4, \"groupMin\": 0,"
				+ " \"bossMinLevel\": 1000, \"repeatShardChance\": -0.5, \"firstKillShards\": 1.5}"));
	}

	@Test
	void writtenSectionReadsBackTheSame() {
		ConfigFile file = file();
		ConfigFile.Values values = file.parse(JsonParser.parseString("{\"vampire\": {\"provokeMinutes\": 7, \"groupRadius\": 32,"
				+ " \"groupMin\": 1, \"groupMax\": 2, \"bossMinLevel\": 10, \"bossCapNoRealm\": 25, \"firstKillShards\": 0,"
				+ " \"repeatShardChance\": 1}}").getAsJsonObject());
		assertEquals(new VampireConfig(7, 32, 1, 2, 10, 25, 0, 1), file.parse(file.toJson(values)).get(VampireConfig.SECTION));
	}
}
