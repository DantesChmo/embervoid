package com.embervoid.entity;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-wide record of each player's current Ash Remnant, saved with the world. A remnant that is not its owner's
 * current one burns away when it next ticks, which covers remnants in unloaded chunks or other dimensions.
 */
public final class RemnantRegistry extends PersistentState {
	private static final String ID = "embervoid_remnants";

	private final Map<UUID, UUID> remnantByOwner = new HashMap<>();

	public static RemnantRegistry get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(RemnantRegistry::fromNbt, RemnantRegistry::new, ID);
	}

	@Nullable
	public UUID getRemnant(UUID owner) {
		return remnantByOwner.get(owner);
	}

	public void setRemnant(UUID owner, UUID remnant) {
		remnantByOwner.put(owner, remnant);
		markDirty();
	}

	/** Forgets the owner's remnant if it is still {@code remnant} (after it was reclaimed). */
	public void clearRemnant(UUID owner, UUID remnant) {
		if (remnantByOwner.remove(owner, remnant)) {
			markDirty();
		}
	}

	private static RemnantRegistry fromNbt(NbtCompound nbt) {
		RemnantRegistry registry = new RemnantRegistry();
		NbtCompound owners = nbt.getCompound("Remnants");
		for (String owner : owners.getKeys()) {
			registry.remnantByOwner.put(UUID.fromString(owner), owners.getUuid(owner));
		}
		return registry;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtCompound owners = new NbtCompound();
		remnantByOwner.forEach((owner, remnant) -> owners.putUuid(owner.toString(), remnant));
		nbt.put("Remnants", owners);
		return nbt;
	}
}
