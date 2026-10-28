package com.embervoid.mob;

import net.minecraft.world.Difficulty;

/** The level formulas (DESIGN §5.4, §3.1) as pure functions. */
public final class MobLevelRules {
	public static final int MIN_LEVEL = 1;
	public static final int MAX_LEVEL = 999;

	/**
	 * Level of a naturally spawned mob.
	 *
	 * @param distance       horizontal distance from world spawn, or 0 where distance does not count
	 * @param belowZero      whether the below-y=0 bonus applies
	 */
	public static int level(MobConfig.Levels config, int base, int dimensionBonus, double distance, boolean belowZero) {
		int distanceBonus = config.blocksPerLevel() <= 0 ? 0
				: (int) Math.min(config.maxDistanceBonus(), Math.floor(distance / config.blocksPerLevel()));
		return clamp(base + dimensionBonus + distanceBonus + (belowZero ? config.belowZeroBonus() : 0));
	}

	public static int clamp(int level) {
		return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
	}

	/** Extra max health as a fraction of base health (an ADD_MULTIPLIED_BASE modifier). */
	public static double healthBonus(MobConfig.Levels config, int level) {
		return config.healthPerLevel() * (level - 1);
	}

	public static float damageMultiplier(MobConfig.Levels config, int level) {
		return 1 + config.damagePerLevel() * (level - 1);
	}

	public static int experience(MobConfig.Levels config, int level, int base) {
		return Math.round(base * (1 + config.experiencePerLevel() * (level - 1)));
	}

	/**
	 * Difficulty-scaled damage to a player, replacing vanilla's scaling. Peaceful stays 0 like vanilla.
	 */
	public static float difficultyDamage(MobConfig.Difficulty config, Difficulty difficulty, float amount) {
		return switch (difficulty) {
			case PEACEFUL -> 0;
			case EASY -> amount * config.easy();
			case NORMAL -> amount * config.normal();
			case HARD -> amount * config.hard();
		};
	}

	private MobLevelRules() {}
}
