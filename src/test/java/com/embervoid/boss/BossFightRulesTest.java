package com.embervoid.boss;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossFightRulesTest {
	private static final float[] HALF = {0.5f};
	private static final float[] TWO_STEPS = {0.6f, 0.3f};
	private static final BlockPos FLOOR = new BlockPos(100, 64, -40);

	@Test
	void aBossWithoutThresholdsStaysInItsFirstPhase() {
		assertEquals(0, BossFightRules.phaseFor(0, 0, new float[0], 0.01f));
	}

	@Test
	void theNextPhaseStartsWhenHealthReachesTheThreshold() {
		assertEquals(0, BossFightRules.phaseFor(0, 0, HALF, 0.75f));
		assertEquals(1, BossFightRules.phaseFor(0, 0, HALF, 0.5f));
		assertEquals(1, BossFightRules.phaseFor(0, 0, HALF, 0.2f));
	}

	@Test
	void oneBlowAcrossSeveralThresholdsStartsEveryPhaseItCrossed() {
		assertEquals(2, BossFightRules.phaseFor(0, 0, TWO_STEPS, 0.1f));
		assertEquals(1, BossFightRules.phaseFor(0, 0, TWO_STEPS, 0.45f));
	}

	@Test
	void aPhaseNeverGoesBackWhenTheBossHeals() {
		assertEquals(2, BossFightRules.phaseFor(2, 0, TWO_STEPS, 1.0f));
		assertEquals(1, BossFightRules.phaseFor(1, 0, TWO_STEPS, 0.9f));
	}

	@Test
	void pastTheLastThresholdThePhaseStays() {
		assertEquals(2, BossFightRules.phaseFor(2, 0, TWO_STEPS, 0.0f));
	}

	@Test
	void healthDoesNotEndAScriptedPhase() {
		assertEquals(0, BossFightRules.phaseFor(0, 1, HALF, 0.1f));
		assertEquals(1, BossFightRules.phaseFor(1, 2, HALF, 0.1f));
	}

	@Test
	void thresholdsCountFromTheFirstPhaseAfterTheScriptedOnes() {
		assertEquals(1, BossFightRules.phaseFor(1, 1, HALF, 0.8f));
		assertEquals(2, BossFightRules.phaseFor(1, 1, HALF, 0.5f));
		assertEquals(3, BossFightRules.phaseFor(1, 1, TWO_STEPS, 0.2f));
	}

	@Test
	void insideMeansWithinTheRadiusOfTheCentreColumn() {
		Vec3d centre = new Vec3d(100.5, 65, -39.5);
		assertTrue(BossFightRules.isInside(FLOOR, 10, centre, 0));
		assertTrue(BossFightRules.isInside(FLOOR, 10, centre.add(10, 0, 0), 0));
		assertFalse(BossFightRules.isInside(FLOOR, 10, centre.add(10.1, 0, 0), 0));
		assertTrue(BossFightRules.isInside(FLOOR, 10, centre.add(6, 0, 8), 0));
		assertFalse(BossFightRules.isInside(FLOOR, 10, centre.add(6, 0, 8.1), 0));
	}

	@Test
	void theMarginWidensOrNarrowsTheCircle() {
		Vec3d onTheWall = new Vec3d(100.5, 65, -39.5 + 10);
		Vec3d justOutside = new Vec3d(100.5, 65, -39.5 + 11.5);
		assertFalse(BossFightRules.isInside(FLOOR, 10, justOutside, 0));
		assertTrue(BossFightRules.isInside(FLOOR, 10, justOutside, 2));
		assertFalse(BossFightRules.isInside(FLOOR, 10, onTheWall, -1));
	}

	@Test
	void insideOnlyWithinTheArenasHeightBand() {
		assertTrue(BossFightRules.isInside(FLOOR, 10, new Vec3d(100.5, 62, -39.5), 0));
		assertFalse(BossFightRules.isInside(FLOOR, 10, new Vec3d(100.5, 30, -39.5), 0));
		assertTrue(BossFightRules.isInside(FLOOR, 10, new Vec3d(100.5, 80, -39.5), 0));
		assertFalse(BossFightRules.isInside(FLOOR, 10, new Vec3d(100.5, 120, -39.5), 0));
	}

	@Test
	void ringSpotsLieOnTheCircleAtTheCentresHeight() {
		Vec3d centre = new Vec3d(10.5, 70, -3.5);
		for (int i = 0; i < 5; i++) {
			Vec3d spot = BossFightRules.ringSpot(centre, i, 5, 3);
			assertEquals(70, spot.y, 1e-9);
			assertEquals(3, Math.hypot(spot.x - centre.x, spot.z - centre.z), 1e-9);
		}
	}

	@Test
	void ringSpotsAreSpreadEvenly() {
		Vec3d centre = Vec3d.ZERO;
		Set<Long> seen = new HashSet<>();
		for (int i = 0; i < 4; i++) {
			Vec3d spot = BossFightRules.ringSpot(centre, i, 4, 3);
			Vec3d next = BossFightRules.ringSpot(centre, (i + 1) % 4, 4, 3);
			// Neighbours on a square inscribed in a circle of 3 are 3·√2 apart.
			assertEquals(3 * Math.sqrt(2), spot.distanceTo(next), 1e-9);
			seen.add(Math.round(spot.x * 1000) * 100_000 + Math.round(spot.z * 1000));
		}
		assertEquals(4, seen.size());
	}
}
