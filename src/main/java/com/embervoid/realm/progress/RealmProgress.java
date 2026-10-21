package com.embervoid.realm.progress;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * What the realm and boss code needs from per-player progress (DESIGN §4.3). The data itself belongs to the
 * sanctum track's {@code progress} API; this interface is the seam. Until that API is on {@code main},
 * {@link TransientRealmProgress} stands in.
 */
public interface RealmProgress {
	boolean hasBeatenBoss(ServerPlayerEntity player, Identifier bossId);

	void markBossBeaten(ServerPlayerEntity player, Identifier bossId);

	boolean isRealmUnlocked(ServerPlayerEntity player, Identifier realmId);

	boolean isRealmCompleted(ServerPlayerEntity player, Identifier realmId);

	/** Marks the realm completed, which unlocks the next one. */
	void markRealmCompleted(ServerPlayerEntity player, Identifier realmId);

	static RealmProgress get() {
		return TransientRealmProgress.INSTANCE;
	}
}
