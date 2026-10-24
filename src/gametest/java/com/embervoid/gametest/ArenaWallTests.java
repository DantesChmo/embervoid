package com.embervoid.gametest;

import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.ArenaWalls;
import com.embervoid.boss.BossEntities;
import com.embervoid.boss.BossFight;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;

/**
 * The fog wall's look ({@link ArenaWalls}): a player near a running fight is told where its wall stands within a
 * second, a player far away is not, and the wall is taken down once the fight ends. How it is drawn is not checked.
 */
public final class ArenaWallTests implements FabricGameTest {
	private static final int RADIUS = 8;

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "arena_walls", tickLimit = 120)
	public void wallSentNearAndRemovedAtTheEnd(TestContext context) {
		BossArena arena = new BossArena(context, null);
		BossArena.FogRecorder near = new BossArena.FogRecorder();
		BossArena.FogRecorder far = new BossArena.FogRecorder();
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 2);
			ServerPlayerEntity nearPlayer = arena.join(4, 4, near);
			arena.join(4, 4 + 400, far);
			BossFight fight = arena.start(BossEntities.TEST_BOSS, 4, 4, RADIUS, 10);
			context.assertTrue(nearPlayer.isAlive(), "the near player");
			arena.await(() -> !near.walls().isEmpty(), 45, "the wall packet", () -> {
				List<Boolean> walls = near.walls();
				context.assertTrue(!walls.get(0), "the first wall packet places the wall: " + walls);
				context.assertTrue(far.walls().isEmpty(), "the far player got a wall: " + far.walls());
				context.assertTrue(ArenaWalls.wasSent(nearPlayer, fight.center()), "the wall is recorded as sent");
				ArenaManager.reset(fight);
				arena.await(() -> near.walls().contains(true), 45, "the wall's removal", () -> {
					context.assertTrue(!ArenaWalls.wasSent(nearPlayer, fight.center()), "the wall is recorded as gone");
					arena.finish();
				});
			});
		});
	}
}
