package com.embervoid.boss;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The real blocks a fight changes in its arena (rising water, smashed pillars, lit braziers). Each block's state from
 * before its first change is kept, and {@link #restore} puts every one back when the fight ends, kill or reset.
 */
public final class ArenaBlocks {
	private final ServerWorld world;
	private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();

	ArenaBlocks(ServerWorld world) {
		this.world = world;
	}

	/** Changes a block, remembering what was there before this fight first touched it. */
	public void set(BlockPos pos, BlockState state) {
		BlockPos at = pos.toImmutable();
		original.putIfAbsent(at, world.getBlockState(at));
		world.setBlockState(at, state, Block.NOTIFY_ALL);
	}

	/**
	 * Remembers a block's state now without changing it, so whatever happens to it during the fight (a player lights
	 * a brazier) is undone when the fight ends.
	 */
	public void remember(BlockPos pos) {
		BlockPos at = pos.toImmutable();
		original.putIfAbsent(at, world.getBlockState(at));
	}

	/** Whether this fight has changed the block. */
	public boolean isChanged(BlockPos pos) {
		return original.containsKey(pos);
	}

	/** The state before the fight changed it, or the current state of an untouched block. */
	public BlockState originalState(BlockPos pos) {
		BlockState state = original.get(pos);
		return state != null ? state : world.getBlockState(pos);
	}

	public int size() {
		return original.size();
	}

	/** Puts every changed block back, last change first. */
	void restore() {
		List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(original.entrySet());
		for (int i = entries.size() - 1; i >= 0; i--) {
			world.setBlockState(entries.get(i).getKey(), entries.get(i).getValue(), Block.NOTIFY_ALL);
		}
		original.clear();
	}
}
