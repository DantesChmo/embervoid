package com.embervoid.gametest;

import com.embervoid.mob.MobLevels;
import com.embervoid.moon.MoonConfig;
import com.embervoid.moon.MoonNightManager;
import com.embervoid.moon.MoonSchedule;
import com.embervoid.moon.MoonSync;
import com.embervoid.moon.api.CurseOfTheNight;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
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
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

	/** The server tells the client about the night when it starts and ends ({@code embervoid:moon_night}). */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "moon_sync")
	public void nightIsSentToClients(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		Restore restore = Restore.save(context);
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		restore.players.add(player);
		guard(context, restore, () -> {
			RecordingHandler handler = new RecordingHandler(server, player);
			MoonNightManager.forceStart(server, MoonNight.BLOOD_NEW_MOON);
			context.assertTrue(handler.moonNights().equals(List.of(MoonNight.BLOOD_NEW_MOON)),
					"after start, sent " + handler.moonNights());
			MoonNightManager.forceStop(server);
			context.assertTrue(handler.moonNights().equals(List.of(MoonNight.BLOOD_NEW_MOON, MoonNight.NONE)),
					"after stop, sent " + handler.moonNights());
			finish(context, restore);
		});
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

	/** Runs {@code body}; on failure cleans up first so the next moon test starts from a clean state. */
	private static void guard(TestContext context, Restore restore, Runnable body) {
		try {
			body.run();
		} catch (RuntimeException | Error e) {
			restore.run(context);
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

		void run(TestContext context) {
			MinecraftServer server = context.getWorld().getServer();
			MoonNightManager.forceStop(server);
			context.getWorld().setTimeOfDay(time);
			server.setDifficulty(difficulty, true);
		}
	}

	/** Replaces a player's network handler and records the custom payloads sent to it. */
	private static final class RecordingHandler extends ServerPlayNetworkHandler {
		private final List<CustomPayloadS2CPacket> payloads = new ArrayList<>();

		RecordingHandler(MinecraftServer server, ServerPlayerEntity player) {
			// The constructor makes this the player's handler.
			super(server, new ClientConnection(NetworkSide.SERVERBOUND), player);
		}

		@Override
		public void sendPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof CustomPayloadS2CPacket payload) {
				payloads.add(payload);
			}
		}

		List<MoonNight> moonNights() {
			List<MoonNight> nights = new ArrayList<>();
			for (CustomPayloadS2CPacket payload : payloads) {
				if (payload.getChannel().equals(MoonSync.PACKET)) {
					nights.add(MoonNight.byOrdinal(new PacketByteBuf(payload.getData().copy()).readVarInt()));
				}
			}
			return nights;
		}
	}
}
