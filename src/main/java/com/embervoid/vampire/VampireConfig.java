package com.embervoid.vampire;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;

/**
 * {@code vampire.*} in {@code config/embervoid.json}: the Vampires of the blood new moon (DESIGN §3.3, PLAN 8.10;
 * the PLAN writes these keys as {@code moon.blood.*}).
 *
 * @param provokeMinutes real-time minutes until the Vampire nearest to a player turns on its own
 * @param groupRadius    players within this many blocks of each other share one Vampire group
 * @param groupMin       fewest Vampires in a group
 * @param groupMax       most Vampires in a group
 */
public record VampireConfig(float provokeMinutes, int groupRadius, int groupMin, int groupMax) {
	private static final VampireConfig DEFAULTS = new VampireConfig(5, 64, 1, 3);

	public static final ConfigSection<VampireConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"vampire", DEFAULTS,
			in -> {
				int min = in.readInt("groupMin", DEFAULTS.groupMin, 1, 10);
				return new VampireConfig(
						in.readFloat("provokeMinutes", DEFAULTS.provokeMinutes, 0.1f, 60),
						in.readInt("groupRadius", DEFAULTS.groupRadius, 8, 512),
						min,
						Math.max(min, in.readInt("groupMax", DEFAULTS.groupMax, 1, 10)));
			},
			(value, out) -> {
				out.addProperty("provokeMinutes", value.provokeMinutes);
				out.addProperty("groupRadius", value.groupRadius);
				out.addProperty("groupMin", value.groupMin);
				out.addProperty("groupMax", value.groupMax);
			}));

	public static VampireConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}
}
