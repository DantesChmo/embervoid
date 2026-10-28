package com.embervoid.sanctum.gear;

import com.embervoid.config.ConfigFile;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanctumGearConfigTest {
	private static ConfigFile file() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(SanctumGearConfig.SECTION);
		return file;
	}

	private static SanctumGearConfig parse(String gearJson) {
		return file().parse(JsonParser.parseString("{\"sanctumGear\": " + gearJson + "}").getAsJsonObject())
				.get(SanctumGearConfig.SECTION);
	}

	@Test
	void emptySectionGivesTheDefaults() {
		assertEquals(SanctumGearConfig.SECTION.defaults(), parse("{}"));
	}

	@Test
	void validValuesAreRead() {
		assertEquals(new SanctumGearConfig(5, 360, 2, 20, 0, 8, 0, 4, 1200, 9, 0, 1200), parse("{\"cleaveRange\": 5,"
				+ " \"cleaveArc\": 360, \"cleaveMultiplier\": 2, \"cleaveStamina\": 20, \"cleaveCooldown\": 0, \"slamRadius\": 8,"
				+ " \"slamMultiplier\": 0, \"slamKnockback\": 4, \"slamSlownessTicks\": 1200, \"slamSlownessAmplifier\": 9,"
				+ " \"slamStamina\": 0, \"slamCooldown\": 1200}"));
	}

	@Test
	void outOfRangeValuesFallBackToDefaults() {
		assertEquals(SanctumGearConfig.SECTION.defaults(), parse("{\"cleaveRange\": 0.5, \"cleaveArc\": 5, \"slamRadius\": 9,"
				+ " \"slamKnockback\": 5, \"slamSlownessAmplifier\": 10, \"slamCooldown\": -1, \"cleaveCooldown\": 2.5}"));
	}

	@Test
	void writtenSectionReadsBackTheSame() {
		ConfigFile file = file();
		ConfigFile.Values values = file.parse(JsonParser.parseString("{\"sanctumGear\": {\"cleaveRange\": 4.5, \"slamCooldown\": 30,"
				+ " \"slamSlownessAmplifier\": 2}}").getAsJsonObject());
		assertEquals(values.get(SanctumGearConfig.SECTION), file.parse(file.toJson(values)).get(SanctumGearConfig.SECTION));
	}
}
