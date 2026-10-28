package com.embervoid.vampire;

import com.embervoid.realm.RealmDefinition;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

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

	/**
	 * The cap: the top of the highest band (landing, zones, final zone) of the last of {@code realms}, in realm order,
	 * that {@code unlocked} says one of the players has; {@code noRealmCap} if none.
	 */
	public static int cap(List<RealmDefinition> realms, Predicate<Identifier> unlocked, int noRealmCap) {
		for (int i = realms.size() - 1; i >= 0; i--) {
			RealmDefinition realm = realms.get(i);
			if (unlocked.test(realm.id())) {
				return topLevel(realm);
			}
		}
		return noRealmCap;
	}

	/** The highest level any of a realm's bands reaches. */
	public static int topLevel(RealmDefinition realm) {
		int top = Math.max(realm.landingLevels().max(), realm.finalLevels().max());
		for (RealmDefinition.LevelBand band : realm.zoneLevels()) {
			top = Math.max(top, band.max());
		}
		return top;
	}

	private VampireLevel() {}
}
