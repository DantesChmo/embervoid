package com.embervoid.moon;

import com.embervoid.moon.CurseRules.Attacker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Curse of the Night (CORE §3.4, {@code CurseManager}): a death to a hostile mob, its projectile or an arena boss adds
 * a stack; deaths with no attacking mob and kills by players do not; a full moon's end voids all stacks.
 */
class CurseRulesTest {
	@Test
	void aHostileKillerCurses() {
		assertTrue(CurseRules.countsForCurse(Attacker.HOSTILE));
	}

	@Test
	void deathsWithoutAnAttackerDoNot() {
		assertFalse(CurseRules.countsForCurse(Attacker.NONE));
	}

	@Test
	void killsByPlayersDoNot() {
		assertFalse(CurseRules.countsForCurse(Attacker.PLAYER));
	}

	@Test
	void killsByOtherCreaturesDoNot() {
		assertFalse(CurseRules.countsForCurse(Attacker.OTHER));
	}

	@Test
	void stacksOfThisFullMoonCycleCount() {
		assertEquals(4, CurseRules.stacks(4, 2, 2));
	}

	@Test
	void stacksFromBeforeAFullMoonEndedAreVoid() {
		assertEquals(0, CurseRules.stacks(4, 1, 2));
		assertEquals(0, CurseRules.stacks(9, 0, 5));
	}
}
