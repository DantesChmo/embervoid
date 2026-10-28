package com.embervoid.reach.boss;

import com.embervoid.reach.boss.KeeperRules.Move;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeeperRulesTest {
	private static final double CLOSE = 3 * 3;
	private static final double MID = 10 * 10;
	private static final double FAR = 25 * 25;

	@Test
	void theLanternGoesWhenTheFirstPhaseEnds() {
		assertTrue(KeeperRules.hasLantern(0));
		assertFalse(KeeperRules.hasLantern(1));
		assertFalse(KeeperRules.hasLantern(2));
	}

	@Test
	void inTheDarkPhasesItTakesHalfOutsideLight() {
		assertEquals(1f, KeeperRules.damageFactor(0, false), "P1 is not dark");
		assertEquals(0.5f, KeeperRules.damageFactor(1, false));
		assertEquals(0.5f, KeeperRules.damageFactor(2, false));
		assertEquals(1f, KeeperRules.damageFactor(1, true));
		assertEquals(1f, KeeperRules.damageFactor(2, true));
	}

	@Test
	void aCrowdAlwaysGetsTheSweepAndTheRollIsNotAsked() {
		AtomicInteger rolls = new AtomicInteger();
		assertEquals(Move.SWEEP, KeeperRules.pick(CLOSE, 0, () -> 2, () -> rolls.incrementAndGet() < 0));
		assertEquals(0, rolls.get());
	}

	@Test
	void aLoneFighterCloseGetsTheComboUnlessTheRollSaysSweep() {
		assertEquals(Move.COMBO, KeeperRules.pick(CLOSE, 0, () -> 1, () -> false));
		assertEquals(Move.SWEEP, KeeperRules.pick(CLOSE, 0, () -> 1, () -> true));
	}

	@Test
	void atRangeItThrowsTheLanternThenThrustsOnceItIsGone() {
		assertEquals(Move.LANTERN_THROW, KeeperRules.pick(MID, 0, () -> 0, () -> false));
		assertEquals(Move.THRUST, KeeperRules.pick(MID, 1, () -> 0, () -> false));
		assertEquals(Move.THRUST, KeeperRules.pick(MID, 2, () -> 0, () -> false));
	}

	@Test
	void beyondTheHalberdNoCrowdIsCountedAndFarAwayItWalks() {
		AtomicInteger asked = new AtomicInteger();
		assertNull(KeeperRules.pick(FAR, 1, asked::incrementAndGet, () -> asked.incrementAndGet() > 0));
		assertEquals(Move.THRUST, KeeperRules.pick(MID, 1, asked::incrementAndGet, () -> asked.incrementAndGet() > 0));
		assertEquals(0, asked.get());
	}

	@Test
	void fromTheSecondPhaseEveryMoveComesSooner() {
		for (Move move : Move.values()) {
			int early = KeeperRules.cooldown(move, 0);
			int late = KeeperRules.cooldown(move, 1);
			assertTrue(late > 0 && late < early, move + ": " + early + " → " + late);
			assertEquals(late, KeeperRules.cooldown(move, 2), move + " in P3");
		}
	}
}
