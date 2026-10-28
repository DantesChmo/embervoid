package com.embervoid.gametest;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.entity.AshRemnantEntity;
import com.embervoid.fx.CombatFx;
import com.embervoid.mob.MobLevels;
import com.embervoid.moon.MoonNightManager;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import com.embervoid.progress.ProgressManager;
import com.embervoid.registry.ModEntities;
import com.embervoid.sanctum.SanctumItems;
import com.embervoid.vampire.VampireArena;
import com.embervoid.vampire.VampireBossEntity;
import com.embervoid.vampire.VampireEntities;
import com.embervoid.vampire.VampireEntity;
import com.embervoid.vampire.VampireFight;
import com.embervoid.vampire.VampireItems;
import com.embervoid.vampire.VampireNight;
import com.embervoid.vampire.VampireState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The blood new moon's Vampires, their boss and its arena on a real server. The test world has the
 * datapack dimensions ({@code TestServerDimensionsMixin}), so the boss fights in {@code embervoid:vampire_arena} and
 * test players are pulled in and sent back as real ones would be.
 *
 * <p>The Vampires are one night per server, so every test has its own batch (tests of one batch run at the same
 * time). Each starts and ends with no moon night on. Every step runs through {@link Fight#step}: a failure stops the
 * night and removes the test's players before it reaches the runner.
 *
 * <p>The test server ticks much faster than 20 per second while chunk entity data loads on other threads, so a boss
 * that just spawned in the freshly force-loaded arena can take a moment to be found: steps wait for conditions
 * ({@link Fight#await}) instead of fixed tick counts.
 */
public final class VampireTests implements FabricGameTest {
	/** Upper bound for waiting on the transformation and the boss to show up. */
	private static final int FORM_LIMIT = VampireEntity.TURN_TICKS + 200;
	/** Upper bound for waiting on the arena to close after a kill. */
	private static final int CLOSE_LIMIT = VampireFight.CLOSE_TICKS + 200;

	@Nullable
	private static MoonNights.EndReason lastEndReason;

	static {
		MoonNights.NIGHT_ENDED.register((server, night, reason) -> lastEndReason = reason);
	}

	/** Blood moon → 1–3 friendly Vampires near the player that ignore normal damage; the night is held. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_friendly", tickLimit = 100)
	public void bloodMoonBringsFriendlyVampires(TestContext context) {
		Fight fight = new Fight(context, null);
		ServerPlayerEntity player = fight.add(TestPlayers.create(context, 2, 2));
		fight.step(() -> {
			MoonNightManager.forceStart(fight.server, MoonNight.BLOOD_NEW_MOON);
			List<VampireEntity> vampires = vampires(context);
			context.assertTrue(vampires.size() >= 1 && vampires.size() <= 3, "expected 1-3 Vampires, got " + vampires.size());
			for (VampireEntity vampire : vampires) {
				context.assertTrue(vampire.distanceTo(player) <= 16, "Vampire " + vampire.distanceTo(player) + " blocks away");
			}
			context.assertTrue(MoonNights.holds(fight.server).contains(VampireNight.HOLD), "night not held by the Vampires");

			VampireEntity vampire = vampires.get(0);
			ZombieEntity zombie = EntityType.ZOMBIE.create(context.getWorld());
			zombie.refreshPositionAndAngles(vampire.getX(), vampire.getY(), vampire.getZ(), 0, 0);
			vampire.damage(context.getWorld().getDamageSources().generic(), 10);
			vampire.damage(context.getWorld().getDamageSources().mobAttack(zombie), 10);
			vampire.damage(context.getWorld().getDamageSources().onFire(), 10);
			context.assertTrue(vampire.getHealth() == vampire.getMaxHealth() && vampire.isAlive(), "Vampire was hurt: " + vampire.getHealth());
			context.assertTrue(stage(fight.server) == VampireState.Stage.GROUPS, "non-player damage provoked it");

			MoonNightManager.forceStop(fight.server);
			context.assertTrue(vampires.stream().allMatch(VampireEntity::isRemoved), "Vampires stayed after the night");
			fight.finish();
		});
	}

	/**
	 * A player hit turns that Vampire (the others vanish) into a boss of level ≥ 15 with the matching mob level; a
	 * moon stop mid-fight removes the boss, sends the player back and gives no credit.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_hit", tickLimit = 400)
	public void playerHitTurnsVampireIntoBoss(TestContext context) {
		Fight fight = new Fight(context, "{\"vampire\": {\"groupMin\": 3, \"groupMax\": 3}}");
		ServerPlayerEntity player = fight.add(creativePlayer(context, 2, 2));
		fight.step(() -> {
			MoonNightManager.forceStart(fight.server, MoonNight.BLOOD_NEW_MOON);
			List<VampireEntity> vampires = vampires(context);
			context.assertTrue(vampires.size() == 3, "expected 3 Vampires, got " + vampires.size());
			VampireEntity hit = vampires.get(0);
			player.attack(hit);
			context.assertTrue(hit.isTurning(), "the hit Vampire is not turning");
			context.assertTrue(vampires.stream().filter(v -> v != hit).allMatch(VampireEntity::isRemoved), "the other Vampires stayed");
		});
		fight.awaitBoss(boss -> {
			VampireState state = VampireState.get(fight.server);
			context.assertTrue(state.bossLevel() >= 15, "boss level " + state.bossLevel());
			context.assertTrue(MobLevels.getLevel(boss) == state.bossLevel(), "mob level " + MobLevels.getLevel(boss) + " != " + state.bossLevel());
			context.assertTrue(vampires(context).isEmpty(), "a friendly Vampire is still there");

			MoonNightManager.forceStop(fight.server);
			context.assertTrue(boss.isRemoved(), "boss stayed after the night was stopped");
			context.assertTrue(stage(fight.server) == VampireState.Stage.NONE, "stage " + stage(fight.server));
			context.assertTrue(player.getWorld() == context.getWorld(), "not sent back when the night was stopped");
			context.assertTrue(!ProgressManager.hasBeatenBoss(player, VampireFight.BOSS_ID), "credit without a kill");
			context.assertTrue(player.getInventory().count(VampireItems.VAMPIRE_FANG) == 0, "loot without a kill");
			fight.finish();
		});
	}

	/** Nobody provokes one: after {@code vampire.provokeMinutes} (0.1 here) the nearest Vampire turns on its own. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_timeout", tickLimit = 600)
	public void unprovokedVampireTurnsAfterTimeout(TestContext context) {
		Fight fight = new Fight(context, "{\"vampire\": {\"provokeMinutes\": 0.1}}");
		fight.add(creativePlayer(context, 2, 2));
		fight.step(() -> MoonNightManager.forceStart(fight.server, MoonNight.BLOOD_NEW_MOON));
		context.waitAndRun(100, () -> fight.step(() -> context.assertTrue(stage(fight.server) == VampireState.Stage.GROUPS,
				"turned before the timeout: " + stage(fight.server))));
		fight.awaitBoss(boss -> fight.finish());
	}

	/**
	 * First kill: a Vampire Fang and two Flask Shards straight into the inventory, credit, and once the arena closed the
	 * night ends with GOAL_MET. Second kill (shard chance 1 here): one more shard, no second fang.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_kill", tickLimit = 1600)
	public void killGivesPersonalLootAndEndsNight(TestContext context) {
		Fight fight = new Fight(context, "{\"vampire\": {\"repeatShardChance\": 1.0}}");
		ServerPlayerEntity player = fight.add(creativePlayer(context, 2, 2));
		int shardsBefore = player.getInventory().count(SanctumItems.FLASK_SHARD);
		fight.provokeAndKill(player, () -> {
			context.assertTrue(player.getInventory().count(VampireItems.VAMPIRE_FANG) == 1, "no Vampire Fang");
			context.assertTrue(player.getInventory().count(SanctumItems.FLASK_SHARD) == shardsBefore + 2,
					"shards: " + (player.getInventory().count(SanctumItems.FLASK_SHARD) - shardsBefore));
			context.assertTrue(ProgressManager.hasBeatenBoss(player, VampireFight.BOSS_ID), "no credit");
			context.assertTrue(MoonNights.current(fight.server) == MoonNight.NONE, "night still on after the kill");
			context.assertTrue(lastEndReason == MoonNights.EndReason.GOAL_MET, "night ended with " + lastEndReason);
			fight.provokeAndKill(player, () -> {
				context.assertTrue(player.getInventory().count(VampireItems.VAMPIRE_FANG) == 1, "a second fang on a repeat kill");
				context.assertTrue(player.getInventory().count(SanctumItems.FLASK_SHARD) == shardsBefore + 3,
						"repeat shards: " + (player.getInventory().count(SanctumItems.FLASK_SHARD) - shardsBefore - 2));
				context.assertTrue(lastEndReason == MoonNights.EndReason.GOAL_MET, "second night ended with " + lastEndReason);
				fight.finish();
			});
		});
	}

	/** Everyone taking part dies: the Vampire leaves, nobody gets credit, the night ends. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_all_dead", tickLimit = 600)
	public void everyoneDeadEndsNight(TestContext context) {
		Fight fight = new Fight(context, null);
		ServerPlayerEntity player = fight.add(creativePlayer(context, 2, 2));
		fight.step(() -> {
			MoonNightManager.forceStart(fight.server, MoonNight.BLOOD_NEW_MOON);
			player.attack(vampires(context).get(0));
		});
		fight.awaitBoss(boss -> {
			player.changeGameMode(GameMode.SURVIVAL);
			player.kill();
			context.assertTrue(player.isDead(), "the player did not die");
			fight.await(() -> stage(fight.server) == VampireState.Stage.NONE, 100, "the fight to end with everyone dead", () -> {
				context.assertTrue(boss.isRemoved(), "boss stayed with everyone dead");
				context.assertTrue(MoonNights.current(fight.server) == MoonNight.NONE, "night still on");
				context.assertTrue(lastEndReason == MoonNights.EndReason.GOAL_MET, "night ended with " + lastEndReason);
				context.assertTrue(!ProgressManager.hasBeatenBoss(player, VampireFight.BOSS_ID), "credit without a kill");
				fight.finish();
			});
		});
	}

	/**
	 * The arena with a player in it: pulled in when the boss forms (return point saved, arena built, chunks forced); the
	 * kill keeps them inside for {@link VampireFight#CLOSE_TICKS}, then they stand at their return point again.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_arena_return", tickLimit = 800)
	public void arenaPullsInAndReturnsAfterKill(TestContext context) {
		Fight fight = new Fight(context, null);
		ServerPlayerEntity player = fight.add(creativePlayer(context, 2, 2));
		BlockPos start = player.getBlockPos();
		fight.step(() -> {
			context.assertTrue(VampireArena.world(fight.server) != null, "no Vampire arena dimension in the test world");
			MoonNightManager.forceStart(fight.server, MoonNight.BLOOD_NEW_MOON);
			player.attack(vampires(context).get(0));
		});
		fight.awaitBoss(boss -> {
			ServerWorld arena = VampireArena.world(fight.server);
			VampireState state = VampireState.get(fight.server);
			context.assertTrue(player.getWorld() == arena, "player not pulled in: " + player.getWorld().getRegistryKey().getValue());
			context.assertTrue(boss.getWorld() == arena && state.fightWorld() == VampireArena.KEY, "boss not in the arena");
			VampireState.ReturnPoint point = state.returnPoint(player.getUuid());
			context.assertTrue(point != null && point.pos().getPos().equals(start)
					&& point.pos().getDimension() == World.OVERWORLD, "return point " + point + ", expected " + start);
			context.assertTrue(VampireArena.isForced(arena), "arena chunks not forced while open");
			context.assertTrue(arena.getBlockState(new BlockPos(0, 64, 0)).isOf(Blocks.CRYING_OBSIDIAN), "arena not built");
			boss.kill();
			context.assertTrue(state.stage() == VampireState.Stage.CLOSING, "not closing after the kill: " + state.stage());
			context.assertTrue(player.getWorld() == arena, "sent back before the arena closed");
			fight.await(() -> stage(fight.server) == VampireState.Stage.NONE, CLOSE_LIMIT, "the arena to close", () -> {
				context.assertTrue(player.getWorld() == context.getWorld() && player.getBlockPos().equals(start),
						"not back at the return point: " + player.getWorld().getRegistryKey().getValue() + " " + player.getBlockPos());
				context.assertTrue(!VampireArena.isForced(arena), "arena chunks still forced after closing");
				context.assertTrue(VampireState.get(fight.server).returnPoint(player.getUuid()) == null, "return point kept");
				context.assertTrue(lastEndReason == MoonNights.EndReason.GOAL_MET, "night ended with " + lastEndReason);
				context.assertTrue(ProgressManager.hasBeatenBoss(player, VampireFight.BOSS_ID), "no credit");
				fight.finish();
			});
		});
	}

	/**
	 * A fighter who dies in the arena leaves a remnant there, respawns outside and stays out (no fighter again, not
	 * pulled back, no credit) while the fight goes on with the survivor. When the arena closes the remnant, with the
	 * items, lies at the fallen player's return point in the overworld and none is left in the arena.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_arena_death", tickLimit = 1000)
	public void arenaDeathStaysOutAndRemnantMovesHome(TestContext context) {
		Fight fight = new Fight(context, null);
		ServerPlayerEntity survivor = fight.add(creativePlayer(context, 2, 2));
		ServerPlayerEntity[] victim = {fight.add(TestPlayers.create(context, 4, 2))};
		BlockPos start = victim[0].getBlockPos();
		fight.step(() -> {
			victim[0].getInventory().setStack(5, new ItemStack(Items.DIAMOND, 7));
			MoonNightManager.forceStart(fight.server, MoonNight.BLOOD_NEW_MOON);
			survivor.attack(vampires(context).get(0));
		});
		fight.awaitBoss(boss -> {
			ServerWorld arena = VampireArena.world(fight.server);
			context.assertTrue(victim[0].getWorld() == arena && survivor.getWorld() == arena, "players not pulled in");
			victim[0].kill();
			context.assertTrue(victim[0].isDead(), "the victim did not die");
			fight.await(() -> !remnants(arena, victim[0]).isEmpty(), 100, "the remnant in the arena", () -> {
				victim[0] = fight.add(TestPlayers.respawn(victim[0]));
				context.assertTrue(victim[0].getWorld() != arena, "respawned inside the arena");
				context.waitAndRun(20, () -> fight.step(() -> {
					context.assertTrue(victim[0].getWorld() != arena, "the fallen player got back in");
					context.assertTrue(stage(fight.server) == VampireState.Stage.FIGHT, "fight ended with a survivor: " + stage(fight.server));
					boss.kill();
					fight.await(() -> stage(fight.server) == VampireState.Stage.NONE, CLOSE_LIMIT, "the arena to close", () -> {
						List<AshRemnantEntity> home = remnants(context.getWorld(), victim[0]);
						context.assertTrue(home.size() == 1, "remnant not at the return point: " + home.size() + " in the overworld");
						context.assertTrue(home.get(0).getBlockPos().equals(start), "remnant at " + home.get(0).getBlockPos() + ", expected " + start);
						context.assertTrue(remnants(arena, victim[0]).isEmpty(), "a remnant stayed in the arena");
						context.assertTrue(survivor.getWorld() == context.getWorld(), "survivor not sent back");
						context.assertTrue(!ProgressManager.hasBeatenBoss(victim[0], VampireFight.BOSS_ID), "the fallen player got credit");
						context.assertTrue(ProgressManager.hasBeatenBoss(survivor, VampireFight.BOSS_ID), "the survivor got no credit");
						home.get(0).interact(victim[0], Hand.MAIN_HAND);
						context.assertTrue(victim[0].getInventory().count(Items.DIAMOND) == 7,
								"remnant lost the items: " + victim[0].getInventory().count(Items.DIAMOND));
						fight.finish();
					});
				}));
			});
		});
	}

	/** With nobody in the overworld to meet them there are no Vampires, and nothing holds the night. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_no_players", tickLimit = 40)
	public void noPlayersNoVampiresNoHold(TestContext context) {
		Fight fight = new Fight(context, null);
		fight.step(() -> {
			MoonNightManager.forceStart(fight.server, MoonNight.BLOOD_NEW_MOON);
			context.assertTrue(vampires(context).isEmpty(), "Vampires with no players");
			context.assertTrue(stage(fight.server) == VampireState.Stage.NONE, "stage " + stage(fight.server));
			context.assertTrue(!MoonNights.holds(fight.server).contains(VampireNight.HOLD), "night held with no Vampires");
			fight.finish();
		});
	}

	/** The Blood Leap's landing shakes and flashes for the fighter, not before it comes down. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "vampire_leap_fx", tickLimit = FORM_LIMIT + 200)
	public void leapLandingShakes(TestContext context) {
		Fight fight = new Fight(context, null);
		BossArena.FogRecorder connection = new BossArena.FogRecorder();
		ServerPlayerEntity player = fight.add(TestPlayers.join(context, 2, 2, connection));
		// Creative: the boss leaves them alone, so the only leap is the test's.
		player.changeGameMode(GameMode.CREATIVE);
		fight.step(() -> {
			MoonNightManager.forceStart(fight.server, MoonNight.BLOOD_NEW_MOON);
			player.attack(vampires(context).get(0));
		});
		fight.awaitBoss(boss -> {
			context.assertTrue(connection.fx.isEmpty(), "effects before the leap: " + connection.fx.size());
			boss.startLeap(boss.getPos().add(6, 0, 0));
			context.waitAndRun(10, () -> fight.step(() -> {
				context.assertTrue(connection.fx.isEmpty(), "effects in the leap's windup: " + connection.fx.size());
				fight.await(() -> connection.fx(CombatFx.SHAKE_PACKET) > 0, 80, "the landing to shake", () -> {
					context.assertTrue(connection.fx(CombatFx.SHAKE_PACKET) == 1 && connection.fx(CombatFx.FLASH_PACKET) == 1,
							connection.fx(CombatFx.SHAKE_PACKET) + " shakes, " + connection.fx(CombatFx.FLASH_PACKET) + " flashes");
					fight.finish();
				});
			}));
		});
	}

	/**
	 * One test's night: its players, its config, its cleanup. Created at the start of a test (no night on, config
	 * applied); {@link #finish} or any failing {@link #step} stops the night, restores the config and removes the
	 * players.
	 */
	private static final class Fight {
		final TestContext context;
		final MinecraftServer server;
		private final List<ServerPlayerEntity> players = new ArrayList<>();

		Fight(TestContext context, @Nullable String configJson) {
			this.context = context;
			this.server = context.getWorld().getServer();
			MoonNightManager.forceStop(server);
			lastEndReason = null;
			if (configJson == null) {
				EmbervoidConfig.useLocal();
			} else {
				EmbervoidConfig.useServer(EmbervoidConfig.fromNetworkString(configJson));
			}
		}

		ServerPlayerEntity add(ServerPlayerEntity player) {
			players.add(player);
			return player;
		}

		void step(Runnable step) {
			try {
				step.run();
			} catch (RuntimeException | Error e) {
				cleanUp();
				throw e;
			}
		}

		/** Runs {@code then} once {@code condition} holds, checking every tick; fails after {@code limit} ticks. */
		void await(BooleanSupplier condition, int limit, String what, Runnable then) {
			step(() -> {
				if (condition.getAsBoolean()) {
					then.run();
				} else if (limit <= 0) {
					throw new GameTestException("gave up waiting for " + what + " (" + debug(server) + ")");
				} else {
					context.waitAndRun(1, () -> await(condition, limit - 1, what, then));
				}
			});
		}

		/**
		 * Waits for the boss to stand in its fight (after the transformation and the pull into the arena), tells pulled
		 * test players their teleport is done (a real client confirms it; until then a player cannot be hurt), then
		 * runs {@code then}.
		 */
		void awaitBoss(Consumer<VampireBossEntity> then) {
			await(() -> boss(server) != null, FORM_LIMIT, "the boss", () -> {
				for (ServerPlayerEntity player : players) {
					player.onTeleportationDone();
				}
				then.accept(boss(server));
			});
		}

		/** Starts a blood night, hits a Vampire, kills the boss, waits for the arena to close, then runs {@code then}. */
		void provokeAndKill(ServerPlayerEntity player, Runnable then) {
			step(() -> {
				MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON);
				List<VampireEntity> vampires = vampires(context);
				context.assertTrue(!vampires.isEmpty(), "no Vampires");
				player.attack(vampires.get(0));
			});
			awaitBoss(boss -> {
				boss.kill();
				await(() -> stage(server) == VampireState.Stage.NONE, CLOSE_LIMIT, "the arena to close", then);
			});
		}

		void finish() {
			cleanUp();
			context.complete();
		}

		private void cleanUp() {
			MoonNightManager.forceStop(server);
			EmbervoidConfig.useLocal();
			for (ServerPlayerEntity player : players) {
				if (server.getPlayerManager().getPlayer(player.getUuid()) == player) {
					server.getPlayerManager().remove(player);
				}
			}
		}
	}

	/** A creative player: the boss leaves them alone, so a test is not cut short by a stray hit. */
	private static ServerPlayerEntity creativePlayer(TestContext context, int x, int z) {
		ServerPlayerEntity player = TestPlayers.create(context, x, z);
		player.changeGameMode(GameMode.CREATIVE);
		return player;
	}

	private static VampireState.Stage stage(MinecraftServer server) {
		return VampireState.get(server).stage();
	}

	private static List<VampireEntity> vampires(TestContext context) {
		return List.copyOf(context.getWorld().getEntitiesByType(VampireEntities.VAMPIRE, v -> !v.isRemoved()));
	}

	private static List<AshRemnantEntity> remnants(ServerWorld world, ServerPlayerEntity owner) {
		return List.copyOf(world.getEntitiesByType(ModEntities.ASH_REMNANT, r -> owner.getUuid().equals(r.getOwner())));
	}

	/** The boss of the running fight, in whatever world it fights in (the arena), or null. */
	@Nullable
	private static VampireBossEntity boss(MinecraftServer server) {
		VampireState state = VampireState.get(server);
		ServerWorld world = state.fightWorld() == null ? null : server.getWorld(state.fightWorld());
		return world != null && state.boss() != null && world.getEntity(state.boss()) instanceof VampireBossEntity boss
				&& !boss.isRemoved() ? boss : null;
	}

	private static String debug(MinecraftServer server) {
		VampireState state = VampireState.get(server);
		return "stage " + state.stage() + ", boss " + state.boss() + " in " + state.fightWorld();
	}
}
