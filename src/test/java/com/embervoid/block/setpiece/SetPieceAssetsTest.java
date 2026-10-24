package com.embervoid.block.setpiece;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The set-piece assets tools/setpiece_art.py writes: every model under geo/block/setpiece has its animation file,
 * texture, item icon and particle model (a glowmask only where something glows, see ClientAssetRulesTest); every
 * animation is named {@code idle}, {@code state.<s>} or {@code action.<a>}; every animated bone exists in the model;
 * the model stays within the art direction's budget (20–80 cubes). A kind whose names do not match its files is caught by the SetPieceTests GameTest.
 */
class SetPieceAssetsTest {
	private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "embervoid");

	@Test
	void everyModelHasItsFilesAndAnimations() throws IOException {
		List<String> names = new ArrayList<>();
		try (Stream<Path> files = Files.list(ASSETS.resolve("geo/block/setpiece"))) {
			files.filter(p -> p.getFileName().toString().endsWith(".geo.json"))
					.forEach(p -> names.add(p.getFileName().toString().replace(".geo.json", "")));
		}
		assertFalse(names.isEmpty(), "no set-piece models");
		List<String> problems = new ArrayList<>();
		for (String name : names) {
			for (String file : List.of("animations/block/setpiece/" + name + ".animation.json", "textures/block/setpiece/" + name + ".png",
					"textures/item/setpiece/" + name + ".png",
					"models/block/setpiece/" + name + ".json")) {
				if (!Files.exists(ASSETS.resolve(file))) {
					problems.add(name + ": missing " + file);
				}
			}
			JsonObject geo = json(ASSETS.resolve("geo/block/setpiece/" + name + ".geo.json"))
					.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
			Set<String> bones = new HashSet<>();
			int cubes = 0;
			for (JsonElement bone : geo.getAsJsonArray("bones")) {
				JsonObject b = bone.getAsJsonObject();
				bones.add(b.get("name").getAsString());
				if (b.has("cubes")) {
					cubes += b.getAsJsonArray("cubes").size();
				}
			}
			if (cubes < 20 || cubes > 80) {
				problems.add(name + ": " + cubes + " cubes, the budget is 20–80");
			}
			Path animationFile = ASSETS.resolve("animations/block/setpiece/" + name + ".animation.json");
			if (!Files.exists(animationFile)) {
				continue;
			}
			JsonObject animations = json(animationFile).getAsJsonObject("animations");
			if (animations.size() == 0) {
				problems.add(name + ": no animations");
			}
			for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
				String animation = entry.getKey();
				if (!animation.equals("idle") && !animation.startsWith("state.") && !animation.startsWith("action.")) {
					problems.add(name + ": animation " + animation + " is not idle, state.* or action.*");
				}
				JsonObject a = entry.getValue().getAsJsonObject();
				boolean loop = a.has("loop") && a.get("loop").getAsBoolean();
				if ((animation.equals("idle") || animation.startsWith("state.")) != loop) {
					problems.add(name + ": " + animation + (loop ? " loops but is an action" : " does not loop"));
				}
				if (a.has("bones")) {
					for (String bone : a.getAsJsonObject("bones").keySet()) {
						if (!bones.contains(bone)) {
							problems.add(name + ": " + animation + " animates unknown bone " + bone);
						}
					}
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	private static JsonObject json(Path path) throws IOException {
		try (Reader reader = Files.newBufferedReader(path)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	@SuppressWarnings("unused")
	private static int size(JsonArray array) {
		return array.size();
	}
}
