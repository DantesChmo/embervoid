package com.embervoid.gametest;

import com.embervoid.boss.ArenaSpec;
import com.embervoid.boss.BossFight;
import com.embervoid.reach.boss.ReachBoss;
import com.embervoid.reach.boss.arena.ArenaFrame;
import com.embervoid.reach.boss.arena.ReachArenas;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A zone boss fight in its real arena template for GameTests: the template placed at its own far spot with its chunks
 * forced, real players joined standing at layout offsets, and, once the chunks tick, the fight started and handed to
 * the test. The boss's brain is held (no moves of its own) unless the test lets it go.
 */
final class PlacedFight<T extends ReachBoss> {
	final BossArena arena;
	final ArenaFrame frame;
	final List<ServerPlayerEntity> players = new ArrayList<>();
	BossFight fight;
	T boss;

	/**
	 * @param spots each fighter's layout offset {x, y, z}: y is the block they stand on
	 */
	static <T extends ReachBoss> void start(TestContext context, ReachArenas.Arena arena, BlockPos center, String bossId,
			EntityType<T> type, int level, int[][] spots, Consumer<PlacedFight<T>> then) {
		PlacedFight<T> placed = new PlacedFight<>(context, arena, center, spots);
		placed.arena.await(() -> placed.world().shouldTickEntity(center.up()), 200, "the arena chunks to tick", () -> {
			ArenaSpec spec = new ArenaSpec(new Identifier("embervoid", bossId), type, level, arena.radius());
			placed.fight = placed.arena.startAt(spec, center);
			if (placed.fight.boss() == null || placed.fight.boss().getType() != type) {
				throw new GameTestException("no " + type.getUntranslatedName() + " in the fight");
			}
			@SuppressWarnings("unchecked")
			T boss = (T) placed.fight.boss();
			boss.holdMoves(Integer.MAX_VALUE);
			placed.boss = boss;
			then.accept(placed);
		});
	}

	private PlacedFight(TestContext context, ReachArenas.Arena arena, BlockPos center, int[][] spots) {
		this.arena = new BossArena(context, null);
		this.arena.forceChunks(center, arena.site());
		this.frame = ArenaTemplateTests.place(context, arena, center);
		for (int[] spot : spots) {
			players.add(this.arena.joinAt(frame.top(spot[0], spot[1], spot[2])));
		}
	}

	ServerPlayerEntity player(int index) {
		return players.get(index);
	}

	ServerWorld world() {
		return arena.context.getWorld();
	}
}
