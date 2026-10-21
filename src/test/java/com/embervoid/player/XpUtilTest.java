package com.embervoid.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XpUtilTest {
	@Test
	void pointsForLevelMatchesSummedVanillaSteps() {
		int sum = 0;
		for (int level = 0; level <= 200; level++) {
			assertEquals(sum, XpUtil.pointsForLevel(level), "level " + level);
			sum += XpUtil.pointsToNextLevel(level);
		}
	}

	@Test
	void knownVanillaTotals() {
		assertEquals(0, XpUtil.pointsForLevel(0));
		assertEquals(160, XpUtil.pointsForLevel(10));
		assertEquals(352, XpUtil.pointsForLevel(16));
		assertEquals(1395, XpUtil.pointsForLevel(30));
	}

	@Test
	void everyPointRoundTripsThroughLevelAndProgress() {
		int max = XpUtil.pointsForLevel(200);
		for (int points = 0; points <= max; points++) {
			XpUtil.LevelProgress state = XpUtil.fromPoints(points);
			assertTrue(state.progress() >= 0 && state.progress() < 1, "progress at " + points);
			assertEquals(points, XpUtil.totalPoints(state.level(), state.progress()), "points " + points);
		}
	}

	@Test
	void levelStartsHaveNoProgress() {
		for (int level = 0; level <= 200; level++) {
			XpUtil.LevelProgress state = XpUtil.fromPoints(XpUtil.pointsForLevel(level));
			assertEquals(level, state.level());
			assertEquals(0f, state.progress());
		}
	}

	@Test
	void spendingAfterEnchantingLeavesExactTotal() {
		// Level 33 with 40% of the bar, then an enchantment costs 3 levels: vanilla keeps the bar fraction.
		int level = 30;
		float progress = 0.4f;
		int have = XpUtil.totalPoints(level, progress);
		assertEquals(1395 + Math.round(0.4f * 112), have);
		XpUtil.LevelProgress after = XpUtil.fromPoints(have - 95);
		assertEquals(have - 95, XpUtil.totalPoints(after.level(), after.progress()));
	}

	@Test
	void negativeAndHugeInputsAreClamped() {
		assertEquals(new XpUtil.LevelProgress(0, 0f), XpUtil.fromPoints(-5));
		XpUtil.LevelProgress top = XpUtil.fromPoints(Integer.MAX_VALUE);
		assertTrue(top.level() > 20_000);
	}
}
