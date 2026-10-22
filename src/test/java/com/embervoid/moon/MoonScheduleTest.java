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
}
