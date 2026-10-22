package com.embervoid.gametest;

import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * Survival server players for tests. They join through the real player manager (so join events fire) and stand on a
 * stone floor inside the test area. A test ends with {@link #complete} so they never leak into other tests.
 */
final class TestPlayers {
	/** A survival player standing at relative {@code (x, 1, z)} on a 3×3 stone floor. */
	static ServerPlayerEntity create(TestContext context, int x, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				context.setBlockState(x + dx, 0, z + dz, Blocks.STONE);
			}
		}
		ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
		player.changeGameMode(GameMode.SURVIVAL);
		Vec3d pos = context.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(x, 1, z)));
		player.teleport(context.getWorld(), pos.x, pos.y, pos.z, 0, 0);
		return player;
	}

	/**
	 * Respawns a dead player the way the death screen does; the returned player replaces {@code player}. As in vanilla,
	 * the connection is handed to the new player only after the respawn (and its events) is done.
	 */
	static ServerPlayerEntity respawn(ServerPlayerEntity player) {
		ServerPlayerEntity respawned = player.getServer().getPlayerManager().respawnPlayer(player, false);
		respawned.networkHandler.player = respawned;
		return respawned;
	}

	/** Removes the players from the server and completes the test. */
	static void complete(TestContext context, ServerPlayerEntity... players) {
		for (ServerPlayerEntity player : players) {
			if (player.getServer().getPlayerManager().getPlayer(player.getUuid()) == player) {
				player.getServer().getPlayerManager().remove(player);
			}
		}
		context.complete();
	}

	private TestPlayers() {}
}
