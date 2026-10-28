package com.embervoid.reach.gear;

/** Bleeding numbers, free of game classes (JUnit-tested). Levels are 1-based; effect amplifiers are level − 1. */
public final class BleedRules {
	/** Bleeding deals its damage once per this many ticks. */
	public static final int DAMAGE_INTERVAL_TICKS = 20;

	/** The level after adding {@code stacks} hits to {@code currentLevel} (0 = not bleeding), capped at {@code maxLevel}. */
	public static int nextLevel(int currentLevel, int stacks, int maxLevel) {
		return Math.max(0, Math.min(maxLevel, currentLevel + stacks));
	}

	public static float damagePerTick(int level, float damagePerLevel) {
		return level * damagePerLevel * DAMAGE_INTERVAL_TICKS / 20.0f;
	}

	/**
	 * Whether Bleeding deals damage on this tick of the entity's age. Timed by age, not by the remaining duration, so
	 * refreshing it with every hit neither adds nor delays ticks.
	 */
	public static boolean damagesAt(int entityAge) {
		return entityAge % DAMAGE_INTERVAL_TICKS == 0;
	}

	private BleedRules() {}
}
