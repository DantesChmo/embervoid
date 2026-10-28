package com.embervoid.reach.gear;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RampStateTest {
	/** 1.5 s between two hits at most. */
	private static final int WINDOW = 30;
	private static final UUID GOLEM = new UUID(0, 1);
	private static final UUID HUSK = new UUID(0, 2);

	@Test
	void fullChargeHitsInARowBuildTheRamp() {
		RampState state = null;
		for (int hit = 1; hit <= 5; hit++) {
			state = RampState.onHit(state, hit * 20L, 1, GOLEM, WINDOW);
			assertEquals(hit, state.hitsOn(GOLEM, hit * 20L, WINDOW));
		}
	}

	@Test
	void aLowChargeHitNeitherBuildsNorBreaksIt() {
		assertNull(RampState.onHit(null, 0, 0.5f, GOLEM, WINDOW));
		RampState two = RampState.onHit(RampState.onHit(null, 0, 1, GOLEM, WINDOW), 10, 1, GOLEM, WINDOW);
		assertSame(two, RampState.onHit(two, 15, 0.5f, GOLEM, WINDOW));
		// Not even when it lands on another target.
		assertSame(two, RampState.onHit(two, 15, 0.5f, HUSK, WINDOW));
		assertEquals(2, two.hitsOn(GOLEM, 20, WINDOW));
	}

	@Test
	void anotherTargetStartsItOver() {
		RampState onGolem = RampState.onHit(RampState.onHit(null, 0, 1, GOLEM, WINDOW), 10, 1, GOLEM, WINDOW);
		assertEquals(0, onGolem.hitsOn(HUSK, 10, WINDOW));
		RampState onHusk = RampState.onHit(onGolem, 20, 1, HUSK, WINDOW);
		assertEquals(1, onHusk.hitsOn(HUSK, 20, WINDOW));
		assertEquals(0, onHusk.hitsOn(GOLEM, 20, WINDOW));
	}

	@Test
	void theRampLastsTheWindowAndNoLonger() {
		RampState state = RampState.onHit(null, 100, 1, GOLEM, WINDOW);
		assertEquals(1, state.hitsOn(GOLEM, 100 + WINDOW, WINDOW));
		assertEquals(0, state.hitsOn(GOLEM, 101 + WINDOW, WINDOW));
		// A hit after the window is the first of a new ramp.
		assertEquals(1, RampState.onHit(state, 101 + WINDOW, 1, GOLEM, WINDOW).hits());
	}

	@Test
	void aHitWithinTheWindowKeepsTheCountGoing() {
		RampState state = RampState.onHit(null, 0, 1, GOLEM, WINDOW);
		state = RampState.onHit(state, WINDOW, 1, GOLEM, WINDOW);
		state = RampState.onHit(state, 2 * WINDOW, 1, GOLEM, WINDOW);
		assertEquals(3, state.hitsOn(GOLEM, 2 * WINDOW, WINDOW));
	}
}
