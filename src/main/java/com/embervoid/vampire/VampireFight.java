package com.embervoid.vampire;

import com.embervoid.Embervoid;
import com.embervoid.boss.BossMusic;
import com.embervoid.boss.EmberHeartItem;
import com.embervoid.mob.MobLevels;
import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.Realms;
import com.embervoid.sanctum.SanctumItems;
import com.embervoid.stats.PlayerStats;
import com.embervoid.vampire.VampireState.Stage;
import com.embervoid.moon.api.MoonNights;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Vampire boss fight (DESIGN §3.3, §10.12, PLAN 8.11–8.12): a field boss with no fog wall, fought in its own
 * arena dimension ({@link VampireArena}). One fight per server, on the blood new moon only.
 *
 * <ul>
 *   <li>Taking part: every overworld player outside a Sanctum core, pulled into the arena when the boss forms. A
 *   player who dies there respawns as usual and cannot come back. (Without the arena dimension: the players within
 *   {@link #RADIUS} of the boss, in the overworld.)</li>
 *   <li>Level: {@link VampireLevel} over the players taking part when it forms.</li>
 *   <li>Shared boss bar "Vampire — Lv N" and music for the fighters; phases from {@link VampireBossEntity}.</li>
 *   <li>On the kill, every fighter alive and near gets credit and personal loot straight into the inventory: the first
 *   kill {@code vampire.firstKillShards} Flask Shards + a Vampire Fang, a repeat kill a {@code
 *   vampire.repeatShardChance} chance of one Flask Shard. XP drops like any boss's. Ten seconds later the arena closes
 *   (everyone back to where they were pulled from) and the night hold is released.</li>
 *   <li>If every fighter has died (or none has been there for {@link #EMPTY_LIMIT} ticks), the Vampire leaves, the
 *   arena closes and the night hold is released, so the night ends.</li>
 * </ul>
 *
 * <p>What is saved lives in {@link VampireState}; the bar and the fighter list are rebuilt after a restart.
 */
public final class VampireFight {
	/** Progress id of the boss, for first-kill credit. */
	public static final Identifier BOSS_ID = Embervoid.id("vampire");
	/** Players this close to the boss take part. */
	static final double RADIUS = 48;
	/** With no fighter near for this long (logged out, ran off) the Vampire leaves. */
	private static final int EMPTY_LIMIT = 600;

	@Nullable
	private static ServerBossBar bar;
	private static final Set<UUID> FIGHTERS = new LinkedHashSet<>();
	/** Where fighters died, so what they dropped follows them home when the arena closes. */
	private static final Map<UUID, Vec3d> DEATHS = new HashMap<>();
	/** Ticks between the kill and the arena closing (time to pick up the XP). */
	public static final int CLOSE_TICKS = 200;
	private static int emptyTicks;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(VampireFight::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof VampireBossEntity boss && belongs(boss)) {
				onBossDeath(boss);
			} else if (entity instanceof ServerPlayerEntity player && FIGHTERS.contains(player.getUuid())) {
				VampireState state = VampireState.get(player.getServer());
				state.fallen.add(player.getUuid());
				state.markDirty();
				DEATHS.put(player.getUuid(), player.getPos());
				leave(player);
			}
		});
	}

	/**
	 * The transformation is over: the overworld players outside a Sanctum core are pulled into the arena and the boss
	 * forms at its centre. Without the arena dimension the boss forms where the Vampire stood, for the players near.
	 */
	static void form(ServerWorld overworld, Vec3d pos, float yaw) {
		MinecraftServer server = overworld.getServer();
		VampireState state = VampireState.get(server);
		ServerWorld world = overworld;
		List<ServerPlayerEntity> near = VampireArena.open(server, state);
		if (near != null) {
			world = VampireArena.world(server);
			pos = VampireArena.center();
		} else {
			near = new ArrayList<>();
			for (ServerPlayerEntity player : overworld.getPlayers()) {
				if (player.isAlive() && !player.isSpectator() && player.getPos().isInRange(pos, RADIUS)) {
					near.add(player);
				}
			}
		}
		VampireConfig config = VampireConfig.get();
		int level = VampireLevel.of(near.stream().map(PlayerStats::level).toList(), config.bossMinLevel(), levelCap(near));

		VampireBossEntity boss = VampireEntities.VAMPIRE_BOSS.create(world);
		if (boss == null) {
			finish(server, state);
			return;
		}
		boss.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0);
		boss.setCustomName(bossName());
		boss.initialize(world, world.getLocalDifficulty(boss.getBlockPos()), SpawnReason.EVENT, null, null);
		state.stage = Stage.FIGHT;
		state.boss = boss.getUuid();
		state.fightWorld = world.getRegistryKey();
		state.fightCenter = BlockPos.ofFloored(pos);
		state.bossLevel = level;
		state.fallen.clear();
		state.markDirty();
		world.spawnEntity(boss);
		MobLevels.setLevel(boss, level);
		world.playSound(null, boss.getBlockPos(), VampireSounds.VAMPIRE_TURNED, SoundCategory.HOSTILE, 1.0f, 1.2f);
		emptyTicks = 0;
		DEATHS.clear();
		for (ServerPlayerEntity player : near) {
			join(player, state);
		}
		Embervoid.LOGGER.info("Vampire boss formed in {} with level {} ({} player(s))", world.getRegistryKey().getValue(), level, near.size());
	}

	/**
	 * Top of the band of the highest realm any of the players has unlocked; {@code vampire.bossCapNoRealm} (the
	 * overworld band) if none has.
	 */
	static int levelCap(List<ServerPlayerEntity> players) {
		for (int i = Realms.ALL.size() - 1; i >= 0; i--) {
			RealmDefinition realm = Realms.ALL.get(i);
			if (players.stream().anyMatch(p -> ProgressManager.isRealmUnlocked(p, realm.id()))) {
				int top = Math.max(realm.landingLevels().max(), realm.depthsLevels().max());
				for (RealmDefinition.LevelBand band : realm.zoneLevels()) {
					top = Math.max(top, band.max());
				}
				return top;
			}
		}
		return VampireConfig.get().bossCapNoRealm();
	}

	private static Text bossName() {
		return Text.translatable(EmberHeartItem.bossKey(BOSS_ID));
	}

	private static ServerBossBar bar(VampireState state) {
		if (bar == null) {
			bar = new ServerBossBar(Text.translatable("embervoid.boss.bar", bossName(), state.bossLevel),
					BossBar.Color.RED, BossBar.Style.NOTCHED_10);
		}
		return bar;
	}

	private static void join(ServerPlayerEntity player, VampireState state) {
		if (FIGHTERS.add(player.getUuid())) {
			bar(state).addPlayer(player);
			BossMusic.play(player, VampireBossEntity.MUSIC);
		}
	}

	private static void leave(ServerPlayerEntity player) {
		if (FIGHTERS.remove(player.getUuid())) {
			if (bar != null) {
				bar.removePlayer(player);
			}
			BossMusic.stop(player);
		}
	}

	@Nullable
	private static ServerWorld fightWorld(MinecraftServer server, VampireState state) {
		return state.fightWorld == null ? null : server.getWorld(state.fightWorld);
	}

	private static void tick(MinecraftServer server) {
		VampireState state = VampireState.get(server);
		if (state.stage == Stage.CLOSING) {
			if (--state.closeTicks <= 0) {
				finish(server, state);
			}
			return;
		}
		if (state.stage != Stage.FIGHT) {
			return;
		}
		ServerWorld world = fightWorld(server, state);
		if (world == null) {
			finish(server, state);
			return;
		}
		Entity bossEntity = state.boss == null ? null : world.getEntity(state.boss);
		Vec3d center = bossEntity != null ? bossEntity.getPos() : Vec3d.ofBottomCenter(state.fightCenter);
		if (bossEntity != null) {
			state.fightCenter = bossEntity.getBlockPos();
		}

		// Fighters who logged out, left the world or ran off are off the bar; they can come back.
		for (UUID uuid : List.copyOf(FIGHTERS)) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
			if (player == null) {
				FIGHTERS.remove(uuid);
			} else if (player.getServerWorld() != world || !player.isAlive() || !player.getPos().isInRange(center, RADIUS * 1.5)) {
				leave(player);
			}
		}
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.isAlive() && !player.isSpectator() && !state.fallen.contains(player.getUuid())
					&& player.getPos().isInRange(center, RADIUS)) {
				join(player, state);
			}
		}

		if (FIGHTERS.isEmpty()) {
			if (everyoneFell(server, state)) {
				fail(server, state, "embervoid.vampire.boss.won");
				return;
			}
			if (++emptyTicks >= EMPTY_LIMIT) {
				fail(server, state, "embervoid.vampire.boss.left");
				return;
			}
		} else {
			emptyTicks = 0;
		}

		if (bossEntity instanceof VampireBossEntity boss) {
			bar(state).setPercent(boss.getHealth() / boss.getMaxHealth());
			boss.updatePhase();
		}
	}

	/**
	 * True if everyone who took part has died. In the arena that is every player pulled in (a player who logged out
	 * there may still come back); in the overworld every living player there has died in the fight already.
	 */
	private static boolean everyoneFell(MinecraftServer server, VampireState state) {
		if (state.fallen.isEmpty()) {
			return false;
		}
		if (state.fightWorld == VampireArena.KEY) {
			return state.fallen.containsAll(state.returnPoints.keySet());
		}
		ServerWorld world = fightWorld(server, state);
		return world == null || world.getPlayers().stream()
				.noneMatch(p -> p.isAlive() && !p.isSpectator() && !state.fallen.contains(p.getUuid()));
	}

	/** The boss died: credit and personal loot for every fighter alive and near, then the night may end. */
	private static void onBossDeath(VampireBossEntity boss) {
		MinecraftServer server = boss.getServer();
		VampireState state = VampireState.get(server);
		VampireConfig config = VampireConfig.get();
		for (UUID uuid : FIGHTERS) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
			if (player == null || player.getServerWorld() != boss.getWorld() || !player.isAlive()
					|| !player.getPos().isInRange(boss.getPos(), RADIUS * 1.5)) {
				continue;
			}
			if (ProgressManager.markBossBeaten(player, BOSS_ID)) {
				player.getInventory().offerOrDrop(new ItemStack(SanctumItems.FLASK_SHARD, config.firstKillShards()));
				player.getInventory().offerOrDrop(new ItemStack(VampireItems.VAMPIRE_FANG));
				player.sendMessage(Text.translatable("embervoid.vampire.boss.first_kill", config.firstKillShards()).formatted(Formatting.GOLD), false);
			} else {
				if (player.getRandom().nextFloat() < config.repeatShardChance()) {
					player.getInventory().offerOrDrop(new ItemStack(SanctumItems.FLASK_SHARD));
				}
				player.sendMessage(Text.translatable("embervoid.boss.victory", bossName()), true);
			}
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.translatable("embervoid.vampire.boss.beaten").formatted(Formatting.DARK_RED), false);
		}
		if (state.fightWorld != VampireArena.KEY) {
			finish(server, state);
			return;
		}
		// A few seconds in the arena to pick up the XP, then everyone goes home.
		stopBarAndMusic(server);
		state.stage = Stage.CLOSING;
		state.closeTicks = CLOSE_TICKS;
		state.boss = null;
		state.markDirty();
		ServerWorld arena = VampireArena.world(server);
		if (arena != null) {
			for (ServerPlayerEntity player : arena.getPlayers()) {
				player.sendMessage(Text.translatable("embervoid.vampire.arena.closing", CLOSE_TICKS / 20).formatted(Formatting.GRAY), false);
			}
		}
	}

	/** Nobody beat it: the Vampire leaves laughing and the night may end. */
	private static void fail(MinecraftServer server, VampireState state, String message) {
		ServerWorld world = fightWorld(server, state);
		Entity boss = world == null || state.boss == null ? null : world.getEntity(state.boss);
		if (boss instanceof VampireBossEntity vampire) {
			vampire.vanish();
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(Text.translatable(message).formatted(Formatting.DARK_RED), false);
		}
		if (world != null) {
			world.playSound(null, state.fightCenter, VampireSounds.VAMPIRE_BOSS_TRIUMPH, SoundCategory.HOSTILE, 2.0f, 0.5f);
		}
		finish(server, state);
	}

	/** Ends the fight, closes the arena and lets the night end. */
	private static void finish(MinecraftServer server, VampireState state) {
		clear(server, state);
		MoonNights.release(server, VampireNight.HOLD);
	}

	/** The night ended some other way (dawn, timeout, command): the boss leaves, nobody gets credit. */
	static void end(MinecraftServer server) {
		VampireState state = VampireState.get(server);
		if (state.stage != Stage.FIGHT && state.stage != Stage.CLOSING) {
			return;
		}
		ServerWorld world = fightWorld(server, state);
		Entity boss = world == null || state.boss == null ? null : world.getEntity(state.boss);
		if (boss instanceof VampireBossEntity vampire) {
			vampire.vanish();
		}
		clear(server, state);
	}

	private static void clear(MinecraftServer server, VampireState state) {
		stopBarAndMusic(server);
		if (state.fightWorld == VampireArena.KEY) {
			VampireArena.close(server, state, DEATHS);
		}
		DEATHS.clear();
		state.stage = Stage.NONE;
		state.boss = null;
		state.fightWorld = null;
		state.fallen.clear();
		state.markDirty();
	}

	private static void stopBarAndMusic(MinecraftServer server) {
		for (UUID uuid : List.copyOf(FIGHTERS)) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
			if (player != null) {
				BossMusic.stop(player);
			}
		}
		FIGHTERS.clear();
		if (bar != null) {
			bar.clearPlayers();
			bar = null;
		}
	}

	/** After a restart the bar and fighters are rebuilt; drop the old ones (integrated server reopened). */
	static void reset() {
		FIGHTERS.clear();
		DEATHS.clear();
		if (bar != null) {
			bar.clearPlayers();
			bar = null;
		}
		emptyTicks = 0;
	}

	/** Whether this boss is the one of the running fight; any other leaves. */
	static boolean belongs(VampireBossEntity boss) {
		MinecraftServer server = boss.getServer();
		if (server == null) {
			return true;
		}
		VampireState state = VampireState.get(server);
		return state.stage == Stage.FIGHT && boss.getUuid().equals(state.boss);
	}

	/** Fighters right now, for the debug command. */
	static int fighters() {
		return FIGHTERS.size();
	}

	private VampireFight() {}
}
