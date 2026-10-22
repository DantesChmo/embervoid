package com.embervoid.boss;

/** Multiplayer scaling of a boss at summon (DESIGN §4.5): pure numbers, see {@link BossConfig}. */
public final class BossScaling {
	/** Max HP multiplier for {@code fighters} players: 1 + perExtra·(n − 1). */
	public static double healthMultiplier(int fighters, double perExtraFighter) {
		return 1 + perExtraFighter * Math.max(0, fighters - 1);
	}

	/** Adds summoned with the boss for {@code fighters} players: perExtra·(n − 1). */
	public static int adds(int fighters, int perExtraFighter) {
		return perExtraFighter * Math.max(0, fighters - 1);
	}

	/** Ticks until the next target switch, {@code roll} in [0, 1) picking between the bounds. */
	public static int switchTicks(float minSeconds, float maxSeconds, double roll) {
		return Math.max(1, Math.round((minSeconds + (maxSeconds - minSeconds) * (float) roll) * 20));
	}

	private BossScaling() {}
}
