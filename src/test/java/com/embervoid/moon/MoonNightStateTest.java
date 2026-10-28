package com.embervoid.moon;

import com.embervoid.moon.api.MoonNight;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoonNightStateTest {
	private static MoonNightState roundTrip(MoonNightState state) {
		return MoonNightState.fromNbt(state.writeNbt(new NbtCompound()));
	}

	@Test
	void aNightOnSurvivesASave() {
		MoonNightState state = new MoonNightState();
		state.night = MoonNight.BLOOD_NEW_MOON;
		state.nightDay = 28;
		state.elapsedTicks = 1234;
		state.lastEndedDay = 24;
		state.curseEpoch = 3;
		state.holds.add(new Identifier("embervoid", "vampire"));
		state.holds.add(new Identifier("embervoid", "wave/00000000-0000-0000-0000-000000000001"));

		MoonNightState loaded = roundTrip(state);
		assertEquals(MoonNight.BLOOD_NEW_MOON, loaded.night());
		assertEquals(28, loaded.nightDay());
		assertEquals(1234, loaded.elapsedTicks());
		assertEquals(24, loaded.lastEndedDay);
		assertEquals(3, loaded.curseEpoch());
		assertEquals(List.copyOf(state.holds()), List.copyOf(loaded.holds()));
	}

	@Test
	void aFreshStateSurvivesASave() {
		MoonNightState loaded = roundTrip(new MoonNightState());
		assertEquals(MoonNight.NONE, loaded.night());
		assertTrue(loaded.holds().isEmpty());
		// "Never ended" stays never: day 0 must not turn into the day a night ended.
		assertEquals(new MoonNightState().lastEndedDay, loaded.lastEndedDay);
	}

	@Test
	void aSaveWithoutTheLastEndedDayBlocksNoDay() {
		NbtCompound old = new MoonNightState().writeNbt(new NbtCompound());
		old.remove("LastEndedDay");
		MoonNightState loaded = MoonNightState.fromNbt(old);
		for (long day : new long[] {0, 24, 28}) {
			assertTrue(loaded.lastEndedDay != day, "day " + day);
		}
	}
}
