package com.embervoid.vampire;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** The blood new moon's Vampire state, saved with the overworld so a restart mid-night picks up where it was. */
public final class VampireState extends PersistentState {
	private static final String ID = "embervoid_vampire";

	public enum Stage {
		/** No Vampires this night. */
		NONE,
		/** Friendly Vampires wait near the players. */
		GROUPS,
		/** One Vampire is turning; the others are gone. */
		TURNING
	}

	/** Counts blood nights, so a Vampire saved in an unloaded chunk knows it belongs to an earlier one. */
	int nightId;
	Stage stage = Stage.NONE;
	/** Ticks left until the nearest Vampire turns on its own (stage {@link Stage#GROUPS}). */
	int provokeTicks;
	/** The Vampire that is turning (stage {@link Stage#TURNING}). */
	@Nullable
	UUID turning;

	public static VampireState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(VampireState::fromNbt, VampireState::new, ID);
	}

	public Stage stage() {
		return stage;
	}

	public int nightId() {
		return nightId;
	}

	private static VampireState fromNbt(NbtCompound nbt) {
		VampireState state = new VampireState();
		state.nightId = nbt.getInt("NightId");
		try {
			state.stage = Stage.valueOf(nbt.getString("Stage"));
		} catch (IllegalArgumentException e) {
			state.stage = Stage.NONE;
		}
		state.provokeTicks = nbt.getInt("ProvokeTicks");
		state.turning = nbt.containsUuid("Turning") ? nbt.getUuid("Turning") : null;
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putInt("NightId", nightId);
		nbt.putString("Stage", stage.name());
		nbt.putInt("ProvokeTicks", provokeTicks);
		if (turning != null) {
			nbt.putUuid("Turning", turning);
		}
		return nbt;
	}
}
