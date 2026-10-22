package com.embervoid.balance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads the mod's data files the balance model describes (structure templates, structure sets, arena JSON, chest loot
 * tables), so the tests compare {@link BalanceData} with what the game actually generates.
 */
final class GameFiles {
	private static final Path RESOURCES = Path.of("src", "main", "resources", "data", "embervoid");
	private static final Path GENERATED = Path.of("src", "main", "generated", "data", "embervoid");

	/** Placed enemies of a template under {@code structures/ashen_reach/}, by entity id path. */
	static Map<String, Integer> placedEnemies(String template) {
		Path file = GENERATED.resolve("structures/ashen_reach/" + template + ".nbt");
		Map<String, Integer> mobs = new HashMap<>();
		try (InputStream in = Files.newInputStream(file)) {
			NbtCompound nbt = NbtIo.readCompressed(in);
			NbtList blocks = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < blocks.size(); i++) {
				NbtCompound block = blocks.getCompound(i);
				if (block.contains("nbt") && block.getCompound("nbt").contains("entity", NbtElement.STRING_TYPE)) {
					String id = block.getCompound("nbt").getString("entity");
					mobs.merge(id.substring(id.indexOf(':') + 1), 1, Integer::sum);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return mobs;
	}

	/** Weights of the structures in a structure set, by structure id path after {@code ashen_reach/<zone>/}. */
	static Map<String, Integer> structureWeights(String set) {
		JsonObject json = json(RESOURCES.resolve("worldgen/structure_set/" + set + ".json"));
		Map<String, Integer> weights = new HashMap<>();
		for (JsonElement entry : json.getAsJsonArray("structures")) {
			String id = entry.getAsJsonObject().get("structure").getAsString();
			weights.put(id.substring(id.lastIndexOf('/') + 1), entry.getAsJsonObject().get("weight").getAsInt());
		}
		return weights;
	}

	/** The {@code boss} block of an arena structure: level and entity. */
	static JsonObject arenaBoss(String structure) {
		return json(RESOURCES.resolve("worldgen/structure/" + structure + ".json")).getAsJsonObject("boss");
	}

	/**
	 * Expected number of {@code item} (full id) one opening of a chest loot table gives: per pool, mean rolls × the
	 * entry's share of the weight × its mean count × the pool's chance.
	 */
	static double expectedCount(String table, String item) {
		JsonObject json = json(GENERATED.resolve("loot_tables/chests/ashen_reach/" + table + ".json"));
		double total = 0;
		for (JsonElement poolElement : json.getAsJsonArray("pools")) {
			JsonObject pool = poolElement.getAsJsonObject();
			double rolls = mean(pool.get("rolls"));
			double chance = 1;
			if (pool.has("conditions")) {
				for (JsonElement condition : pool.getAsJsonArray("conditions")) {
					if (condition.getAsJsonObject().has("chance")) {
						chance *= condition.getAsJsonObject().get("chance").getAsDouble();
					}
				}
			}
			JsonArray entries = pool.getAsJsonArray("entries");
			double weights = 0;
			for (JsonElement entry : entries) {
				weights += weight(entry.getAsJsonObject());
			}
			for (JsonElement entryElement : entries) {
				JsonObject entry = entryElement.getAsJsonObject();
				if (!entry.has("name") || !entry.get("name").getAsString().equals(item)) {
					continue;
				}
				double count = 1;
				if (entry.has("functions")) {
					for (JsonElement function : entry.getAsJsonArray("functions")) {
						JsonObject f = function.getAsJsonObject();
						if (f.get("function").getAsString().endsWith("set_count")) {
							count = mean(f.get("count"));
						}
					}
				}
				total += rolls * weight(entry) / weights * count * chance;
			}
		}
		return total;
	}

	private static double weight(JsonObject entry) {
		return entry.has("weight") ? entry.get("weight").getAsDouble() : 1;
	}

	private static double mean(JsonElement number) {
		if (number.isJsonPrimitive()) {
			return number.getAsDouble();
		}
		JsonObject range = number.getAsJsonObject();
		return (range.get("min").getAsDouble() + range.get("max").getAsDouble()) / 2;
	}

	private static JsonObject json(Path file) {
		try {
			return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private GameFiles() {}
}
