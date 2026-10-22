package com.embervoid.vampire;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
		TURNING,
		/** The boss is fighting ({@link VampireFight}). */
		FIGHT,
		/** The boss is beaten; the arena closes in a few seconds. */
		CLOSING
	}

	/** Where a player was pulled into the arena from. */
	public record ReturnPoint(GlobalPos pos, float yaw) {}

	/** Counts blood nights, so a Vampire saved in an unloaded chunk knows it belongs to an earlier one. */
	int nightId;
	Stage stage = Stage.NONE;
	/** Ticks left until the nearest Vampire turns on its own (stage {@link Stage#GROUPS}). */
	int provokeTicks;
	/** The Vampire that is turning (stage {@link Stage#TURNING}). */
	@Nullable
	UUID turning;
	/** The boss (stage {@link Stage#FIGHT}). */
	@Nullable
	UUID boss;
	/** Where the boss fights (stage {@link Stage#FIGHT}). */
	@Nullable
	RegistryKey<World> fightWorld;
	BlockPos fightCenter = BlockPos.ORIGIN;
	int bossLevel;
	/** Players who took part and died: they cannot join the fight again. */
	final Set<UUID> fallen = new HashSet<>();
	/** Return points of players pulled into the arena; kept for players who logged out inside until they log in. */
	final Map<UUID, ReturnPoint> returnPoints = new HashMap<>();
	/** Ticks until the arena closes (stage {@link Stage#CLOSING}). */
	int closeTicks;

	public static VampireState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(VampireState::fromNbt, VampireState::new, ID);
	}

	public Stage stage() {
		return stage;
	}

	public int nightId() {
		return nightId;
	}

	/** The boss of the running fight, or null. */
	@Nullable
	public UUID boss() {
		return boss;
	}

	public int bossLevel() {
		return bossLevel;
	}

	/** Where the player was pulled into the arena from, or null. */
	@Nullable
	public ReturnPoint returnPoint(UUID player) {
		return returnPoints.get(player);
	}

	/** The world the boss fights in, or null. */
	@Nullable
	public RegistryKey<World> fightWorld() {
		return fightWorld;
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
		state.boss = nbt.containsUuid("Boss") ? nbt.getUuid("Boss") : null;
		Identifier world = Identifier.tryParse(nbt.getString("FightWorld"));
		state.fightWorld = world == null || nbt.getString("FightWorld").isEmpty() ? null : RegistryKey.of(RegistryKeys.WORLD, world);
		state.fightCenter = BlockPos.fromLong(nbt.getLong("FightCenter"));
		state.bossLevel = nbt.getInt("BossLevel");
		for (NbtElement element : nbt.getList("Fallen", NbtElement.INT_ARRAY_TYPE)) {
			state.fallen.add(NbtHelper.toUuid(element));
		}
		for (NbtElement element : nbt.getList("ReturnPoints", NbtElement.COMPOUND_TYPE)) {
			NbtCompound entry = (NbtCompound) element;
			Identifier dimension = Identifier.tryParse(entry.getString("Dimension"));
			if (entry.containsUuid("Player") && dimension != null) {
				state.returnPoints.put(entry.getUuid("Player"), new ReturnPoint(GlobalPos.create(
						RegistryKey.of(RegistryKeys.WORLD, dimension), BlockPos.fromLong(entry.getLong("Pos"))), entry.getFloat("Yaw")));
			}
		}
		state.closeTicks = nbt.getInt("CloseTicks");
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
		if (boss != null) {
			nbt.putUuid("Boss", boss);
		}
		if (fightWorld != null) {
			nbt.putString("FightWorld", fightWorld.getValue().toString());
		}
		nbt.putLong("FightCenter", fightCenter.asLong());
		nbt.putInt("BossLevel", bossLevel);
		NbtList fallenList = new NbtList();
		for (UUID uuid : fallen) {
			fallenList.add(NbtHelper.fromUuid(uuid));
		}
		nbt.put("Fallen", fallenList);
		NbtList points = new NbtList();
		for (Map.Entry<UUID, ReturnPoint> point : returnPoints.entrySet()) {
			NbtCompound entry = new NbtCompound();
			entry.putUuid("Player", point.getKey());
			entry.putString("Dimension", point.getValue().pos().getDimension().getValue().toString());
			entry.putLong("Pos", point.getValue().pos().getPos().asLong());
			entry.putFloat("Yaw", point.getValue().yaw());
			points.add(entry);
		}
		nbt.put("ReturnPoints", points);
		nbt.putInt("CloseTicks", closeTicks);
		return nbt;
	}
}
