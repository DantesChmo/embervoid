package com.embervoid.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
 * Rules the client enforces only when it loads resources, which GameTests and the server check never do. One broken
 * file fails the whole resource reload: the game drops back to no resources at all and every letter on screen is a
 * box (0.5.0). GeckoLib refuses a channel whose 0 keyframe is not its first and draws a channel out of time order
 * backwards; vanilla refuses a model element whose from or to leaves -16..32. Two more fail only where they are
 * drawn (0.5.2): a model whose parents end in builtin/generated (item/generated, item/handheld) is baked from its
 * layer textures and its elements dropped, so it draws nothing; GeckoLib crashes the client on a glowmask without a
 * lit pixel.
 */
class ClientAssetRulesTest {
	private static final List<Path> ASSET_ROOTS = List.of(
			Path.of("src", "main", "resources", "assets", "embervoid"),
			Path.of("src", "main", "generated", "assets", "embervoid"));

	/** Parents that end in builtin/generated. */
	private static final List<String> GENERATED_PARENTS = List.of("item/generated", "minecraft:item/generated", "item/handheld",
			"minecraft:item/handheld", "item/handheld_rod", "minecraft:item/handheld_rod", "builtin/generated", "minecraft:builtin/generated");

	@Test
	void everyAnimationChannelRunsForwardInTime() throws IOException {
		List<String> problems = new ArrayList<>();
		for (Path file : files("animations", ".animation.json")) {
			JsonObject animations = json(file).getAsJsonObject("animations");
			for (Map.Entry<String, JsonElement> animation : animations.entrySet()) {
				JsonObject bones = animation.getValue().getAsJsonObject().getAsJsonObject("bones");
				if (bones == null) {
					continue;
				}
				for (Map.Entry<String, JsonElement> bone : bones.entrySet()) {
					for (Map.Entry<String, JsonElement> channel : bone.getValue().getAsJsonObject().entrySet()) {
						if (!channel.getValue().isJsonObject()) {
							continue;
						}
						double last = -1;
						for (String key : channel.getValue().getAsJsonObject().keySet()) {
							double time;
							try {
								time = Double.parseDouble(key);
							} catch (NumberFormatException e) {
								break;
							}
							if (time <= last) {
								problems.add(file.getFileName() + " " + animation.getKey() + " " + bone.getKey() + " "
										+ channel.getKey() + ": " + key + " after " + last);
								break;
							}
							last = time;
						}
					}
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void everyModelElementStaysInVanillaBounds() throws IOException {
		List<String> problems = new ArrayList<>();
		for (Path file : files("models", ".json")) {
			JsonObject model = json(file);
			if (!model.has("elements")) {
				continue;
			}
			for (JsonElement element : model.getAsJsonArray("elements")) {
				JsonObject box = element.getAsJsonObject();
				for (String corner : List.of("from", "to")) {
					for (JsonElement c : box.getAsJsonArray(corner)) {
						if (c.getAsDouble() < -16 || c.getAsDouble() > 32) {
							problems.add(file.getFileName() + " " + corner + " " + box.get(corner));
							break;
						}
					}
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void noElementModelBakesAsAGeneratedItem() throws IOException {
		List<String> problems = new ArrayList<>();
		for (Path file : files("models", ".json")) {
			JsonObject model = json(file);
			if (model.has("elements") && model.has("parent") && GENERATED_PARENTS.contains(model.get("parent").getAsString())) {
				problems.add(file.getFileName() + " parent " + model.get("parent").getAsString());
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void everyGlowmaskHasALitPixel() throws IOException {
		List<String> problems = new ArrayList<>();
		for (Path file : files("textures", "_glowmask.png")) {
			BufferedImage image = ImageIO.read(file.toFile());
			boolean lit = false;
			for (int y = 0; y < image.getHeight() && !lit; y++) {
				for (int x = 0; x < image.getWidth() && !lit; x++) {
					lit = (image.getRGB(x, y) >>> 24) != 0;
				}
			}
			if (!lit) {
				problems.add(file.getFileName().toString());
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	private static List<Path> files(String folder, String suffix) throws IOException {
		List<Path> files = new ArrayList<>();
		for (Path root : ASSET_ROOTS) {
			Path dir = root.resolve(folder);
			if (!Files.isDirectory(dir)) {
				continue;
			}
			try (Stream<Path> walk = Files.walk(dir)) {
				walk.filter(p -> p.toString().endsWith(suffix)).forEach(files::add);
			}
		}
		assertTrue(!files.isEmpty(), "no " + folder + " found");
		return files;
	}

	private static JsonObject json(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}
}
