package com.embervoid.sanctum.boss;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanctumBossArenaTest {
	private static final BlockPos PIECE = new BlockPos(320, -20, -96);
	private static final BlockPos FLOOR = new BlockPos(330, -20, -77);

	@Test
	void anUnturnedWingHasItsCentreAtTheTemplateSpot() {
		assertEquals(PIECE.add(10, 0, 19), SanctumBossArena.center(PIECE, BlockRotation.NONE));
	}

	@Test
	void turningTheWingTurnsItsCentreAroundThePieceCorner() {
		// The template spot (10, 19) a quarter turn clockwise is (-19, 10), a half turn (-10, -19).
		assertEquals(PIECE.add(-19, 0, 10), SanctumBossArena.center(PIECE, BlockRotation.CLOCKWISE_90));
		assertEquals(PIECE.add(-10, 0, -19), SanctumBossArena.center(PIECE, BlockRotation.CLOCKWISE_180));
		assertEquals(PIECE.add(19, 0, -10), SanctumBossArena.center(PIECE, BlockRotation.COUNTERCLOCKWISE_90));
	}

	@Test
	void everyTurnKeepsTheCentreOnTheFloorAndAsFarFromTheCorner() {
		Set<BlockPos> centres = new HashSet<>();
		for (BlockRotation rotation : BlockRotation.values()) {
			BlockPos centre = SanctumBossArena.center(PIECE, rotation);
			assertEquals(PIECE.getY(), centre.getY());
			assertEquals(10 * 10 + 19 * 19, centre.getSquaredDistance(PIECE));
			centres.add(centre);
		}
		assertEquals(4, centres.size());
	}

	@Test
	void aPlayerWithinEightBlocksOfTheCentreStepsIn() {
		Vec3d middle = new Vec3d(330.5, -19, -76.5);
		assertTrue(SanctumBossArena.stepsIn(FLOOR, middle));
		assertTrue(SanctumBossArena.stepsIn(FLOOR, middle.add(8, 0, 0)));
		assertTrue(SanctumBossArena.stepsIn(FLOOR, middle.add(0, 0, -8)));
		assertFalse(SanctumBossArena.stepsIn(FLOOR, middle.add(8.2, 0, 0)));
		assertFalse(SanctumBossArena.stepsIn(FLOOR, middle.add(6, 0, 6)), "8.5 blocks out on the diagonal");
	}

	@Test
	void onlyFromTheFloorUpToTheArenasHeight() {
		assertTrue(SanctumBossArena.stepsIn(FLOOR, new Vec3d(330.5, -20, -76.5)), "standing on the floor block's level");
		assertFalse(SanctumBossArena.stepsIn(FLOOR, new Vec3d(330.5, -21, -76.5)), "below the floor");
		assertTrue(SanctumBossArena.stepsIn(FLOOR, new Vec3d(330.5, -12, -76.5)));
		assertFalse(SanctumBossArena.stepsIn(FLOOR, new Vec3d(330.5, 0, -76.5)), "high above, in the stairwell");
	}
}
