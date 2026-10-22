package com.embervoid.reach.boss;

/** Registers the Ashen Reach bosses (PLAN 12, 13.4); the one call from {@code Embervoid.onInitialize}. */
public final class ReachBosses {
	public static void register() {
		ReachBossEntities.register();
	}

	private ReachBosses() {}
}
