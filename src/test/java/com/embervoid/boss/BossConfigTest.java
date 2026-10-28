package com.embervoid.boss;

import com.embervoid.config.ConfigFile;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossConfigTest {
	private static ConfigFile file() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(BossConfig.SECTION);
		return file;
	}

	private static BossConfig parse(String bossJson) {
		return file().parse(JsonParser.parseString("{\"boss\": " + bossJson + "}").getAsJsonObject()).get(BossConfig.SECTION);
	}

	@Test
	void emptySectionGivesTheDefaults() {
		assertEquals(BossConfig.SECTION.defaults(), parse("{}"));
	}

	@Test
	void validValuesAreRead() {
		BossConfig config = parse("{\"healthPerExtraFighter\": 1.0, \"addsPerExtraFighter\": 2, \"addLevelOffset\": -4,"
				+ " \"targetSwitchMinSeconds\": 3, \"targetSwitchMaxSeconds\": 6, \"repeatSoulChance\": 0.5}");
		assertEquals(new BossConfig(1.0f, 2, -4, 3, 6, 0.5f), config);
	}

	@Test
	void longestSwitchIsNeverShorterThanTheShortest() {
		BossConfig config = parse("{\"targetSwitchMinSeconds\": 30, \"targetSwitchMaxSeconds\": 5}");
		assertEquals(30, config.targetSwitchMinSeconds());
		assertEquals(30, config.targetSwitchMaxSeconds());
	}

	@Test
	void outOfRangeValuesFallBackToDefaults() {
		BossConfig defaults = BossConfig.SECTION.defaults();
		BossConfig config = parse("{\"healthPerExtraFighter\": -1, \"addsPerExtraFighter\": 11, \"repeatSoulChance\": 1.5,"
				+ " \"targetSwitchMinSeconds\": 0.1, \"addLevelOffset\": \"three\"}");
		assertEquals(defaults, config);
	}

	@Test
	void writtenSectionReadsBackTheSame() {
		ConfigFile file = file();
		ConfigFile.Values values = file.parse(JsonParser.parseString("{\"boss\": {\"healthPerExtraFighter\": 0.75,"
				+ " \"addsPerExtraFighter\": 3, \"addLevelOffset\": 5, \"targetSwitchMinSeconds\": 4, \"targetSwitchMaxSeconds\": 9,"
				+ " \"repeatSoulChance\": 0.1}}").getAsJsonObject());
		assertEquals(new BossConfig(0.75f, 3, 5, 4, 9, 0.1f), file.parse(file.toJson(values)).get(BossConfig.SECTION));
	}
}
