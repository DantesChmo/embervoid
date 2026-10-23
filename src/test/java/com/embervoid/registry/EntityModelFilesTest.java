package com.embervoid.registry;

import com.embervoid.vampire.VampireEntity;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The GeckoLib entity models keep what the code relies on when their art is regenerated (tools/overworld_mob_art.py,
 * tools/reach_mob_art.py, tools/reach_boss_art.py): every model has its animation file and texture, every animated
 * bone exists, and the overworld entities have the animations their classes play, the bones their renderers use and
 * telegraphs at least as long as their windups. A mismatch only shows in game as a frozen or missing part.
 * (Sizes against tools/model_bounds.json: {@code python3 tools/model_preview.py check}.)
 */
class EntityModelFilesTest {
	private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "embervoid");

	/** Animations each overworld entity class plays (DefaultAnimations names and its own RawAnimations). */
	private static final Map<String, List<String>> ANIMATIONS = Map.of(
			"ash_ghoul", List.of("misc.idle", "move.walk", "attack.swing"),
			"hollow", List.of("misc.idle", "move.walk", "attack.swing", "attack.block"),
			"ember_wisp", List.of("misc.idle", "move.fly"),
			"ashen_maiden", List.of("misc.idle", "misc.interact"),
			"ashen_zombie", List.of("misc.idle", "move.walk", "attack.swing"),
			"ghost", List.of("misc.idle", "move.fly", "attack.swing"),
			"vampire", List.of("misc.idle", "move.walk", "misc.transform"),
			"vampire_boss", List.of("misc.idle", "move.walk", "attack.swing", "attack.leap", "attack.nova", "misc.mist"));

	/** Bones the renderers use: the head they turn (models built with turnsHead) and the Hollow's held item. */
	private static final Map<String, List<String>> BONES = Map.of(
			"ash_ghoul", List.of("head"),
			"hollow", List.of("head", "right_item"),
			"ashen_maiden", List.of("head"),
			"ashen_zombie", List.of("head"),
			"vampire", List.of("head"),
			"vampire_boss", List.of("head"));

	/** Shortest length (s) of a telegraphed animation: the windup it shows, in ticks / 20. */
	private static final Map<String, Double> MIN_LENGTH = Map.of(
			"hollow/attack.swing", 8 / 20.0,                       // HollowEntity.WINDUP_TICKS
			"vampire/misc.transform", VampireEntity.TURN_TICKS / 20.0,
			"vampire_boss/attack.leap", 20 / 20.0,                 // VampireBossEntity.LEAP_WINDUP
			"vampire_boss/attack.nova", 30 / 20.0,                 // NOVA_WINDUP
			"vampire_boss/misc.mist", 12 / 20.0);                  // MIST_WINDUP

	@Test
	void everyModelHasItsAnimationsAndTextureAndAnimatesOnlyItsBones() throws IOException {
		List<String> problems = new ArrayList<>();
		List<String> names = new ArrayList<>();
		try (Stream<Path> files = Files.list(ASSETS.resolve("geo/entity"))) {
			files.map(f -> f.getFileName().toString()).filter(f -> f.endsWith(".geo.json"))
					.forEach(f -> names.add(f.substring(0, f.length() - ".geo.json".length())));
		}
		for (String name : names) {
			Path animationFile = ASSETS.resolve("animations/entity/" + name + ".animation.json");
			if (!Files.exists(ASSETS.resolve("textures/entity/" + name + ".png"))) {
				problems.add(name + ": no texture");
			}
			if (!Files.exists(animationFile)) {
				problems.add(name + ": no animation file");
				continue;
			}
			Set<String> bones = bones(name);
			for (Map.Entry<String, JsonElement> animation : animations(name).entrySet()) {
				JsonObject animated = animation.getValue().getAsJsonObject().getAsJsonObject("bones");
				for (String bone : animated == null ? Set.<String>of() : animated.keySet()) {
					if (!bones.contains(bone)) {
						problems.add(name + ": " + animation.getKey() + " animates missing bone " + bone);
					}
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void overworldEntitiesKeepWhatTheirCodeUses() throws IOException {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : ANIMATIONS.entrySet()) {
			String name = entry.getKey();
			JsonObject animations = animations(name);
			for (String animation : entry.getValue()) {
				if (!animations.has(animation)) {
					problems.add(name + ": animation " + animation + " is played by its class but missing");
					continue;
				}
				Double min = MIN_LENGTH.get(name + "/" + animation);
				double length = animations.getAsJsonObject(animation).get("animation_length").getAsDouble();
				if (min != null && length + 1e-6 < min) {
					problems.add(name + ": " + animation + " lasts " + length + " s, shorter than its " + min + " s windup");
				}
			}
			Set<String> bones = bones(name);
			for (String bone : BONES.getOrDefault(name, List.of())) {
				if (!bones.contains(bone)) {
					problems.add(name + ": bone " + bone + " is used by its renderer but missing");
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	private static Set<String> bones(String name) throws IOException {
		Set<String> bones = new HashSet<>();
		JsonObject geo = json(ASSETS.resolve("geo/entity/" + name + ".geo.json"))
				.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
		for (JsonElement bone : geo.getAsJsonArray("bones")) {
			bones.add(bone.getAsJsonObject().get("name").getAsString());
		}
		return bones;
	}

	private static JsonObject animations(String name) throws IOException {
		return json(ASSETS.resolve("animations/entity/" + name + ".animation.json")).getAsJsonObject("animations");
	}

	private static JsonObject json(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}
}
