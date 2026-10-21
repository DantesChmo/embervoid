package com.embervoid.config;

import com.embervoid.Embervoid;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Locale;

/**
 * Reads typed values out of one config object. Every read falls back to the given default when the key is missing
 * or invalid; invalid values are logged with their full path ({@code stamina.max}). Either way the file is marked
 * for rewriting, so the user sees the corrected value next time.
 */
public final class SectionReader {
	private final JsonObject json;
	private final String path;
	private final Dirty dirty;

	/** Shared by all readers of one file, so a nested reader can mark the whole file for rewriting. */
	static final class Dirty {
		boolean value;
	}

	SectionReader(JsonObject json, String path, Dirty dirty) {
		this.json = json;
		this.path = path;
		this.dirty = dirty;
	}

	/** Dotted path of this object in the file, e.g. {@code structures.ruins}. */
	public String path() {
		return path;
	}

	/**
	 * Keys present in this object, in file order. For objects whose keys are data rather than fixed settings (e.g.
	 * a level per entity type id), so users can add entries for other mods and dimensions.
	 */
	public List<String> keys() {
		return List.copyOf(json.keySet());
	}

	/** Reader for a nested object; missing or non-object values read as empty (all defaults). */
	public SectionReader section(String key) {
		JsonElement element = json.get(key);
		if (element != null && element.isJsonObject()) {
			return new SectionReader(element.getAsJsonObject(), path(key), dirty);
		}
		if (element != null) {
			Embervoid.LOGGER.warn("Config: '{}' should be an object, using defaults", path(key));
		}
		dirty.value = true;
		return new SectionReader(new JsonObject(), path(key), dirty);
	}

	public int readInt(String key, int def, int min, int max) {
		JsonPrimitive value = primitive(key);
		if (value == null) {
			return def;
		}
		if (value.isNumber()) {
			double number = value.getAsDouble();
			if (number == Math.rint(number) && number >= min && number <= max) {
				return (int) number;
			}
		}
		return invalid(key, value, "a whole number from " + min + " to " + max, def);
	}

	public float readFloat(String key, float def, float min, float max) {
		JsonPrimitive value = primitive(key);
		if (value == null) {
			return def;
		}
		if (value.isNumber()) {
			float number = value.getAsFloat();
			if (number >= min && number <= max) {
				return number;
			}
		}
		return invalid(key, value, "a number from " + min + " to " + max, def);
	}

	public boolean readBoolean(String key, boolean def) {
		JsonPrimitive value = primitive(key);
		if (value == null) {
			return def;
		}
		if (value.isBoolean()) {
			return value.getAsBoolean();
		}
		return invalid(key, value, "true or false", def);
	}

	public String readString(String key, String def) {
		JsonPrimitive value = primitive(key);
		if (value == null) {
			return def;
		}
		if (value.isString()) {
			return value.getAsString();
		}
		return invalid(key, value, "a string", def);
	}

	/** Reads an enum constant written in lower case ({@link #enumId}), ignoring case. */
	public <E extends Enum<E>> E readEnum(String key, E def) {
		JsonPrimitive value = primitive(key);
		if (value == null) {
			return def;
		}
		E[] constants = def.getDeclaringClass().getEnumConstants();
		if (value.isString()) {
			for (E constant : constants) {
				if (enumId(constant).equalsIgnoreCase(value.getAsString())) {
					return constant;
				}
			}
		}
		StringBuilder expected = new StringBuilder("one of ");
		for (int i = 0; i < constants.length; i++) {
			expected.append(i == 0 ? "" : ", ").append(enumId(constants[i]));
		}
		return invalid(key, value, expected.toString(), def);
	}

	/** How an enum constant is written in config files: its name in lower case. */
	public static String enumId(Enum<?> constant) {
		return constant.name().toLowerCase(Locale.ROOT);
	}

	/**
	 * For checks across keys (e.g. a max below a min): logs the message, marks the file for rewriting, and returns
	 * {@code fixed} so the caller can write {@code max = reader.fix(base, "...")}.
	 */
	public <T> T fix(T fixed, String message, Object... args) {
		Embervoid.LOGGER.warn("Config: " + message, args);
		dirty.value = true;
		return fixed;
	}

	/** The primitive at {@code key}, or null (marking the file dirty) if it is missing or not a single value. */
	private JsonPrimitive primitive(String key) {
		JsonElement element = json.get(key);
		if (element == null) {
			dirty.value = true;
			return null;
		}
		if (element.isJsonPrimitive()) {
			return element.getAsJsonPrimitive();
		}
		Embervoid.LOGGER.warn("Config: {} = {} is invalid (expected a single value), using default", path(key), element);
		dirty.value = true;
		return null;
	}

	private <T> T invalid(String key, JsonElement value, String expected, T def) {
		Embervoid.LOGGER.warn("Config: {} = {} is invalid (expected {}), using default {}",
				path(key), value, expected, def instanceof Enum<?> constant ? enumId(constant) : def);
		dirty.value = true;
		return def;
	}

	private String path(String key) {
		return path.isEmpty() ? key : path + "." + key;
	}
}
