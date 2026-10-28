package com.embervoid.boss;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossNamesTest {
	private static final Path LANG = Path.of("src", "main", "resources", "assets", "embervoid", "lang");

	@Test
	void theKeyIsTheBossIdWithDots() {
		assertEquals("boss.embervoid.fallen_sister", BossNames.translationKey(new Identifier("embervoid", "fallen_sister")));
		assertEquals("boss.embervoid.ashen_reach.zone_a", BossNames.translationKey(new Identifier("embervoid", "ashen_reach/zone_a")));
	}

	@Test
	void aBossOfAnotherNamespaceHasItsOwnKey() {
		assertNotEquals(BossNames.translationKey(new Identifier("embervoid", "vampire")),
				BossNames.translationKey(new Identifier("othermod", "vampire")));
	}

	@Test
	void everyModBossHasANameInBothLanguages() throws IOException {
		List<Identifier> bosses = List.of(
				new Identifier("embervoid", "fallen_sister"),
				new Identifier("embervoid", "vampire"),
				new Identifier("embervoid", "ashen_reach/zone_a"),
				new Identifier("embervoid", "ashen_reach/zone_b"),
				new Identifier("embervoid", "ashen_reach/zone_c"),
				new Identifier("embervoid", "ashen_reach/final"));
		for (String language : List.of("en_us", "ru_ru")) {
			JsonObject lang = JsonParser.parseString(Files.readString(LANG.resolve(language + ".json"))).getAsJsonObject();
			for (Identifier boss : bosses) {
				assertTrue(lang.has(BossNames.translationKey(boss)), boss + " has no name in " + language);
			}
		}
	}
}
