package com.embervoid.block.prop;

import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropShapeTest {
	private static final double[] BOX = {1, 0, 2, 5, 8, 12};

	@Test
	void fourQuarterTurnsComeBackToTheStart() {
		assertArrayEquals(BOX, PropShape.turn(BOX, 4));
		assertArrayEquals(BOX, PropShape.turn(BOX, 0));
	}

	@Test
	void oneQuarterTurnIsClockwiseSeenFromAbove() {
		// The box hugs the north-west corner; after a clockwise turn it hugs the north-east one.
		double[] turned = PropShape.turn(new double[]{0, 0, 0, 4, 1, 2}, 1);
		assertArrayEquals(new double[]{14, 0, 0, 16, 1, 4}, turned);
	}

	@Test
	void turnedBoxesStayOrderedAndInsideTheBlock() {
		for (int turns = 0; turns < 4; turns++) {
			double[] b = PropShape.turn(BOX, turns);
			assertTrue(b[0] < b[3] && b[1] < b[4] && b[2] < b[5], "turn " + turns);
			for (double c : b) {
				assertTrue(c >= 0 && c <= 16, "turn " + turns);
			}
			assertEquals(BOX[1], b[1]);
			assertEquals(BOX[4], b[4]);
		}
	}

	@Test
	void facingsMapToTheBlockstateRotation() {
		assertEquals(0, PropShape.quarterTurns(Direction.NORTH));
		assertEquals(1, PropShape.quarterTurns(Direction.EAST));
		assertEquals(2, PropShape.quarterTurns(Direction.SOUTH));
		assertEquals(3, PropShape.quarterTurns(Direction.WEST));
	}

	@Test
	void boxesNeedSixCoordinatesEach() {
		assertThrows(IllegalArgumentException.class, () -> PropShape.of());
		assertThrows(IllegalArgumentException.class, () -> PropShape.of(0, 0, 0, 16, 16));
	}

	@Test
	void boxesForAFacingAreTheNorthBoxesTurned() {
		PropShape shape = PropShape.of(BOX);
		assertArrayEquals(PropShape.turn(BOX, 2), shape.boxes(Direction.SOUTH)[0]);
		assertTrue(shape.get(Direction.EAST).getBoundingBox().maxX <= 1);
	}
}
