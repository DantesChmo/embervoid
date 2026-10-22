package com.embervoid.moon;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.config.SectionReader;
import com.google.gson.JsonObject;

/**
 * The {@code moon} config section (DESIGN §3.3, §10.12). Holds the numbers of all three 0.3.0 tracks, so the night
 * mobs and the Vampire read their keys here too.
 *
 * @param enabled         false: no moon night starts on its own (the debug command still works)
 * @param cycleInterval   events come every this many lunar cycles (8 days each), first in cycle {@code cycleInterval}
 * @param fallbackMinutes a moon night ends after this many real-time minutes even if its goal is not met
 * @param fullMoon        full moon numbers
 * @param blood           blood new moon numbers
 */
public record MoonConfig(boolean enabled, int cycleInterval, float fallbackMinutes, FullMoon fullMoon, Blood blood) {
	/**
	 * @param levelBonus       extra levels for overworld hostiles that spawn on a full moon
	 * @param followRange      follow range multiplier for overworld hostiles during a full moon
	 * @param zombieLevelBonus Ashen Zombie level = spot level + this
	 * @param ghostMissChance  chance that a hit on a Ghost misses
	 */
	public record FullMoon(int levelBonus, float followRange, int zombieLevelBonus, float ghostMissChance) {}

	/**
	 * @param vampirism      share of the damage a hostile deals to a player that heals it
	 * @param provokeMinutes after this many minutes with no Vampire provoked, the nearest one turns
	 */
	public record Blood(float vampirism, float provokeMinutes) {}

	public static final MoonConfig DEFAULT = new MoonConfig(true, 3, 30,
			new FullMoon(3, 1.5f, 5, 0.15f),
			new Blood(0.25f, 5));

	public static final ConfigSection<MoonConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"moon", DEFAULT, MoonConfig::read, MoonConfig::write));

	public static MoonConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}

	/** {@link #fallbackMinutes} in server ticks. */
	public long fallbackTicks() {
		return Math.round(fallbackMinutes * 60 * 20);
	}

	private static MoonConfig read(SectionReader in) {
		FullMoon fullDef = DEFAULT.fullMoon;
		SectionReader full = in.section("fullMoon");
		Blood bloodDef = DEFAULT.blood;
		SectionReader blood = in.section("blood");
		return new MoonConfig(
				in.readBoolean("enabled", DEFAULT.enabled),
				in.readInt("cycleInterval", DEFAULT.cycleInterval, 1, 1_000),
				in.readFloat("fallbackMinutes", DEFAULT.fallbackMinutes, 1, 24 * 60),
				new FullMoon(
						full.readInt("levelBonus", fullDef.levelBonus, 0, 100),
						full.readFloat("followRange", fullDef.followRange, 1, 10),
						full.readInt("zombieLevelBonus", fullDef.zombieLevelBonus, 0, 100),
						full.readFloat("ghostMissChance", fullDef.ghostMissChance, 0, 1)),
				new Blood(
						blood.readFloat("vampirism", bloodDef.vampirism, 0, 10),
						blood.readFloat("provokeMinutes", bloodDef.provokeMinutes, 0, 60)));
	}

	private static void write(MoonConfig value, JsonObject out) {
		out.addProperty("enabled", value.enabled);
		out.addProperty("cycleInterval", value.cycleInterval);
		out.addProperty("fallbackMinutes", value.fallbackMinutes);
		JsonObject full = new JsonObject();
		full.addProperty("levelBonus", value.fullMoon.levelBonus);
		full.addProperty("followRange", value.fullMoon.followRange);
		full.addProperty("zombieLevelBonus", value.fullMoon.zombieLevelBonus);
		full.addProperty("ghostMissChance", value.fullMoon.ghostMissChance);
		out.add("fullMoon", full);
		JsonObject blood = new JsonObject();
		blood.addProperty("vampirism", value.blood.vampirism);
		blood.addProperty("provokeMinutes", value.blood.provokeMinutes);
		out.add("blood", blood);
	}
}
