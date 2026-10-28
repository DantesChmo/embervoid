package com.embervoid.moon;

import com.embervoid.moon.api.MoonNight;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoonNightTest {
	@Test
	void byOrdinalRoundTripsAndFallsBackToNone() {
		for (MoonNight night : MoonNight.values()) {
			assertEquals(night, MoonNight.byOrdinal(night.ordinal()));
		}
		assertEquals(MoonNight.NONE, MoonNight.byOrdinal(-1));
		assertEquals(MoonNight.NONE, MoonNight.byOrdinal(MoonNight.values().length));
	}

	@Test
	void onlyNoneIsNoEvent() {
		assertFalse(MoonNight.NONE.isEvent());
		assertTrue(MoonNight.FULL_MOON.isEvent());
		assertTrue(MoonNight.BLOOD_NEW_MOON.isEvent());
	}

	@Test
	void idsAreLowerCaseNames() {
		assertEquals("full_moon", MoonNight.FULL_MOON.id());
		assertEquals("blood_new_moon", MoonNight.BLOOD_NEW_MOON.id());
	}

	@Test
	void everyEventNightHasItsAnnouncementsInEveryLanguage() throws IOException {
		for (String lang : List.of("en_us", "ru_ru", "de_de", "fr_fr", "zh_cn")) {
			JsonObject keys = JsonParser.parseString(Files.readString(
					Path.of("src/main/resources/assets/embervoid/lang/" + lang + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
			for (MoonNight night : MoonNight.values()) {
				if (!night.isEvent()) {
					continue;
				}
				for (String part : List.of("title", "subtitle", "started", "ended")) {
					String key = "embervoid.moon." + part + "." + night.id();
					assertTrue(keys.has(key), lang + " lacks " + key);
				}
			}
		}
	}
}
