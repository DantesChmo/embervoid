package com.embervoid.reach.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossRulesTest {
	@Test
	void aHitBetweenMovesFlinches() {
		assertTrue(BossRules.flinches(true, 5, true, false, 100, 0));
	}

	@Test
	void noFlinchOverAWindupOnADeadBossOrForNothing() {
		assertFalse(BossRules.flinches(true, 5, true, true, 100, 0), "over a windup");
		assertFalse(BossRules.flinches(true, 5, false, false, 100, 0), "dead");
		assertFalse(BossRules.flinches(true, 0, true, false, 100, 0), "no damage");
		assertFalse(BossRules.flinches(false, 5, true, false, 100, 0), "the hit did not land");
	}

	@Test
	void flinchesComeHalfASecondApartAtMost() {
		// Half a second is 10 ticks; a hit within them plays no second flinch.
		assertFalse(BossRules.flinches(true, 5, true, false, 110, 100));
		assertFalse(BossRules.flinches(true, 5, true, false, 105, 100));
		assertTrue(BossRules.flinches(true, 5, true, false, 111, 100));
	}

	@Test
	void theFirstHitOfAFightFlinchesWhateverTheAge() {
		// A boss that never flinched keeps its last flinch far in the past.
		assertTrue(BossRules.flinches(true, 5, true, false, 0, -100));
	}

	@Test
	void aHitOfTheShareOrMoreStaggers() {
		assertTrue(BossRules.staggers(40, 400, 0.1f));
		assertTrue(BossRules.staggers(41, 400, 0.1f));
		assertFalse(BossRules.staggers(39, 400, 0.1f));
	}

	@Test
	void lightAtTheFeetOrTwoBlocksUpCounts() {
		assertTrue(BossRules.lit(8, 0, 8));
		assertTrue(BossRules.lit(0, 8, 8));
		assertTrue(BossRules.lit(15, 15, 8));
		assertFalse(BossRules.lit(7, 7, 8));
		assertFalse(BossRules.lit(0, 0, 8));
	}
}
