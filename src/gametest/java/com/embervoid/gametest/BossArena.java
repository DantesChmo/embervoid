package com.embervoid.gametest;

import com.embervoid.boss.ArenaFog;
import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.ArenaSpec;
import com.embervoid.boss.BossFight;
import com.embervoid.config.EmbervoidConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * A boss arena for GameTests: a stone floor in the test area, real (non-creative) players joined through the player
 * manager, and fights started with {@link ArenaManager#start}. Every step runs through {@link #step}: a failure ends
 * the fights, removes the players and puts the config and difficulty back before it reaches the runner.
 */
final class BossArena {
	/** A newly joined player cannot be hurt for 60 ticks; wait that out before hitting one. */
	static final int JOIN_INVULNERABLE_TICKS = 65;

	final TestContext context;
	final MinecraftServer server;
	private final Difficulty difficulty;
	private final List<ServerPlayerEntity> players = new ArrayList<>();
	private final List<BossFight> fights = new ArrayList<>();
	private final List<ChunkPos> forced = new ArrayList<>();

	/** {@code configJson}: a config to use for the test (e.g. {@code {"boss": {...}}}), or null for the local one. */
	BossArena(TestContext context, @Nullable String configJson) {
		this.context = context;
		this.server = context.getWorld().getServer();
		this.difficulty = server.getSaveProperties().getDifficulty();
		server.setDifficulty(Difficulty.NORMAL, true);
		if (configJson == null) {
			EmbervoidConfig.useLocal();
		} else {
			EmbervoidConfig.useServer(EmbervoidConfig.fromNetworkString(configJson));
		}
	}

	/** A stone floor of radius {@code r} around relative {@code (x, 0, z)}. */
	void floor(int x, int z, int r) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				context.setBlockState(x + dx, 0, z + dz, Blocks.STONE);
			}
		}
	}

	/** A survival player joined on {@code connection}, standing at relative {@code (x, 1, z)}. */
	ServerPlayerEntity join(int x, int z, ClientConnection connection) {
		ServerPlayerEntity player = new ServerPlayerEntity(server, context.getWorld(), new GameProfile(UUID.randomUUID(), "boss-test-player"));
		server.getPlayerManager().onPlayerConnect(connection, player);
		player.changeGameMode(GameMode.SURVIVAL);
		Vec3d pos = context.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(x, 1, z)));
		player.teleport(context.getWorld(), pos.x, pos.y, pos.z, 0, 0);
		players.add(player);
		return player;
	}

	/** A survival player joined standing at an absolute position (in a placed arena). */
	ServerPlayerEntity joinAt(Vec3d pos) {
		return joinAt(pos, new FogRecorder());
	}

	ServerPlayerEntity joinAt(Vec3d pos, ClientConnection connection) {
		ServerPlayerEntity player = new ServerPlayerEntity(server, context.getWorld(), new GameProfile(UUID.randomUUID(), "boss-test-player"));
		server.getPlayerManager().onPlayerConnect(connection, player);
		player.changeGameMode(GameMode.SURVIVAL);
		player.teleport(context.getWorld(), pos.x, pos.y, pos.z, 0, 0);
		players.add(player);
		return player;
	}

	/**
	 * Force-loads the chunks within {@code radius} of an absolute position (an arena placed far from the test area,
	 * where no test player's chunk ticket reaches: the test players never move as far as the chunk manager knows).
	 * Released when the test ends.
	 */
	void forceChunks(BlockPos center, int radius) {
		ChunkPos min = new ChunkPos(center.add(-radius, 0, -radius));
		ChunkPos max = new ChunkPos(center.add(radius, 0, radius));
		for (int x = min.x; x <= max.x; x++) {
			for (int z = min.z; z <= max.z; z++) {
				context.getWorld().setChunkForced(x, z, true);
				forced.add(new ChunkPos(x, z));
			}
		}
	}

	/** Starts a fight at an absolute arena centre (the floor block). */
	BossFight startAt(ArenaSpec spec, BlockPos center) {
		BossFight fight = ArenaManager.start(context.getWorld(), spec, center);
		context.assertTrue(fight != null, "the fight did not start");
		fights.add(fight);
		return fight;
	}

	ServerPlayerEntity join(int x, int z) {
		return join(x, z, new FogRecorder());
	}

	/** Starts a fight of {@code entity} at relative {@code (x, 0, z)} (the floor block), level {@code level}. */
	BossFight start(EntityType<?> entity, int x, int z, int radius, int level) {
		BlockPos center = context.getAbsolutePos(new BlockPos(x, 0, z));
		ArenaSpec spec = new ArenaSpec(new Identifier("embervoid-gametest", "boss_" + fights.size()), entity, level, radius);
		BossFight fight = ArenaManager.start(context.getWorld(), spec, center);
		context.assertTrue(fight != null, "the fight did not start");
		fights.add(fight);
		return fight;
	}

	void step(Runnable step) {
		try {
			step.run();
		} catch (RuntimeException | Error e) {
			cleanUp();
			throw e;
		}
	}

	/** Runs {@code step} after {@code ticks} ticks. */
	void after(int ticks, Runnable step) {
		context.waitAndRun(ticks, () -> step(step));
	}

	/** Runs {@code then} once {@code condition} holds, checking every tick; fails after {@code limit} ticks. */
	void await(BooleanSupplier condition, int limit, String what, Runnable then) {
		step(() -> {
			if (condition.getAsBoolean()) {
				then.run();
			} else if (limit <= 0) {
				throw new GameTestException("gave up waiting for " + what);
			} else {
				context.waitAndRun(1, () -> await(condition, limit - 1, what, then));
			}
		});
	}

	void finish() {
		cleanUp();
		context.complete();
	}

	private void cleanUp() {
		for (BossFight fight : fights) {
			if (!fight.isEnded()) {
				ArenaManager.reset(fight);
			}
		}
		for (ServerPlayerEntity player : players) {
			if (server.getPlayerManager().getPlayer(player.getUuid()) == player) {
				server.getPlayerManager().remove(player);
			}
		}
		for (ChunkPos chunk : forced) {
			context.getWorld().setChunkForced(chunk.x, chunk.z, false);
		}
		forced.clear();
		EmbervoidConfig.useLocal();
		server.setDifficulty(difficulty, true);
	}

	/** A connection that records the arena fog packets sent to it ({density, colour}; density < 0 = clear). */
	static final class FogRecorder extends ClientConnection {
		final List<float[]> fog = new ArrayList<>();

		FogRecorder() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {
			send(packet, null);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof CustomPayloadS2CPacket payload && payload.getChannel().equals(ArenaFog.PACKET)) {
				PacketByteBuf buf = new PacketByteBuf(payload.getData().copy());
				buf.readIdentifier();
				fog.add(new float[]{buf.readFloat(), buf.readInt()});
			}
		}
	}
}
