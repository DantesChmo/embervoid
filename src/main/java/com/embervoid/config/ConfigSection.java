package com.embervoid.config;

import com.google.gson.JsonObject;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * One top-level object in a config file, e.g. {@code "stamina": {...}}. A system declares its section once, as a
 * constant next to the code that uses it, and registers it with the file it belongs to:
 *
 * <pre>{@code
 * public record MobsConfig(int baseLevel) {
 *     public static final ConfigSection<MobsConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
 *             "mobs", new MobsConfig(4),
 *             in -> new MobsConfig(in.readInt("baseLevel", 4, 1, 100)),
 *             (value, out) -> out.addProperty("baseLevel", value.baseLevel())));
 * }
 * // anywhere later:
 * int base = EmbervoidConfig.get().section(MobsConfig.SECTION).baseLevel();
 * }</pre>
 *
 * <p>Register during mod initialization: reference the constant from your initializer, since a nested record's
 * static fields only run when the record is first used.
 *
 * <p>The reader must never throw: {@link SectionReader} falls back to the default for any missing or invalid key and
 * logs it. The writer must write every key the reader reads, so a fresh file documents the whole section.
 *
 * @param <T> an immutable value (usually a record) holding the section's settings
 */
public final class ConfigSection<T> {
	private final String name;
	private final T defaults;
	private final Function<SectionReader, T> reader;
	private final BiConsumer<T, JsonObject> writer;

	private ConfigSection(String name, T defaults, Function<SectionReader, T> reader, BiConsumer<T, JsonObject> writer) {
		this.name = Objects.requireNonNull(name);
		this.defaults = Objects.requireNonNull(defaults);
		this.reader = Objects.requireNonNull(reader);
		this.writer = Objects.requireNonNull(writer);
	}

	/**
	 * @param name     top-level key in the file; unique within the file
	 * @param defaults the value used when the file has nothing for this section
	 * @param reader   builds the value from the section's JSON object
	 * @param writer   writes the value's keys into an empty JSON object
	 */
	public static <T> ConfigSection<T> of(String name, T defaults, Function<SectionReader, T> reader,
			BiConsumer<T, JsonObject> writer) {
		return new ConfigSection<>(name, defaults, reader, writer);
	}

	public String name() {
		return name;
	}

	public T defaults() {
		return defaults;
	}

	T read(SectionReader in) {
		return reader.apply(in);
	}

	JsonObject write(T value) {
		JsonObject json = new JsonObject();
		writer.accept(value, json);
		return json;
	}

	@Override
	public String toString() {
		return "ConfigSection[" + name + "]";
	}
}
