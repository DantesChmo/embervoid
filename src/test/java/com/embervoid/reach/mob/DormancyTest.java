package com.embervoid.reach.mob;

import com.embervoid.reach.mob.Dormancy.Change;
import com.embervoid.reach.mob.Dormancy.Stage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DormancyTest {
	/** 1 s and 15 s in ticks. */
	private static final int ONE_SECOND = 20;
	private static final int FIFTEEN_SECONDS = 300;

	@Test
	void aDormantHollowWakesOnlyWhenAPlayerComesClose() {
		Dormancy dormancy = new Dormancy();
		assertEquals(Change.NONE, dormancy.tick(Stage.DORMANT, () -> false, false, () -> true));
		assertEquals(Change.WAKE, dormancy.tick(Stage.DORMANT, () -> true, false, () -> true));
	}

	@Test
	void wakingTakesASecond() {
		Dormancy dormancy = new Dormancy();
		dormancy.woke();
		for (int tick = 1; tick < ONE_SECOND; tick++) {
			assertEquals(Change.NONE, dormancy.tick(Stage.AWAKENING, () -> true, true, () -> true), "at " + tick);
		}
		assertEquals(Change.AWAKE, dormancy.tick(Stage.AWAKENING, () -> true, true, () -> true));
	}

	@Test
	void awakeWithNothingToFightItSettlesAfterFifteenSecondsAtHome() {
		Dormancy dormancy = new Dormancy();
		for (int tick = 1; tick < FIFTEEN_SECONDS; tick++) {
			assertEquals(Change.NONE, dormancy.tick(Stage.AWAKE, () -> true, false, () -> true), "at " + tick);
		}
		assertEquals(Change.SETTLE, dormancy.tick(Stage.AWAKE, () -> true, false, () -> true));
	}

	@Test
	void awayFromHomeItKeepsWalkingBackAndSettlesOnArrival() {
		Dormancy dormancy = new Dormancy();
		for (int tick = 0; tick < FIFTEEN_SECONDS * 2; tick++) {
			assertEquals(Change.NONE, dormancy.tick(Stage.AWAKE, () -> true, false, () -> false));
		}
		assertEquals(Change.SETTLE, dormancy.tick(Stage.AWAKE, () -> true, false, () -> true));
	}

	@Test
	void aFightStartsTheIdleCountOver() {
		Dormancy dormancy = new Dormancy();
		for (int tick = 0; tick < FIFTEEN_SECONDS - 1; tick++) {
			dormancy.tick(Stage.AWAKE, () -> true, false, () -> true);
		}
		dormancy.tick(Stage.AWAKE, () -> true, true, () -> true);
		assertEquals(Change.NONE, dormancy.tick(Stage.AWAKE, () -> true, false, () -> true));
	}

	@Test
	void goingDormantStartsTheIdleCountOver() {
		Dormancy dormancy = new Dormancy();
		for (int tick = 0; tick < FIFTEEN_SECONDS - 1; tick++) {
			dormancy.tick(Stage.AWAKE, () -> true, false, () -> true);
		}
		dormancy.slept();
		assertEquals(Change.NONE, dormancy.tick(Stage.AWAKE, () -> true, false, () -> true));
	}

	@Test
	void playersAreLookedForOnlyWhileDormantAndHomeOnlyAfterIdling() {
		Dormancy dormancy = new Dormancy();
		AtomicInteger playerLookups = new AtomicInteger();
		AtomicInteger homeLookups = new AtomicInteger();
		dormancy.woke();
		dormancy.tick(Stage.AWAKENING, () -> playerLookups.incrementAndGet() > 0, false, () -> homeLookups.incrementAndGet() > 0);
		for (int tick = 0; tick < 10; tick++) {
			dormancy.tick(Stage.AWAKE, () -> playerLookups.incrementAndGet() > 0, false, () -> homeLookups.incrementAndGet() > 0);
		}
		assertEquals(0, playerLookups.get());
		assertEquals(0, homeLookups.get());
	}
}
