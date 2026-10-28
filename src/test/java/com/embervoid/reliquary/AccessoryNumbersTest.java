package com.embervoid.reliquary;

import com.embervoid.stats.Stat;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessoryNumbersTest {
	/** Vanilla lets 3 blocks of fall through without damage. */
	private static final float VANILLA_SAFE = 3;

	@Test
	void ashFeathersMakeFallsUpToTheirDistanceFree() {
		for (float safeFall : new float[] {3, 4, 8, 20.5f}) {
			float ignored = AccessoryNumbers.ignoredFallDistance(safeFall);
			// What is left of a fall of exactly safeFall is what vanilla already lets through.
			assertEquals(VANILLA_SAFE, safeFall - ignored, 1e-5, "safe fall " + safeFall);
		}
	}

	@Test
	void ashFeathersTakeTheSameOffEveryLongerFall() {
		// Safe fall 8: a 12-block fall hurts like a 7-block one.
		assertEquals(5, AccessoryNumbers.ignoredFallDistance(8), 1e-5);
	}

	@Test
	void ashFeathersNeverAddFallDistance() {
		assertEquals(0, AccessoryNumbers.ignoredFallDistance(2));
	}

	@Test
	void delversRingMinesLikeHaste() {
		// Vanilla Haste: +20 % mining speed per level.
		assertEquals(1.0f, AccessoryNumbers.hasteMiningMultiplier(0), 1e-6);
		assertEquals(1.2f, AccessoryNumbers.hasteMiningMultiplier(1), 1e-6);
		assertEquals(1.4f, AccessoryNumbers.hasteMiningMultiplier(2), 1e-6);
	}

	@Test
	void aOneStatArtifactAddsToThatStatOnly() {
		int[] bonus = new int[Stat.COUNT];
		AccessoryNumbers.addPoints(bonus, EnumSet.of(Stat.STRENGTH), 3);
		int[] expected = new int[Stat.COUNT];
		expected[Stat.STRENGTH.ordinal()] = 3;
		assertArrayEquals(expected, bonus);
	}

	@Test
	void theSealAddsToEveryStat() {
		int[] bonus = new int[Stat.COUNT];
		AccessoryNumbers.addPoints(bonus, EnumSet.allOf(Stat.class), 1);
		for (int value : bonus) {
			assertEquals(1, value);
		}
	}

	@Test
	void pointsOfSeveralArtifactsAddUp() {
		int[] bonus = new int[Stat.COUNT];
		AccessoryNumbers.addPoints(bonus, Set.of(Stat.VITALITY), 3);
		AccessoryNumbers.addPoints(bonus, EnumSet.allOf(Stat.class), 1);
		assertEquals(4, bonus[Stat.VITALITY.ordinal()]);
		assertEquals(1, bonus[Stat.FORTUNE.ordinal()]);
	}
}
