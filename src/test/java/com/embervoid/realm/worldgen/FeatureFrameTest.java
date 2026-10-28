package com.embervoid.realm.worldgen;

import com.embervoid.reach.boss.arena.ArenaFrame;
import com.embervoid.reach.boss.arena.ReachArenas;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureFrameTest {
	private static final int[][] CENTRES = {{0, -300}, {0, 300}, {300, 0}, {-300, 0}, {250, -260}, {-410, 90},
			{128, 640}, {-1, -1}, {7, 7}, {-500, -499}};

	@Test
	void theArenaCodeTurnsArenasTheWayThePlacementDoes() {
		for (int[] c : CENTRES) {
			BlockPos center = new BlockPos(c[0], 64, c[1]);
			assertEquals(FeatureFrame.rotationTowardsLanding(center), ArenaFrame.rotationTowardsLanding(center), center.toShortString());
		}
	}

	@Test
	void theShrineGapIsTheSameOnBothSides() {
		assertEquals(ReachArenas.SHRINE_GAP, FeatureFrame.SHRINE_DISTANCE);
	}

	@Test
	void theShrineStandsWhereTheArenaLayoutExpectsIt() {
		int radius = 20;
		for (int[] c : CENTRES) {
			BlockPos center = new BlockPos(c[0], 64, c[1]);
			BlockPos expected = ArenaFrame.of(center).at(0, 1, -(radius + ReachArenas.SHRINE_GAP));
			assertEquals(expected, FeatureFrame.shrinePos(center, radius, false), center.toShortString());
		}
	}

	@Test
	void anArenaShrineFacesTheLanding() {
		int radius = 20;
		for (int[] c : CENTRES) {
			BlockPos center = new BlockPos(c[0], 64, c[1]);
			BlockPos shrine = FeatureFrame.shrinePos(center, radius, false);
			// From the centre the shrine lies on the landing's (the origin's) side.
			long towardsLanding = (long) (shrine.getX() - center.getX()) * -center.getX()
					+ (long) (shrine.getZ() - center.getZ()) * -center.getZ();
			assertTrue(towardsLanding > 0, center.toShortString());
			assertEquals(radius + 4, Math.abs(shrine.getX() - center.getX()) + Math.abs(shrine.getZ() - center.getZ()));
			assertEquals(center.getY() + 1, shrine.getY());
		}
	}

	@Test
	void theLandingShrineStandsOnItsCentre() {
		BlockPos center = new BlockPos(0, 70, 0);
		assertEquals(center.up(), FeatureFrame.shrinePos(center, 30, true));
	}
}
