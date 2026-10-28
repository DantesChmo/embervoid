package com.embervoid.config;

import com.embervoid.Embervoid;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A JSON config file made of {@link ConfigSection}s. Hand-rolled on Gson (bundled with Minecraft), no config library.
 *
 * <p>Missing keys are filled with defaults, invalid values are logged and replaced with defaults, and the file is
 * rewritten when anything had to be fixed. A file that is not valid JSON is moved to {@code <name>.broken} and
 * replaced with defaults. Top-level keys no registered section owns are kept as they are.
 *
 * <p>Sections can be registered before or after {@link #load}; a section registered later is read from the
 * already loaded file at once (and the file rewritten if it lacked that section), so initializer order does not
 * matter.
 */
public final class ConfigFile {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final String fileName;
	private final Map<String, ConfigSection<?>> sections = new LinkedHashMap<>();
	/** The file's JSON as last loaded; null until {@link #load}. */
	private JsonObject root;
	private Path path;
	private volatile Values values = new Values(Map.of());
	private Runnable onChange = () -> {};

	public ConfigFile(String fileName) {
		this.fileName = fileName;
	}

	/** Called after the values change (load, or a section registered after load). */
	void onChange(Runnable listener) {
		this.onChange = listener;
	}

	/** Adds a section. Throws if another section already has its name. */
	public synchronized <T> ConfigSection<T> register(ConfigSection<T> section) {
		ConfigSection<?> previous = sections.putIfAbsent(section.name(), section);
		if (previous != null && previous != section) {
			throw new IllegalStateException("Config section '" + section.name() + "' is already registered in " + fileName);
		}
		if (root != null) {
			apply();
		}
		return section;
	}

	/** Reads (or creates) {@code dir/<fileName>}. Never throws: on any problem the defaults are used. */
	public synchronized void load(Path dir) {
		path = dir.resolve(fileName);
		JsonObject loaded = null;
		boolean exists = Files.exists(path);
		if (exists) {
			try {
				JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
				if (parsed.isJsonObject()) {
					loaded = parsed.getAsJsonObject();
				} else {
					Embervoid.LOGGER.warn("Config {} is not a JSON object", path);
				}
			} catch (Exception e) {
				Embervoid.LOGGER.warn("Config {} is not valid JSON: {}", path, e.getMessage());
			}
			if (loaded == null) {
				backupBroken();
			}
		}
		root = loaded == null ? new JsonObject() : loaded;
		boolean dirty = apply();
		if (!dirty && loaded == null) {
			write();
		}
	}

	/** Current values: defaults until loaded. */
	public Values values() {
		return values;
	}

	/** Parses a whole file's JSON with the same validation as {@link #load}, without touching the disk. */
	public synchronized Values parse(JsonObject json) {
		return read(json, new SectionReader.Dirty());
	}

	public synchronized JsonObject toJson(Values values) {
		JsonObject json = new JsonObject();
		sections.values().forEach(section -> json.add(section.name(), writeSection(section, values)));
		return json;
	}

	/** Re-reads all sections from {@link #root}, rewrites the file if anything was fixed. Returns that. */
	private boolean apply() {
		SectionReader.Dirty dirty = new SectionReader.Dirty();
		values = read(root, dirty);
		if (dirty.value) {
			write();
		}
		onChange.run();
		return dirty.value;
	}

	private Values read(JsonObject json, SectionReader.Dirty dirty) {
		Map<ConfigSection<?>, Object> read = new IdentityHashMap<>();
		SectionReader top = new SectionReader(json, "", dirty);
		for (ConfigSection<?> section : sections.values()) {
			read.put(section, section.read(top.section(section.name())));
		}
		return new Values(read);
	}

	private void write() {
		if (path == null) {
			return;
		}
		// Start from the file as read, so sections nobody registered (yet) survive the rewrite.
		JsonObject json = root.deepCopy();
		sections.values().forEach(section -> json.add(section.name(), writeSection(section, values)));
		root = json;
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Embervoid.LOGGER.warn("Could not write config {}", path, e);
		}
	}

	private static <T> JsonObject writeSection(ConfigSection<T> section, Values values) {
		return section.write(values.get(section));
	}

	private void backupBroken() {
		Path backup = path.resolveSibling(fileName + ".broken");
		try {
			Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
			Embervoid.LOGGER.warn("Moved the broken config to {} and wrote defaults", backup);
		} catch (IOException e) {
			Embervoid.LOGGER.warn("Could not back up the broken config {}", path, e);
		}
	}

	/** An immutable snapshot of every section's value. */
	public static final class Values {
		private final Map<ConfigSection<?>, Object> values;

		Values(Map<ConfigSection<?>, Object> values) {
			this.values = Collections.unmodifiableMap(values);
		}

		/** The section's value; its defaults if the section was not registered when this snapshot was taken. */
		@SuppressWarnings("unchecked")
		public <T> T get(ConfigSection<T> section) {
			Object value = values.get(section);
			return value != null ? (T) value : section.defaults();
		}
	}
}
