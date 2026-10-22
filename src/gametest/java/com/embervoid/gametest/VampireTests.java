package com.embervoid.gametest;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.mob.MobLevels;
import com.embervoid.moon.MoonNightManager;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import com.embervoid.progress.ProgressManager;
import com.embervoid.sanctum.SanctumItems;
import com.embervoid.vampire.VampireBossEntity;
import com.embervoid.vampire.VampireEntities;
import com.embervoid.vampire.VampireEntity;
import com.embervoid.vampire.VampireFight;
import com.embervoid.vampire.VampireItems;
import com.embervoid.vampire.VampireNight;
import com.embervoid.vampire.VampireState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The blood new moon's Vampires and their boss (PLAN 8.10–8.11) on a real server. The test world has only the
 * vanilla dimensions, so the boss fights in the overworld here (the arena falls back to that and logs an error); the
 * arena dimension itself is covered by the server check.
 *
 * <p>The Vampires are one night per server, so every test has its own batch (tests of one batch run at the same
 * time). Each starts and ends with no moon night on; every step runs {@link #guarded}, so a failure stops the night
 * and removes the player before it reaches the runner.
 */
public final class VampireTests implements FabricGameTest {
	/** Ticks from a provoking hit until the boss stands there: the transformation plus a little slack. */
	private static final int FORM_TICKS = VampireEntity.TURN_TICKS + 10;

	@Nullable
	private static MoonNights.EndReason lastEndReason;

	static {
		MoonNights.NIGHT_ENDED.register((server, night, reason) -> lastEndReason = reason);
	}

	/** Blood moon → 1–3 friendly Vampires near the player that ignore normal damage; the night is held. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_friendly", tickLimit = 100)
	public void bloodMoonBringsFriendlyVampires(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		reset(server, null);
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		guarded(context, player, () -> {
			MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON);
			List<VampireEntity> vampires = vampires(context);
			context.assertTrue(vampires.size() >= 1 && vampires.size() <= 3, "expected 1-3 Vampires, got " + vampires.size());
			for (VampireEntity vampire : vampires) {
				context.assertTrue(vampire.distanceTo(player) <= 16, "Vampire " + vampire.distanceTo(player) + " blocks away");
			}
			context.assertTrue(MoonNights.holds(server).contains(VampireNight.HOLD), "night not held by the Vampires");

			VampireEntity vampire = vampires.get(0);
			ZombieEntity zombie = EntityType.ZOMBIE.create(context.getWorld());
			zombie.refreshPositionAndAngles(vampire.getX(), vampire.getY(), vampire.getZ(), 0, 0);
			vampire.damage(context.getWorld().getDamageSources().generic(), 10);
			vampire.damage(context.getWorld().getDamageSources().mobAttack(zombie), 10);
			vampire.damage(context.getWorld().getDamageSources().onFire(), 10);
			context.assertTrue(vampire.getHealth() == vampire.getMaxHealth() && vampire.isAlive(), "Vampire was hurt: " + vampire.getHealth());
			context.assertTrue(VampireState.get(server).stage() == VampireState.Stage.GROUPS, "non-player damage provoked it");

			MoonNightManager.forceStop(server);
			context.assertTrue(vampires.stream().allMatch(VampireEntity::isRemoved), "Vampires stayed after the night");
			finish(context, player);
		});
	}

	/**
	 * A player hit turns that Vampire (the others vanish) into a boss of level ≥ 15 with the matching mob level; a
	 * moon stop mid-fight removes the boss and gives no credit.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_hit", tickLimit = 200)
	public void playerHitTurnsVampireIntoBoss(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		reset(server, "{\"vampire\": {\"groupMin\": 3, \"groupMax\": 3}}");
		ServerPlayerEntity player = creativePlayer(context);
		guarded(context, player, () -> {
			MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON);
			List<VampireEntity> vampires = vampires(context);
			context.assertTrue(vampires.size() == 3, "expected 3 Vampires, got " + vampires.size());
			VampireEntity hit = vampires.get(0);
			player.attack(hit);
			context.assertTrue(hit.isTurning(), "the hit Vampire is not turning");
			context.assertTrue(vampires.stream().filter(v -> v != hit).allMatch(VampireEntity::isRemoved), "the other Vampires stayed");
		});
		context.waitAndRun(FORM_TICKS, () -> guarded(context, player, () -> {
			VampireState state = VampireState.get(server);
			context.assertTrue(state.stage() == VampireState.Stage.FIGHT, "no fight after the transformation: " + state.stage());
			VampireBossEntity boss = boss(context);
			context.assertTrue(boss != null, "no boss");
			context.assertTrue(state.bossLevel() >= 15, "boss level " + state.bossLevel());
			context.assertTrue(MobLevels.getLevel(boss) == state.bossLevel(), "mob level " + MobLevels.getLevel(boss) + " != " + state.bossLevel());
			context.assertTrue(vampires(context).isEmpty(), "a friendly Vampire is still there");

			MoonNightManager.forceStop(server);
			context.assertTrue(boss.isRemoved(), "boss stayed after the night was stopped");
			context.assertTrue(VampireState.get(server).stage() == VampireState.Stage.NONE, "stage " + VampireState.get(server).stage());
			context.assertTrue(!ProgressManager.hasBeatenBoss(player, VampireFight.BOSS_ID), "credit without a kill");
			context.assertTrue(player.getInventory().count(VampireItems.VAMPIRE_FANG) == 0, "loot without a kill");
			finish(context, player);
		}));
	}

	/** Nobody provokes one: after {@code vampire.provokeMinutes} (0.1 here) the nearest Vampire turns on its own. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_timeout", tickLimit = 300)
	public void unprovokedVampireTurnsAfterTimeout(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		reset(server, "{\"vampire\": {\"provokeMinutes\": 0.1}}");
		ServerPlayerEntity player = creativePlayer(context);
		guarded(context, player, () -> MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON));
		context.waitAndRun(100, () -> guarded(context, player, () -> context.assertTrue(
				VampireState.get(server).stage() == VampireState.Stage.GROUPS, "turned before the timeout: " + VampireState.get(server).stage())));
		context.waitAndRun(130 + FORM_TICKS, () -> guarded(context, player, () -> {
			VampireState state = VampireState.get(server);
			context.assertTrue(state.stage() == VampireState.Stage.FIGHT && boss(context) != null, "no boss after the timeout: " + state.stage());
			finish(context, player);
		}));
	}

	/**
	 * First kill: a Vampire Fang and two Flask Shards straight into the inventory, credit, and the night ends with
	 * GOAL_MET. Second kill (shard chance 1 here): one more shard, no second fang.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_kill", tickLimit = 400)
	public void killGivesPersonalLootAndEndsNight(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		reset(server, "{\"vampire\": {\"repeatShardChance\": 1.0}}");
		ServerPlayerEntity player = creativePlayer(context);
		int shardsBefore = player.getInventory().count(SanctumItems.FLASK_SHARD);
		provokeAndKill(context, player, () -> {
			context.assertTrue(player.getInventory().count(VampireItems.VAMPIRE_FANG) == 1, "no Vampire Fang");
			context.assertTrue(player.getInventory().count(SanctumItems.FLASK_SHARD) == shardsBefore + 2,
					"shards: " + (player.getInventory().count(SanctumItems.FLASK_SHARD) - shardsBefore));
			context.assertTrue(ProgressManager.hasBeatenBoss(player, VampireFight.BOSS_ID), "no credit");
			context.assertTrue(MoonNights.current(server) == MoonNight.NONE, "night still on after the kill");
			context.assertTrue(lastEndReason == MoonNights.EndReason.GOAL_MET, "night ended with " + lastEndReason);
			provokeAndKill(context, player, () -> {
				context.assertTrue(player.getInventory().count(VampireItems.VAMPIRE_FANG) == 1, "a second fang on a repeat kill");
				context.assertTrue(player.getInventory().count(SanctumItems.FLASK_SHARD) == shardsBefore + 3,
						"repeat shards: " + (player.getInventory().count(SanctumItems.FLASK_SHARD) - shardsBefore - 2));
				context.assertTrue(lastEndReason == MoonNights.EndReason.GOAL_MET, "second night ended with " + lastEndReason);
				finish(context, player);
			});
		});
	}

	/** Everyone taking part dies: the Vampire leaves, nobody gets credit, the night ends. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_all_dead", tickLimit = 200)
	public void everyoneDeadEndsNight(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		reset(server, null);
		ServerPlayerEntity player = creativePlayer(context);
		guarded(context, player, () -> {
			MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON);
			player.attack(vampires(context).get(0));
		});
		context.waitAndRun(FORM_TICKS, () -> guarded(context, player, () -> {
			context.assertTrue(boss(context) != null, "no boss");
			player.changeGameMode(GameMode.SURVIVAL);
			player.kill();
		}));
		context.waitAndRun(FORM_TICKS + 5, () -> guarded(context, player, () -> {
			context.assertTrue(boss(context) == null, "boss stayed with everyone dead");
			context.assertTrue(VampireState.get(server).stage() == VampireState.Stage.NONE, "stage " + VampireState.get(server).stage());
			context.assertTrue(MoonNights.current(server) == MoonNight.NONE, "night still on");
			context.assertTrue(lastEndReason == MoonNights.EndReason.GOAL_MET, "night ended with " + lastEndReason);
			context.assertTrue(!ProgressManager.hasBeatenBoss(player, VampireFight.BOSS_ID), "credit without a kill");
			finish(context, player);
		}));
	}

	/** With nobody in the overworld to meet them there are no Vampires, and nothing holds the night. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_no_players", tickLimit = 40)
	public void noPlayersNoVampiresNoHold(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		reset(server, null);
		guarded(context, null, () -> {
			MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON);
			context.assertTrue(vampires(context).isEmpty(), "Vampires with no players");
			context.assertTrue(VampireState.get(server).stage() == VampireState.Stage.NONE, "stage " + VampireState.get(server).stage());
			context.assertTrue(!MoonNights.holds(server).contains(VampireNight.HOLD), "night held with no Vampires");
			reset(server, null);
			context.complete();
		});
	}

	/** Starts a blood night, hits a Vampire, kills the boss once it formed, then runs {@code then} (guarded). */
	private static void provokeAndKill(TestContext context, ServerPlayerEntity player, Runnable then) {
		MinecraftServer server = context.getWorld().getServer();
		guarded(context, player, () -> {
			MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON);
			List<VampireEntity> vampires = vampires(context);
			context.assertTrue(!vampires.isEmpty(), "no Vampires");
			player.attack(vampires.get(0));
		});
		context.waitAndRun(FORM_TICKS, () -> guarded(context, player, () -> {
			VampireBossEntity boss = boss(context);
			context.assertTrue(boss != null, "no boss");
			boss.kill();
			context.waitAndRun(2, () -> guarded(context, player, then));
		}));
	}

	/** A creative player: the boss leaves them alone, so a test is not cut short by a stray hit. */
	private static ServerPlayerEntity creativePlayer(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		player.changeGameMode(GameMode.CREATIVE);
		return player;
	}

	private static List<VampireEntity> vampires(TestContext context) {
		return List.copyOf(context.getWorld().getEntitiesByType(VampireEntities.VAMPIRE, v -> !v.isRemoved()));
	}

	@Nullable
	private static VampireBossEntity boss(TestContext context) {
		List<? extends VampireBossEntity> bosses = context.getWorld().getEntitiesByType(VampireEntities.VAMPIRE_BOSS, b -> !b.isRemoved());
		return bosses.isEmpty() ? null : bosses.get(0);
	}

	/**
	 * Runs a test step; if it fails, the night is stopped, the config reset and the player removed before the failure
	 * reaches the test runner, so a broken test cannot leave a moon night running into the next batch.
	 */
	private static void guarded(TestContext context, @Nullable ServerPlayerEntity player, Runnable step) {
		try {
			step.run();
		} catch (RuntimeException | Error e) {
			reset(context.getWorld().getServer(), null);
			if (player != null && player.getServer().getPlayerManager().getPlayer(player.getUuid()) == player) {
				player.getServer().getPlayerManager().remove(player);
			}
			throw e;
		}
	}

	/** Leaves no night and no player behind, and completes the test. */
	private static void finish(TestContext context, ServerPlayerEntity player) {
		reset(context.getWorld().getServer(), null);
		TestPlayers.complete(context, player);
	}

	/** No moon night, the config file's values (or {@code configJson} on top of the defaults). */
	private static void reset(MinecraftServer server, @Nullable String configJson) {
		MoonNightManager.forceStop(server);
		lastEndReason = null;
		if (configJson == null) {
			EmbervoidConfig.useLocal();
		} else {
			EmbervoidConfig.useServer(EmbervoidConfig.fromNetworkString(configJson));
		}
	}
}
