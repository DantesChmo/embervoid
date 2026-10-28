package com.embervoid.reach.gear;

/** Timing of the coatings and oils that burn for a while (Smoldering Resin, Amber Lantern Oil), free of game classes. */
public final class ConsumableRules {
	/** The game time a coating put on at {@code now} for {@code ticks} burns out. */
	public static long expiresAt(long now, long ticks) {
		return now + ticks;
	}

	/** Whether a coating that burns out at {@code expiresAt} (0: none) still burns at {@code now}. */
	public static boolean burning(long expiresAt, long now) {
		return expiresAt > now;
	}

	private ConsumableRules() {}
}
