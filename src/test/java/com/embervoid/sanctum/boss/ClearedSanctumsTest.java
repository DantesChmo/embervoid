package com.embervoid.sanctum.boss;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClearedSanctumsTest {
	private static final BlockPos A = new BlockPos(120, -12, -3400);
	private static final BlockPos B = new BlockPos(-800, 30, 64);

	@Test
	void anArenaIsClearedOnlyOnce() {
		ClearedSanctums cleared = new ClearedSanctums();
		assertFalse(cleared.isCleared(A));
		assertTrue(cleared.clear(A));
		assertFalse(cleared.clear(A));
		assertTrue(cleared.isCleared(A));
		assertEquals(1, cleared.size());
	}

	@Test
	void clearingOneArenaLeavesOthers() {
		ClearedSanctums cleared = new ClearedSanctums();
		cleared.clear(A);
		assertFalse(cleared.isCleared(B));
		assertFalse(cleared.isCleared(A.up()));
	}

	@Test
	void clearingMarksTheStateForSaving() {
		ClearedSanctums cleared = new ClearedSanctums();
		cleared.clear(A);
		assertTrue(cleared.isDirty());
		cleared.setDirty(false);
		cleared.clear(A);
		assertFalse(cleared.isDirty());
	}

	@Test
	void savedAsALongArrayOfPositions() {
		ClearedSanctums cleared = new ClearedSanctums();
		cleared.clear(A);
		cleared.clear(B);
		NbtCompound nbt = cleared.writeNbt(new NbtCompound());
		assertTrue(nbt.contains("Arenas", NbtElement.LONG_ARRAY_TYPE));
		long[] saved = nbt.getLongArray("Arenas");
		Arrays.sort(saved);
		long[] expected = {A.asLong(), B.asLong()};
		Arrays.sort(expected);
		assertArrayEquals(expected, saved);
	}

	@Test
	void aSavedStateLoadsBackTheSameArenas() {
		ClearedSanctums cleared = new ClearedSanctums();
		cleared.clear(A);
		cleared.clear(B);
		ClearedSanctums loaded = ClearedSanctums.fromNbt(cleared.writeNbt(new NbtCompound()));
		assertTrue(loaded.isCleared(A));
		assertTrue(loaded.isCleared(B));
		assertEquals(2, loaded.size());
		assertFalse(loaded.clear(A), "a loaded arena stays cleared");
	}

	@Test
	void aWorldWithoutSavedArenasLoadsNoneCleared() {
		ClearedSanctums loaded = ClearedSanctums.fromNbt(new NbtCompound());
		assertEquals(0, loaded.size());
		assertFalse(loaded.isCleared(A));
	}
}
