package com.embervoid.block.setpiece;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which set pieces a chunk keeps when it holds more than the cap: the lowest and northernmost, the same on every server. */
class SetPieceCapTest {
	@Test
	void keepsTheLowestNorthernmostPieces() {
		List<BlockPos> pieces = List.of(new BlockPos(5, 70, 9), new BlockPos(1, 64, 3), new BlockPos(2, 64, 3), new BlockPos(0, 64, 8),
				new BlockPos(9, 66, 1));
		List<BlockPos> kept = SetPieceCap.keep(pieces, 3);
		assertEquals(List.of(new BlockPos(1, 64, 3), new BlockPos(2, 64, 3), new BlockPos(0, 64, 8)), kept);
	}

	@Test
	void keepsEverythingUnderTheCap() {
		List<BlockPos> pieces = List.of(new BlockPos(3, 1, 3), new BlockPos(1, 1, 1));
		assertEquals(2, SetPieceCap.keep(pieces, 8).size());
		assertTrue(SetPieceCap.keep(pieces, 0).isEmpty());
	}

	@Test
	void orderOfTheInputDoesNotMatter() {
		List<BlockPos> a = List.of(new BlockPos(0, 1, 0), new BlockPos(0, 1, 1), new BlockPos(0, 2, 0));
		List<BlockPos> b = List.of(new BlockPos(0, 2, 0), new BlockPos(0, 1, 1), new BlockPos(0, 1, 0));
		assertEquals(SetPieceCap.keep(a, 2), SetPieceCap.keep(b, 2));
	}
}
