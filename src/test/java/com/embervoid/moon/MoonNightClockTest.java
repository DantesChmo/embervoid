package com.embervoid.moon;

import com.embervoid.moon.MoonNightClock.Step;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights.EndReason;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The moon-night state machine (CORE §3.4, {@code MoonNights}): a scheduled night starts at dusk and ends at dawn,
 * a held night stops the clock just before dawn, the real-time fallback ends it anyway, and a night that ended does
 * not start again the same day. Events every 3rd cycle: the full moon of day 24 is the first.
 */
class MoonNightClockTest {
	private static final long DAY = 24_000;
	private static final long FULL_MOON_DAY = 24;
	private static final long BLOOD_MOON_DAY = 28;
	/** One real-time minute of fallback: 60 s × 20 ticks. */
	private static final MoonConfig ONE_MINUTE = config(true, 1);

	private static MoonConfig config(boolean enabled, float fallbackMinutes) {
		return new MoonConfig(enabled, 3, fallbackMinutes, MoonConfig.DEFAULT.fullMoon(), MoonConfig.DEFAULT.blood());
	}

	private static long at(long day, long timeInDay) {
		return day * DAY + timeInDay;
	}

	private static MoonNightState on(MoonNight night, long day) {
		MoonNightState state = new MoonNightState();
		state.night = night;
		state.nightDay = day;
		state.elapsedTicks = 1;
		return state;
	}

	@Test
	void theFullMoonStartsAtDusk() {
		MoonNightState state = new MoonNightState();
		assertEquals(MoonNightClock.IDLE, MoonNightClock.tick(state, at(FULL_MOON_DAY, 12_999), ONE_MINUTE));
		assertEquals(new Step.Start(MoonNight.FULL_MOON, FULL_MOON_DAY),
				MoonNightClock.tick(state, at(FULL_MOON_DAY, 13_000), ONE_MINUTE));
	}

	@Test
	void theBloodNewMoonStartsAtDuskToo() {
		assertEquals(new Step.Start(MoonNight.BLOOD_NEW_MOON, BLOOD_MOON_DAY),
				MoonNightClock.tick(new MoonNightState(), at(BLOOD_MOON_DAY, 15_000), ONE_MINUTE));
	}

	@Test
	void ordinaryNightsAndDaysStartNothing() {
		MoonNightState state = new MoonNightState();
		assertEquals(MoonNightClock.IDLE, MoonNightClock.tick(state, at(25, 15_000), ONE_MINUTE));
		assertEquals(MoonNightClock.IDLE, MoonNightClock.tick(state, at(8, 15_000), ONE_MINUTE));
		assertEquals(MoonNightClock.IDLE, MoonNightClock.tick(state, at(FULL_MOON_DAY, 6_000), ONE_MINUTE));
	}

	@Test
	void aDisabledScheduleStartsNothing() {
		assertEquals(MoonNightClock.IDLE, MoonNightClock.tick(new MoonNightState(), at(FULL_MOON_DAY, 15_000), config(false, 1)));
	}

	@Test
	void aNightThatEndedDoesNotStartAgainTheSameDay() {
		MoonNightState state = new MoonNightState();
		state.lastEndedDay = FULL_MOON_DAY;
		assertEquals(MoonNightClock.IDLE, MoonNightClock.tick(state, at(FULL_MOON_DAY, 15_000), ONE_MINUTE));
		// The next event still comes.
		assertInstanceOf(Step.Start.class, MoonNightClock.tick(state, at(BLOOD_MOON_DAY, 15_000), ONE_MINUTE));
	}

	@Test
	void anUnheldNightRunsUntilDawn() {
		MoonNightState state = on(MoonNight.FULL_MOON, FULL_MOON_DAY);
		assertEquals(MoonNightClock.IDLE, MoonNightClock.tick(state, at(FULL_MOON_DAY, 22_999), ONE_MINUTE));
		assertEquals(new Step.End(EndReason.DAWN), MoonNightClock.tick(state, at(FULL_MOON_DAY, 23_000), ONE_MINUTE));
	}

	@Test
	void aNightSkippedPastByTheClockEndsAsDawn() {
		MoonNightState state = on(MoonNight.FULL_MOON, FULL_MOON_DAY);
		assertEquals(new Step.End(EndReason.DAWN), MoonNightClock.tick(state, at(FULL_MOON_DAY + 1, 1_000), ONE_MINUTE));
		assertEquals(new Step.End(EndReason.DAWN), MoonNightClock.tick(state, at(FULL_MOON_DAY + 1, 15_000), ONE_MINUTE));
	}

