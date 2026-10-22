package com.embervoid.nightmob;

import com.embervoid.Embervoid;
import com.embervoid.mob.MobLevels;
import com.embervoid.nightmob.moonstub.CurseOfTheNight;
import com.embervoid.nightmob.moonstub.MoonNight;
import com.embervoid.nightmob.moonstub.MoonNights;
import com.embervoid.sanctum.SanctumItems;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.SpawnHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Full moon waves (DESIGN §3.3, §10.12; PLAN 8.9). When the full moon starts, every overworld player gets a wave:
 * one Ashen Zombie per Curse of the Night stack (at most {@link NightMobConfig#maxZombies}), or
 * {@link NightMobConfig#ghostCount} Ghosts with no curse, {@code spawnMinDistance}–{@code spawnMaxDistance} blocks
 * away. Each wave holds the night ({@code embervoid:wave/<uuid>}) until all its mobs are gone; a cleared Ghost wave
 * gives its owner a Flask Shard. A player outside the overworld when the night starts gets their wave on arrival
 * while the night is still on. When the night ends, every wave mob left vanishes.
 */
public final class NightWaves {
	/** The full moon night ({@link WaveState#night}) a wave mob came on; absent on any other mob. */
	@SuppressWarnings("UnstableApiUsage")
	private static final AttachmentType<Long> WAVE_NIGHT = AttachmentRegistry.<Long>builder()
			.persistent(Codec.LONG)
			.buildAndRegister(Embervoid.id("wave_night"));
	/** Rings tried in order: the configured one, then closer in when nothing fits there (a cramped cave). */
	private static final int TRIES_PER_MOB = 24;
	private static final int CLOSE_MIN_DISTANCE = 6;
	/** How far above or below the player's feet a zombie may stand. */
	private static final int STANDING_RANGE = 8;
	private static final int CHECK_INTERVAL_TICKS = 20;
	/** Stale wave mobs found while loading, discarded at the end of the tick (not in the middle of loading them). */
	private static final List<Entity> STALE = new ArrayList<>();

	public static void register() {
		MoonNights.NIGHT_STARTED.register((server, night) -> {
			if (night == MoonNight.FULL_MOON) {
				startNight(server);
			}
		});
		MoonNights.NIGHT_ENDED.register((server, night, reason) -> {
			if (night == MoonNight.FULL_MOON) {
				endNight(server);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(NightWaves::resume);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> STALE.clear());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!STALE.isEmpty()) {
				STALE.forEach(Entity::discard);
				STALE.clear();
			}
			if (server.getTicks() % CHECK_INTERVAL_TICKS == 0) {
				serveArrivals(server);
			}
		});
		ServerEntityEvents.ENTITY_LOAD.register(NightWaves::onLoad);
		ServerEntityEvents.ENTITY_UNLOAD.register(NightWaves::onUnload);
		WaveCommands.register();
	}

	/** The hold id of a player's wave. */
	public static Identifier holdId(UUID player) {
		return Embervoid.id("wave/" + player);
	}

	private static void startNight(MinecraftServer server) {
		WaveState state = WaveState.get(server);
		// Anything left from an earlier night (e.g. a crash before its end) is stale now.
		state.waves.clear();
		state.served.clear();
		state.night++;
		state.markDirty();
		serveArrivals(server);
	}

	private static void endNight(MinecraftServer server) {
		WaveState state = WaveState.get(server);
		List<UUID> mobs = new ArrayList<>();
		state.waves.values().forEach(wave -> mobs.addAll(wave.mobs));
		// Forget the waves first, so the discards below are not taken for kills.
		state.waves.clear();
		state.served.clear();
		state.markDirty();
		ServerWorld overworld = server.getOverworld();
		for (UUID uuid : mobs) {
			Entity mob = overworld.getEntity(uuid);
			if (mob != null) {
				vanish(overworld, mob);
			}
		}
		// Mobs in unloaded chunks vanish when they load (onLoad).
	}

	/** After a restart: a night that ended while the server was down leaves no waves behind; an open one keeps them. */
	@SuppressWarnings("UnstableApiUsage")
	private static void resume(MinecraftServer server) {
		WaveState state = WaveState.get(server);
		if (MoonNights.current(server) != MoonNight.FULL_MOON) {
			if (!state.waves.isEmpty() || !state.served.isEmpty()) {
				state.waves.clear();
				state.served.clear();
				state.markDirty();
				// Mobs in chunks that loaded before this (spawn chunks) were still in a wave then.
				for (Entity entity : server.getOverworld().iterateEntities()) {
					if (entity.hasAttached(WAVE_NIGHT)) {
						STALE.add(entity);
					}
				}
			}
			return;
		}
		state.waves.keySet().forEach(owner -> MoonNights.hold(server, holdId(owner)));
	}

	/** Gives a wave to every overworld player who has not had one this full moon, and any shards owed. */
	private static void serveArrivals(MinecraftServer server) {
		WaveState state = WaveState.get(server);
		boolean fullMoon = MoonNights.current(server) == MoonNight.FULL_MOON;
		for (ServerPlayerEntity player : server.getOverworld().getPlayers()) {
			if (state.pendingShards.remove(player.getUuid())) {
				state.markDirty();
				giveShard(player);
			}
			if (fullMoon && !player.isSpectator() && !state.served.contains(player.getUuid())) {
				spawnWave(player);
			}
		}
	}

	/**
	 * Spawns {@code player}'s wave around them now (also used by the debug command, with or without a full moon) and
	 * returns how many mobs it has. Replaces a wave they still have.
	 */
	static int spawnWave(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		ServerWorld world = server.getOverworld();
		WaveState state = WaveState.get(server);
		NightMobConfig config = NightMobConfig.get();
		WaveState.Wave old = state.waves.remove(player.getUuid());
		if (old != null) {
			old.mobs.forEach(uuid -> {
				Entity mob = world.getEntity(uuid);
				if (mob != null) {
					vanish(world, mob);
				}
			});
		}
		WaveRules.Wave plan = WaveRules.waveFor(CurseOfTheNight.stacks(player), config.maxZombies(), config.ghostCount());
		state.served.add(player.getUuid());
		state.markDirty();
		if (player.getWorld() != world) {
			return 0;
		}

		WaveState.Wave wave = new WaveState.Wave(plan.kind());
		for (int i = 0; i < plan.count(); i++) {
			HostileEntity mob = plan.kind() == WaveRules.Kind.ZOMBIES
					? spawnZombie(world, player, config) : spawnGhost(world, player, config);
			if (mob == null) {
				continue;
			}
			mob.setAttached(WAVE_NIGHT, state.night);
			wave.mobs.add(mob.getUuid());
			MobLevels.setLevel(mob, MobLevels.defaultLevel(mob, world) + config.zombieLevelBonus());
			mob.setTarget(player);
		}
		if (wave.mobs.size() < plan.count()) {
			Embervoid.LOGGER.warn("Full moon wave for {}: placed {} of {} {}", player.getName().getString(),
					wave.mobs.size(), plan.count(), plan.kind());
		}
		if (wave.mobs.isEmpty()) {
			if (old != null) {
				MoonNights.release(server, holdId(player.getUuid()));
			}
			return 0;
		}
		state.waves.put(player.getUuid(), wave);
		MoonNights.hold(server, holdId(player.getUuid()));
		String key = plan.kind() == WaveRules.Kind.ZOMBIES ? "embervoid.wave.zombies" : "embervoid.wave.ghosts";
		player.sendMessage(Text.translatable(key, wave.mobs.size()).formatted(Formatting.DARK_RED), false);
		return wave.mobs.size();
	}

	@Nullable
	private static HostileEntity spawnZombie(ServerWorld world, ServerPlayerEntity player, NightMobConfig config) {
		EntityType<AshenZombieEntity> type = NightMobs.ASHEN_ZOMBIE;
		BlockPos pos = findGround(world, player, type, config.spawnMinDistance(), config.spawnMaxDistance());
		if (pos == null) {
			pos = findGround(world, player, type, Math.min(CLOSE_MIN_DISTANCE, config.spawnMinDistance()), config.spawnMinDistance());
		}
		return pos == null ? null : spawn(world, type, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ParticleTypes.ASH);
	}

	/** Safe ground for {@code type} in the ring around the player, near the player's own height; null if none. */
	@Nullable
	private static BlockPos findGround(ServerWorld world, ServerPlayerEntity player, EntityType<?> type,
			int minDistance, int maxDistance) {
		for (int tries = 0; tries < TRIES_PER_MOB; tries++) {
			double[] offset = WaveRules.ringOffset(world.random.nextDouble(), world.random.nextDouble(), minDistance, maxDistance);
			int x = MathHelper.floor(player.getX() + offset[0]);
			int z = MathHelper.floor(player.getZ() + offset[1]);
			if (!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z))) {
				continue;
			}
			OptionalInt y = WaveRules.findStandingY(player.getBlockY(), STANDING_RANGE, candidate -> {
				BlockPos pos = new BlockPos(x, candidate, z);
				return SpawnHelper.canSpawn(SpawnRestriction.Location.ON_GROUND, world, pos, type)
						&& world.isSpaceEmpty(type.createSimpleBoundingBox(x + 0.5, candidate, z + 0.5));
			});
			if (y.isPresent()) {
				return new BlockPos(x, y.getAsInt(), z);
			}
		}
		return null;
	}

	/** Ghosts pass through blocks, so any spot in the ring will do; open air is preferred. */
	@Nullable
	private static HostileEntity spawnGhost(ServerWorld world, ServerPlayerEntity player, NightMobConfig config) {
		EntityType<GhostEntity> type = NightMobs.GHOST;
		double[] spot = null;
		for (int tries = 0; tries < TRIES_PER_MOB; tries++) {
			double[] offset = WaveRules.ringOffset(world.random.nextDouble(), world.random.nextDouble(),
					config.spawnMinDistance(), config.spawnMaxDistance());
			double x = player.getX() + offset[0];
			double z = player.getZ() + offset[1];
			double y = MathHelper.clamp(player.getY() + 2 + world.random.nextInt(4), world.getBottomY() + 1, world.getTopY() - 3);
			if (!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(MathHelper.floor(x)),
					ChunkSectionPos.getSectionCoord(MathHelper.floor(z)))) {
				continue;
			}
			spot = new double[]{x, y, z};
			if (world.isSpaceEmpty(type.createSimpleBoundingBox(x, y, z))) {
				break;
			}
		}
		return spot == null ? null : spawn(world, type, spot[0], spot[1], spot[2], ParticleTypes.SOUL);
	}

	@Nullable
	private static <T extends HostileEntity> T spawn(ServerWorld world, EntityType<T> type, double x, double y, double z,
			DefaultParticleType particle) {
		T mob = type.create(world);
		if (mob == null) {
			return null;
		}
		mob.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360, 0);
		mob.initialize(world, world.getLocalDifficulty(mob.getBlockPos()), SpawnReason.EVENT, null, null);
		// A wave mob stays until it is killed or the night ends, however far its player runs.
		mob.setPersistent();
		world.spawnEntityAndPassengers(mob);
		world.spawnParticles(particle, x, y + 1, z, 30, 0.4, 0.8, 0.4, 0.02);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, 10, 0.3, 0.3, 0.3, 0.01);
		return mob;
	}

	/** A wave mob from another night (or from no wave any more) vanishes as soon as it loads. */
	@SuppressWarnings("UnstableApiUsage")
	private static void onLoad(Entity entity, ServerWorld world) {
		Long night = entity.getAttached(WAVE_NIGHT);
		if (night == null) {
			return;
		}
		WaveState state = WaveState.get(world.getServer());
		if (night != state.night || state.ownerOf(entity.getUuid()) == null) {
			STALE.add(entity);
		}
	}

	/** A wave mob killed or discarded leaves its wave; the last one clears it. */
	@SuppressWarnings("UnstableApiUsage")
	private static void onUnload(Entity entity, ServerWorld world) {
		Entity.RemovalReason reason = entity.getRemovalReason();
		if (reason == null || !reason.shouldDestroy() || !entity.hasAttached(WAVE_NIGHT)) {
			return;
		}
		MinecraftServer server = world.getServer();
		WaveState state = WaveState.get(server);
		UUID owner = state.ownerOf(entity.getUuid());
		if (owner == null) {
			return;
		}
		WaveState.Wave wave = state.waves.get(owner);
		wave.mobs.remove(entity.getUuid());
		state.markDirty();
		if (!wave.mobs.isEmpty()) {
			return;
		}
		state.waves.remove(owner);
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(owner);
		if (player != null) {
			player.sendMessage(Text.translatable("embervoid.wave.cleared").formatted(Formatting.GOLD), false);
		}
		if (wave.kind == WaveRules.Kind.GHOSTS && reason == Entity.RemovalReason.KILLED) {
			if (player != null) {
				giveShard(player);
			} else {
				state.pendingShards.add(owner);
			}
		}
		// Last, as it may end the night and with it call endNight.
		MoonNights.release(server, holdId(owner));
	}

	private static void giveShard(ServerPlayerEntity player) {
		player.getInventory().offerOrDrop(new ItemStack(SanctumItems.FLASK_SHARD));
		player.sendMessage(Text.translatable("embervoid.wave.shard").formatted(Formatting.GOLD), false);
	}

	/** Removes {@code owner}'s wave (debug): its mobs vanish and its hold is released. */
	static int clearWave(MinecraftServer server, UUID owner) {
		WaveState state = WaveState.get(server);
		WaveState.Wave wave = state.waves.remove(owner);
		if (wave == null) {
			return 0;
		}
		state.markDirty();
		ServerWorld world = server.getOverworld();
		for (UUID uuid : wave.mobs) {
			Entity mob = world.getEntity(uuid);
			if (mob != null) {
				vanish(world, mob);
			}
		}
		MoonNights.release(server, holdId(owner));
		return wave.mobs.size();
	}

	private static void vanish(ServerWorld world, Entity mob) {
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getBodyY(0.5), mob.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
		mob.discard();
	}

	private NightWaves() {}
}
