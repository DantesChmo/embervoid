package com.embervoid.vampire;

import java.util.Collection;

/**
 * The Vampire boss's level (DESIGN §3.3): the highest level among the players taking part, at least
 * {@code vampire.bossMinLevel} (15), capped at the top of the band of the highest realm any of them has unlocked
 * (the overworld band, {@code vampire.bossCapNoRealm} 20, if none). The cap never goes below the minimum. Pure logic.
 */
public final class VampireLevel {
	/**
	 * @param playerLevels character levels of the players taking part
	 * @param minLevel     the lowest level the boss can have
	 * @param cap          top of the highest unlocked realm's band
	 */
	public static int of(Collection<Integer> playerLevels, int minLevel, int cap) {
		int highest = playerLevels.stream().mapToInt(Integer::intValue).max().orElse(minLevel);
		return Math.min(Math.max(highest, minLevel), Math.max(cap, minLevel));
	}

	private VampireLevel() {}
}
