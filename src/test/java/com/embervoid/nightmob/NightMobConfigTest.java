package com.embervoid.nightmob;

import com.embervoid.config.ConfigFile;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NightMobConfigTest {
	private static NightMobConfig parse(String section) {
		ConfigFile file = new ConfigFile("test.json");
		file.register(NightMobConfig.SECTION);
		return file.parse(JsonParser.parseString("{\"nightMobs\": " + section + "}").getAsJsonObject()).get(NightMobConfig.SECTION);
	}

	@Test
	void emptySectionGivesTheDefaults() {
		assertEquals(NightMobConfig.DEFAULT, parse("{}"));
	}

	@Test
	void maxDistanceBelowTheMinFallsBackToAtLeastTheMin() {
		NightMobConfig config = parse("{\"spawnMinDistance\": 48, \"spawnMaxDistance\": 20}");
		assertEquals(48, config.spawnMinDistance());
		assertEquals(48, config.spawnMaxDistance());
	}

	@Test
	void missingMaxDistanceKeepsTheDefaultWhenItIsAboveTheMin() {
		NightMobConfig config = parse("{\"spawnMinDistance\": 8}");
		assertEquals(8, config.spawnMinDistance());
		assertEquals(NightMobConfig.DEFAULT.spawnMaxDistance(), config.spawnMaxDistance());
	}

	@Test
	void ringAlwaysHasRoomForEveryValidMin() {
		for (int min = 1; min <= 64; min++) {
			NightMobConfig config = parse("{\"spawnMinDistance\": " + min + ", \"spawnMaxDistance\": 1}");
			assertTrue(config.spawnMaxDistance() >= config.spawnMinDistance(), "min " + min);
			assertTrue(config.spawnMaxDistance() <= 64, "min " + min);
		}
	}

	@Test
	void missChanceIsAShare() {
		assertEquals(NightMobConfig.DEFAULT.ghostMissChance(), parse("{\"ghostMissChance\": 1.5}").ghostMissChance());
		assertEquals(1f, parse("{\"ghostMissChance\": 1}").ghostMissChance());
	}

	@Test
	void writtenConfigReadsBackTheSame() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(NightMobConfig.SECTION);
		ConfigFile.Values values = file.parse(JsonParser.parseString(
				"{\"nightMobs\": {\"maxZombies\": 4, \"ghostCount\": 0, \"spawnMinDistance\": 10, \"spawnMaxDistance\": 12}}")
				.getAsJsonObject());
		assertEquals(values.get(NightMobConfig.SECTION), file.parse(file.toJson(values)).get(NightMobConfig.SECTION));
	}
}
