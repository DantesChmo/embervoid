package com.embervoid.reach.boss.arena;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaFrameTest {
	private static double flatDistanceToLanding(BlockPos pos) {
		return Math.hypot(pos.getX(), pos.getZ());
	}

	@Test
	void arenaSouthOfTheLandingIsNotTurned() {
		assertEquals(BlockRotation.NONE, ArenaFrame.rotationTowardsLanding(new BlockPos(0, 64, 500)));
		assertEquals(BlockRotation.CLOCKWISE_180, ArenaFrame.rotationTowardsLanding(new BlockPos(0, 64, -500)));
		assertEquals(BlockRotation.COUNTERCLOCKWISE_90, ArenaFrame.rotationTowardsLanding(new BlockPos(500, 64, 0)));
		assertEquals(BlockRotation.CLOCKWISE_90, ArenaFrame.rotationTowardsLanding(new BlockPos(-500, 64, 0)));
	}

	@Test
	void theTemplatesNorthSideFacesTheLandingFromEveryDirection() {
		for (int degrees = 0; degrees < 360; degrees += 15) {
			double angle = Math.toRadians(degrees);
			BlockPos center = new BlockPos((int) Math.round(Math.cos(angle) * 600), 70, (int) Math.round(Math.sin(angle) * 600));
			ArenaFrame frame = ArenaFrame.of(center);
			BlockPos north = frame.at(0, 0, -40);
			BlockPos south = frame.at(0, 0, 40);
			assertTrue(flatDistanceToLanding(north) < flatDistanceToLanding(center) - 20, "north side at " + degrees + "°");
			assertTrue(flatDistanceToLanding(south) > flatDistanceToLanding(center) + 20, "south side at " + degrees + "°");
		}
	}

	@Test
	void eastAndWestStayOnTheirSidesAfterTurning() {
		// Turning keeps the layout's handedness: east is always 90° clockwise of north, seen from above.
		for (BlockPos center : new BlockPos[]{new BlockPos(0, 64, 500), new BlockPos(500, 64, 0), new BlockPos(0, 64, -500),
				new BlockPos(-500, 64, 0)}) {
			ArenaFrame frame = ArenaFrame.of(center);
			BlockPos north = frame.at(0, 0, -1).subtract(center);
			BlockPos east = frame.at(1, 0, 0).subtract(center);
			// Clockwise from above in Minecraft (x east, z south): (x, z) → (−z, x).
			assertEquals(new BlockPos(-north.getZ(), 0, north.getX()), east, "at " + center);
		}
	}

	@Test
	void heightIsNeverTurned() {
		ArenaFrame frame = ArenaFrame.of(new BlockPos(-300, 50, 120));
		assertEquals(new BlockPos(-300, 57, 120), frame.at(0, 7, 0));
		assertEquals(frame.at(3, 0, -5).up(4), frame.at(3, 4, -5));
	}

	@Test
	void theCentreIsTheZeroOffset() {
		BlockPos.Mutable mutable = new BlockPos.Mutable(40, 66, -900);
		ArenaFrame frame = ArenaFrame.of(mutable);
		mutable.set(0, 0, 0);
		assertEquals(new BlockPos(40, 66, -900), frame.center());
		assertEquals(frame.center(), frame.at(0, 0, 0));
	}

	@Test
	void topIsTheMiddleOfTheBlocksUpperFace() {
		ArenaFrame frame = ArenaFrame.of(new BlockPos(0, 64, 500));
		assertEquals(new Vec3d(2.5, 66, 497.5), frame.top(2, 1, -3));
	}

	@Test
	void diagonalPlacementsTurnByTheirZ() {
		assertEquals(BlockRotation.NONE, ArenaFrame.rotationTowardsLanding(new BlockPos(300, 64, 300)));
		assertEquals(BlockRotation.CLOCKWISE_180, ArenaFrame.rotationTowardsLanding(new BlockPos(-300, 64, -300)));
	}
}
