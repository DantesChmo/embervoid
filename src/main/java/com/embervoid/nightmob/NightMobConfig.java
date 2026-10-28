package com.embervoid.nightmob;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.config.SectionReader;
import com.embervoid.mob.MobLevelRules;
import com.google.gson.JsonObject;

/**
 * The {@code nightMobs} config section: the full moon's own mobs and their waves (DESIGN §3.3). Kept
 * apart from the {@code moon} section, which belongs to the moon-night state; a file holds one section per name.
 *
 * @param zombieLevelBonus levels an Ashen Zombie of a wave gets on top of the spot's level
 * @param ghostMissChance  share of the attacks on a Ghost that pass through it
 * @param maxZombies       most Ashen Zombies in one player's wave (one per curse stack)
 * @param ghostCount       Ghosts in the wave of a player with no curse
 * @param spawnMinDistance a wave spawns at least this many blocks (horizontally) from its player...
 * @param spawnMaxDistance ...and at most this many
 */
public record NightMobConfig(int zombieLevelBonus, float ghostMissChance, int maxZombies, int ghostCount,
		int spawnMinDistance, int spawnMaxDistance) {
	public static final NightMobConfig DEFAULT = new NightMobConfig(5, 0.15f, 10, 3, 16, 32);

	public static final ConfigSection<NightMobConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"nightMobs", DEFAULT, NightMobConfig::read, NightMobConfig::write));

	public static NightMobConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}

	private static NightMobConfig read(SectionReader in) {
		int min = in.readInt("spawnMinDistance", DEFAULT.spawnMinDistance, 1, 64);
		return new NightMobConfig(
				in.readInt("zombieLevelBonus", DEFAULT.zombieLevelBonus, 0, MobLevelRules.MAX_LEVEL),
				in.readFloat("ghostMissChance", DEFAULT.ghostMissChance, 0, 1),
				in.readInt("maxZombies", DEFAULT.maxZombies, 0, 100),
				in.readInt("ghostCount", DEFAULT.ghostCount, 0, 100),
				min,
				in.readInt("spawnMaxDistance", Math.max(DEFAULT.spawnMaxDistance, min), min, 64));
	}

	private static void write(NightMobConfig config, JsonObject out) {
		out.addProperty("zombieLevelBonus", config.zombieLevelBonus);
		out.addProperty("ghostMissChance", config.ghostMissChance);
		out.addProperty("maxZombies", config.maxZombies);
		out.addProperty("ghostCount", config.ghostCount);
		out.addProperty("spawnMinDistance", config.spawnMinDistance);
		out.addProperty("spawnMaxDistance", config.spawnMaxDistance);
	}
}