	@Test
	void aHeldNightStopsTheClockJustBeforeDawn() {
		MoonNightState state = on(MoonNight.FULL_MOON, FULL_MOON_DAY);
		state.holds.add(new Identifier("embervoid", "wave/test"));
		long dawn = at(FULL_MOON_DAY, 23_000);

		assertEquals(MoonNightClock.IDLE, MoonNightClock.tick(state, at(FULL_MOON_DAY, 20_000), ONE_MINUTE));
		for (long time : new long[] {dawn - 1, dawn, dawn + 500, at(FULL_MOON_DAY + 1, 6_000)}) {
			Step step = MoonNightClock.tick(state, time, ONE_MINUTE);
			Step.HoldClock hold = assertInstanceOf(Step.HoldClock.class, step, "at " + time);
			assertTrue(hold.timeOfDay() < dawn && hold.timeOfDay() >= dawn - 20, "held at " + hold.timeOfDay());
			assertTrue(MoonSchedule.isNight(hold.timeOfDay()));
			assertTrue(hold.timeOfDay() < time);
		}
	}

	@Test
	void aClockAlreadyHeldIsLeftAlone() {
		MoonNightState state = on(MoonNight.FULL_MOON, FULL_MOON_DAY);
		state.holds.add(new Identifier("embervoid", "wave/test"));
		Step.HoldClock hold = assertInstanceOf(Step.HoldClock.class,
				MoonNightClock.tick(state, at(FULL_MOON_DAY, 23_000), ONE_MINUTE));
		assertEquals(MoonNightClock.IDLE, MoonNightClock.tick(state, hold.timeOfDay(), ONE_MINUTE));
	}

	@Test
	void theFallbackEndsANightEvenWhenHeld() {
		MoonNightState state = on(MoonNight.BLOOD_NEW_MOON, BLOOD_MOON_DAY);
		state.holds.add(new Identifier("embervoid", "vampire"));
		state.elapsedTicks = 60 * 20 - 1;
		assertFalse(MoonNightClock.tick(state, at(BLOOD_MOON_DAY, 15_000), ONE_MINUTE) instanceof Step.End);
		state.elapsedTicks = 60 * 20;
		assertEquals(new Step.End(EndReason.TIMEOUT), MoonNightClock.tick(state, at(BLOOD_MOON_DAY, 15_000), ONE_MINUTE));
	}

	@Test
	void theFallbackAlsoEndsAnUnheldNightBeforeDawn() {
		MoonNightState state = on(MoonNight.FULL_MOON, FULL_MOON_DAY);
		state.elapsedTicks = 2 * 60 * 20;
		assertEquals(new Step.End(EndReason.TIMEOUT), MoonNightClock.tick(state, at(FULL_MOON_DAY, 14_000), config(true, 2)));
	}

	@Test
	void bedsRefuseFromTheEveningOfAnEventDay() {
		MoonNightState state = new MoonNightState();
		assertFalse(MoonNightClock.sleepBlocked(state, at(FULL_MOON_DAY, 11_999), ONE_MINUTE));
		assertTrue(MoonNightClock.sleepBlocked(state, at(FULL_MOON_DAY, 12_000), ONE_MINUTE));
		assertTrue(MoonNightClock.sleepBlocked(state, at(BLOOD_MOON_DAY, 12_500), ONE_MINUTE));
	}

	@Test
	void bedsWorkOnOrdinaryEvenings() {
		assertFalse(MoonNightClock.sleepBlocked(new MoonNightState(), at(25, 12_500), ONE_MINUTE));
		assertFalse(MoonNightClock.sleepBlocked(new MoonNightState(), at(FULL_MOON_DAY, 12_500), config(false, 1)));
	}

	@Test
	void bedsWorkAgainOnceTheNightEnded() {
		MoonNightState state = new MoonNightState();
		state.lastEndedDay = FULL_MOON_DAY;
		assertFalse(MoonNightClock.sleepBlocked(state, at(FULL_MOON_DAY, 22_000), ONE_MINUTE));
	}

	@Test
	void bedsRefuseThroughoutAMoonNight() {
		// A night started by command can run on a day the schedule has no event for.
		assertTrue(MoonNightClock.sleepBlocked(on(MoonNight.FULL_MOON, 5), at(5, 3_000), ONE_MINUTE));
	}
}
