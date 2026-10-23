package com.embervoid.hub;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/** Saved with the hub dimension: whether its structure has been placed, so it is placed only once per world. */
public final class HubState extends PersistentState {
	private static final String ID = "embervoid_hub";

	boolean placed;
	/** The hub world's game time when the structure was placed (for the debug command). */
	long placedAt;

	public static HubState get(ServerWorld hub) {
		return hub.getPersistentStateManager().getOrCreate(HubState::fromNbt, HubState::new, ID);
	}

	public boolean placed() {
		return placed;
	}

	private static HubState fromNbt(NbtCompound nbt) {
		HubState state = new HubState();
		state.placed = nbt.getBoolean("Placed");
		state.placedAt = nbt.getLong("PlacedAt");
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putBoolean("Placed", placed);
		nbt.putLong("PlacedAt", placedAt);
		return nbt;
	}
}
