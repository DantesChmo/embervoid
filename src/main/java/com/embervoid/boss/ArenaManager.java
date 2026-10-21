package com.embervoid.boss;

import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.Realms;
import com.embervoid.realm.api.ShrineHooks;
import com.embervoid.realm.worldgen.RealmFeatureStructure;
import com.embervoid.realm.worldgen.RealmFeatures;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the boss fights of all arenas. Arenas are found from the generated realm structures, so nothing about them
 * is saved: after a restart a leftover boss discards itself and the next visitor summons a fresh one.
 */
public final class ArenaManager {
	private record ArenaKey(RegistryKey<World> world, BlockPos center) {}

	private static final Map<ArenaKey, BossFight> FIGHTS = new HashMap<>();
	/** How often players are checked for entering an arena, in ticks. */
	private static final int ENTER_CHECK_INTERVAL = 5;

	static void register() {
		ServerTickEvents.END_WORLD_TICK.register(ArenaManager::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ArenaBoss) {
				BossFight fight = fightOf(entity.getUuid());
				if (fight != null) {
					fight.onBossDeath();
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			FIGHTS.values().forEach(BossFight::reset);
			FIGHTS.clear();
		});
		// DESIGN §10.8: an arena's shrine stays dormant for players who have not beaten its boss.
		ShrineHooks.ALLOW_REST.register((player, world, pos) -> {
			RealmFeatures.Placed feature = RealmFeatures.at(world, pos);
			if (feature == null || feature.structure().kind() != RealmFeatureStructure.Kind.ARENA
					|| !feature.piece().shrinePos().equals(pos)) {
				return true;
			}
			if (ProgressManager.hasBeatenBoss(player, feature.structure().boss().orElseThrow().bossId())) {
				return true;
			}
			return false; // the shrine tells the player
		});
	}

	private static void tick(ServerWorld world) {
		if (!Realms.isRealm(world)) {
			return;
		}
		if (world.getTime() % ENTER_CHECK_INTERVAL == 0) {
			for (ServerPlayerEntity player : world.getPlayers()) {
				if (!player.isSpectator() && player.isAlive()) {
					tryStart(world, player);
				}
			}
		}
		Iterator<BossFight> iterator = FIGHTS.values().iterator();
		while (iterator.hasNext()) {
			BossFight fight = iterator.next();
			if (fight.isEnded()) {
				iterator.remove();
			} else if (fight.world() == world) {
				fight.tick();
			}
		}
	}

	/** Summon rule: a player who has not beaten the boss steps inside an arena with no fight running. */
	private static void tryStart(ServerWorld world, ServerPlayerEntity player) {
		RealmFeatures.Placed feature = RealmFeatures.at(world, player.getBlockPos());
		if (feature == null || feature.structure().kind() != RealmFeatureStructure.Kind.ARENA) {
			return;
		}
		ArenaSpec spec = feature.structure().boss().orElseThrow();
		BlockPos center = feature.piece().center();
		double dx = player.getX() - (center.getX() + 0.5);
		double dz = player.getZ() - (center.getZ() + 0.5);
		ArenaKey key = new ArenaKey(world.getRegistryKey(), center);
		if (dx * dx + dz * dz > (spec.radius() - 1) * (spec.radius() - 1) || FIGHTS.containsKey(key)
				|| ProgressManager.hasBeatenBoss(player, spec.bossId())) {
			return;
		}
		BossFight fight = new BossFight(world, spec, center);
		FIGHTS.put(key, fight);
		if (!fight.start()) {
			FIGHTS.remove(key);
		}
	}

	@Nullable
	private static BossFight fightOf(UUID bossUuid) {
		for (BossFight fight : FIGHTS.values()) {
			if (bossUuid.equals(fight.bossUuid()) && !fight.isEnded()) {
				return fight;
			}
		}
		return null;
	}

	/** Call from an {@link ArenaBoss}'s server tick: a boss without a running fight (e.g. after a restart) leaves. */
	public static void tickBoss(MobEntity boss) {
		if (!boss.getWorld().isClient && fightOf(boss.getUuid()) == null) {
			boss.discard();
		}
	}

	/** Running fights, for the debug command. */
	public static Iterable<BossFight> fights() {
		return FIGHTS.values();
	}

	private ArenaManager() {}
}
