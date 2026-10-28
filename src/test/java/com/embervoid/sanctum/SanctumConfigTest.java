package com.embervoid.sanctum;

import com.embervoid.config.ConfigFile;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanctumConfigTest {
	private static ConfigFile file() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(SanctumConfig.SECTION);
		return file;
	}

	private static SanctumConfig parse(String sanctumJson) {
		return file().parse(JsonParser.parseString("{\"sanctum\": " + sanctumJson + "}").getAsJsonObject()).get(SanctumConfig.SECTION);
	}

	@Test
	void emptySectionGivesTheDefaults() {
		assertEquals(SanctumConfig.SECTION.defaults(), parse("{}"));
	}

	@Test
	void validValuesAreRead() {
		assertEquals(new SanctumConfig(1, 0, 0.5f, 0.75f, 8, 1, 0), parse("{\"mapChanceCartographer\": 1, \"mapChanceTemple\": 0,"
				+ " \"mapChanceRuinedPortal\": 0.5, \"mapChanceShipwreck\": 0.75, \"mapSearchRadius\": 8, \"hallGuardChance\": 1,"
				+ " \"hallArcherChance\": 0}"));
	}

	@Test
	void chancesOutsideZeroToOneAndTinySearchRadiusFallBack() {
		assertEquals(SanctumConfig.SECTION.defaults(), parse("{\"mapChanceCartographer\": 1.1, \"mapChanceTemple\": -0.1,"
				+ " \"mapSearchRadius\": 7, \"hallGuardChance\": \"often\", \"hallArcherChance\": true}"));
	}

	@Test
	void writtenSectionReadsBackTheSame() {
		ConfigFile file = file();
		ConfigFile.Values values = file.parse(JsonParser.parseString("{\"sanctum\": {\"mapChanceCartographer\": 0.5,"
				+ " \"mapSearchRadius\": 120, \"hallGuardChance\": 0.9}}").getAsJsonObject());
		SanctumConfig read = values.get(SanctumConfig.SECTION);
		assertEquals(120, read.mapSearchRadius());
		assertEquals(read, file.parse(file.toJson(values)).get(SanctumConfig.SECTION));
	}
}
