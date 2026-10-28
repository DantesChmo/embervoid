package com.embervoid.reach.boss;

import com.embervoid.reach.boss.ColossusRules.Move;
import com.embervoid.reach.boss.ColossusRules.Zone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColossusRulesTest {
	/** The zone of a hit from {@code degrees} off its facing (facing +z), {@code distance} away. */
	private static Zone hitFrom(double degrees, double distance) {
		double angle = Math.toRadians(degrees);
		return ColossusRules.zone(0, 1, Math.sin(angle) * distance, Math.cos(angle) * distance);
	}

	@Test
	void theFrontSpansSixtyDegreesEachWay() {
		for (double degrees : new double[]{0, 30, -30, 59, -59}) {
			assertEquals(Zone.FRONT, hitFrom(degrees, 3), degrees + "°");
		}
		assertEquals(Zone.SIDE, hitFrom(61, 3));
		assertEquals(Zone.SIDE, hitFrom(-61, 3));
	}

	@Test
	void theBackLiesBeyondAHundredAndTwentyDegrees() {
		for (double degrees : new double[]{180, 150, -150, 121, -121}) {
			assertEquals(Zone.BACK, hitFrom(degrees, 3), degrees + "°");
		}
		assertEquals(Zone.SIDE, hitFrom(119, 3));
		assertEquals(Zone.SIDE, hitFrom(90, 3));
		assertEquals(Zone.SIDE, hitFrom(-90, 3));
	}

	@Test
	void theZoneDependsOnDirectionNotDistance() {
		for (double distance : new double[]{0.5, 3, 40}) {
			assertEquals(Zone.FRONT, hitFrom(10, distance));
			assertEquals(Zone.BACK, hitFrom(170, distance));
		}
	}

	@Test
	void theZoneTurnsWithItsFacing() {
		// Facing +x: a hit from +x is the front, from −x the back.
		assertEquals(Zone.FRONT, ColossusRules.zone(1, 0, 4, 0.5));
		assertEquals(Zone.BACK, ColossusRules.zone(1, 0, -4, 0.5));
		assertEquals(Zone.SIDE, ColossusRules.zone(1, 0, 0, 4));
	}

	@Test
	void aHitFromRightOnTopCountsAsTheSide() {
		assertEquals(Zone.SIDE, ColossusRules.zone(0, 1, 0, 0));
	}

	@Test
	void frontHalvesBackAddsHalfAndTheBareCoreDoubles() {
		assertEquals(0.5f, ColossusRules.damageFactor(Zone.FRONT, false));
		assertEquals(0.5f, ColossusRules.damageFactor(Zone.FRONT, true));
		assertEquals(1f, ColossusRules.damageFactor(Zone.SIDE, false));
		assertEquals(1f, ColossusRules.damageFactor(Zone.SIDE, true));
		assertEquals(1.5f, ColossusRules.damageFactor(Zone.BACK, false));
		assertEquals(2f, ColossusRules.damageFactor(Zone.BACK, true));
	}

	@Test
	void aPillarWithinReachIsSmashedWhateverTheDistance() {
		for (double distance : new double[]{2, 6, 15, 30}) {
			assertEquals(Move.SMASH, ColossusRules.pick(distance * distance, true, true), "at " + distance);
		}
	}

	@Test
	void closeItHitsAtMidRangeItLeapsOrThrowsFarItThrows() {
		assertEquals(Move.MELEE, ColossusRules.pick(3 * 3, false, false));
		assertEquals(Move.RANGED, ColossusRules.pick(15 * 15, false, false));
		assertEquals(Move.FAR, ColossusRules.pick(30 * 30, false, false));
		// Between the fist's reach and the leap's: it walks in.
		assertNull(ColossusRules.pick(7 * 7, false, false));
	}

	@Test
	void aTargetBehindCoverOutOfReachIsWalkedUpTo() {
		assertNull(ColossusRules.pick(30 * 30, true, false));
		assertNull(ColossusRules.pick(7 * 7, true, false));
		// Cover does not save a target standing in the fist's reach or the leap's band.
		assertEquals(Move.MELEE, ColossusRules.pick(3 * 3, true, false));
		assertEquals(Move.RANGED, ColossusRules.pick(15 * 15, true, false));
	}

	@Test
	void everyMoveComesSoonerOnceTheStoneIsShed() {
		for (Move move : Move.values()) {
			int p1 = ColossusRules.cooldown(move, false);
			int p2 = ColossusRules.cooldown(move, true);
			assertTrue(p1 > 0 && p2 > 0 && p2 < p1, move + ": " + p1 + " → " + p2);
		}
	}
}
