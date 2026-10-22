package com.embervoid.nightmob;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.config.SectionReader;
import com.embervoid.mob.MobLevelRules;
import com.google.gson.JsonObject;

/**
 * The {@code nightMobs} config section: the full moon's own mobs and their waves (DESIGN §3.3, PLAN 8.7–8.9). Kept
 * apart from the {@code moon} section, which belongs to the moon-night state; a file holds one section per name.
 *
 * @param zombieLevelBonus levels an Ashen Zombie of a wave gets on top of the spot's level
 * @param ghostMissChance  share of the attacks on a Ghost that pass through it
 */
public record NightMobConfig(int zombieLevelBonus, float ghostMissChance) {
	public static final NightMobConfig DEFAULT = new NightMobConfig(5, 0.15f);

	public static final ConfigSection<NightMobConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"nightMobs", DEFAULT, NightMobConfig::read, NightMobConfig::write));

	public static NightMobConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}

	private static NightMobConfig read(SectionReader in) {
		return new NightMobConfig(
				in.readInt("zombieLevelBonus", DEFAULT.zombieLevelBonus, 0, MobLevelRules.MAX_LEVEL),
				in.readFloat("ghostMissChance", DEFAULT.ghostMissChance, 0, 1));
	}

	private static void write(NightMobConfig config, JsonObject out) {
		out.addProperty("zombieLevelBonus", config.zombieLevelBonus);
		out.addProperty("ghostMissChance", config.ghostMissChance);
	}
}
