package com.embervoid.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
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

import java.util.UUID;

/**
 * Helpers for the Realm 1 mob tests.
 *
 * <p>Two traps of the test API: {@code TestContext.spawnMob} clears the mob's goals (use {@link #mob} when its AI
 * must run), and the mock test player always answers {@code isCreative()} with true, so mobs that skip creative
 * players ignore it (use {@link #player}).
 */
final class ReachTests {
	/** A mob with its goals intact, at relative (x, y, z). */
	@SuppressWarnings("unchecked")
	static <T extends MobEntity> T mob(TestContext context, EntityType<T> type, int x, int y, int z) {
		return (T) context.spawnEntity(type, x, y, z);
	}

	/**
	 * A real survival player (joined through the player manager, like MoonTests does) standing at relative
	 * {@code (x, 1, z)} on a 3×3 stone floor. Remove it with {@code TestPlayers.complete} or {@link #guard}.
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
		return player;
	}

	/** Moves a test player to relative (x, y, z). */
	static void teleport(TestContext context, ServerPlayerEntity player, double x, double y, double z) {
		Vec3d at = context.getAbsolute(new Vec3d(x, y, z));
		player.teleport(context.getWorld(), at.x, at.y, at.z, 0, 0);
	}

	/**
	 * Runs {@code body}; if an assertion fails, removes the test's players before rethrowing, so a failed test leaves
	 * no player behind for later batches (full moon waves would go after it).
	 */
	static Runnable guard(TestContext context, Runnable body, ServerPlayerEntity... players) {
		return () -> {
			try {
				body.run();
			} catch (RuntimeException e) {
				var manager = context.getWorld().getServer().getPlayerManager();
				for (ServerPlayerEntity player : players) {
					if (manager.getPlayer(player.getUuid()) == player) {
						manager.remove(player);
					}
				}
				throw e;
			}
		};
	}

	/** A connection that drops every packet. */
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
