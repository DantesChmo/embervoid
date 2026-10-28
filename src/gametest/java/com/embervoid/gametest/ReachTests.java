package com.embervoid.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Helpers for the Realm 1 mob tests.
 *
 * <p>Two traps of the test API: {@code TestContext.spawnMob} clears the mob's goals (use {@link #mob} when its AI
 * must run), and the mock test player always answers {@code isCreative()} with true, so mobs that skip creative
 * players ignore it (use {@link #player}).
 *
 * <p>Test areas are reused by later batches and entities outlive their test, so everything a test spawns goes
 * through these helpers and is removed by {@link #finish} (or by {@link #guard} when an assertion fails).
 */
final class ReachTests {
	private static final Map<TestContext, List<Entity>> SPAWNED = Collections.synchronizedMap(new WeakHashMap<>());

	private static <T extends Entity> T track(TestContext context, T entity) {
		SPAWNED.computeIfAbsent(context, c -> new ArrayList<>()).add(entity);
		return entity;
	}

	/** A mob with its goals intact, at relative (x, y, z). */
	@SuppressWarnings("unchecked")
	static <T extends MobEntity> T mob(TestContext context, EntityType<T> type, int x, int y, int z) {
		return track(context, (T) context.spawnEntity(type, x, y, z));
	}

	/** A mob without goals ({@code spawnMob}): only its own tick logic runs. */
	static <T extends MobEntity> T still(TestContext context, EntityType<T> type, int x, int y, int z) {
		return track(context, context.spawnMob(type, x, y, z));
	}

	/** A 100 HP target on the ground that does not move or fight back, and that no mob of other tests hunts. */
	static CowEntity dummy(TestContext context, int x, int z) {
		CowEntity cow = still(context, EntityType.COW, x, 0, z);
		cow.setAiDisabled(true);
		cow.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100);
		cow.setHealth(100);
		return cow;
	}

	/** Records an entity the test made itself, for {@link #finish}. */
	static <T extends Entity> T own(TestContext context, T entity) {
		return track(context, entity);
	}

	/** Removes everything the test spawned (players leave the server) and completes it. */
	static void finish(TestContext context) {
		cleanUp(context);
		context.complete();
	}

	private static void cleanUp(TestContext context) {
		List<Entity> entities = SPAWNED.remove(context);
		if (entities == null) {
			return;
		}
		var manager = context.getWorld().getServer().getPlayerManager();
		for (Entity entity : entities) {
			if (entity instanceof ServerPlayerEntity player) {
				if (manager.getPlayer(player.getUuid()) == player) {
					manager.remove(player);
				}
			} else {
				entity.discard();
			}
		}
	}

	/**
	 * A real survival player (joined through the player manager, like MoonTests does) standing at relative
	 * {@code (x, 1, z)} on a 3×3 stone floor. It leaves with {@link #finish} (or {@link #guard} on a failure).
	 */
	static ServerPlayerEntity player(TestContext context, int x, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				context.setBlockState(x + dx, 0, z + dz, Blocks.STONE);
			}
		}
		MinecraftServer server = context.getWorld().getServer();
		ServerPlayerEntity player = new ServerPlayerEntity(server, context.getWorld(),
				new GameProfile(UUID.randomUUID(), "reach-test-player"));
		server.getPlayerManager().onPlayerConnect(new SilentConnection(), player);
		player.changeGameMode(GameMode.SURVIVAL);
		Vec3d pos = context.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(x, 1, z)));
		player.teleport(context.getWorld(), pos.x, pos.y, pos.z, 0, 0);
		return track(context, player);
	}

	/** A mock player ({@code TestPlayers}), for damage sources and effects: mobs ignore it. */
	static ServerPlayerEntity mockPlayer(TestContext context, int x, int z) {
		return track(context, TestPlayers.create(context, x, z));
	}

	static void teleport(TestContext context, ServerPlayerEntity player, double x, double y, double z) {
		Vec3d at = context.getAbsolute(new Vec3d(x, y, z));
		player.teleport(context.getWorld(), at.x, at.y, at.z, 0, 0);
	}

	/**
	 * Runs {@code body}; if an assertion fails, removes what the test spawned before rethrowing, so a failed test
	 * leaves no player (full moon waves would go after it) or mob behind for later batches.
	 */
	static Runnable guard(TestContext context, Runnable body) {
		return () -> {
			try {
				body.run();
			} catch (RuntimeException e) {
				cleanUp(context);
				throw e;
			}
		};
	}

	private static final class SilentConnection extends ClientConnection {
		SilentConnection() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {}
	}

	private ReachTests() {}
}
