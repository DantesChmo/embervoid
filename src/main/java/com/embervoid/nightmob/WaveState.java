package com.embervoid.nightmob;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The full moon waves of the current night, saved with the overworld: each player's living wave mobs and who has
 * had a wave this night. {@link #night} counts full moon nights; a wave mob remembers the night it came on (see
 * {@code NightWaves}), so one left in an unloaded chunk after its night vanishes when it loads.
 */
final class WaveState extends PersistentState {
	private static final String ID = "embervoid_night_waves";

	/** One player's wave: what it is and the UUIDs of its mobs still alive. */
	static final class Wave {
		final WaveRules.Kind kind;
		final Set<UUID> mobs = new HashSet<>();

		Wave(WaveRules.Kind kind) {
			this.kind = kind;
		}
	}

	long night;
	/** Living waves by owner. */
	final Map<UUID, Wave> waves = new LinkedHashMap<>();
	/** Players who got their wave this night (also when nothing could be placed). */
	final Set<UUID> served = new HashSet<>();
	/** Owners of a Ghost wave cleared while they were offline: they get the Flask Shard when they are back. */
	final Set<UUID> pendingShards = new HashSet<>();

	static WaveState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(WaveState::fromNbt, WaveState::new, ID);
	}

	/** The owner of the wave that {@code mob} belongs to, or null. */
	@Nullable
	UUID ownerOf(UUID mob) {
		for (Map.Entry<UUID, Wave> entry : waves.entrySet()) {
			if (entry.getValue().mobs.contains(mob)) {
				return entry.getKey();
			}
		}
		return null;
	}

	static WaveState fromNbt(NbtCompound nbt) {
		WaveState state = new WaveState();
		state.night = nbt.getLong("Night");
		NbtCompound waves = nbt.getCompound("Waves");
		for (String owner : waves.getKeys()) {
			NbtCompound waveNbt = waves.getCompound(owner);
			WaveRules.Kind kind = "ghosts".equals(waveNbt.getString("Kind")) ? WaveRules.Kind.GHOSTS : WaveRules.Kind.ZOMBIES;
			Wave wave = new Wave(kind);
			for (NbtElement mob : waveNbt.getList("Mobs", NbtElement.INT_ARRAY_TYPE)) {
				wave.mobs.add(NbtHelper.toUuid(mob));
			}
			state.waves.put(UUID.fromString(owner), wave);
		}
		readUuids(nbt.getList("Served", NbtElement.INT_ARRAY_TYPE), state.served);
		readUuids(nbt.getList("PendingShards", NbtElement.INT_ARRAY_TYPE), state.pendingShards);
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putLong("Night", night);
		NbtCompound waves = new NbtCompound();
		this.waves.forEach((owner, wave) -> {
			NbtCompound waveNbt = new NbtCompound();
			waveNbt.putString("Kind", wave.kind == WaveRules.Kind.GHOSTS ? "ghosts" : "zombies");
			waveNbt.put("Mobs", writeUuids(wave.mobs));
			waves.put(owner.toString(), waveNbt);
		});
		nbt.put("Waves", waves);
		nbt.put("Served", writeUuids(served));
		nbt.put("PendingShards", writeUuids(pendingShards));
		return nbt;
	}

	private static void readUuids(NbtList list, Set<UUID> into) {
		for (NbtElement element : list) {
			into.add(NbtHelper.toUuid(element));
		}
	}

	private static NbtList writeUuids(Set<UUID> uuids) {
		NbtList list = new NbtList();
		uuids.forEach(uuid -> list.add(NbtHelper.fromUuid(uuid)));
		return list;
	}
}
