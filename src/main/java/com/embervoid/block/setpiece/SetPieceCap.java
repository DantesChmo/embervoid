package com.embervoid.block.setpiece;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The cap on set pieces per chunk ({@link SetPieceConfig#maxPerChunk}). A chunk's pieces are counted when one of
 * them comes alive on the server (chunk load, worldgen, a command, a player); at the end of that world tick the
 * pieces beyond the cap become air (or water where they were waterlogged). Which ones stay is {@link #keep}'s
 * deterministic choice, so a structure loses the same pieces on every server.
 */
public final class SetPieceCap {
	private static final Map<RegistryKey<World>, Set<ChunkPos>> PENDING = new HashMap<>();

	public static void register() {
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof SetPieceBlockEntity) {
				PENDING.computeIfAbsent(world.getRegistryKey(), key -> new LinkedHashSet<>()).add(new ChunkPos(entity.getPos()));
			}
		});
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			Set<ChunkPos> chunks = PENDING.remove(world.getRegistryKey());
			if (chunks != null) {
				for (ChunkPos chunk : chunks) {
					enforce(world, chunk);
				}
			}
		});
	}

	/** Set pieces in a loaded chunk. */
	public static List<BlockPos> piecesIn(World world, ChunkPos chunk) {
		List<BlockPos> pieces = new ArrayList<>();
		if (!world.isChunkLoaded(chunk.x, chunk.z)) {
			return pieces;
		}
		WorldChunk loaded = world.getChunk(chunk.x, chunk.z);
		for (BlockEntity entity : loaded.getBlockEntities().values()) {
			if (entity instanceof SetPieceBlockEntity && !entity.isRemoved()) {
				pieces.add(entity.getPos().toImmutable());
			}
		}
		return pieces;
	}

	/** Whether the chunk of {@code pos} already holds the cap. */
	public static boolean isFull(World world, BlockPos pos) {
		return piecesIn(world, new ChunkPos(pos)).size() >= SetPieceConfig.get().maxPerChunk();
	}

	/** Removes the pieces beyond the cap from a chunk. Returns how many went. */
	public static int enforce(ServerWorld world, ChunkPos chunk) {
		List<BlockPos> pieces = piecesIn(world, chunk);
		int max = SetPieceConfig.get().maxPerChunk();
		if (pieces.size() <= max) {
			return 0;
		}
		List<BlockPos> kept = keep(pieces, max);
		int removed = 0;
		for (BlockPos pos : pieces) {
			if (kept.contains(pos)) {
				continue;
			}
			boolean water = world.getBlockState(pos).getFluidState().isStill();
			world.setBlockState(pos, water ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState());
			removed++;
		}
		Embervoid.LOGGER.warn("Chunk {} held {} set pieces, cap {}: removed {}", chunk, pieces.size(), max, removed);
		return removed;
	}

	/**
	 * Which pieces stay when a chunk holds more than {@code max}: sorted by y, then z, then x, the first {@code max}
	 * (the lowest and northernmost, so a structure keeps its ground-level machinery before its roof decor). Pure.
	 */
	public static List<BlockPos> keep(List<BlockPos> pieces, int max) {
		List<BlockPos> sorted = new ArrayList<>(pieces);
		sorted.sort(Comparator.comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getX));
		return List.copyOf(sorted.subList(0, Math.max(0, Math.min(max, sorted.size()))));
	}

	private SetPieceCap() {}
}
