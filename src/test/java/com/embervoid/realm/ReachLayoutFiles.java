package com.embervoid.realm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The Ashen Reach layout as its data files give it (the dimension's biome source, its noise settings and the arena
 * structure sets), so the layout tests check what the game generates.
 */
final class ReachLayoutFiles {
	private static final Path DATA = Path.of("src", "main", "resources", "data", "embervoid");
	static final String[] ZONES = {"a", "b", "c"};

	/** {@code biome_source} of {@code dimension/ashen_reach.json}. */
	static JsonObject biomeSource() {
		return json("dimension/ashen_reach.json").getAsJsonObject("generator").getAsJsonObject("biome_source");
	}

	static RealmLayout.Sectors sectors() {
		JsonObject source = biomeSource();
		return RealmLayout.Sectors.degrees(source.get("start_angle").getAsDouble(),
				source.getAsJsonArray("zones").size(), source.get("final_width").getAsDouble());
	}

	static double radius() {
		return biomeSource().get("radius").getAsDouble();
	}

	static double landingRadius() {
		return biomeSource().get("landing_radius").getAsDouble();
	}

	static double warpRadians() {
		return Math.toRadians(biomeSource().get("warp").getAsDouble());
	}

	/** Every {@code embervoid:zone_blend} and {@code embervoid:radial_falloff} in the realm's noise router. */
	static List<JsonObject> densityFunctions(String type) {
		List<JsonObject> found = new ArrayList<>();
		collect(json("worldgen/noise_settings/ashen_reach.json").get("noise_router"), type, found);
		return found;
	}

	/** {@code placement} of an arena's structure set. */
	static JsonObject arenaPlacement(String zone) {
		return json("worldgen/structure_set/ashen_reach_arena_" + zone + ".json").getAsJsonObject("placement");
	}

	/** Site radius of an arena (the flat ground the road ends at). */
	static int siteRadius(String zone) {
		return json("worldgen/structure/ashen_reach_arena_" + zone + ".json").getAsJsonObject("site").get("radius").getAsInt();
	}

	/** Where an arena lands for a seed, as {@code RealmFeaturePlacement} computes it (before snapping to its chunk). */
	static int[] arenaPosition(long seed, String zone) {
		JsonObject placement = arenaPlacement(zone);
		double start = Math.toRadians(placement.get("start_angle").getAsDouble());
		double width = Math.toRadians(placement.get("sector_width").getAsDouble());
		return RealmLayout.featurePosition(seed, placement.get("salt").getAsInt(),
				start + placement.get("sector").getAsInt() * width, width, placement.get("angle_spread").getAsDouble(),
				placement.get("min_distance").getAsInt(), placement.get("max_distance").getAsInt());
	}

	/** The arena centres for a seed, snapped to their start chunk's corner as the structure places them. */
	static List<int[]> arenas(long seed) {
		List<int[]> arenas = new ArrayList<>();
		for (String zone : ZONES) {
			int[] pos = arenaPosition(seed, zone);
			arenas.add(new int[]{Math.floorDiv(pos[0], 16) * 16, Math.floorDiv(pos[1], 16) * 16});
		}
		return arenas;
	}

	static List<Integer> siteRadii() {
		List<Integer> radii = new ArrayList<>();
		for (String zone : ZONES) {
			radii.add(siteRadius(zone));
		}
		return radii;
	}

	private static void collect(JsonElement element, String type, List<JsonObject> found) {
		if (element.isJsonObject()) {
			JsonObject object = element.getAsJsonObject();
			if (object.has("type") && object.get("type").isJsonPrimitive() && type.equals(object.get("type").getAsString())) {
				found.add(object);
			}
			for (var entry : object.entrySet()) {
				collect(entry.getValue(), type, found);
			}
		} else if (element.isJsonArray()) {
			for (JsonElement child : element.getAsJsonArray()) {
				collect(child, type, found);
			}
		}
	}

	private static JsonObject json(String path) {
		try {
			return JsonParser.parseString(Files.readString(DATA.resolve(path))).getAsJsonObject();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private ReachLayoutFiles() {}
}
