package com.embervoid.sanctum.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallenSisterRulesTest {
	/** The Sanctum arena's braziers (PLAN 16.3). */
	private static final int SIX = 6;

	@Test
	void noBrazierBurnsAtFullHealth() {
		assertEquals(0, FallenSisterRules.litBraziers(1.0f, SIX));
	}

	@Test
	void oneBrazierMoreForEachSeventhOfHealthLost() {
		for (int lost = 0; lost <= SIX; lost++) {
			float atTheMark = 1 - lost / 7f;
			assertEquals(lost, FallenSisterRules.litBraziers(atTheMark, SIX), "at " + lost + " sevenths lost");
			if (lost < SIX) {
				float justBefore = atTheMark - 0.5f / 7f;
				assertEquals(lost, FallenSisterRules.litBraziers(justBefore, SIX), "halfway to the next seventh");
			}
		}
	}

	@Test
	void halfHealthLightsHalfTheSix() {
		assertEquals(3, FallenSisterRules.litBraziers(0.5f, SIX));
	}

	@Test
	void allBurnNearTheEndAndNeverMoreThanThereAre() {
		assertEquals(SIX, FallenSisterRules.litBraziers(0.1f, SIX));
		assertEquals(SIX, FallenSisterRules.litBraziers(0.0f, SIX));
		assertEquals(SIX, FallenSisterRules.litBraziers(-0.5f, SIX));
	}

	@Test
	void overhealingLightsNone() {
		assertEquals(0, FallenSisterRules.litBraziers(1.5f, SIX));
	}

	@Test
	void moreBurnAsHealthFalls() {
		for (int braziers : new int[]{1, 4, SIX, 9}) {
			int previous = 0;
			for (int step = 1000; step >= 0; step--) {
				int lit = FallenSisterRules.litBraziers(step / 1000f, braziers);
				assertTrue(lit >= previous, "never fewer with less health");
				assertTrue(lit - previous <= 1, "one at a time");
				previous = lit;
			}
			assertEquals(braziers, previous);
		}
	}

	@Test
	void anArenaWithoutBraziersLightsNone() {
		assertEquals(0, FallenSisterRules.litBraziers(0.0f, 0));
	}
}
