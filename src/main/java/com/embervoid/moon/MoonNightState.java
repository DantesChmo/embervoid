package com.embervoid.moon;

import com.embervoid.moon.api.MoonNight;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The overworld's moon-night state, saved with the world: which night is on, for which day, how long it has run and
 * who holds it. Changed only through {@link MoonNightManager}.
 */
public final class MoonNightState extends PersistentState {
	private static final String ID = "embervoid_moon";

	MoonNight night = MoonNight.NONE;
	/** Day whose night is on (see {@link MoonSchedule}); meaningless while {@link #night} is NONE. */
	long nightDay;
	/** Server ticks the current night has run, for the real-time fallback. */
	long elapsedTicks;
	/** Day of the last night that ended, so a night stopped early does not start again. */
	long lastEndedDay = Long.MIN_VALUE;
	final Set<Identifier> holds = new LinkedHashSet<>();
	/** Number of full moons that have ended; curse stacks from an earlier count are void (see {@link CurseManager}). */
	long curseEpoch;

	public static MoonNightState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(MoonNightState::fromNbt, MoonNightState::new, ID);
	}

	public MoonNight night() {
		return night;
	}

	public long nightDay() {
		return nightDay;
	}

	public long elapsedTicks() {
		return elapsedTicks;
	}

	public long curseEpoch() {
		return curseEpoch;
	}

	public Set<Identifier> holds() {
		return Collections.unmodifiableSet(holds);
	}

	private static MoonNightState fromNbt(NbtCompound nbt) {
		MoonNightState state = new MoonNightState();
		state.night = MoonNight.byOrdinal(nbt.getInt("Night"));
		state.nightDay = nbt.getLong("NightDay");
		state.elapsedTicks = nbt.getLong("Elapsed");
		state.curseEpoch = nbt.getLong("CurseEpoch");
		state.lastEndedDay = nbt.contains("LastEndedDay") ? nbt.getLong("LastEndedDay") : Long.MIN_VALUE;
		NbtList holds = nbt.getList("Holds", NbtElement.STRING_TYPE);
		for (int i = 0; i < holds.size(); i++) {
			Identifier id = Identifier.tryParse(holds.getString(i));
			if (id != null) {
				state.holds.add(id);
			}
		}
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putInt("Night", night.ordinal());
		nbt.putLong("NightDay", nightDay);
		nbt.putLong("Elapsed", elapsedTicks);
		nbt.putLong("LastEndedDay", lastEndedDay);
		nbt.putLong("CurseEpoch", curseEpoch);
		NbtList list = new NbtList();
		holds.forEach(id -> list.add(NbtString.of(id.toString())));
		nbt.put("Holds", list);
		return nbt;
	}
}
