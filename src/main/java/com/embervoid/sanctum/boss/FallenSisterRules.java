package com.embervoid.sanctum.boss;

/** The Fallen Sister's rules that need no world ({@link FallenSisterEntity}). */
public final class FallenSisterRules {
	/**
	 * How many of {@code braziers} burn at a health fraction: one more for each {@code 1 / (braziers + 1)} of her
	 * health lost (each seventh for the arena's six), all of them near the end.
	 */
	public static int litBraziers(float healthFraction, int braziers) {
		// The small nudge lights a brazier at exactly its share of health lost despite float rounding.
		return Math.max(0, Math.min(braziers, (int) Math.floor((1 - healthFraction) * (braziers + 1) + 1e-4)));
	}

	private FallenSisterRules() {}
}
