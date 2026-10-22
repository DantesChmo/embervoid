package com.embervoid.boss;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;

/**
 * {@code boss.*} in {@code config/embervoid.json}: the boss framework (DESIGN §4.5 "Multiplayer").
 *
 * @param healthPerExtraFighter  boss max HP is ×(1 + this·(n − 1)) for n fighters at summon
 * @param addsPerExtraFighter    adds summoned with the boss per fighter beyond the first
 * @param addLevelOffset         adds are this many levels below the boss
 * @param targetSwitchMinSeconds shortest time between two target switches (with two fighters or more)
 * @param targetSwitchMaxSeconds longest time between two target switches
 * @param repeatSoulChance       chance of the boss's soul for a credited fighter who had already beaten it (not in a rematch)
 */
public record BossConfig(float healthPerExtraFighter, int addsPerExtraFighter, int addLevelOffset, float targetSwitchMinSeconds,
		float targetSwitchMaxSeconds, float repeatSoulChance) {
	private static final BossConfig DEFAULTS = new BossConfig(0.5f, 1, 3, 8, 10, 0.25f);

	public static final ConfigSection<BossConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"boss", DEFAULTS,
			in -> {
				float min = in.readFloat("targetSwitchMinSeconds", DEFAULTS.targetSwitchMinSeconds, 0.5f, 600);
				return new BossConfig(
						in.readFloat("healthPerExtraFighter", DEFAULTS.healthPerExtraFighter, 0, 10),
						in.readInt("addsPerExtraFighter", DEFAULTS.addsPerExtraFighter, 0, 10),
						in.readInt("addLevelOffset", DEFAULTS.addLevelOffset, -50, 50),
						min,
						Math.max(min, in.readFloat("targetSwitchMaxSeconds", DEFAULTS.targetSwitchMaxSeconds, 0.5f, 600)),
						in.readFloat("repeatSoulChance", DEFAULTS.repeatSoulChance, 0, 1));
			},
			(value, out) -> {
				out.addProperty("healthPerExtraFighter", value.healthPerExtraFighter);
				out.addProperty("addsPerExtraFighter", value.addsPerExtraFighter);
				out.addProperty("addLevelOffset", value.addLevelOffset);
				out.addProperty("targetSwitchMinSeconds", value.targetSwitchMinSeconds);
				out.addProperty("targetSwitchMaxSeconds", value.targetSwitchMaxSeconds);
				out.addProperty("repeatSoulChance", value.repeatSoulChance);
			}));

	public static BossConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}
}
