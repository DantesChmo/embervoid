package com.embervoid.vampire;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;

/**
 * {@code vampire.*} in {@code config/embervoid.json}: the Vampires of the blood new moon and their boss (DESIGN §3.3,
 * §10.12, PLAN 8.10–8.12; the PLAN writes these keys as {@code moon.blood.*}).
 *
 * @param provokeMinutes    real-time minutes until the Vampire nearest to a player turns on its own
 * @param groupRadius       players within this many blocks of each other share one Vampire group
 * @param groupMin          fewest Vampires in a group
 * @param groupMax          most Vampires in a group
 * @param bossMinLevel      the boss's lowest level
 * @param bossCapNoRealm    the boss's level cap when no player taking part has unlocked a realm (overworld band top)
 * @param firstKillShards   Flask Shards for a player's first kill (plus a Vampire Fang)
 * @param repeatShardChance chance of one Flask Shard on a repeat kill
 */
public record VampireConfig(float provokeMinutes, int groupRadius, int groupMin, int groupMax, int bossMinLevel,
		int bossCapNoRealm, int firstKillShards, float repeatShardChance) {
	private static final VampireConfig DEFAULTS = new VampireConfig(5, 64, 1, 3, 15, 20, 2, 0.5f);

	public static final ConfigSection<VampireConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"vampire", DEFAULTS,
			in -> {
				int min = in.readInt("groupMin", DEFAULTS.groupMin, 1, 10);
				return new VampireConfig(
						in.readFloat("provokeMinutes", DEFAULTS.provokeMinutes, 0.1f, 60),
						in.readInt("groupRadius", DEFAULTS.groupRadius, 8, 512),
						min,
						Math.max(min, in.readInt("groupMax", DEFAULTS.groupMax, 1, 10)),
						in.readInt("bossMinLevel", DEFAULTS.bossMinLevel, 1, 999),
						in.readInt("bossCapNoRealm", DEFAULTS.bossCapNoRealm, 1, 999),
						in.readInt("firstKillShards", DEFAULTS.firstKillShards, 0, 64),
						in.readFloat("repeatShardChance", DEFAULTS.repeatShardChance, 0, 1));
			},
			(value, out) -> {
				out.addProperty("provokeMinutes", value.provokeMinutes);
				out.addProperty("groupRadius", value.groupRadius);
				out.addProperty("groupMin", value.groupMin);
				out.addProperty("groupMax", value.groupMax);
				out.addProperty("bossMinLevel", value.bossMinLevel);
				out.addProperty("bossCapNoRealm", value.bossCapNoRealm);
				out.addProperty("firstKillShards", value.firstKillShards);
				out.addProperty("repeatShardChance", value.repeatShardChance);
			}));

	public static VampireConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}
}
