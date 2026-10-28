package com.embervoid.boss;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * The real blocks a fight changes in its arena (rising water, smashed pillars, lit braziers). Each block's state from
 * before its first change is kept, and {@link #restore} puts every one back when the fight ends, kill or reset.
 */
public final class ArenaBlocks {
	private final BlockChanges<BlockState> changes;

	ArenaBlocks(ServerWorld world) {
		this.changes = new BlockChanges<>(new BlockChanges.Access<>() {
			@Override
			public BlockState get(BlockPos pos) {
				return world.getBlockState(pos);
			}

			@Override
			public void set(BlockPos pos, BlockState state) {
				world.setBlockState(pos, state, Block.NOTIFY_ALL);
			}
		});
	}

	/** Changes a block, remembering what was there before this fight first touched it. */
	public void set(BlockPos pos, BlockState state) {
		changes.set(pos, state);
	}

	/**
	 * Remembers a block's state now without changing it, so whatever happens to it during the fight (a player lights
	 * a brazier) is undone when the fight ends.
	 */
	public void remember(BlockPos pos) {
		changes.remember(pos);
	}

	/**
	 * Remembers {@code state} as the block's state before the fight, unless it is already remembered: for blocks the
	 * fight itself changed indirectly (water that flowed from the fight's own water).
	 */
	public void rememberAs(BlockPos pos, BlockState state) {
		changes.rememberAs(pos, state);
	}

	/** Whether this fight has changed the block. */
	public boolean isChanged(BlockPos pos) {
		return changes.isChanged(pos);
	}

	/** The state before the fight changed it, or the current state of an untouched block. */
	public BlockState originalState(BlockPos pos) {
		return changes.originalState(pos);
	}

	public int size() {
		return changes.size();
	}

	/** Puts every changed block back, last change first. */
	void restore() {
		changes.restore();
	}
}
