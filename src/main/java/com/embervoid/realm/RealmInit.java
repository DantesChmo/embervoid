package com.embervoid.realm;

import com.embervoid.realm.worldgen.RealmWorldgen;

/** Registers the realm framework; the one call from {@code Embervoid.onInitialize}. */
public final class RealmInit {
	public static void register() {
		RealmWorldgen.register();
		RealmBlocks.register();
		RealmPlayerData.register();
		RealmItems.register();
		RealmEvents.register();
		RealmMobLevels.register();
		RealmCommands.register();
	}

	private RealmInit() {}
}
