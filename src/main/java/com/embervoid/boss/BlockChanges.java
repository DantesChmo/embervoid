package com.embervoid.boss;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Blocks changed in a world of states {@code S}, each with its state from before its first change, put back in
 * reverse order ({@link ArenaBlocks} over the real world).
 */
public final class BlockChanges<S> {
	/** Reads and writes the states of the world's blocks. */
	public interface Access<S> {
		S get(BlockPos pos);

		void set(BlockPos pos, S state);
	}

	private final Access<S> world;
	private final Map<BlockPos, S> original = new LinkedHashMap<>();

	public BlockChanges(Access<S> world) {
		this.world = world;
	}

	/** Changes a block, remembering what was there before it was first touched. */
	public void set(BlockPos pos, S state) {
		BlockPos at = pos.toImmutable();
		original.putIfAbsent(at, world.get(at));
		world.set(at, state);
	}

	/** Remembers a block's state now without changing it. */
	public void remember(BlockPos pos) {
		BlockPos at = pos.toImmutable();
		original.putIfAbsent(at, world.get(at));
	}

	/** Remembers {@code state} as the block's earlier state, unless one is already remembered. */
	public void rememberAs(BlockPos pos, S state) {
		original.putIfAbsent(pos.toImmutable(), state);
	}

	public boolean isChanged(BlockPos pos) {
		return original.containsKey(pos);
	}

	/** The remembered earlier state, or the current state of an untouched block. */
	public S originalState(BlockPos pos) {
		S state = original.get(pos);
		return state != null ? state : world.get(pos);
	}

	public int size() {
		return original.size();
	}

	/** Puts every remembered block back, the last one first, and forgets them. */
	public void restore() {
		List<Map.Entry<BlockPos, S>> entries = new ArrayList<>(original.entrySet());
		for (int i = entries.size() - 1; i >= 0; i--) {
			world.set(entries.get(i).getKey(), entries.get(i).getValue());
		}
		original.clear();
	}
}
