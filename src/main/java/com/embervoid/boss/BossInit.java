package com.embervoid.boss;

import java.util.Objects;

/** Registers the boss framework; called from {@code RealmInit}. */
public final class BossInit {
	public static void register() {
		Objects.requireNonNull(BossConfig.SECTION);
		BossEntities.register();
		BossItems.register();
		Telegraph.register();
		ArenaManager.register();
		BossCommands.register();
	}

	private BossInit() {}
}
