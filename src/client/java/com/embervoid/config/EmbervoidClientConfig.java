package com.embervoid.config;

import com.embervoid.Embervoid;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Objects;

/**
 * Client preferences at {@code config/embervoid-client.json}: how things look, never how the game plays. The server
 * never reads this file and it is not synced. Systems add their own {@link ConfigSection} with {@link #register},
 * like {@link EmbervoidConfig}.
 */
public final class EmbervoidClientConfig {
	private static final ConfigFile FILE = new ConfigFile(Embervoid.MOD_ID + "-client.json");

	/** @param hideWhenFull hide the stamina bar while stamina is full */
	public record StaminaBar(boolean hideWhenFull) {
		public static final ConfigSection<StaminaBar> SECTION = register(ConfigSection.of(
				"staminaBar", new StaminaBar(true),
				in -> new StaminaBar(in.readBoolean("hideWhenFull", true)),
				(value, out) -> out.addProperty("hideWhenFull", value.hideWhenFull)));
	}

	/** Adds a section to {@code embervoid-client.json}; before or after {@link #load()} both work. */
	public static <T> ConfigSection<T> register(ConfigSection<T> section) {
		return FILE.register(section);
	}

	/** A section's current value (its defaults until {@link #load()} runs). */
	public static <T> T get(ConfigSection<T> section) {
		return FILE.values().get(section);
	}

	public static StaminaBar staminaBar() {
		return get(StaminaBar.SECTION);
	}

	/** Reads (or creates) the file. Never throws. */
	public static void load() {
		Objects.requireNonNull(StaminaBar.SECTION);
		FILE.load(FabricLoader.getInstance().getConfigDir());
	}

	private EmbervoidClientConfig() {}
}
