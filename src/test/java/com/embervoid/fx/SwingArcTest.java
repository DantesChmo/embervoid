package com.embervoid.fx;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingArcTest {
	private static final double EPS = 1e-6;

	private static void assertVec(Vec3d expected, Vec3d actual) {
		assertTrue(expected.squaredDistanceTo(actual) < EPS, "expected " + expected + ", got " + actual);
	}

	@Test
	void flatArcPointsAheadAndToTheRight() {
		SwingArc flat = new SwingArc(-90, 90, 0, 2, 1.5f, 10, 0xFFFFFF);
		assertVec(new Vec3d(0, 1.5, 2), flat.local(0, 1));
		assertVec(new Vec3d(2, 1.5, 0), flat.local(90, 1));
		assertVec(new Vec3d(-1, 1.5, 0), flat.local(-90, 0.5f));
	}

	@Test
	void overheadChopComesDownFromAboveIntoTheFront() {
		SwingArc chop = SwingArc.overhead(3, 2.5f, 10, 0xFF0000);
		Vec3d start = chop.local(chop.angle(0), 1);
		Vec3d end = chop.local(chop.angle(1), 1);
		assertEquals(0, start.x, EPS);
		assertEquals(0, end.x, EPS);
		assertTrue(start.y > 2.5 + 2.5, "does not start high: " + start);
		assertTrue(start.z < 0, "does not start behind: " + start);
		assertTrue(end.y < 2.5 && end.z > 2, "does not end low and ahead: " + end);
	}

	@Test
	void sweepsCrossTheFrontFromTheirSide() {
		SwingArc left = SwingArc.sweep(3, 1.5f, true, 8, 0);
		SwingArc right = SwingArc.sweep(3, 1.5f, false, 8, 0);
		assertTrue(left.local(left.angle(0), 1).x < -2.5 && left.local(left.angle(1), 1).x > 2.5);
		assertTrue(right.local(right.angle(0), 1).x > 2.5 && right.local(right.angle(1), 1).x < -2.5);
		assertTrue(left.local(left.angle(0.5f), 1).z > 2.9, "the middle of the sweep is not ahead");
	}

	@Test
	void turnedToTheSwingersYaw() {
		Vec3d base = new Vec3d(10, 64, 10);
		Vec3d ahead = new Vec3d(0, 1, 2);
		Vec3d right = new Vec3d(2, 1, 0);
		// Yaw 0 faces south (+z); its right is west (−x).
		assertVec(new Vec3d(10, 65, 12), SwingArc.toWorld(base, 0, ahead));
		assertVec(new Vec3d(8, 65, 10), SwingArc.toWorld(base, 0, right));
		// Yaw 90 faces west (−x); its right is north (−z).
		assertVec(new Vec3d(8, 65, 10), SwingArc.toWorld(base, 90, ahead));
		assertVec(new Vec3d(10, 65, 8), SwingArc.toWorld(base, 90, right));
		// Yaw −90 faces east.
		assertVec(new Vec3d(12, 65, 10), SwingArc.toWorld(base, -90, ahead));
	}

	@Test
	void headLeadsTheTailWhichCatchesUp() {
		SwingArc arc = new SwingArc(-60, 60, 0, 3, 1, 10, 0);
		assertEquals(0, arc.head(0));
		assertEquals(1, arc.head(10));
		assertEquals(1, arc.head(15));
		assertTrue(arc.head(3) > 0.3f, "eased out: the blade starts fast");
		assertEquals(0, arc.tail(SwingArc.LAG * 10));
		for (float age = 0; age <= arc.lifetime(); age += 0.5f) {
			assertTrue(arc.tail(age) <= arc.head(age), "tail ahead at " + age);
		}
		assertEquals(1, arc.tail(arc.lifetime()), 1e-6);
		assertEquals(-60, arc.angle(0));
		assertEquals(60, arc.angle(1));
		assertEquals(60, arc.angle(2));
	}

	@Test
	void survivesThePacket() {
		SwingArc arc = new SwingArc(110, -40, 90, 3.5f, 2.8f, 9, 0xFF7A2A);
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		arc.write(buf);
		assertEquals(arc, SwingArc.read(buf));
		assertEquals(0, buf.readableBytes());
		assertEquals(1, new SwingArc(0, 1, 0, 1, 1, 0, 0).ticks());
	}
}
