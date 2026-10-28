package com.embervoid.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossScalingTest {
	@Test
	void healthGrowsByHalfPerExtraFighter() {
		assertEquals(1.0, BossScaling.healthMultiplier(1, 0.5), 1e-9);
		assertEquals(1.5, BossScaling.healthMultiplier(2, 0.5), 1e-9);
		assertEquals(2.5, BossScaling.healthMultiplier(4, 0.5), 1e-9);
		assertEquals(1.0, BossScaling.healthMultiplier(0, 0.5), 1e-9);
	}

	@Test
	void oneAddPerExtraFighter() {
		assertEquals(0, BossScaling.adds(1, 1));
		assertEquals(3, BossScaling.adds(4, 1));
		assertEquals(0, BossScaling.adds(0, 1));
	}

	@Test
	void switchTimeStaysInBounds() {
		assertEquals(160, BossScaling.switchTicks(8, 10, 0));
		assertEquals(200, BossScaling.switchTicks(8, 10, 1));
		int mid = BossScaling.switchTicks(8, 10, 0.5);
		assertTrue(mid > 160 && mid < 200);
	}

	@Test
	void switchTimeIsAtLeastOneTick() {
		assertEquals(1, BossScaling.switchTicks(0, 0, 0.5));
	}

	@Test
	void switchTimeGrowsWithTheRoll() {
		int previous = 0;
		for (double roll = 0; roll < 1; roll += 0.1) {
			int ticks = BossScaling.switchTicks(8, 10, roll);
			assertTrue(ticks >= previous);
			previous = ticks;
		}
	}

	@Test
	void noExtraHealthOrAddsWhenScalingIsOff() {
		assertEquals(1.0, BossScaling.healthMultiplier(4, 0), 1e-9);
		assertEquals(0, BossScaling.adds(4, 0));
	}

	@Test
	void addsScaleWithTheirPerFighterCount() {
		assertEquals(6, BossScaling.adds(4, 2));
	}
}
