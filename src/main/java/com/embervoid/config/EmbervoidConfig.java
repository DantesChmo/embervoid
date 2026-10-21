package com.embervoid.config;

import com.embervoid.Embervoid;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Mod config at {@code config/embervoid.json}. Hand-rolled on Gson so the mod needs no config library.
 *
 * <p>Missing keys are filled with defaults, invalid values are logged and replaced with defaults, and the file is
 * rewritten when anything had to be fixed. A file that is not valid JSON is moved to {@code embervoid.json.broken}
 * and replaced with defaults. Keys are documented in README.md.
 *
 * <p>On a client connected to a server, {@link #get()} returns the server's config instead (see {@link ConfigSync}),
 * so both sides agree on values like the flask drink time; the local file is back in effect after disconnecting.
 */
public record EmbervoidConfig(Remnant remnant, Stamina stamina, Flask flask, Structures structures) {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = Embervoid.MOD_ID + ".json";

	public static final EmbervoidConfig DEFAULT = new EmbervoidConfig(
			new Remnant(RemnantMode.XP_AND_ITEMS),
			new Stamina(100, 1.0f, 20),
			new Flask(3, 8, 8.0f, 32),
			new Structures(
					new StructurePlacement(24, 8),
					new StructurePlacement(40, 16),
					new StructurePlacement(64, 24)));

	/** What {@link #get()} returns: the local file, or the server's config while connected to one. */
	private static volatile EmbervoidConfig current = DEFAULT;
	/** The config read from the local file. */
	private static EmbervoidConfig local = DEFAULT;

	/** What happens to a player's XP and items on death. */
	public enum RemnantMode {
		/** Vanilla death: nothing is left in an Ash Remnant. */
		OFF,
		/** XP goes into the Ash Remnant, items drop as usual. */
		XP,
		/** XP and items go into the Ash Remnant. */
		XP_AND_ITEMS;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** @param mode see {@link RemnantMode} */
	public record Remnant(RemnantMode mode) {}

	/**
	 * @param max             stamina pool size
	 * @param regenPerTick    stamina restored per tick once regeneration starts
	 * @param regenDelayTicks ticks after spending stamina before it starts regenerating
	 */
	public record Stamina(int max, float regenPerTick, int regenDelayTicks) {}

	/**
	 * @param baseCharges charges of a fresh flask
	 * @param maxCharges  cap for charges after upgrades (never below baseCharges)
	 * @param healAmount  health restored per sip, in half-hearts
	 * @param drinkTicks  how long one sip takes
	 */
	public record Flask(int baseCharges, int maxCharges, float healAmount, int drinkTicks) {}

	/** Structure set placement, in chunks; separation is always less than spacing. */
	public record StructurePlacement(int spacing, int separation) {}

	public record Structures(StructurePlacement ruins, StructurePlacement crypt, StructurePlacement fortress) {}

	/** The config in effect; defaults until {@link #load()} runs. */
	public static EmbervoidConfig get() {
		return current;
	}

	/** Reads (or creates) the config file. Never throws: on any problem the defaults are used. */
	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		JsonObject root = null;
		if (Files.exists(path)) {
			try {
				JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
				if (parsed.isJsonObject()) {
					root = parsed.getAsJsonObject();
				} else {
					Embervoid.LOGGER.warn("Config {} is not a JSON object", path);
				}
			} catch (Exception e) {
				Embervoid.LOGGER.warn("Config {} is not valid JSON: {}", path, e.getMessage());
			}
			if (root == null) {
				backupBroken(path);
			}
		}

		Reader reader = new Reader(root == null ? new JsonObject() : root);
		local = reader.read();
		current = local;
		if (root == null || reader.dirty) {
			write(path, local);
		}
	}

	/** Uses a config received from the server until {@link #useLocal()} (client side). */
	public static void useServer(EmbervoidConfig config) {
		current = config;
	}

	/** Goes back to the local file's config, e.g. after leaving a server (client side). */
	public static void useLocal() {
		current = local;
	}

	/** Compact JSON for sending the config over the network. */
	public static String toNetworkString(EmbervoidConfig config) {
		return toJson(config).toString();
	}

	/** Parses {@link #toNetworkString} output, with the same validation as the file. */
	public static EmbervoidConfig fromNetworkString(String json) {
		JsonElement parsed = JsonParser.parseString(json);
		return new Reader(parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject()).read();
	}

	private static void backupBroken(Path path) {
		Path backup = path.resolveSibling(FILE_NAME + ".broken");
		try {
			Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
			Embervoid.LOGGER.warn("Moved the broken config to {} and wrote defaults", backup);
		} catch (IOException e) {
			Embervoid.LOGGER.warn("Could not back up the broken config {}", path, e);
		}
	}

	private static void write(Path path, EmbervoidConfig config) {
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(toJson(config)) + System.lineSeparator(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Embervoid.LOGGER.warn("Could not write config {}", path, e);
		}
	}

	private static JsonObject toJson(EmbervoidConfig config) {
		JsonObject remnant = new JsonObject();
		remnant.addProperty("mode", config.remnant.mode.id());

		JsonObject stamina = new JsonObject();
		stamina.addProperty("max", config.stamina.max);
		stamina.addProperty("regenPerTick", config.stamina.regenPerTick);
		stamina.addProperty("regenDelayTicks", config.stamina.regenDelayTicks);

		JsonObject flask = new JsonObject();
		flask.addProperty("baseCharges", config.flask.baseCharges);
		flask.addProperty("maxCharges", config.flask.maxCharges);
		flask.addProperty("healAmount", config.flask.healAmount);
		flask.addProperty("drinkTicks", config.flask.drinkTicks);

		JsonObject structures = new JsonObject();
		structures.add("ruins", toJson(config.structures.ruins));
		structures.add("crypt", toJson(config.structures.crypt));
		structures.add("fortress", toJson(config.structures.fortress));

		JsonObject root = new JsonObject();
		root.add("remnant", remnant);
		root.add("stamina", stamina);
		root.add("flask", flask);
		root.add("structures", structures);
		return root;
	}

	private static JsonObject toJson(StructurePlacement placement) {
		JsonObject json = new JsonObject();
		json.addProperty("spacing", placement.spacing);
		json.addProperty("separation", placement.separation);
		return json;
	}

	/** Reads typed values out of a JSON tree, falling back to defaults and remembering if anything was fixed. */
	private static final class Reader {
		private final JsonObject root;
		private boolean dirty;

		private Reader(JsonObject root) {
			this.root = root;
		}

		private EmbervoidConfig read() {
			JsonObject remnant = section(root, "remnant");
			JsonObject stamina = section(root, "stamina");
			JsonObject flask = section(root, "flask");
			JsonObject structures = section(root, "structures");
			EmbervoidConfig def = DEFAULT;

			int baseCharges = readInt(flask, "flask.baseCharges", def.flask.baseCharges, 1, 20);
			int maxCharges = readInt(flask, "flask.maxCharges", def.flask.maxCharges, 1, 20);
			if (maxCharges < baseCharges) {
				Embervoid.LOGGER.warn("Config: flask.maxCharges ({}) is below flask.baseCharges ({}), raising it",
						maxCharges, baseCharges);
				maxCharges = baseCharges;
				dirty = true;
			}

			return new EmbervoidConfig(
					new Remnant(readMode(remnant, "remnant.mode", def.remnant.mode)),
					new Stamina(
							readInt(stamina, "stamina.max", def.stamina.max, 1, 10_000),
							readFloat(stamina, "stamina.regenPerTick", def.stamina.regenPerTick, 0.01f, 1_000f),
							readInt(stamina, "stamina.regenDelayTicks", def.stamina.regenDelayTicks, 0, 1_200)),
					new Flask(
							baseCharges,
							maxCharges,
							readFloat(flask, "flask.healAmount", def.flask.healAmount, 0.5f, 1_000f),
							readInt(flask, "flask.drinkTicks", def.flask.drinkTicks, 1, 200)),
					new Structures(
							readPlacement(structures, "structures.ruins", def.structures.ruins),
							readPlacement(structures, "structures.crypt", def.structures.crypt),
							readPlacement(structures, "structures.fortress", def.structures.fortress)));
		}

		private StructurePlacement readPlacement(JsonObject parent, String path, StructurePlacement def) {
			JsonObject json = section(parent, path.substring(path.lastIndexOf('.') + 1));
			int spacing = readInt(json, path + ".spacing", def.spacing, 2, 4_096);
			int separation = readInt(json, path + ".separation", def.separation, 0, 4_095);
			if (separation >= spacing) {
				Embervoid.LOGGER.warn("Config: {}.separation ({}) must be less than spacing ({}), lowering it",
						path, separation, spacing);
				separation = spacing - 1;
				dirty = true;
			}
			return new StructurePlacement(spacing, separation);
		}

		/** Returns the named child object, or an empty one (marking the file dirty) if it is missing or not an object. */
		private JsonObject section(JsonObject parent, String key) {
			JsonElement element = parent.get(key);
			if (element != null && element.isJsonObject()) {
				return element.getAsJsonObject();
			}
			if (element != null) {
				Embervoid.LOGGER.warn("Config: '{}' should be an object, using defaults", key);
			}
			dirty = true;
			return new JsonObject();
		}

		private RemnantMode readMode(JsonObject json, String path, RemnantMode def) {
			JsonPrimitive value = primitive(json, path);
			if (value == null) {
				return def;
			}
			if (value.isString()) {
				for (RemnantMode mode : RemnantMode.values()) {
					if (mode.id().equalsIgnoreCase(value.getAsString())) {
						return mode;
					}
				}
			}
			return invalid(path, value, "one of off, xp, xp_and_items", def);
		}

		private int readInt(JsonObject json, String path, int def, int min, int max) {
			JsonPrimitive value = primitive(json, path);
			if (value == null) {
				return def;
			}
			if (value.isNumber()) {
				double number = value.getAsDouble();
				if (number == Math.rint(number) && number >= min && number <= max) {
					return (int) number;
				}
			}
			return invalid(path, value, "a whole number from " + min + " to " + max, def);
		}

		private float readFloat(JsonObject json, String path, float def, float min, float max) {
			JsonPrimitive value = primitive(json, path);
			if (value == null) {
				return def;
			}
			if (value.isNumber()) {
				float number = value.getAsFloat();
				if (number >= min && number <= max) {
					return number;
				}
			}
			return invalid(path, value, "a number from " + min + " to " + max, def);
		}

		/** The primitive at the last segment of {@code path}, or null (marking the file dirty) if it is missing. */
		private JsonPrimitive primitive(JsonObject json, String path) {
			JsonElement element = json.get(path.substring(path.lastIndexOf('.') + 1));
			if (element == null) {
				dirty = true;
				return null;
			}
			if (element.isJsonPrimitive()) {
				return element.getAsJsonPrimitive();
			}
			Embervoid.LOGGER.warn("Config: {} = {} is invalid (expected a single value), using default", path, element);
			dirty = true;
			return null;
		}

		private <T> T invalid(String path, JsonElement value, String expected, T def) {
			Embervoid.LOGGER.warn("Config: {} = {} is invalid (expected {}), using default {}",
					path, value, expected, def instanceof RemnantMode mode ? mode.id() : def);
			dirty = true;
			return def;
		}
	}
}
