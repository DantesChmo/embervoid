package com.embervoid.stats;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatTest {
	@Test
	void byIdTakesLongAndShortIdsInAnyCase() {
		for (Stat stat : Stat.values()) {
			assertEquals(stat, Stat.byId(stat.id()));
			assertEquals(stat, Stat.byId(stat.shortId()));
			assertEquals(stat, Stat.byId(stat.id().toUpperCase()));
			assertEquals(stat, Stat.byId(stat.shortId().toUpperCase()));
		}
		assertEquals(Stat.FORTUNE, Stat.byId("for"));
		assertEquals(Stat.KINDLING, Stat.byId("Kindling"));
	}

	@Test
	void unknownIdsAreNull() {
		assertNull(Stat.byId("luck"));
		assertNull(Stat.byId(""));
		assertNull(Stat.byId("vi"));
	}

	@Test
	void noTwoStatsShareAnId() {
		Set<String> ids = new HashSet<>();
		for (Stat stat : Stat.values()) {
			assertTrue(ids.add(stat.id()), stat.id());
			assertTrue(ids.add(stat.shortId()), stat.shortId());
			assertEquals(3, stat.shortId().length());
		}
	}

	@Test
	void namesAndLevelUpMessagesAreTranslatedInBothLanguages() throws IOException {
		for (String lang : List.of("en_us", "ru_ru")) {
			JsonObject keys = JsonParser.parseString(Files.readString(
					Path.of("src/main/resources/assets/embervoid/lang/" + lang + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
			for (Stat stat : Stat.values()) {
				assertHas(keys, lang, key(stat.displayName().getContent()));
				assertHas(keys, lang, key(stat.shortName().getContent()));
			}
			for (PlayerStats.LevelUpResult result : PlayerStats.LevelUpResult.values()) {
				assertHas(keys, lang, key(result.message().getContent()));
			}
		}
	}

	private static String key(Object content) {
		return ((TranslatableTextContent) content).getKey();
	}

	private static void assertHas(JsonObject keys, String lang, String key) {
		assertTrue(keys.has(key), lang + " lacks " + key);
	}
}
