package com.embervoid.gametest;

import com.embervoid.mob.MobLevels;
import com.embervoid.moon.MoonConfig;
import com.embervoid.moon.MoonNightManager;
import com.embervoid.moon.MoonSchedule;
import com.embervoid.moon.MoonSync;
import com.embervoid.moon.api.CurseOfTheNight;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Moon nights on a real server (PLAN 8.1–8.6). The moon state and the clock are global, so every test has a batch
 * of its own (batches run one after another), and each one stops any moon night and restores the clock and the
 * difficulty when it ends, pass or fail.
 */
public final class MoonTests implements FabricGameTest {
	private static final Identifier TEST_HOLD = new Identifier("embervoid-gametest", "hold");
	/** A newly joined or respawned player cannot be hurt for 60 ticks; wait that out before hitting one. */
	private static final int JOIN_INVULNERABLE_TICKS = 65;

	@Nullable
	private static MoonNights.EndReason lastEndReason;

	static {
		MoonNights.NIGHT_ENDED.register((server, night, reason) -> lastEndReason = reason);
	}

	/** Days 24 and 28 start a full moon and a blood new moon at dusk on their own; day 16 does not. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_schedule", tickLimit = 100)
	public void scheduleStartsNightsAtDusk(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		Restore restore = Restore.save(context);
		guard(context, restore, () -> context.getWorld().setTimeOfDay(16 * MoonSchedule.DAY_TICKS + MoonSchedule.NIGHT_START));
		context.waitAndRun(3, () -> guard(context, restore, () -> {
			context.assertTrue(MoonNights.current(server) == MoonNight.NONE, "day 16 started " + MoonNights.current(server));
			context.getWorld().setTimeOfDay(24 * MoonSchedule.DAY_TICKS + MoonSchedule.NIGHT_START);
		}));
		context.waitAndRun(6, () -> guard(context, restore, () -> {
			context.assertTrue(MoonNights.current(server) == MoonNight.FULL_MOON, "day 24: " + MoonNights.current(server));
			MoonNightManager.forceStop(server);
			context.getWorld().setTimeOfDay(28 * MoonSchedule.DAY_TICKS + MoonSchedule.NIGHT_START);
		}));
		context.waitAndRun(9, () -> guard(context, restore, () -> {
			context.assertTrue(MoonNights.current(server) == MoonNight.BLOOD_NEW_MOON, "day 28: " + MoonNights.current(server));
			finish(context, restore);
		}));
	}

	/** A hold stops the clock before dawn even if time is pushed past it; the last release ends the night at dawn. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_hold", tickLimit = 100)
	public void holdStopsClockUntilReleased(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		Restore restore = Restore.save(context);
		long[] dawn = new long[1];
		guard(context, restore, () -> {
			MoonNightManager.forceStart(server, MoonNight.FULL_MOON);
			context.assertTrue(MoonNights.hold(server, TEST_HOLD), "hold refused during a moon night");
			dawn[0] = MoonSchedule.dawnOf(MoonSchedule.day(context.getWorld().getTimeOfDay()));
			context.getWorld().setTimeOfDay(dawn[0] + 500);
		});
		context.waitAndRun(5, () -> guard(context, restore, () -> {
			long time = context.getWorld().getTimeOfDay();
			context.assertTrue(time < dawn[0], "held clock at " + time + ", dawn is " + dawn[0]);
			context.assertTrue(MoonNights.current(server) == MoonNight.FULL_MOON, "held night ended");
			lastEndReason = null;
			MoonNights.release(server, TEST_HOLD);
			context.assertTrue(MoonNights.current(server) == MoonNight.NONE, "night still on after the last release");
			context.assertTrue(lastEndReason == MoonNights.EndReason.GOAL_MET, "end reason " + lastEndReason);
			context.assertTrue(context.getWorld().getTimeOfDay() == dawn[0],
					"clock " + context.getWorld().getTimeOfDay() + " after release, expected dawn " + dawn[0]);
			context.assertTrue(!MoonNights.hold(server, TEST_HOLD), "hold accepted with no moon night");
			finish(context, restore);
		}));
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_bed")
	public void bedRefusedOnMoonNight(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		Restore restore = Restore.save(context);
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		restore.players.add(player);
		guard(context, restore, () -> {
			BlockPos foot = new BlockPos(3, 1, 2);
			BlockPos head = foot.north();
			context.setBlockState(foot, Blocks.RED_BED.getDefaultState()
					.with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.FOOT));
			context.setBlockState(head, Blocks.RED_BED.getDefaultState()
					.with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD));
			MoonNightManager.forceStart(server, MoonNight.FULL_MOON);

			// The night's darkness is otherwise only recalculated on the next world tick.
			context.getWorld().calculateAmbientDarkness();
			BlockPos bed = context.getAbsolutePos(head);
			var result = player.trySleep(bed);
			context.assertTrue(result.left().isPresent() && result.left().get() == PlayerEntity.SleepFailureReason.OTHER_PROBLEM,
					"bed not refused: " + result);
			context.assertTrue(!player.isSleeping(), "player is sleeping on a moon night");
			context.assertTrue(bed.equals(player.getSpawnPointPosition()), "respawn point not set at the bed");
			finish(context, restore);
		});
	}

	/** A zombie spawned on the full moon gets +3 levels and ×1.5 follow range; the range reverts when it ends. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_full_mobs")
	public void fullMoonStrengthensNewZombies(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		Restore restore = Restore.save(context);
		guard(context, restore, () -> {
			MoonNightManager.forceStart(server, MoonNight.FULL_MOON);
			ZombieEntity zombie = context.spawnMob(EntityType.ZOMBIE, 2, 1, 2);
			int expected = MobLevels.defaultLevel(zombie, context.getWorld()) + MoonConfig.get().fullMoon().levelBonus();
			context.assertTrue(MobLevels.getLevel(zombie) == expected,
					"zombie level " + MobLevels.getLevel(zombie) + ", expected " + expected);
			double base = zombie.getAttributeBaseValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
			double range = zombie.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
			double factor = MoonConfig.get().fullMoon().followRange();
			context.assertTrue(Math.abs(range - base * factor) < 0.01, "follow range " + range + ", expected " + base * factor);

			MoonNightManager.forceStop(server);
			range = zombie.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
			context.assertTrue(Math.abs(range - base) < 0.01, "follow range " + range + " after the night, expected " + base);
			context.assertTrue(MobLevels.getLevel(zombie) == expected, "level changed after the night");
			finish(context, restore);
		});
	}

	/**
	 * A death to a zombie adds a stack (kept through respawn); /kill, a fall and a player kill do not; the end of the
	 * full moon clears them. One fresh player per kind of death.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_curse", tickLimit = 200)
	public void curseCountsMobKillsAndResetsAfterFullMoon(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		Restore restore = Restore.save(context);
		server.setDifficulty(Difficulty.NORMAL, true);
		ServerPlayerEntity byZombie = TestPlayers.create(context, 1, 1);
		ServerPlayerEntity byKill = TestPlayers.create(context, 4, 1);
		ServerPlayerEntity byFall = TestPlayers.create(context, 7, 1);
		ServerPlayerEntity byPlayer = TestPlayers.create(context, 1, 4);
		ServerPlayerEntity killer = TestPlayers.create(context, 4, 4);
		restore.players.addAll(List.of(byZombie, byKill, byFall, byPlayer, killer));
		// Fresh players cannot be hurt for their first 60 ticks (join invulnerability).
		context.waitAndRun(JOIN_INVULNERABLE_TICKS, () -> guard(context, restore, () -> {
			ZombieEntity zombie = context.spawnMob(EntityType.ZOMBIE, 7, 1, 4);
			zombie.setAiDisabled(true);

			byZombie.damage(context.getWorld().getDamageSources().mobAttack(zombie), 1000);
			context.assertTrue(byZombie.isDead(), "the zombie's hit did not kill");
			context.assertTrue(CurseOfTheNight.stacks(byZombie) == 1, "zombie kill: stacks " + CurseOfTheNight.stacks(byZombie));
			ServerPlayerEntity respawned = respawn(restore, byZombie);
			context.assertTrue(CurseOfTheNight.stacks(respawned) == 1, "stacks after respawn: " + CurseOfTheNight.stacks(respawned));

			byKill.kill();
			byFall.damage(context.getWorld().getDamageSources().fall(), 1000);
			context.assertTrue(byFall.isDead(), "the fall did not kill");
			byPlayer.damage(context.getWorld().getDamageSources().playerAttack(killer), 1000);
			for (ServerPlayerEntity player : List.of(byKill, byFall, byPlayer)) {
				context.assertTrue(CurseOfTheNight.stacks(player) == 0,
						player.getName().getString() + " got a stack from a death that does not count");
			}

			MoonNightManager.forceStart(server, MoonNight.FULL_MOON);
			MoonNightManager.forceStop(server);
			context.assertTrue(CurseOfTheNight.stacks(respawned) == 0, "stacks after the full moon: " + CurseOfTheNight.stacks(respawned));
			finish(context, restore);
		}));
	}

	/** On the blood new moon a zombie that hits a player heals a quarter of the damage dealt; not on other nights. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_vampirism", tickLimit = 200)
	public void bloodMoonZombieHealsFromHits(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		Restore restore = Restore.save(context);
		server.setDifficulty(Difficulty.NORMAL, true);
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		restore.players.add(player);
		context.waitAndRun(JOIN_INVULNERABLE_TICKS, () -> guard(context, restore, () -> {
			ZombieEntity zombie = context.spawnMob(EntityType.ZOMBIE, 2, 1, 4);
			zombie.setAiDisabled(true);

			// An ordinary night first: no healing.
			zombie.setHealth(10);
			float health = player.getHealth();
			zombie.tryAttack(player);
			context.assertTrue(player.getHealth() < health, "the zombie's hit did no damage");
			context.assertTrue(zombie.getHealth() == 10, "zombie healed without a blood moon: " + zombie.getHealth());

			MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON);
			player.setHealth(player.getMaxHealth());
			player.timeUntilRegen = 0;
			health = player.getHealth();
			zombie.tryAttack(player);
			float dealt = health - player.getHealth();
			float expected = 10 + dealt * MoonConfig.get().blood().vampirism();
			context.assertTrue(dealt > 0, "the zombie's hit did no damage on the blood moon");
			context.assertTrue(Math.abs(zombie.getHealth() - expected) < 0.01,
					"zombie health " + zombie.getHealth() + " after dealing " + dealt + ", expected " + expected);
			finish(context, restore);
		}));
	}

	/**
	 * The server tells the client about the night when it starts and ends ({@code embervoid:moon_night}); the first
	 * packet is the join's (no night).
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_sync")
	public void nightIsSentToClients(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		Restore restore = Restore.save(context);
		RecordingConnection connection = new RecordingConnection();
		ServerPlayerEntity player = joinPlayer(context, 2, 2, connection);
		restore.players.add(player);
		guard(context, restore, () -> {
			MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON);
			context.assertTrue(connection.moonNights().equals(List.of(MoonNight.NONE, MoonNight.BLOOD_NEW_MOON)),
					"after start, sent " + connection.moonNights());
			MoonNightManager.forceStop(server);
			context.assertTrue(connection.moonNights().equals(List.of(MoonNight.NONE, MoonNight.BLOOD_NEW_MOON, MoonNight.NONE)),
					"after stop, sent " + connection.moonNights());
			finish(context, restore);
		});
	}

	/**
	 * A zombie that targets a player 30 blocks away and then loses sight of them behind a wall keeps the target for
	 * 100 ticks on a full moon. On an ordinary night vanilla drops it (60 ticks unseen). The zombie cannot move
	 * (speed 0), so only targeting is tested.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_target_full", tickLimit = 400)
	public void fullMoonZombieKeepsUnseenTarget(TestContext context) {
		testTargetBehindWall(context, true);
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_target_ordinary", tickLimit = 400)
	public void ordinaryNightZombieLosesUnseenTarget(TestContext context) {
		testTargetBehindWall(context, false);
	}

	private static void testTargetBehindWall(TestContext context, boolean fullMoon) {
		MinecraftServer server = context.getWorld().getServer();
		Restore restore = Restore.save(context);
		server.setDifficulty(Difficulty.NORMAL, true);
		// A real player: the mock players count as creative, and hostiles never target creative players.
		ServerPlayerEntity player = joinPlayer(context, 2, 32, new RecordingConnection());
		restore.players.add(player);
		ZombieEntity[] zombie = new ZombieEntity[1];
		guard(context, restore, () -> {
			if (fullMoon) {
				MoonNightManager.forceStart(server, MoonNight.FULL_MOON);
			} else {
				context.getWorld().setTimeOfDay(18_000);
			}
			for (int x = 0; x <= 4; x++) {
				for (int z = 0; z <= 4; z++) {
					context.setBlockState(x, 0, z, Blocks.STONE);
				}
			}
			zombie[0] = context.spawnMob(EntityType.ZOMBIE, 2, 1, 2);
			zombie[0].getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
		});
		awaitTarget(context, restore, zombie, player, 0, () -> {
			// Out of sight: a wall between them.
			for (int x = 0; x <= 4; x++) {
				for (int y = 1; y <= 4; y++) {
					context.setBlockState(x, y, 5, Blocks.STONE);
				}
			}
			context.assertTrue(!zombie[0].canSee(player), "the zombie still sees the player through the wall");
			context.waitAndRun(100, () -> guard(context, restore, () -> {
				if (fullMoon) {
					context.assertTrue(zombie[0].getTarget() == player,
							"full moon: target lost behind the wall (" + zombie[0].getTarget() + ")");
				} else {
					context.assertTrue(zombie[0].getTarget() == null,
							"ordinary night: target kept behind the wall (" + zombie[0].getTarget() + ")");
				}
				finish(context, restore);
			}));
		});
	}

	/** Waits (up to 200 ticks) until the zombie targets the player, then runs {@code then}. */
	private static void awaitTarget(TestContext context, Restore restore, ZombieEntity[] zombie, ServerPlayerEntity player,
			int waited, Runnable then) {
		context.waitAndRun(1, () -> guard(context, restore, () -> {
			if (zombie[0].getTarget() == player) {
				then.run();
				return;
			}
			context.assertTrue(waited < 200, "the zombie never targeted the player 30 blocks away (follow range "
					+ zombie[0].getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE) + ")");
			awaitTarget(context, restore, zombie, player, waited + 1, then);
		}));
	}

	/** The night is sent to a player who joins during it, respawns, goes to the Nether and comes back. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_sync_events", tickLimit = 100)
	public void nightIsSentOnJoinRespawnAndDimensionChange(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		Restore restore = Restore.save(context);
		RecordingConnection connection = new RecordingConnection();
		guard(context, restore, () -> {
			MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON);
			ServerPlayerEntity player = joinPlayer(context, 2, 2, connection);
			restore.players.add(player);
			assertSent(context, connection, 1, "join");

			player.kill();
			player = respawn(restore, player);
			assertSent(context, connection, 2, "respawn");

			player.teleport(server.getWorld(World.NETHER), 0.5, 100, 0.5, 0, 0);
			context.assertTrue(player.getWorld().getRegistryKey() == World.NETHER, "player not in the Nether");
			assertSent(context, connection, 3, "entering the Nether");

			Vec3d back = context.getAbsolute(new Vec3d(2.5, 1, 2.5));
			player.teleport(context.getWorld(), back.x, back.y, back.z, 0, 0);
			assertSent(context, connection, 4, "returning to the overworld");
			finish(context, restore);
		});
	}

	private static void assertSent(TestContext context, RecordingConnection connection, int count, String when) {
		List<MoonNight> sent = connection.moonNights();
		context.assertTrue(sent.size() == count && sent.stream().allMatch(night -> night == MoonNight.BLOOD_NEW_MOON),
				"after " + when + ", sent " + sent + " (expected " + count + "× blood_new_moon)");
	}

	/**
	 * A survival player that joins through the real player manager on {@code connection} and stands at relative
	 * {@code (x, 1, z)} on a 3×3 stone floor. Unlike the mock players it is not creative.
	 */
	private static ServerPlayerEntity joinPlayer(TestContext context, int x, int z, ClientConnection connection) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				context.setBlockState(x + dx, 0, z + dz, Blocks.STONE);
			}
		}
		MinecraftServer server = context.getWorld().getServer();
		ServerPlayerEntity player = new ServerPlayerEntity(server, context.getWorld(),
				new GameProfile(UUID.randomUUID(), "moon-test-player"));
		server.getPlayerManager().onPlayerConnect(connection, player);
		player.changeGameMode(GameMode.SURVIVAL);
		Vec3d pos = context.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(x, 1, z)));
		player.teleport(context.getWorld(), pos.x, pos.y, pos.z, 0, 0);
		return player;
	}

	/** Respawns a player who died (a hit that did not kill, e.g. with PvP off, leaves them as they are). */
	private static ServerPlayerEntity respawn(Restore restore, ServerPlayerEntity player) {
		if (player.isAlive()) {
			return player;
		}
		ServerPlayerEntity respawned = TestPlayers.respawn(player);
		restore.players.remove(player);
		restore.players.add(respawned);
		return respawned;
	}

	/**
	 * Runs {@code body}; on failure first stops the night, restores the clock and difficulty and removes the players,
	 * so the next test starts from a clean state.
	 */
	private static void guard(TestContext context, Restore restore, Runnable body) {
		try {
			body.run();
		} catch (RuntimeException | Error e) {
			restore.run(context);
			restore.removePlayers();
			throw e;
		}
	}

	private static void finish(TestContext context, Restore restore) {
		restore.run(context);
		TestPlayers.complete(context, restore.players.toArray(ServerPlayerEntity[]::new));
	}

	/** What a moon test changes globally, to put back at its end. */
	private static final class Restore {
		final long time;
		final Difficulty difficulty;
		final List<ServerPlayerEntity> players = new ArrayList<>();

		private Restore(long time, Difficulty difficulty) {
			this.time = time;
			this.difficulty = difficulty;
		}

		static Restore save(TestContext context) {
			MinecraftServer server = context.getWorld().getServer();
			return new Restore(context.getWorld().getTimeOfDay(), server.getSaveProperties().getDifficulty());
		}

		void removePlayers() {
			for (ServerPlayerEntity player : players) {
				if (player.getServer().getPlayerManager().getPlayer(player.getUuid()) == player) {
					player.getServer().getPlayerManager().remove(player);
				}
			}
		}

		void run(TestContext context) {
			MinecraftServer server = context.getWorld().getServer();
			MoonNightManager.forceStop(server);
			context.getWorld().setTimeOfDay(time);
			server.setDifficulty(difficulty, true);
		}
	}

	/** A connection that records the moon nights sent to it instead of sending anything. */
	private static final class RecordingConnection extends ClientConnection {
		private final List<MoonNight> nights = new ArrayList<>();

		RecordingConnection() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {
			send(packet, null);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof CustomPayloadS2CPacket payload && payload.getChannel().equals(MoonSync.PACKET)) {
				nights.add(MoonNight.byOrdinal(new PacketByteBuf(payload.getData().copy()).readVarInt()));
			}
		}

		List<MoonNight> moonNights() {
			return nights;
		}
	}
}
