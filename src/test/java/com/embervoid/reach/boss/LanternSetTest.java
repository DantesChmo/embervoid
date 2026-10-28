package com.embervoid.reach.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanternSetTest {
	/** 10 s and 2 s in ticks. */
	private static final int TEN_SECONDS = 200;
	private static final int TWO_SECONDS = 40;

	@Test
	void sevenHangWhileRootedAndSixOnceItWalks() {
		assertEquals(7, LanternSet.size(true));
		assertEquals(6, LanternSet.size(false));
	}

	@Test
	void onlyTheLastLanternOfTheSetStunsTheTree() {
		LanternSet set = new LanternSet();
		for (int left = 6; left > 0; left--) {
			assertEquals(LanternSet.Break.FLINCH, set.onBroken(left, true));
			assertFalse(set.isStunned(), left + " left");
		}
		assertEquals(LanternSet.Break.STUN, set.onBroken(0, true));
		assertTrue(set.isStunned());
	}

	@Test
	void theStunLastsTenSecondsAndTheFogReturnsWithItsEnd() {
		LanternSet set = new LanternSet();
		set.onBroken(0, true);
		for (int tick = 1; tick < TEN_SECONDS; tick++) {
			LanternSet.Tick step = set.tick(0);
			assertTrue(set.isStunned(), "stun over after " + tick);
			assertFalse(step.stunEnded() || step.fogReturns() || step.rehang(), "at " + tick);
		}
		LanternSet.Tick last = set.tick(0);
		assertTrue(last.stunEnded() && last.fogReturns());
		assertFalse(set.isStunned());
	}

	@Test
	void theWholeSetHangsAgainTwoSecondsAfterTheStun() {
		LanternSet set = new LanternSet();
		set.onBroken(0, true);
		for (int tick = 0; tick < TEN_SECONDS; tick++) {
			set.tick(0);
		}
		int ticksAfterStun = 0;
		LanternSet.Tick step;
		do {
			step = set.tick(0);
			ticksAfterStun++;
		} while (!step.rehang() && ticksAfterStun < 1000);
		assertEquals(TWO_SECONDS, ticksAfterStun);
	}

	@Test
	void hangingLanternsHoldTheRehangBack() {
		LanternSet set = new LanternSet();
		for (int tick = 0; tick < 1000; tick++) {
			assertFalse(set.tick(3).rehang(), "at " + tick);
		}
		// The last one gone outside a stun: the set waits its full delay again.
		int ticks = 1;
		while (!set.tick(0).rehang()) {
			ticks++;
		}
		assertEquals(TWO_SECONDS, ticks);
	}

	@Test
	void withoutAFightTheFogIsNotTouched() {
		LanternSet set = new LanternSet();
		set.onBroken(0, false);
		boolean fogReturned = false;
		for (int tick = 0; tick < TEN_SECONDS + TWO_SECONDS; tick++) {
			fogReturned |= set.tick(0).fogReturns();
		}
		assertFalse(fogReturned);
	}

	@Test
	void uprootingEndsTheStunAtOnceButTheFogLiftRunsOut() {
		LanternSet set = new LanternSet();
		set.onBroken(0, true);
		set.tick(0);
		set.endStun();
		assertFalse(set.isStunned());
		int fogBackAt = -1;
		for (int tick = 2; tick <= TEN_SECONDS; tick++) {
			LanternSet.Tick step = set.tick(1);
			assertFalse(step.stunEnded(), "a stun ended twice");
			if (step.fogReturns()) {
				fogBackAt = tick;
			}
		}
		assertEquals(TEN_SECONDS, fogBackAt);
	}

	@Test
	void aSecondStunNeedsTheWholeNewSetBroken() {
		LanternSet set = new LanternSet();
		set.onBroken(0, true);
		for (int tick = 0; tick < TEN_SECONDS + TWO_SECONDS; tick++) {
			set.tick(0);
		}
		assertEquals(LanternSet.Break.FLINCH, set.onBroken(LanternSet.size(true) - 1, true));
		assertFalse(set.isStunned());
		assertEquals(LanternSet.Break.STUN, set.onBroken(0, true));
	}
}
