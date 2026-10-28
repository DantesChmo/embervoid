package com.embervoid.reach.boss;

import com.embervoid.reach.boss.LordRules.Hazard;
import com.embervoid.reach.boss.LordRules.Move;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LordRulesTest {
	@Test
	void onTheWallAndComingDownHeCannotBeTouched() {
		assertTrue(LordRules.onWall(0));
		assertFalse(LordRules.touchable(0, false));
		assertFalse(LordRules.touchable(1, true), "while he comes down");
		assertTrue(LordRules.touchable(1, false));
		assertTrue(LordRules.touchable(2, false));
	}

	@Test
	void theArmorShattersInTheLastPhase() {
		assertFalse(LordRules.armorShattered(0));
		assertFalse(LordRules.armorShattered(1));
		assertTrue(LordRules.armorShattered(2));
	}

	@Test
	void shatteredArmorTakesAQuarterMore() {
		assertEquals(10, LordRules.damageTaken(10, false, false), 1e-6);
		assertEquals(12.5f, LordRules.damageTaken(10, true, false), 1e-6);
	}

	@Test
	void inTheDarkOutsideLightHeTakesHalf() {
		assertEquals(5, LordRules.damageTaken(10, false, true), 1e-6);
		// Both: 10 × 1.25 × 0.5.
		assertEquals(6.25f, LordRules.damageTaken(10, true, true), 1e-6);
	}

	@Test
	void theHazardsComeInTurnStormFogDarkness() {
		assertEquals(Hazard.FOG, Hazard.ASH_STORM.next());
		assertEquals(Hazard.DARKNESS, Hazard.FOG.next());
		assertEquals(Hazard.ASH_STORM, Hazard.DARKNESS.next());
		Set<Hazard> seen = new HashSet<>();
		Hazard hazard = Hazard.ASH_STORM;
		for (int i = 0; i < 3; i++) {
			seen.add(hazard);
			hazard = hazard.next();
		}
		assertEquals(3, seen.size());
	}

	@Test
	void hazardNumbersFollowTheOrderOfTheJavadoc() {
		assertEquals(Hazard.ASH_STORM, Hazard.of(0));
		assertEquals(Hazard.FOG, Hazard.of(1));
		assertEquals(Hazard.DARKNESS, Hazard.of(2));
	}

	@Test
	void closeHeSwingsFartherHeLungesFarHeWalks() {
		assertEquals(Move.COMBO, LordRules.pick(3 * 3));
		assertEquals(Move.LUNGE, LordRules.pick(10 * 10));
		assertNull(LordRules.pick(20 * 20));
	}

	@Test
	void withTheArmorGoneEveryMoveComesSooner() {
		for (Move move : Move.values()) {
			int whole = LordRules.cooldown(move, false);
			int shattered = LordRules.cooldown(move, true);
			assertTrue(shattered > 0 && shattered < whole, move + ": " + whole + " → " + shattered);
		}
	}
}
