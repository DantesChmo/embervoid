package com.embervoid.config;

import com.embervoid.Embervoid;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Map;
import java.util.Objects;

/**
 * Gameplay config at {@code config/embervoid.json}, read by the server (the integrated server in single player).
 * Client preferences live in {@code embervoid-client.json} instead ({@code EmbervoidClientConfig}).
 *
 * <p>The file is a set of {@link ConfigSection}s. This class declares the core ones (remnant, stamina, flask);
 * other systems declare their own section next to their code and {@link #register} it, without editing this class.
 * Keys are documented in README.md.
 *
 * <p>On a client connected to a server, {@link #get()} returns the server's config instead (see {@link ConfigSync}),
 * so both sides agree on values like the flask drink time; the local file is back in effect after disconnecting.
 */
public final class EmbervoidConfig {
	private static final ConfigFile FILE = new ConfigFile(Embervoid.MOD_ID + ".json");

	/** What happens to a player's XP and items on death. */
	public enum RemnantMode {
		/** Vanilla death: nothing is left in an Ash Remnant. */
		OFF,
		/** XP goes into the Ash Remnant, items drop as usual. */
		XP,
		/** XP and items go into the Ash Remnant. */
		XP_AND_ITEMS;

		public String id() {
			return SectionReader.enumId(this);
		}
	}

	/** What a new death does to the previous Ash Remnant (DESIGN §10.7). */
	public enum SecondDeath {
		/** The XP burns; the items stay in the old remnant until picked up. */
		BURN_XP,
		/** The old remnant burns with everything in it. */
		BURN_ALL;

		public String id() {
			return SectionReader.enumId(this);
		}
	}

	public record Remnant(RemnantMode mode, SecondDeath secondDeath) {
		public static final ConfigSection<Remnant> SECTION = register(ConfigSection.of(
				"remnant", new Remnant(RemnantMode.XP_AND_ITEMS, SecondDeath.BURN_XP),
				in -> new Remnant(in.readEnum("mode", RemnantMode.XP_AND_ITEMS), in.readEnum("secondDeath", SecondDeath.BURN_XP)),
				(value, out) -> {
					out.addProperty("mode", value.mode.id());
					out.addProperty("secondDeath", value.secondDeath.id());
				}));
	}

	/**
	 * @param max             stamina pool size at base Endurance
	 * @param regenPerTick    stamina restored per tick once regeneration starts, at base Endurance
	 * @param regenDelayTicks ticks after spending stamina before it starts regenerating
	 */
	public record Stamina(int max, float regenPerTick, int regenDelayTicks) {
		public static final ConfigSection<Stamina> SECTION = register(ConfigSection.of(
				"stamina", new Stamina(100, 1.0f, 20),
				in -> new Stamina(
						in.readInt("max", 100, 1, 10_000),
						in.readFloat("regenPerTick", 1.0f, 0.01f, 1_000f),
						in.readInt("regenDelayTicks", 20, 0, 1_200)),
				(value, out) -> {
					out.addProperty("max", value.max);
					out.addProperty("regenPerTick", value.regenPerTick);
					out.addProperty("regenDelayTicks", value.regenDelayTicks);
				}));
	}

	/**
	 * @param baseCharges charges every player starts with
	 * @param maxCharges  cap for charges after upgrades (never below baseCharges)
	 * @param healAmount  health restored per sip, in half-hearts
	 * @param drinkTicks  how long one sip takes
	 */
	public record Flask(int baseCharges, int maxCharges, float healAmount, int drinkTicks) {
		public static final ConfigSection<Flask> SECTION = register(ConfigSection.of(
				"flask", new Flask(3, 8, 8.0f, 32),
				in -> {
					int base = in.readInt("baseCharges", 3, 1, 20);
					int max = in.readInt("maxCharges", 8, 1, 20);
					if (max < base) {
						max = in.fix(base, "flask.maxCharges ({}) is below flask.baseCharges ({}), raising it", max, base);
					}
					return new Flask(base, max,
							in.readFloat("healAmount", 8.0f, 0.5f, 1_000f),
							in.readInt("drinkTicks", 32, 1, 200));
				},
				(value, out) -> {
					out.addProperty("baseCharges", value.baseCharges);
					out.addProperty("maxCharges", value.maxCharges);
					out.addProperty("healAmount", value.healAmount);
					out.addProperty("drinkTicks", value.drinkTicks);
				}));
	}

	/** Defaults of every section registered so far. */
	public static final EmbervoidConfig DEFAULT = new EmbervoidConfig(new ConfigFile.Values(Map.of()));

	/** The local file's values, refreshed whenever the file is (re)read. */
	private static volatile EmbervoidConfig local = DEFAULT;
	/** What {@link #get()} returns: the local file, or the server's config while connected to one. */
	private static volatile EmbervoidConfig current = DEFAULT;

	static {
		FILE.onChange(() -> {
			boolean followLocal = current == local;
			local = new EmbervoidConfig(FILE.values());
			if (followLocal) {
				current = local;
			}
		});
	}

	private final ConfigFile.Values values;

	private EmbervoidConfig(ConfigFile.Values values) {
		this.values = values;
	}

	/**
	 * Adds a section to {@code embervoid.json}. Call it from a static field initializer (see {@link ConfigSection}) and
	 * make sure that field is touched during mod initialization (e.g. from your track's initializer): the server
	 * sends its config to clients on join, and a section registered after that is not part of what they receive.
	 * Registering before or after {@link #load()} both work. Throws if the name is taken.
	 */
	public static <T> ConfigSection<T> register(ConfigSection<T> section) {
		return FILE.register(section);
	}

	/** The config in effect; defaults until {@link #load()} runs. */
	public static EmbervoidConfig get() {
		return current;
	}

	public <T> T section(ConfigSection<T> section) {
		return values.get(section);
	}

	public Remnant remnant() {
		return section(Remnant.SECTION);
	}

	public Stamina stamina() {
		return section(Stamina.SECTION);
	}

	public Flask flask() {
		return section(Flask.SECTION);
	}

	/** Reads (or creates) the config file. Never throws: on any problem the defaults are used. */
	public static void load() {
		// The core sections register when their records are first used; make sure that is before the file is read.
		Objects.requireNonNull(Remnant.SECTION);
		Objects.requireNonNull(Stamina.SECTION);
		Objects.requireNonNull(Flask.SECTION);
		FILE.load(FabricLoader.getInstance().getConfigDir());
	}

	/** Uses a config received from the server until {@link #useLocal()} (client side). */
	public static void useServer(EmbervoidConfig config) {
		current = config;
	}

	/** Goes back to the local file's config, e.g. after leaving a server (client side). */
	public static void useLocal() {
		current = local;
	}

	public static String toNetworkString(EmbervoidConfig config) {
		return FILE.toJson(config.values).toString();
	}

	/** Parses {@link #toNetworkString} output, with the same validation as the file. */
	public static EmbervoidConfig fromNetworkString(String json) {
		JsonElement parsed = JsonParser.parseString(json);
		return new EmbervoidConfig(FILE.parse(parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject()));
	}
}
