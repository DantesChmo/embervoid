package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.gametest.mixin.ServerPlayerEntityAccessor;
import com.embervoid.mob.MobLevels;
import com.embervoid.moon.MoonNightManager;
import com.embervoid.moon.api.CurseOfTheNight;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import com.embervoid.nightmob.AshenZombieEntity;
import com.embervoid.nightmob.GhostEntity;
import com.embervoid.nightmob.NightMobConfig;
import com.embervoid.nightmob.NightMobs;
import com.embervoid.nightmob.NightWaves;
import com.embervoid.registry.ModEntities;
import com.embervoid.sanctum.SanctumItems;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Night mobs and full moon waves (PLAN 8.7–8.9) on a real server player. The moon night is global, so every test
 * that starts one runs in a batch of its own (tests of one batch run at the same time; batches one after another)
 * and stops the night at the end.
 */
public final class NightMobTests implements FabricGameTest {
	/** How the last moon night ended, recorded by a listener registered once. */
	private static volatile MoonNights.EndReason lastEnd;

	static {
		MoonNights.NIGHT_ENDED.register((server, night, reason) -> lastEnd = reason);
	}

	/** Curse 3 + full moon: 3 Ashen Zombies in the ring at spot level + 5, the wave holds the night. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "nightmobs_zombie_wave")
	public void cursedPlayerGetsZombieWave(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		safe(context, player, () -> {
			List<MobEntity> wave = startFullMoon(context, player, 3);
			context.assertTrue(wave.size() == 3, "expected 3 wave mobs, got " + wave.size());
			ServerWorld world = context.getWorld();
			for (MobEntity mob : wave) {
				context.assertTrue(mob instanceof AshenZombieEntity, "wave mob is " + mob.getType().getUntranslatedName());
				assertInRing(context, player, mob);
				int expected = MobLevels.defaultLevel(mob, world) + 5;
				context.assertTrue(MobLevels.getLevel(mob) == expected,
						"zombie level " + MobLevels.getLevel(mob) + ", expected spot level + 5 = " + expected);
			}
			context.assertTrue(MoonNights.holds(world.getServer()).contains(NightWaves.holdId(player.getUuid())),
					"wave does not hold the night: " + MoonNights.holds(world.getServer()));
			finish(context, player);
		}).run();
	}

	/** Killing the whole wave releases its hold and ends the night with GOAL_MET. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "nightmobs_wave_cleared", tickLimit = 100)
	public void killingTheWaveEndsTheNight(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		safe(context, player, () -> {
			MinecraftServer server = context.getWorld().getServer();
			List<MobEntity> wave = startFullMoon(context, player, 3);
			context.assertTrue(wave.size() == 3, "expected 3 wave mobs, got " + wave.size());
			killNearPlayer(wave, player);
			// A dead mob is removed 20 ticks after its death.
			context.waitAndRun(30, safe(context, player, () -> {
				context.assertTrue(!MoonNights.holds(server).contains(NightWaves.holdId(player.getUuid())), "hold still open");
				context.assertTrue(MoonNights.current(server) == MoonNight.NONE, "night still on: " + MoonNights.current(server));
				context.assertTrue(lastEnd == MoonNights.EndReason.GOAL_MET, "night ended with " + lastEnd);
				finish(context, player);
			}));
		}).run();
	}

	/** No curse: {@code nightMobs.ghostCount} Ghosts; killing them all gives a Flask Shard. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "nightmobs_ghost_wave", tickLimit = 100)
	public void uncursedPlayerGetsGhostsAndAShard(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		safe(context, player, () -> {
			List<MobEntity> wave = startFullMoon(context, player, 0);
			int ghosts = NightMobConfig.get().ghostCount();
			context.assertTrue(wave.size() == ghosts, "expected " + ghosts + " ghosts, got " + wave.size());
			for (MobEntity mob : wave) {
				context.assertTrue(mob instanceof GhostEntity, "wave mob is " + mob.getType().getUntranslatedName());
				assertInRing(context, player, mob);
			}
			context.assertTrue(player.getInventory().count(SanctumItems.FLASK_SHARD) == 0, "shard before the wave is beaten");
			killNearPlayer(wave, player);
			context.waitAndRun(30, safe(context, player, () -> {
				context.assertTrue(player.getInventory().count(SanctumItems.FLASK_SHARD) == 1,
						"Flask Shards after the ghost wave: " + player.getInventory().count(SanctumItems.FLASK_SHARD));
				context.assertTrue(MoonNights.current(context.getWorld().getServer()) == MoonNight.NONE, "night still on");
				finish(context, player);
			}));
		}).run();
	}

	/**
	 * A Ghost wave ends when its player dies (R40.1c): the Ghosts vanish and the wave's hold is released, while the
	 * death still adds its curse stack. Another hold keeps the night on, as another player's wave would.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "nightmobs_ghost_death", tickLimit = 100)
	public void ghostWaveEndsWhenItsPlayerDies(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		MinecraftServer server = context.getWorld().getServer();
		Identifier other = Embervoid.id("test/other_wave");
		safe(context, player, () -> {
			List<MobEntity> wave = startFullMoon(context, player, 0);
			context.assertTrue(!wave.isEmpty() && wave.get(0) instanceof GhostEntity, "no ghost wave: " + wave);
			MoonNights.hold(server, other);
			((ServerPlayerEntityAccessor) player).embervoid$setJoinInvulnerabilityTicks(0);
			player.damage(wave.get(0).getDamageSources().mobAttack(wave.get(0)), 1000);
			context.assertTrue(player.isDead(), "the player survived");
			context.waitAndRun(2, safe(context, player, () -> {
				context.assertTrue(NightWaves.waveOf(server, player.getUuid()).isEmpty(), "the wave is still there");
				for (MobEntity ghost : wave) {
					context.assertTrue(ghost.isRemoved(), "a ghost stayed");
				}
				context.assertTrue(!MoonNights.holds(server).contains(NightWaves.holdId(player.getUuid())), "the wave still holds the night");
				context.assertTrue(MoonNights.current(server) == MoonNight.FULL_MOON, "the night ended with another hold open");
				context.assertTrue(CurseOfTheNight.stacks(player) == 1, "curse stacks after the death: " + CurseOfTheNight.stacks(player));
				MoonNights.release(server, other);
				context.getWorld().getEntitiesByType(ModEntities.ASH_REMNANT, remnant -> player.getUuid().equals(remnant.getOwner()))
						.forEach(Entity::discard);
				finish(context, player);
			}));
		}).run();
	}

	/** Stopping the night removes the wave mobs. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "nightmobs_stop", tickLimit = 60)
	public void stoppingTheNightRemovesTheWave(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		safe(context, player, () -> {
			List<MobEntity> wave = startFullMoon(context, player, 2);
			context.assertTrue(wave.size() == 2, "expected 2 wave mobs, got " + wave.size());
			MoonNightManager.forceStop(context.getWorld().getServer());
			for (MobEntity mob : wave) {
				context.assertTrue(mob.isRemoved(), "wave mob still there after the night ended");
			}
			context.assertTrue(NightWaves.waveOf(context.getWorld().getServer(), player.getUuid()).isEmpty(), "wave not forgotten");
			finish(context, player);
		}).run();
	}

	/** A player in the Nether when the full moon starts gets the wave on coming back while the night is on. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "nightmobs_away", tickLimit = 120)
	public void playerAwayGetsTheWaveOnReturn(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		safe(context, player, () -> {
			MinecraftServer server = context.getWorld().getServer();
			ServerWorld nether = server.getWorld(World.NETHER);
			context.assertTrue(nether != null, "the test world has no Nether");
			Vec3d home = player.getPos();
			player.teleport(nether, 0.5, 120, 0.5, 0, 0);
			startFullMoon(context, player, 0);
			context.waitAndRun(25, safe(context, player, () -> {
				context.assertTrue(NightWaves.waveOf(server, player.getUuid()).isEmpty(), "wave spawned while in the Nether");
				player.teleport(context.getWorld(), home.x, home.y, home.z, 0, 0);
				context.waitAndRun(25, safe(context, player, () -> {
					int count = NightWaves.waveOf(server, player.getUuid()).size();
					int ghosts = NightMobConfig.get().ghostCount();
					context.assertTrue(count == ghosts, "after returning: " + count + " wave mobs, expected " + ghosts + " ghosts");
					finish(context, player);
				}));
			}));
		}).run();
	}

	/** A Ghost inside solid blocks takes no suffocation damage. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "nightmobs_ghost", tickLimit = 80)
	public void ghostDoesNotSuffocate(TestContext context) {
		for (int x = 1; x <= 3; x++) {
			for (int y = 1; y <= 4; y++) {
				for (int z = 1; z <= 3; z++) {
					context.setBlockState(x, y, z, Blocks.STONE);
				}
			}
		}
		GhostEntity ghost = context.spawnMob(NightMobs.GHOST, 2, 2, 2);
		ghost.setAiDisabled(true);
		context.waitAndRun(60, () -> {
			context.assertTrue(ghost.isAlive() && ghost.getHealth() == ghost.getMaxHealth(),
					"ghost in stone: " + ghost.getHealth() + "/" + ghost.getMaxHealth());
			context.complete();
		});
	}

	/** Miss chance 1: every hit passes through; miss chance 0: every hit lands. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "nightmobs_ghost")
	public void ghostMissChance(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 1, 1);
		GhostEntity always = context.spawnMob(NightMobs.GHOST, 4, 2, 4);
		GhostEntity never = context.spawnMob(NightMobs.GHOST, 6, 2, 6);
		always.setAiDisabled(true);
		never.setAiDisabled(true);
		try {
			useMissChance(1);
			for (int i = 0; i < 20; i++) {
				always.timeUntilRegen = 0;
				context.assertTrue(!always.damage(context.getWorld().getDamageSources().playerAttack(player), 5),
						"a hit landed with miss chance 1");
			}
			context.assertTrue(always.getHealth() == always.getMaxHealth(), "ghost hurt with miss chance 1: " + always.getHealth());
			useMissChance(0);
			context.assertTrue(never.damage(context.getWorld().getDamageSources().playerAttack(player), 5),
					"the hit missed with miss chance 0");
			context.assertTrue(never.getHealth() < never.getMaxHealth(), "ghost not hurt with miss chance 0");
		} finally {
			EmbervoidConfig.useLocal();
		}
		TestPlayers.complete(context, player);
	}

	/** An Ashen Zombie under the noon sun does not burn; a vanilla zombie next to it does (the control). */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "nightmobs_daylight", tickLimit = 140)
	public void ashenZombieDoesNotBurnInDaylight(TestContext context) {
		context.getWorld().setTimeOfDay(6000);
		AshenZombieEntity ashen = context.spawnMob(NightMobs.ASHEN_ZOMBIE, 2, 1, 2);
		ZombieEntity zombie = context.spawnMob(EntityType.ZOMBIE, 5, 1, 5);
		ashen.setAiDisabled(true);
		zombie.setAiDisabled(true);
		context.waitAndRun(120, () -> {
			context.assertTrue(context.getWorld().isSkyVisible(zombie.getBlockPos()) && zombie.isOnFire(),
					"control: the vanilla zombie did not burn, so this spot is not in daylight");
			context.assertTrue(!ashen.isOnFire() && ashen.getHealth() == ashen.getMaxHealth(), "the Ashen Zombie burned");
			context.complete();
		});
	}

	/** Sets the player's curse, starts a full moon (which serves overworld players at once) and returns the wave. */
	private static List<MobEntity> startFullMoon(TestContext context, ServerPlayerEntity player, int stacks) {
		MinecraftServer server = context.getWorld().getServer();
		MoonNightManager.forceStop(server);
		// A UUID selector is not a player selector, so run it as the player.
		int result = server.getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(2).withSilent(),
				"embervoid moon curse @s " + stacks);
		context.assertTrue(CurseOfTheNight.stacks(player) == stacks,
				"curse command (result " + result + ") left " + CurseOfTheNight.stacks(player) + " stacks, wanted " + stacks);
		MoonNightManager.forceStart(server, MoonNight.FULL_MOON);
		context.assertTrue(MoonNights.current(server) == MoonNight.FULL_MOON, "full moon did not start");
		List<MobEntity> mobs = new ArrayList<>();
		for (UUID uuid : NightWaves.waveOf(server, player.getUuid())) {
			if (server.getOverworld().getEntity(uuid) instanceof MobEntity mob) {
				mobs.add(mob);
			}
		}
		return mobs;
	}

	/** 16–32 blocks from the player horizontally (a little slack for the block-centred position). */
	private static void assertInRing(TestContext context, ServerPlayerEntity player, Entity mob) {
		double distance = Math.hypot(mob.getX() - player.getX(), mob.getZ() - player.getZ());
		context.assertTrue(distance >= 15 && distance <= 33, "wave mob " + distance + " blocks away, expected 16–32");
	}

	private static void useMissChance(float chance) {
		JsonObject json = JsonParser.parseString(EmbervoidConfig.toNetworkString(EmbervoidConfig.get())).getAsJsonObject();
		json.getAsJsonObject("nightMobs").addProperty("ghostMissChance", chance);
		EmbervoidConfig.useServer(EmbervoidConfig.fromNetworkString(json.toString()));
	}

	/**
	 * Runs {@code body}; if an assertion fails, stops the night and removes the player first, so a failed test does
	 * not leave a full moon (and its wave) running into the next batches.
	 */
	/**
	 * Kills the wave next to its player. Wave mobs spawn 16–32 blocks out, and as the suite grows the test can sit
	 * where some of those chunks are not entity-ticking: a mob killed there never finishes dying (no removal, no
	 * wave cleared). Brought next to the player first, they all tick.
	 */
	private static void killNearPlayer(List<MobEntity> wave, ServerPlayerEntity player) {
		for (MobEntity mob : wave) {
			mob.refreshPositionAndAngles(player.getX() + 2, player.getY(), player.getZ() + 2, 0, 0);
			mob.kill();
		}
	}

	private static Runnable safe(TestContext context, ServerPlayerEntity player, Runnable body) {
		return () -> {
			try {
				body.run();
			} catch (RuntimeException e) {
				MinecraftServer server = context.getWorld().getServer();
				MoonNightManager.forceStop(server);
				if (server.getPlayerManager().getPlayer(player.getUuid()) == player) {
					server.getPlayerManager().remove(player);
				}
				throw e;
			}
		};
	}

	private static void finish(TestContext context, ServerPlayerEntity player) {
		MoonNightManager.forceStop(context.getWorld().getServer());
		TestPlayers.complete(context, player);
	}
}
