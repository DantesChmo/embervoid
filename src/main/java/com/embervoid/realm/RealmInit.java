package com.embervoid.realm;

import com.embervoid.boss.BossInit;
import com.embervoid.reach.world.ReachWorld;
import com.embervoid.realm.placed.PlacedEnemies;
import com.embervoid.realm.worldgen.RealmWorldgen;
import java.util.Objects;

/** Registers the realm framework; the one call from {@code Embervoid.onInitialize}. */
public final class RealmInit {
	public static void register() {
		Objects.requireNonNull(RealmConfig.SECTION);
		RealmWorldgen.register();
		PlacedEnemies.register();
		RealmBlocks.register();
		RealmPlayerData.register();
		RealmItems.register();
		RealmEvents.register();
		FinalZoneLock.register();
		RealmMobLevels.register();
		RealmCommands.register();
		BossInit.register();
		ReachWorld.register();
	}

	private RealmInit() {}
}
