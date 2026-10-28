package com.embervoid.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileTest {
	record Test1(int count, boolean on) {}

	private static ConfigSection<Test1> section(String name) {
		return ConfigSection.of(name, new Test1(5, true),
				in -> new Test1(in.readInt("count", 5, 0, 10), in.readBoolean("on", true)),
				(value, out) -> {
					out.addProperty("count", value.count());
					out.addProperty("on", value.on());
				});
	}

	private static JsonObject readFile(Path path) throws IOException {
		return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	@Test
	void firstLoadWritesDefaults(@TempDir Path dir) throws IOException {
		ConfigFile file = new ConfigFile("test.json");
		ConfigSection<Test1> a = file.register(section("a"));
		file.load(dir);
		assertEquals(new Test1(5, true), file.values().get(a));
		assertEquals(5, readFile(dir.resolve("test.json")).getAsJsonObject("a").get("count").getAsInt());
	}

	@Test
	void missingKeysAreFilledAndInvalidValuesReplaced(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("test.json"), "{\"a\": {\"count\": 42}}");
		ConfigFile file = new ConfigFile("test.json");
		ConfigSection<Test1> a = file.register(section("a"));
		file.load(dir);
		assertEquals(new Test1(5, true), file.values().get(a));
		JsonObject written = readFile(dir.resolve("test.json")).getAsJsonObject("a");
		assertEquals(5, written.get("count").getAsInt());
		assertTrue(written.get("on").getAsBoolean());
	}

	@Test
	void validValuesAreKept(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("test.json"), "{\"a\": {\"count\": 7, \"on\": false}}");
		ConfigFile file = new ConfigFile("test.json");
		ConfigSection<Test1> a = file.register(section("a"));
		file.load(dir);
		assertEquals(new Test1(7, false), file.values().get(a));
	}

	@Test
	void brokenFileIsBackedUpAndReplaced(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("test.json"), "{ not json");
		ConfigFile file = new ConfigFile("test.json");
		ConfigSection<Test1> a = file.register(section("a"));
		file.load(dir);
		assertEquals(new Test1(5, true), file.values().get(a));
		assertTrue(Files.exists(dir.resolve("test.json.broken")));
		assertEquals(5, readFile(dir.resolve("test.json")).getAsJsonObject("a").get("count").getAsInt());
	}

	@Test
	void sectionRegisteredAfterLoadIsReadAndWritten(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("test.json"), "{\"a\": {\"count\": 1, \"on\": true}, \"b\": {\"count\": 9}}");
		ConfigFile file = new ConfigFile("test.json");
		file.register(section("a"));
		file.load(dir);
		// "b" is unknown so far, but must survive the rewrite.
		ConfigSection<Test1> b = file.register(section("b"));
		assertEquals(new Test1(9, true), file.values().get(b));
		JsonObject written = readFile(dir.resolve("test.json"));
		assertEquals(1, written.getAsJsonObject("a").get("count").getAsInt());
		assertTrue(written.getAsJsonObject("b").get("on").getAsBoolean());
	}

	@Test
	void unknownSectionsSurviveRewrites(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("test.json"), "{\"other\": {\"x\": 1}}");
		ConfigFile file = new ConfigFile("test.json");
		file.register(section("a"));
		file.load(dir);
		assertEquals(1, readFile(dir.resolve("test.json")).getAsJsonObject("other").get("x").getAsInt());
	}

	@Test
	void keysListsTheObjectInFileOrder() {
		ConfigFile file = new ConfigFile("test.json");
		ConfigSection<java.util.List<String>> keys = file.register(ConfigSection.of("map", java.util.List.of(),
				in -> in.keys(), (value, out) -> value.forEach(key -> out.addProperty(key, 1))));
		ConfigFile.Values values = file.parse(JsonParser.parseString(
				"{\"map\": {\"modded:beast\": 5, \"minecraft:zombie\": 3}}").getAsJsonObject());
		assertEquals(java.util.List.of("modded:beast", "minecraft:zombie"), values.get(keys));
		assertEquals(java.util.List.of(), file.parse(new JsonObject()).get(keys));
	}

	@Test
	void mobLevelsAcceptAddedEntitiesAndDimensions() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(com.embervoid.mob.MobConfig.SECTION);
		com.embervoid.mob.MobConfig.Levels levels = file.parse(JsonParser.parseString("{\"mobs\": {\"levels\": {"
				+ "\"defaultBaseLevel\": 6,"
				+ "\"baseLevels\": {\"othermod:beast\": 12, \"othermod:broken\": \"x\"},"
				+ "\"dimensionBonus\": {\"othermod:deep\": 7}}}}").getAsJsonObject())
				.get(com.embervoid.mob.MobConfig.SECTION).levels();
		assertEquals(12, levels.baseLevels().get("othermod:beast"));
		assertEquals(6, levels.baseLevels().get("othermod:broken"));
		assertEquals(7, levels.dimensionBonus().get("othermod:deep"));
		assertTrue(levels.baseLevels().keySet().containsAll(com.embervoid.mob.MobConfig.DEFAULT.levels().baseLevels().keySet()));
	}

	@Test
	void duplicateNamesAreRejected() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(section("a"));
		assertThrows(IllegalStateException.class, () -> file.register(section("a")));
	}

	@Test
	void unregisteredSectionReadsAsDefaults() {
		ConfigFile file = new ConfigFile("test.json");
		assertEquals(new Test1(5, true), file.values().get(section("a")));
	}

	@Test
	void networkRoundTrip() {
		ConfigFile file = new ConfigFile("test.json");
		ConfigSection<Test1> a = file.register(section("a"));
		ConfigFile.Values values = file.parse(JsonParser.parseString("{\"a\": {\"count\": 3, \"on\": false}}").getAsJsonObject());
		assertEquals(new Test1(3, false), file.parse(file.toJson(values)).get(a));
	}

	@Test
	void coreSectionsValidate() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(EmbervoidConfig.Flask.SECTION);
		file.register(EmbervoidConfig.Remnant.SECTION);
		ConfigFile.Values values = file.parse(JsonParser.parseString(
				"{\"flask\": {\"baseCharges\": 5, \"maxCharges\": 2}, \"remnant\": {\"mode\": \"XP\"}}").getAsJsonObject());
		assertEquals(5, values.get(EmbervoidConfig.Flask.SECTION).maxCharges());
		assertEquals(EmbervoidConfig.RemnantMode.XP, values.get(EmbervoidConfig.Remnant.SECTION).mode());
		assertFalse(values.get(EmbervoidConfig.Flask.SECTION).drinkTicks() <= 0);
	}
}
