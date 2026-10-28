package com.embervoid.reliquary;

import com.embervoid.stats.Stat;

import java.util.Set;

/** What the accessories give, from their config numbers ({@link RingEffects}, {@link ArtifactItem}). */
final class AccessoryNumbers {
	/** Fall distance vanilla already lets through without damage. */
	static final float VANILLA_SAFE_FALL = 3;
	/** Mining speed vanilla's Haste adds per level. */
	static final float HASTE_PER_LEVEL = 0.2f;

	/**
	 * Ring of Ash Feathers: blocks taken off a fall before vanilla's own safe distance, so falls up to
	 * {@code featherSafeFall} are free.
	 */
	static float ignoredFallDistance(float featherSafeFall) {
		return Math.max(0, featherSafeFall - VANILLA_SAFE_FALL);
	}

	/** Delver's Ring: mining speed multiplier of Haste at {@code hasteLevel}. */
	static float hasteMiningMultiplier(int hasteLevel) {
		return 1 + HASTE_PER_LEVEL * hasteLevel;
	}

	/** Adds {@code points} to each of {@code stats} in {@code bonus}, indexed by {@link Stat#ordinal()}. */
	static void addPoints(int[] bonus, Set<Stat> stats, int points) {
		stats.forEach(stat -> bonus[stat.ordinal()] += points);
	}

	private AccessoryNumbers() {}
}
