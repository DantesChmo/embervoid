package com.embervoid.boss;

/** Registers the boss framework; called from {@code RealmInit}. */
public final class BossInit {
	public static void register() {
		BossEntities.register();
		BossItems.register();
		Telegraph.register();
		ArenaManager.register();
		BossCommands.register();
	}

	private BossInit() {}
}
