package com.embervoid.reach.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnRulesTest {
	@Test
	void aTorchStopsSpawnsAroundIt() {
		// Vanilla hostiles spawn at block light 0 only; realm mobs in dim light too, but not by a torch (14).
		assertTrue(SpawnRules.darkEnough(0));
		assertTrue(SpawnRules.darkEnough(5));
		assertFalse(SpawnRules.darkEnough(14));
		assertFalse(SpawnRules.darkEnough(15));
	}

	@Test
	void theLightLimitIsOneStep() {
		boolean wasDark = true;
		int flips = 0;
		for (int light = 0; light <= 15; light++) {
			boolean dark = SpawnRules.darkEnough(light);
			if (dark != wasDark) {
				flips++;
				assertTrue(wasDark, "dark again at " + light);
			}
			wasDark = dark;
		}
		assertTrue(flips == 1);
	}

	@Test
	void naturalSpawnsUnderCoverNeedTheSkyHidden() {
		assertTrue(SpawnRules.underCover(true, false));
		assertFalse(SpawnRules.underCover(true, true));
	}

	@Test
	void spawnersAndEggsIgnoreCover() {
		assertTrue(SpawnRules.underCover(false, true));
		assertTrue(SpawnRules.underCover(false, false));
	}
}
