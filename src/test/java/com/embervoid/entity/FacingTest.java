package com.embervoid.entity;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacingTest {
	private static final Vec3d SOUTH = new Vec3d(0, 0, 1);
	private static final Vec3d NORTH = new Vec3d(0, 0, -1);
	private static final Vec3d EAST = new Vec3d(1, 0, 0);
	private static final Vec3d WEST = new Vec3d(-1, 0, 0);
	/** cos 60°: a cone 120° wide. */
	private static final double CONE_120 = 0.5;
	/** cos 110°. */
	private static final double PAST_110 = Math.cos(Math.toRadians(110));

	@Test
	void yawZeroFacesSouthAndNinetyFacesWest() {
		assertEquals(1, Facing.cosineTo(0, SOUTH), 1e-3);
		assertEquals(-1, Facing.cosineTo(0, NORTH), 1e-3);
		assertEquals(1, Facing.cosineTo(90, WEST), 1e-3);
		assertEquals(0, Facing.cosineTo(0, EAST), 1e-3);
	}

	@Test
	void onlyTheHorizontalBearingCounts() {
		assertEquals(1, Facing.cosineTo(0, new Vec3d(0, 50, 2)), 1e-3);
		assertEquals(Facing.cosineTo(30, new Vec3d(3, 0, 4)), Facing.cosineTo(30, new Vec3d(30, -7, 40)), 1e-9);
	}

	@Test
	void inFrontWithinTheCone() {
		assertTrue(Facing.isInFront(0, SOUTH, CONE_120));
		assertTrue(Facing.isInFront(0, new Vec3d(1, 0, 1), CONE_120), "45° off");
		assertFalse(Facing.isInFront(0, new Vec3d(2, 0, 1), CONE_120), "about 63° off");
		assertFalse(Facing.isInFront(0, EAST, CONE_120));
		assertFalse(Facing.isInFront(0, NORTH, CONE_120));
	}

	@Test
	void theConeTurnsWithTheBody() {
		assertTrue(Facing.isInFront(180, NORTH, CONE_120));
		assertFalse(Facing.isInFront(180, SOUTH, CONE_120));
		assertTrue(Facing.isInFront(-90, EAST, CONE_120));
	}

	@Test
	void behindIsPastTheAngle() {
		assertTrue(Facing.isBehind(0, NORTH, PAST_110));
		assertTrue(Facing.isBehind(0, new Vec3d(1, 0, -1), PAST_110), "135° round");
		assertFalse(Facing.isBehind(0, EAST, PAST_110), "beside is not behind");
		assertFalse(Facing.isBehind(0, new Vec3d(1, 0, -0.2), PAST_110), "about 101° round");
		assertFalse(Facing.isBehind(0, SOUTH, PAST_110));
	}

	@Test
	void someoneStandingInsideTheBodyIsNeitherInFrontNorBehind() {
		assertFalse(Facing.isInFront(0, Vec3d.ZERO, CONE_120));
		assertFalse(Facing.isBehind(0, Vec3d.ZERO, PAST_110));
		assertFalse(Facing.isInFront(0, new Vec3d(0, 3, 0), CONE_120), "straight above");
		assertFalse(Facing.isBehind(0, new Vec3d(0, -3, 0), PAST_110), "straight below");
	}
}
