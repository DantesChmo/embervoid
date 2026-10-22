package com.embervoid.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every file sounds.json points at exists: our own under assets/embervoid/sounds, vanilla ones in the 1.20.1 asset
 * index Loom downloaded (skipped when that index is not on this machine). A missing file only shows in game as
 * silence, so it is caught here.
 */
class SoundFilesTest {
	private static final Path SOUNDS_JSON = Path.of("src", "main", "resources", "assets", "embervoid", "sounds.json");
	private static final Path OWN_SOUNDS = Path.of("src", "main", "resources", "assets", "embervoid", "sounds");

	@Test
	void everySoundFileExists() throws IOException {
		JsonObject vanilla = vanillaIndex();
		List<String> missing = new ArrayList<>();
		for (Map.Entry<String, JsonElement> event : json(SOUNDS_JSON).entrySet()) {
			for (JsonElement sound : event.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
				String name = sound.isJsonObject() ? sound.getAsJsonObject().get("name").getAsString() : sound.getAsString();
				String namespace = name.contains(":") ? name.substring(0, name.indexOf(':')) : "minecraft";
				String path = name.substring(name.indexOf(':') + 1);
				boolean exists = namespace.equals("embervoid") ? Files.exists(OWN_SOUNDS.resolve(path + ".ogg"))
						: vanilla == null || vanilla.has(namespace + "/sounds/" + path + ".ogg");
				if (!exists) {
					missing.add(event.getKey() + " → " + name);
				}
			}
		}
		assertTrue(missing.isEmpty(), "missing sound files: " + missing);
		Assumptions.assumeTrue(vanilla != null, "no 1.20.1 asset index from Loom here: vanilla files not checked");
	}

	/** The {@code objects} of Loom's 1.20.1 asset index, or null if it is not downloaded. */
	private static JsonObject vanillaIndex() throws IOException {
		String gradleHome = System.getenv("GRADLE_USER_HOME");
		Path indexes = (gradleHome != null ? Path.of(gradleHome) : Path.of(System.getProperty("user.home"), ".gradle"))
				.resolve("caches/fabric-loom/assets/indexes");
		if (!Files.isDirectory(indexes)) {
			return null;
		}
		try (Stream<Path> files = Files.list(indexes)) {
			Path index = files.filter(f -> f.getFileName().toString().startsWith("1.20.1")).findFirst().orElse(null);
			return index == null ? null : json(index).getAsJsonObject("objects");
		}
	}

	private static JsonObject json(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}
}
