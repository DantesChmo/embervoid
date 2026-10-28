package com.embervoid.reach.gear;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhipArcTest {
	private static final double EPS = 1e-6;
	private static final double REACH = 6;
	/** Half a zombie's (or a husk's) width. */
	private static final double HALF_MOB = 0.3;
	private final WhipArc arc = new WhipArc(REACH);

	private static void assertVec(Vec3d expected, Vec3d actual, double eps) {
		assertTrue(expected.distanceTo(actual) < eps, "expected " + expected + ", got " + actual);
	}

	@Test
	void runsFromTheHandToTheCrosshairAtReach() {
		assertVec(WhipArc.HAND, arc.curve(0), EPS);
		assertVec(new Vec3d(0, 0, REACH), arc.curve(1), EPS);
		assertVec(new Vec3d(0, 0, REACH), arc.point(arc.length()), EPS);
		assertTrue(arc.length() > REACH - WhipArc.HAND.z && arc.length() < REACH + 0.5, "length " + arc.length());
	}

	@Test
	void bowsUpAndRightWhereTheSwingCameFrom() {
		Vec3d middle = arc.point(arc.length() / 2);
		assertTrue(middle.x > 0 && middle.y > 0, "middle " + middle);
	}

	@Test
	void samplesStepFromTheHandToTheTip() {
		List<WhipArc.Sample> samples = arc.samples();
		assertEquals(0, samples.get(0).along(), EPS);
		assertVec(WhipArc.HAND, samples.get(0).local(), EPS);
		WhipArc.Sample last = samples.get(samples.size() - 1);
		assertEquals(arc.length(), last.along(), EPS);
		assertVec(new Vec3d(0, 0, REACH), last.local(), EPS);
		for (int i = 1; i < samples.size(); i++) {
			double step = samples.get(i).along() - samples.get(i - 1).along();
			assertTrue(step > 0 && step <= WhipArc.STEP + EPS, "step " + step + " at " + i);
			double gap = samples.get(i).local().distanceTo(samples.get(i - 1).local());
			// Closer than twice the hit radius: no mob slips between two samples.
			assertTrue(gap < 2 * WhipArc.RADIUS, "gap " + gap + " at " + i);
		}
	}

	/** A mob on the crosshair anywhere from a block out to the reach is on the lash. */
	@Test
	void theCrosshairLineIsOnTheLash() {
		for (double z = 1; z <= REACH; z += 0.1) {
			Vec3d aim = new Vec3d(0, 0, z);
			double nearest = arc.samples().stream().mapToDouble(s -> s.local().distanceTo(aim)).min().orElseThrow();
			assertTrue(nearest < WhipArc.RADIUS + HALF_MOB, "the crosshair at " + z + " is " + nearest + " off the lash");
		}
	}

	@Test
	void twoBlocksAsideIsOffTheLash() {
		for (double z = 0; z <= REACH; z += 0.1) {
			for (Vec3d aside : List.of(new Vec3d(2, 0, z), new Vec3d(-2, 0, z))) {
				double nearest = arc.samples().stream().mapToDouble(s -> s.local().distanceTo(aside)).min().orElseThrow();
				assertTrue(nearest > WhipArc.RADIUS + HALF_MOB, aside + " is on the lash");
			}
		}
	}

	@Test
	void theTipIsTheLastBlockAndAHalf() {
		double band = 1.5;
		assertTrue(arc.inTip(arc.length(), band));
		assertTrue(arc.inTip(arc.length() - band, band));
		assertFalse(arc.inTip(arc.length() - band - 0.05, band));
		assertFalse(arc.inTip(0, band));
		assertTrue(WhipArc.inTip(5, 6, 1.5));
		assertFalse(WhipArc.inTip(4, 6, 1.5));
		// On the crosshair, the tip starts about 1.5 blocks short of the reach.
		Vec3d start = arc.point(arc.length() - band);
		assertTrue(start.z > REACH - band - 0.1 && start.z < REACH - band + 0.3, "tip starts at " + start);
	}

	/** Each point of the swing comes down onto the hit curve, the tip last: the drawn lash is where it hits. */
	@Test
	void theSwingLaysTheLashOnTheHitCurve() {
		for (double along = 0; along <= arc.length(); along += 0.5) {
			Vec3d swung = arc.swing(arc.crackAge(along), along, WhipArc.CRACK_WINDUP);
			assertVec(arc.point(along), swung, 1e-4);
		}
		assertEquals(WhipArc.UNROLL_TICKS + WhipArc.LAG_TICKS, arc.crackAge(arc.length()), EPS);
		assertEquals(WhipArc.UNROLL_TICKS, arc.crackAge(0), EPS);
	}

	@Test
	void theCrackStartsBackOverTheRightShoulderAndRollsOut() {
		Vec3d tip = arc.swing(0, arc.length(), WhipArc.CRACK_WINDUP);
		assertTrue(tip.z < 0 && tip.y > 2 && tip.x > 0, "the tip does not start back over the right shoulder: " + tip);
		// When the hand end is down, the tip still trails high behind it.
		Vec3d trailing = arc.swing(arc.crackAge(0), arc.length(), WhipArc.CRACK_WINDUP);
		assertTrue(trailing.y > 1, "the tip came down with the hand: " + trailing);
		// After the crack it overshoots below the curve.
		Vec3d after = arc.swing(arc.crackAge(arc.length()) + 2, arc.length(), WhipArc.CRACK_WINDUP);
		assertTrue(after.y < -0.5, "no overshoot: " + after);
		// The swing never stretches the lash: every point keeps its distance from the hand.
		for (float age = 0; age < 8; age += 0.5f) {
			for (double along = 0; along <= arc.length(); along += 1) {
				assertEquals(arc.point(along).distanceTo(WhipArc.HAND), arc.swing(age, along, WhipArc.CRACK_WINDUP).distanceTo(WhipArc.HAND), 1e-6);
			}
		}
	}

	@Test
	void theLookFrameTurnsWithTheLook() {
		Vec3d eye = new Vec3d(10, 64, 10);
		// Facing south (+z), the right hand is west (−x).
		assertVec(new Vec3d(10, 64, 13), WhipArc.toWorld(eye, 0, 0, false, new Vec3d(0, 0, 3)), EPS);
		assertVec(new Vec3d(9, 64, 10), WhipArc.toWorld(eye, 0, 0, false, new Vec3d(1, 0, 0)), EPS);
		assertVec(new Vec3d(11, 64, 10), WhipArc.toWorld(eye, 0, 0, true, new Vec3d(1, 0, 0)), EPS);
		assertVec(new Vec3d(10, 65, 10), WhipArc.toWorld(eye, 0, 0, false, new Vec3d(0, 1, 0)), EPS);
		// Facing west (+90): ahead is −x, right is north (−z).
		assertVec(new Vec3d(8, 64, 10), WhipArc.toWorld(eye, 90, 0, false, new Vec3d(0, 0, 2)), EPS);
		assertVec(new Vec3d(10, 64, 9), WhipArc.toWorld(eye, 90, 0, false, new Vec3d(1, 0, 0)), EPS);
		// Looking straight down: ahead is down, up is ahead of the feet.
		assertVec(new Vec3d(10, 62, 10), WhipArc.toWorld(eye, 0, 90, false, new Vec3d(0, 0, 2)), 1e-5);
		assertVec(new Vec3d(10, 64, 11), WhipArc.toWorld(eye, 0, 90, false, new Vec3d(0, 1, 0)), 1e-5);
	}
}
