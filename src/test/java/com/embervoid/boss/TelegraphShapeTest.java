package com.embervoid.boss;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegraphShapeTest {
	private static final Vec3d O = new Vec3d(0, 64, 0);

	@Test
	void ringIsADiscAtAPoint() {
		TelegraphShape ring = TelegraphShape.ring(new Vec3d(10, 64, 10), 3);
		assertTrue(ring.contains(new Vec3d(10, 64, 10)));
		assertTrue(ring.contains(new Vec3d(12.9, 64, 10)));
		assertFalse(ring.contains(new Vec3d(13.1, 64, 10)));
		assertFalse(ring.contains(new Vec3d(0, 64, 0)));
	}

	@Test
	void heightBand() {
		TelegraphShape ring = TelegraphShape.ring(O, 3);
		assertTrue(ring.contains(new Vec3d(0, 64 + TelegraphShape.ABOVE, 0)));
		assertFalse(ring.contains(new Vec3d(0, 64 + TelegraphShape.ABOVE + 0.1, 0)));
		assertTrue(ring.contains(new Vec3d(0, 64 - TelegraphShape.BELOW, 0)));
		assertFalse(ring.contains(new Vec3d(0, 64 - TelegraphShape.BELOW - 0.1, 0)));
	}

	@Test
	void lineIsAStrip() {
		TelegraphShape line = TelegraphShape.line(O, new Vec3d(10, 64, 0), 1);
		assertTrue(line.contains(new Vec3d(5, 64, 0.9)));
		assertTrue(line.contains(new Vec3d(0, 64, -0.9)));
		assertFalse(line.contains(new Vec3d(5, 64, 1.1)));
		assertFalse(line.contains(new Vec3d(-0.5, 64, 0)));
		assertFalse(line.contains(new Vec3d(10.5, 64, 0)));
	}

	@Test
	void lineFollowsASlope() {
		TelegraphShape line = TelegraphShape.line(O, new Vec3d(10, 74, 0), 1);
		assertTrue(line.contains(new Vec3d(5, 69, 0)));
		assertFalse(line.contains(new Vec3d(5, 64, 0)));
	}

	@Test
	void coneHasAnAngleAndARange() {
		TelegraphShape cone = TelegraphShape.cone(O, new Vec3d(1, 0, 0), 6, 30);
		assertTrue(cone.contains(new Vec3d(5, 64, 0)));
		assertTrue(cone.contains(new Vec3d(4, 64, 2))); // ~26.6°
		assertFalse(cone.contains(new Vec3d(3, 64, 3))); // 45°
		assertFalse(cone.contains(new Vec3d(-3, 64, 0))); // behind
		assertFalse(cone.contains(new Vec3d(6.1, 64, 0))); // too far
		assertTrue(cone.contains(new Vec3d(0.2, 64, -0.2))); // on the apex
	}

	@Test
	void shadowIsADisc() {
		TelegraphShape shadow = TelegraphShape.shadow(O, 2);
		assertTrue(shadow.contains(new Vec3d(1.9, 64, 0)));
		assertFalse(shadow.contains(new Vec3d(2.1, 64, 0)));
	}
}
