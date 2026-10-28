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
import net.minecraft.text.Text;
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
	/** Arenas primed for a rematch (Ash of Rematch): game time until which the next visitor summons the boss. */
	private static final Map<ArenaKey, Long> REMATCHES = new HashMap<>();
	/** How long a primed rematch waits for someone to step in, in ticks. */
	public static final int REMATCH_TICKS = 1200;
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
		// Target switching counts who hurts the boss (DESIGN §4.5 "Multiplayer").
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ArenaBoss && source.getAttacker() instanceof ServerPlayerEntity player) {
				BossFight fight = fightOf(entity.getUuid());
				if (fight != null) {
					fight.recordDamage(player, amount);
				}
			}
			return true;
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			FIGHTS.values().forEach(BossFight::reset);
			FIGHTS.clear();
			REMATCHES.clear();
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
			player.sendMessage(Text.translatable("embervoid.shrine.dormant"), true);
			return false;
		});
	}

	private static void tick(ServerWorld world) {
		if (Realms.isRealm(world) && world.getTime() % ENTER_CHECK_INTERVAL == 0) {
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
		if (dx * dx + dz * dz > (spec.radius() - 1) * (spec.radius() - 1) || fightAt(world, center) != null) {
			return;
		}
		ArenaKey key = new ArenaKey(world.getRegistryKey(), center);
		Long primed = REMATCHES.get(key);
		boolean rematch = primed != null && world.getTime() <= primed;
		if (primed != null && !rematch) {
			REMATCHES.remove(key);
		}
		if (!rematch && ProgressManager.hasBeatenBoss(player, spec.bossId())) {
			return;
		}
		BossFight fight = start(world, spec, center);
		if (fight != null && rematch) {
			REMATCHES.remove(key);
			fight.setRematch(true);
		}
	}

	/**
	 * Primes an arena for a rematch (Ash of Rematch): for {@link #REMATCH_TICKS} the next player to step in
	 * summons its boss even if everyone has beaten it; that fight {@link BossFight#isRematch is a rematch}.
	 */
	public static void primeRematch(ServerWorld world, BlockPos center) {
		REMATCHES.put(new ArenaKey(world.getRegistryKey(), center.toImmutable()), world.getTime() + REMATCH_TICKS);
	}

	public static boolean isRematchPrimed(ServerWorld world, BlockPos center) {
		Long primed = REMATCHES.get(new ArenaKey(world.getRegistryKey(), center));
		return primed != null && world.getTime() <= primed;
	}

	/**
	 * Starts a fight in any world: summons the boss at {@code center} (the arena floor block) and makes everyone inside
	 * {@code spec.radius()} a fighter. Returns null if a fight already runs there or the boss cannot be created.
	 * The summon rule calls this for realm arenas; GameTests and rematches call it directly.
	 */
	@Nullable
	public static BossFight start(ServerWorld world, ArenaSpec spec, BlockPos center) {
		ArenaKey key = new ArenaKey(world.getRegistryKey(), center.toImmutable());
		BossFight running = FIGHTS.get(key);
		if (running != null && !running.isEnded()) {
			return null;
		}
		BossFight fight = new BossFight(world, spec, center.toImmutable());
		FIGHTS.put(key, fight);
		if (!fight.start()) {
			FIGHTS.remove(key);
			return null;
		}
		return fight;
	}

	/** The fight running at an arena center, or null. */
	@Nullable
	public static BossFight fightAt(ServerWorld world, BlockPos center) {
		BossFight fight = FIGHTS.get(new ArenaKey(world.getRegistryKey(), center));
		return fight == null || fight.isEnded() ? null : fight;
	}

	/** Ends a running fight without a kill, as if every fighter had left (debug, tests). */
	public static void reset(BossFight fight) {
		fight.reset();
	}

	@Nullable
	static BossFight fightOf(UUID bossUuid) {
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
