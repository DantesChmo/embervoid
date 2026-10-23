package com.embervoid.block.setpiece;

import com.embervoid.boss.BossFight;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.List;

/**
 * Arena events (PLAN 20.1): what a boss fight tells the set pieces around it. {@link BossFight} fires
 * {@code summon}, {@code phase <n>}, {@code hazard <id>}, {@code bossHit heavy}, {@code kill} and {@code reset};
 * a boss move that is none of those fires its own word ({@code raise_dead}). Every set piece within the arena radius
 * + {@link #MARGIN} (and {@link #HEIGHT} blocks up or down) answers through its kind's reaction table.
 */
public final class ArenaEvents {
	/** Blocks beyond the fog wall that still count as the arena (the walls and niches around it). */
	public static final int MARGIN = 8;
	/** Blocks above and below the arena floor that count. */
	public static final int HEIGHT = 24;

	public static final String SUMMON = "summon";
	public static final String KILL = "kill";
	public static final String RESET = "reset";
	public static final String PHASE = "phase";
	public static final String HAZARD = "hazard";
	public static final String BOSS_HIT_HEAVY = "bossHit heavy";

	/** Fires {@code event} to the set pieces of a fight's arena. Returns how many pieces received it. */
	public static int fire(BossFight fight, String event) {
		return fire(fight.world(), fight.center(), fight.spec().radius() + MARGIN, event);
	}

	/** Fires {@code event} to every set piece within {@code radius} (horizontally) of {@code center} in loaded chunks. */
	public static int fire(ServerWorld world, BlockPos center, int radius, String event) {
		int count = 0;
		ChunkPos min = new ChunkPos(center.add(-radius, 0, -radius));
		ChunkPos max = new ChunkPos(center.add(radius, 0, radius));
		double r2 = (radius + 0.5) * (radius + 0.5);
		for (int cx = min.x; cx <= max.x; cx++) {
			for (int cz = min.z; cz <= max.z; cz++) {
				if (!world.isChunkLoaded(cx, cz)) {
					continue;
				}
				WorldChunk chunk = world.getChunk(cx, cz);
				for (BlockEntity entity : List.copyOf(chunk.getBlockEntities().values())) {
					if (!(entity instanceof SetPieceBlockEntity piece) || piece.isRemoved()) {
						continue;
					}
					BlockPos pos = piece.getPos();
					double dx = pos.getX() - center.getX();
					double dz = pos.getZ() - center.getZ();
					if (dx * dx + dz * dz > r2 || Math.abs(pos.getY() - center.getY()) > HEIGHT) {
						continue;
					}
					piece.onArenaEvent(event);
					count++;
				}
			}
		}
		return count;
	}

	private ArenaEvents() {}
}
