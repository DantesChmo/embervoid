package com.embervoid.realm.progress;

import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.Realms;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TEMPORARY stand-in for the sanctum track's progress API: in memory only, lost on restart, never saved, so it is
 * not a second copy of that data. Delete once {@link RealmProgress#get()} delegates to the real API.
 */
final class TransientRealmProgress implements RealmProgress {
	static final TransientRealmProgress INSTANCE = new TransientRealmProgress();

	private final Map<UUID, Set<Identifier>> beaten = new ConcurrentHashMap<>();
	private final Map<UUID, Set<Identifier>> completed = new ConcurrentHashMap<>();

	@Override
	public boolean hasBeatenBoss(ServerPlayerEntity player, Identifier bossId) {
		return beaten.getOrDefault(player.getUuid(), Set.of()).contains(bossId);
	}

	@Override
	public void markBossBeaten(ServerPlayerEntity player, Identifier bossId) {
		beaten.computeIfAbsent(player.getUuid(), uuid -> new HashSet<>()).add(bossId);
	}

	@Override
	public boolean isRealmUnlocked(ServerPlayerEntity player, Identifier realmId) {
		// Realm 1 is always open; each later realm opens when the one before it is completed.
		RealmDefinition previous = null;
		for (RealmDefinition realm : Realms.ALL) {
			if (realm.id().equals(realmId)) {
				return previous == null || isRealmCompleted(player, previous.id());
			}
			previous = realm;
		}
		return false;
	}

	@Override
	public boolean isRealmCompleted(ServerPlayerEntity player, Identifier realmId) {
		return completed.getOrDefault(player.getUuid(), Set.of()).contains(realmId);
	}

	@Override
	public void markRealmCompleted(ServerPlayerEntity player, Identifier realmId) {
		completed.computeIfAbsent(player.getUuid(), uuid -> new HashSet<>()).add(realmId);
	}
}
