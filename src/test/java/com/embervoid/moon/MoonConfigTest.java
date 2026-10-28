package com.embervoid.moon;

import com.embervoid.config.ConfigFile;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MoonConfigTest {
	private static MoonConfig parse(String json) {
		ConfigFile file = new ConfigFile("test.json");
		file.register(MoonConfig.SECTION);
		return file.parse(JsonParser.parseString(json).getAsJsonObject()).get(MoonConfig.SECTION);
	}

	@Test
	void emptyFileGivesTheDefaults() {
		assertEquals(MoonConfig.DEFAULT, parse("{}"));
	}

	@Test
	void validValuesAreRead() {
		MoonConfig config = parse("{\"moon\": {\"enabled\": false, \"cycleInterval\": 1, \"fallbackMinutes\": 2.5,"
				+ " \"fullMoon\": {\"levelBonus\": 7, \"followRange\": 3}, \"blood\": {\"vampirism\": 0.5}}}");
		assertFalse(config.enabled());
		assertEquals(1, config.cycleInterval());
		assertEquals(2.5f, config.fallbackMinutes());
		assertEquals(new MoonConfig.FullMoon(7, 3), config.fullMoon());
		assertEquals(new MoonConfig.Blood(0.5f), config.blood());
	}

	@Test
	void aZeroIntervalWouldNeverScheduleAndFallsBack() {
		assertEquals(MoonConfig.DEFAULT.cycleInterval(), parse("{\"moon\": {\"cycleInterval\": 0}}").cycleInterval());
	}

	@Test
	void followRangeCannotShrink() {
		assertEquals(MoonConfig.DEFAULT.fullMoon().followRange(),
				parse("{\"moon\": {\"fullMoon\": {\"followRange\": 0.5}}}").fullMoon().followRange());
	}

	@Test
	void fallbackMinutesBecomeServerTicks() {
		assertEquals(1_800, parse("{\"moon\": {\"fallbackMinutes\": 1.5}}").fallbackTicks());
	}

	@Test
	void writtenConfigReadsBackTheSame() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(MoonConfig.SECTION);
		ConfigFile.Values values = file.parse(JsonParser.parseString(
				"{\"moon\": {\"cycleInterval\": 5, \"blood\": {\"vampirism\": 1.25}}}").getAsJsonObject());
		assertEquals(values.get(MoonConfig.SECTION), file.parse(file.toJson(values)).get(MoonConfig.SECTION));
	}
}
