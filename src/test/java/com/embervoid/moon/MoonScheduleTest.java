package com.embervoid.moon;

import com.embervoid.moon.api.MoonNight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoonScheduleTest {
	@Test
	void defaultScheduleSkipsTheFirstCycles() {
		assertEquals(MoonNight.NONE, MoonSchedule.nightOf(0, 3));
		assertEquals(MoonNight.NONE, MoonSchedule.nightOf(4, 3));
		assertEquals(MoonNight.NONE, MoonSchedule.nightOf(8, 3));
		assertEquals(MoonNight.NONE, MoonSchedule.nightOf(16, 3));
	}

	@Test
	void eventsEveryThirdCycle() {
		assertEquals(MoonNight.FULL_MOON, MoonSchedule.nightOf(24, 3));
		assertEquals(MoonNight.BLOOD_NEW_MOON, MoonSchedule.nightOf(28, 3));
		assertEquals(MoonNight.FULL_MOON, MoonSchedule.nightOf(48, 3));
		assertEquals(MoonNight.BLOOD_NEW_MOON, MoonSchedule.nightOf(52, 3));
		assertEquals(MoonNight.FULL_MOON, MoonSchedule.nightOf(72, 3));
		assertEquals(MoonNight.NONE, MoonSchedule.nightOf(56, 3));
	}

	@Test
	void otherPhasesStayVanilla() {
		for (int day = 25; day < 32; day++) {
			if (day != 28) {
				assertEquals(MoonNight.NONE, MoonSchedule.nightOf(day, 3), "day " + day);
			}
		}
	}

	@Test
	void intervalOneMeansEveryCycleButTheFirst() {
		assertEquals(MoonNight.NONE, MoonSchedule.nightOf(0, 1));
		assertEquals(MoonNight.FULL_MOON, MoonSchedule.nightOf(8, 1));
		assertEquals(MoonNight.BLOOD_NEW_MOON, MoonSchedule.nightOf(12, 1));
	}

	@Test
	void nightWindow() {
		assertFalse(MoonSchedule.isNight(24 * 24_000L + 12_999));
		assertTrue(MoonSchedule.isNight(24 * 24_000L + 13_000));
		assertTrue(MoonSchedule.isNight(24 * 24_000L + 22_999));
		assertFalse(MoonSchedule.isNight(24 * 24_000L + 23_000));
		assertEquals(24, MoonSchedule.day(24 * 24_000L + 23_999));
		assertEquals(-1, MoonSchedule.day(-1));
	}

	@Test
	void nextEvent() {
		assertEquals(24, MoonSchedule.nextEventDay(0, 3));
		assertEquals(28, MoonSchedule.nextEventDay(25, 3));
		assertEquals(48, MoonSchedule.nextEventDay(29, 3));
	}

	@Test
	void nextEventDayOnAnEventDayIsThatDay() {
		assertEquals(24, MoonSchedule.nextEventDay(24, 3));
		assertEquals(28, MoonSchedule.nextEventDay(28, 3));
	}

	@Test
	void nextEventDayFromBeforeDayZeroStartsAtZero() {
		assertEquals(24, MoonSchedule.nextEventDay(-100, 3));
	}

	@Test
	void nextEventDayIsAlwaysFoundAndAlwaysAnEvent() {
		for (int interval = 1; interval <= 12; interval++) {
			for (long from = 0; from < 400; from += 7) {
				long next = MoonSchedule.nextEventDay(from, interval);
				assertTrue(next >= from, "interval " + interval + " from " + from);
				assertTrue(MoonSchedule.nightOf(next, interval).isEvent(), "interval " + interval + " from " + from);
				for (long day = from; day < next; day++) {
					assertFalse(MoonSchedule.nightOf(day, interval).isEvent(), "skipped day " + day);
				}
			}
		}
	}

	@Test
	void nonPositiveIntervalOrNegativeDayHasNoEvents() {
		assertEquals(MoonNight.NONE, MoonSchedule.nightOf(24, 0));
		assertEquals(MoonNight.NONE, MoonSchedule.nightOf(24, -3));
		assertEquals(MoonNight.NONE, MoonSchedule.nightOf(-24, 3));
	}

	@Test
	void timeInDayWrapsNegativeTimesIntoTheDay() {
		assertEquals(0, MoonSchedule.timeInDay(0));
		assertEquals(23_999, MoonSchedule.timeInDay(-1));
		assertEquals(13_000, MoonSchedule.timeInDay(5 * MoonSchedule.DAY_TICKS + 13_000));
		assertTrue(MoonSchedule.isNight(-2_000));
		assertEquals(-2, MoonSchedule.day(-MoonSchedule.DAY_TICKS - 1));
	}

	@Test
	void dawnEndsTheNightOfItsOwnDay() {
		long dawn = MoonSchedule.dawnOf(24);
		assertEquals(24, MoonSchedule.day(dawn));
		assertTrue(MoonSchedule.isNight(dawn - 1));
		assertFalse(MoonSchedule.isNight(dawn));
	}
}
