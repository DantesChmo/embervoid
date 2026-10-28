package com.embervoid.nightmob;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveRulesTest {
	@Test
	void noCurseBringsGhosts() {
		assertEquals(new WaveRules.Wave(WaveRules.Kind.GHOSTS, 5), WaveRules.waveFor(0, 10, 5));
	}

	@Test
	void oneZombiePerStack() {
		assertEquals(new WaveRules.Wave(WaveRules.Kind.ZOMBIES, 1), WaveRules.waveFor(1, 10, 5));
		assertEquals(new WaveRules.Wave(WaveRules.Kind.ZOMBIES, 7), WaveRules.waveFor(7, 10, 5));
	}

	@Test
	void zombiesAreCapped() {
		assertEquals(new WaveRules.Wave(WaveRules.Kind.ZOMBIES, 10), WaveRules.waveFor(10, 10, 5));
		assertEquals(new WaveRules.Wave(WaveRules.Kind.ZOMBIES, 10), WaveRules.waveFor(40, 10, 5));
	}

	@Test
	void ringOffsetStaysInTheRing() {
		for (int a = 0; a < 10; a++) {
			for (int d = 0; d <= 10; d++) {
				double[] offset = WaveRules.ringOffset(a / 10.0, Math.min(d / 10.0, 0.999999), 16, 32);
				double distance = Math.hypot(offset[0], offset[1]);
				assertTrue(distance >= 16 - 1e-9 && distance <= 32 + 1e-9, "distance " + distance);
			}
		}
		assertEquals(16, Math.hypot(WaveRules.ringOffset(0.3, 0, 16, 32)[0], WaveRules.ringOffset(0.3, 0, 16, 32)[1]), 1e-9);
	}

	@Test
	void standingYPrefersThePlayersLevel() {
		assertEquals(OptionalInt.of(64), WaveRules.findStandingY(64, 8, y -> true));
		assertEquals(OptionalInt.of(65), WaveRules.findStandingY(64, 8, y -> y == 65 || y == 63));
		assertEquals(OptionalInt.of(56), WaveRules.findStandingY(64, 8, y -> y == 56));
		assertEquals(OptionalInt.empty(), WaveRules.findStandingY(64, 8, y -> y == 73 || y == 55));
	}

	@Test
	void standingYChecksAboveBeforeBelowAtEachDistance() {
		List<Integer> checked = new ArrayList<>();
		WaveRules.findStandingY(10, 2, y -> {
			checked.add(y);
			return false;
		});
		assertEquals(List.of(10, 11, 9, 12, 8), checked);
	}

	@Test
	void zeroRangeOnlyTriesTheStart() {
		assertEquals(OptionalInt.of(5), WaveRules.findStandingY(5, 0, y -> y == 5));
		assertEquals(OptionalInt.empty(), WaveRules.findStandingY(5, 0, y -> y == 6));
	}

	@Test
	void negativeStacksCountAsNoCurse() {
		assertEquals(new WaveRules.Wave(WaveRules.Kind.GHOSTS, 3), WaveRules.waveFor(-1, 10, 3));
	}

	@Test
	void ringDistanceIsSpreadByArea() {
		// Half the rolls land inside the circle that halves the ring's area.
		double halfArea = Math.sqrt((16 * 16 + 32 * 32) / 2.0);
		double[] middle = WaveRules.ringOffset(0, 0.5, 16, 32);
		assertEquals(halfArea, Math.hypot(middle[0], middle[1]), 1e-9);
		double[] outer = WaveRules.ringOffset(0, 1, 16, 32);
		assertEquals(32, Math.hypot(outer[0], outer[1]), 1e-9);
	}

	@Test
	void ringAngleRollTurnsAFullCircle() {
		for (int i = 0; i < 8; i++) {
			double roll = i / 16.0;
			double[] here = WaveRules.ringOffset(roll, 0.3, 10, 20);
			double[] opposite = WaveRules.ringOffset(roll + 0.5, 0.3, 10, 20);
			double[] around = WaveRules.ringOffset(roll + 1, 0.3, 10, 20);
			assertEquals(-here[0], opposite[0], 1e-9);
			assertEquals(-here[1], opposite[1], 1e-9);
			assertEquals(here[0], around[0], 1e-9);
			assertEquals(here[1], around[1], 1e-9);
		}
	}
}
