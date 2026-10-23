package com.embervoid.sanctum.boss;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashSet;
import java.util.Set;

/**
 * The Ruined Sanctums of one world whose Fallen Sister is beaten (DESIGN §2 "Clearing"), by arena floor centre. Saved
 * with the world; a cleared dungeon never summons her again.
 */
public final class ClearedSanctums extends PersistentState {
	private static final String ID = "embervoid_cleared_sanctums";

	private final Set<Long> arenas = new HashSet<>();

	public static ClearedSanctums get(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(ClearedSanctums::fromNbt, ClearedSanctums::new, ID);
	}

	public boolean isCleared(BlockPos center) {
		return arenas.contains(center.asLong());
	}

	/** Marks an arena cleared; returns false if it already was. */
	public boolean clear(BlockPos center) {
		boolean added = arenas.add(center.asLong());
		if (added) {
			markDirty();
		}
		return added;
	}

	public int size() {
		return arenas.size();
	}

	private static ClearedSanctums fromNbt(NbtCompound nbt) {
		ClearedSanctums state = new ClearedSanctums();
		if (nbt.contains("Arenas", NbtElement.LONG_ARRAY_TYPE)) {
			for (long arena : nbt.getLongArray("Arenas")) {
				state.arenas.add(arena);
			}
		}
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.put("Arenas", new NbtLongArray(arenas.stream().mapToLong(Long::longValue).toArray()));
		return nbt;
	}
}
