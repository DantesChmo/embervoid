package com.embervoid.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShakeCurveTest {
	@Test
	void fullStrengthNearTheSourceNoneAtTheRadius() {
		assertEquals(1, ShakeCurve.falloff(0, 20));
		assertEquals(1, ShakeCurve.falloff(20 * ShakeCurve.INNER, 20));
		assertEquals(0, ShakeCurve.falloff(20, 20));
		assertEquals(0, ShakeCurve.falloff(35, 20));
		assertEquals(0, ShakeCurve.falloff(0, 0));
	}

	@Test
	void falloffOnlyFallsWithDistance() {
		float last = 1;
		for (double d = 0; d <= 24; d += 0.5) {
			float f = ShakeCurve.falloff(d, 24);
			assertTrue(f <= last && f >= 0, d + " blocks: " + f + " after " + last);
			last = f;
		}
		// Eased, not linear: halfway out of the inner circle leaves a quarter.
		assertEquals(0.25, ShakeCurve.falloff(4.8 + (24 - 4.8) / 2, 24), 1e-3);
	}

	@Test
	void envelopeRisesAtOnceAndDiesAway() {
		assertEquals(0, ShakeCurve.envelope(0, 20));
		assertEquals(0, ShakeCurve.envelope(-1, 20));
		float peak = ShakeCurve.envelope(ShakeCurve.RISE_TICKS, 20);
		assertTrue(peak > 0.85f, "peak " + peak);
		float last = peak;
		for (float age = ShakeCurve.RISE_TICKS; age < 20; age += 0.5f) {
			float e = ShakeCurve.envelope(age, 20);
			assertTrue(e <= last, age + ": " + e + " after " + last);
			last = e;
		}
		assertEquals(0, ShakeCurve.envelope(20, 20));
		assertEquals(0, ShakeCurve.envelope(30, 20));
		assertEquals(0, ShakeCurve.envelope(5, 0));
	}

	@Test
	void waveStaysInRangeAndMoves() {
		float min = 1;
		float max = -1;
		for (float t = 0; t < 40; t += 0.1f) {
			for (int axis = 0; axis < 2; axis++) {
				float w = ShakeCurve.wave(t, axis);
				assertTrue(w >= -1 && w <= 1, "wave " + w + " at " + t);
				min = Math.min(min, w);
				max = Math.max(max, w);
			}
		}
		assertTrue(max - min > 1.2f, "barely moves: " + min + " to " + max);
		// Yaw and pitch do not move together.
		assertTrue(Math.abs(ShakeCurve.wave(3.3f, 0) - ShakeCurve.wave(3.3f, 1)) > 1e-3);
	}

	@Test
	void shakesAddUpToACap() {
		assertEquals(0.7f, ShakeCurve.combine(0.7f));
		assertEquals(ShakeCurve.MAX_STRENGTH, ShakeCurve.combine(5));
		assertEquals(0, ShakeCurve.combine(-1));
		for (float t = 0; t < 10; t += 0.25f) {
			float[] offsets = ShakeCurve.offsets(10, t);
			assertTrue(Math.abs(offsets[0]) <= ShakeCurve.MAX_STRENGTH * ShakeCurve.MAX_DEGREES + 1e-4, "yaw " + offsets[0]);
			assertTrue(Math.abs(offsets[1]) <= ShakeCurve.MAX_STRENGTH * ShakeCurve.MAX_DEGREES + 1e-4, "pitch " + offsets[1]);
		}
		float[] none = ShakeCurve.offsets(0, 3);
		assertEquals(0, none[0], 0);
		assertEquals(0, none[1], 0);
	}

	@Test
	void strongerShakesLastLonger() {
		assertTrue(CombatFx.shakeTicks(1) > CombatFx.shakeTicks(0.3f));
		assertEquals(CombatFx.shakeTicks(1.5f), CombatFx.shakeTicks(9));
	}
}
