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
import com.embervoid.vampire.moonstub.MoonNights;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Vampire boss fight (DESIGN §3.3, §10.12, PLAN 8.11): a field boss with no fog wall. One fight per server, on
 * the blood new moon only.
 *
 * <ul>
 *   <li>Taking part: every player near the boss when it forms, and anyone alive who comes within {@link #RADIUS}
 *   later, except players who already died in this fight.</li>
 *   <li>Level: {@link VampireLevel} over the players near it when it forms.</li>
 *   <li>Shared boss bar "Vampire — Lv N" and music for the fighters; phases from {@link VampireBossEntity}.</li>
 *   <li>On the kill, every fighter alive and near gets credit and personal loot straight into the inventory: the first
 *   kill {@code vampire.firstKillShards} Flask Shards + a Vampire Fang, a repeat kill a {@code
 *   vampire.repeatShardChance} chance of one Flask Shard. XP drops like any boss's. The night hold is released.</li>
 *   <li>If every fighter has died (or none has been near for {@link #EMPTY_LIMIT} ticks), the Vampire leaves and the
 *   night hold is released.</li>
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
				leave(player);
			}
		});
	}

	/** The transformation is over: the boss forms at {@code pos} and the players near it take part. */
	static void form(ServerWorld world, Vec3d pos, float yaw) {
		MinecraftServer server = world.getServer();
		VampireState state = VampireState.get(server);
		List<ServerPlayerEntity> near = new ArrayList<>();
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.isAlive() && !player.isSpectator() && player.getPos().isInRange(pos, RADIUS)) {
				near.add(player);
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
		emptyTicks = 0;
		for (ServerPlayerEntity player : near) {
			join(player, state);
		}
		Embervoid.LOGGER.info("Vampire boss formed at {} with level {} ({} player(s) near)", state.fightCenter.toShortString(), level, near.size());
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
			if (!state.fallen.isEmpty() && noFighterLeft(server, state)) {
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

	/** True if every player who could still take part is dead or gone from the fight's world. */
	private static boolean noFighterLeft(MinecraftServer server, VampireState state) {
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
		finish(server, state);
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
			world.playSound(null, state.fightCenter, SoundEvents.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 2.0f, 0.5f);
		}
		finish(server, state);
	}

	/** Ends the fight and lets the night end. */
	private static void finish(MinecraftServer server, VampireState state) {
		clear(server, state);
		MoonNights.release(server, VampireNight.HOLD);
	}

	/** The night ended some other way (dawn, timeout, command): the boss leaves, nobody gets credit. */
	static void end(MinecraftServer server) {
		VampireState state = VampireState.get(server);
		if (state.stage != Stage.FIGHT) {
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
		state.stage = Stage.NONE;
		state.boss = null;
		state.fightWorld = null;
		state.fallen.clear();
		state.markDirty();
	}

	/** After a restart the bar and fighters are rebuilt; drop the old ones (integrated server reopened). */
	static void reset() {
		FIGHTERS.clear();
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
