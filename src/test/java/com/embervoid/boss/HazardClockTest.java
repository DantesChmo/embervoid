package com.embervoid.boss;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HazardClockTest {
	/** Runs a hazard of {@code duration} ticks through; returns the ages at which an every-{@code n} action fired. */
	private static List<Integer> firings(int duration, int n) {
		HazardClock clock = new HazardClock(duration);
		List<Integer> fired = new ArrayList<>();
		do {
			if (clock.every(n)) {
				fired.add(clock.age());
			}
		} while (clock.advance());
		return fired;
	}

	private static int lifetime(int duration) {
		HazardClock clock = new HazardClock(duration);
		int ticks = 1;
		while (clock.advance()) {
			ticks++;
		}
		return ticks;
	}

	@Test
	void aHazardLastsItsDuration() {
		assertEquals(1, lifetime(1));
		assertEquals(60, lifetime(60));
		assertEquals(200, lifetime(200));
	}

	@Test
	void aHazardWithNoDurationStillGetsOneTick() {
		assertEquals(1, lifetime(0));
	}

	@Test
	void anActionFiresAtOnceAndThenEveryInterval() {
		assertEquals(List.of(0, 20, 40), firings(60, 20));
		assertEquals(List.of(0, 20, 40, 60), firings(61, 20));
		assertEquals(List.of(0), firings(5, 20));
	}

	@Test
	void anIntervalOfOneFiresEveryTick() {
		assertEquals(10, firings(10, 1).size());
	}

	@Test
	void anAshStormBlowsOnceASecondFromItsStart() {
		List<Integer> blows = firings(100, AshStormHazard.HIT_INTERVAL);
		assertEquals(0, blows.get(0));
		assertEquals(5, blows.size(), "a 5-second storm");
		for (int i = 1; i < blows.size(); i++) {
			assertEquals(20, blows.get(i) - blows.get(i - 1));
		}
	}
}
