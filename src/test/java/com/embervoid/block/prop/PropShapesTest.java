package com.embervoid.block.prop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.math.Direction;
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
 * The prop hitboxes (PropShapes) against the models tools/prop_art.py writes: every box lies inside its model, the
 * shape reaches within two pixels of each side of the model, and both hold for every facing with the model turned
 * the way Minecraft turns a blockstate's y rotation. The generated blockstates turn each facing by that rotation and
 * use only models that have a shape. A mismatch only shows in game as a hitbox that floats off the prop.
 */
class PropShapesTest {
	private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "embervoid");
	private static final Path GENERATED = Path.of("src", "main", "generated", "assets", "embervoid");
	private static final double EPS = 0.01;
	/** How far the shape may stop short of the model on any side, in pixels. */
	private static final double SLACK = 2.0;

	@Test
	void everyShapeFitsItsModelInEveryFacing() throws IOException {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, PropShape> entry : PropShapes.all().entrySet()) {
			String model = entry.getKey();
			Path file = ASSETS.resolve("models/block/prop/" + model + ".json");
			if (!Files.exists(file)) {
				problems.add(model + ": no model file");
				continue;
			}
			List<double[]> corners = corners(read(file));
			for (Direction facing : Direction.Type.HORIZONTAL) {
				double[] bounds = turnedBounds(corners, PropShape.quarterTurns(facing));
				double[] reach = {99, 99, 99, -99, -99, -99};
				for (double[] box : entry.getValue().boxes(facing)) {
					for (int i = 0; i < 3; i++) {
						if (box[i] < bounds[i] - EPS || box[i + 3] > bounds[i + 3] + EPS || box[i] >= box[i + 3]) {
							problems.add(model + " " + facing + ": box " + fmt(box) + " outside the model " + fmt(bounds));
							break;
						}
					}
					for (int i = 0; i < 3; i++) {
						reach[i] = Math.min(reach[i], box[i]);
						reach[i + 3] = Math.max(reach[i + 3], box[i + 3]);
					}
				}
				for (int i = 0; i < 3; i++) {
					if (reach[i] - bounds[i] > SLACK || bounds[i + 3] - reach[i + 3] > SLACK) {
						problems.add(model + " " + facing + ": shape " + fmt(reach) + " stops short of the model " + fmt(bounds));
						break;
					}
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void modelsStayInMinecraftsLimitsAndHaveTheirTextures() throws IOException {
		List<String> problems = new ArrayList<>();
		try (Stream<Path> files = Files.list(ASSETS.resolve("models/block/prop"))) {
			for (Path file : files.toList()) {
				JsonObject model = read(file);
				for (JsonElement texture : model.getAsJsonObject("textures").asMap().values()) {
					String id = texture.getAsString();
					if (id.startsWith("embervoid:") && !Files.exists(ASSETS.resolve("textures/" + id.substring(10) + ".png"))) {
						problems.add(file.getFileName() + ": no texture " + id);
					}
				}
				for (JsonElement e : model.getAsJsonArray("elements")) {
					JsonObject element = e.getAsJsonObject();
					for (String key : new String[]{"from", "to"}) {
						for (double v : numbers(element.getAsJsonArray(key))) {
							if (v < -16 || v > 32) {
								problems.add(file.getFileName() + ": " + key + " " + element.get(key) + " beyond -16..32");
							}
						}
					}
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void blockstatesTurnEachFacingAndUseOnlyShapedModels() throws IOException {
		List<String> problems = new ArrayList<>();
		Set<String> used = new HashSet<>();
		try (Stream<Path> files = Files.list(GENERATED.resolve("blockstates"))) {
			for (Path file : files.toList()) {
				JsonObject variants = read(file).getAsJsonObject("variants");
				if (variants == null) {
					continue;
				}
				for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
					JsonObject v = variant.getValue().getAsJsonObject();
					String model = v.get("model").getAsString();
					if (!model.startsWith("embervoid:block/prop/")) {
						continue;
					}
					String name = model.substring("embervoid:block/prop/".length());
					used.add(name);
					if (!PropShapes.all().containsKey(name)) {
						problems.add(file.getFileName() + " " + variant.getKey() + ": no shape for " + name);
					}
					Direction facing = facing(variant.getKey());
					int y = v.has("y") ? v.get("y").getAsInt() : 0;
					if (facing == null || y != PropShape.quarterTurns(facing) * 90) {
						problems.add(file.getFileName() + " " + variant.getKey() + ": y " + y);
					}
				}
			}
		}
		for (String model : PropShapes.all().keySet()) {
			if (!used.contains(model)) {
				problems.add(model + ": a shape no blockstate uses");
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	/** Every element's eight corners after its own rotation, in pixels. */
	private static List<double[]> corners(JsonObject model) {
		List<double[]> out = new ArrayList<>();
		for (JsonElement e : model.getAsJsonArray("elements")) {
			JsonObject element = e.getAsJsonObject();
			double[] from = numbers(element.getAsJsonArray("from"));
			double[] to = numbers(element.getAsJsonArray("to"));
			JsonObject rotation = element.getAsJsonObject("rotation");
			for (int k = 0; k < 8; k++) {
				double[] p = {(k & 1) == 0 ? from[0] : to[0], (k & 2) == 0 ? from[1] : to[1], (k & 4) == 0 ? from[2] : to[2]};
				out.add(rotation == null ? p : rotate(p, rotation));
			}
		}
		return out;
	}

	/** An element rotation: right-handed about the axis through the origin, as Minecraft's quaternions turn it. */
	private static double[] rotate(double[] p, JsonObject rotation) {
		double[] o = numbers(rotation.getAsJsonArray("origin"));
		double a = Math.toRadians(rotation.get("angle").getAsDouble());
		double c = Math.cos(a), s = Math.sin(a);
		double x = p[0] - o[0], y = p[1] - o[1], z = p[2] - o[2];
		switch (rotation.get("axis").getAsString()) {
			case "x" -> {
				double ny = y * c - z * s;
				z = y * s + z * c;
				y = ny;
			}
			case "y" -> {
				double nx = x * c + z * s;
				z = -x * s + z * c;
				x = nx;
			}
			default -> {
				double nx = x * c - y * s;
				y = x * s + y * c;
				x = nx;
			}
		}
		return new double[]{x + o[0], y + o[1], z + o[2]};
	}

	/**
	 * Bounds of the corners turned like a blockstate's y rotation of quarterTurns * 90: Minecraft rotates the model by
	 * -y degrees about the vertical through the block's centre.
	 */
	private static double[] turnedBounds(List<double[]> corners, int quarterTurns) {
		double a = Math.toRadians(-90.0 * quarterTurns);
		double c = Math.cos(a), s = Math.sin(a);
		double[] b = {99, 99, 99, -99, -99, -99};
		for (double[] p : corners) {
			double x = p[0] - 8, z = p[2] - 8;
			double[] q = {x * c + z * s + 8, p[1], -x * s + z * c + 8};
			for (int i = 0; i < 3; i++) {
				b[i] = Math.min(b[i], q[i]);
				b[i + 3] = Math.max(b[i + 3], q[i]);
			}
		}
		return b;
	}

	private static Direction facing(String variant) {
		for (String part : variant.split(",")) {
			if (part.startsWith("facing=")) {
				return Direction.byName(part.substring(7));
			}
		}
		return null;
	}

	private static double[] numbers(JsonArray array) {
		double[] out = new double[array.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = array.get(i).getAsDouble();
		}
		return out;
	}

	private static String fmt(double[] box) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < box.length; i++) {
			sb.append(i > 0 ? " " : "").append(Math.round(box[i] * 100) / 100.0);
		}
		return sb.append("]").toString();
	}

	private static JsonObject read(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}
}
