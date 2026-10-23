package com.embervoid.hub.stele;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Frame detection of the Cleft Stele (PLAN 15.3) on a map of cells; anything not set is open air. */
class SteleFrameTest {
	private static final BlockPos ORIGIN = new BlockPos(10, 64, -3);

	private final Map<BlockPos, SteleFrame.Cell> cells = new HashMap<>();

	private SteleFrame.Cell cell(BlockPos pos) {
		return cells.getOrDefault(pos, SteleFrame.Cell.OPEN);
	}

	private Optional<SteleFrame> find(BlockPos pos) {
		return SteleFrame.find(this::cell, pos);
	}

	/** Lays the 24 stones of a frame; the crack is left as it is. */
	private SteleFrame build(BlockPos origin, Direction.Axis axis) {
		SteleFrame frame = new SteleFrame(origin, axis);
		frame.stones().forEach(pos -> cells.put(pos, SteleFrame.Cell.STONE));
		return frame;
	}

	@Test
	void shapeIs24StonesAroundACrackOf6() {
		SteleFrame frame = new SteleFrame(ORIGIN, Direction.Axis.X);
		assertEquals(24, frame.stones().size());
		assertEquals(6, frame.crack().size());
		assertEquals(ORIGIN.east(2), frame.crack().get(0));
		assertEquals(ORIGIN.east(2).up(5), frame.crack().get(5));
		assertTrue(frame.stones().stream().noneMatch(frame.crack()::contains));
		assertEquals(ORIGIN.south(4).up(5), new SteleFrame(ORIGIN, Direction.Axis.Z).at(4, 5));
	}

	@Test
	void validFrameAlongXIsFoundFromEveryStone() {
		SteleFrame frame = build(ORIGIN, Direction.Axis.X);
		for (BlockPos stone : frame.stones()) {
			assertEquals(Optional.of(frame), find(stone), "from " + stone);
		}
	}

	@Test
	void validFrameAlongZIsFoundFromEveryStone() {
		SteleFrame frame = build(ORIGIN, Direction.Axis.Z);
		for (BlockPos stone : frame.stones()) {
			assertEquals(Optional.of(frame), find(stone), "from " + stone);
		}
	}

	@Test
	void frameStandingOnAFloorOfStoneIsFound() {
		for (int x = -2; x <= 7; x++) {
			for (int z = -2; z <= 2; z++) {
				cells.put(ORIGIN.add(x, -1, z), SteleFrame.Cell.STONE);
			}
		}
		SteleFrame frame = build(ORIGIN, Direction.Axis.X);
		assertEquals(Optional.of(frame), find(ORIGIN));
	}

	@Test
	void aMissingStoneBreaksTheFrame() {
		SteleFrame frame = build(ORIGIN, Direction.Axis.X);
		for (BlockPos stone : frame.stones()) {
			cells.remove(stone);
			BlockPos clicked = stone.equals(frame.at(0, 0)) ? frame.at(4, 5) : frame.at(0, 0);
			assertFalse(find(clicked).isPresent(), "without " + stone);
			cells.put(stone, SteleFrame.Cell.STONE);
		}
	}

	@Test
	void aWrongBlockInTheFrameBreaksIt() {
		SteleFrame frame = build(ORIGIN, Direction.Axis.Z);
		cells.put(frame.at(3, 2), SteleFrame.Cell.OTHER);
		assertFalse(find(frame.at(0, 0)).isPresent());
	}

	@Test
	void anObstructedCrackBreaksTheFrame() {
		SteleFrame frame = build(ORIGIN, Direction.Axis.X);
		for (BlockPos crack : frame.crack()) {
			cells.put(crack, SteleFrame.Cell.OTHER);
			assertFalse(find(frame.at(0, 0)).isPresent(), "blocked at " + crack);
			cells.remove(crack);
		}
		assertTrue(find(frame.at(0, 0)).isPresent());
	}

	@Test
	void stoneInTheCrackBreaksTheFrame() {
		SteleFrame frame = build(ORIGIN, Direction.Axis.X);
		cells.put(frame.at(SteleFrame.CRACK, 3), SteleFrame.Cell.STONE);
		assertFalse(find(frame.at(0, 0)).isPresent());
	}

	@Test
	void tooNarrowOrTooShortIsNoFrame() {
		// One column on each side of the crack.
		for (int y = 0; y < SteleFrame.HEIGHT; y++) {
			cells.put(ORIGIN.up(y), SteleFrame.Cell.STONE);
			cells.put(ORIGIN.east(2).up(y), SteleFrame.Cell.STONE);
		}
		assertFalse(find(ORIGIN).isPresent());

		// Five tall.
		cells.clear();
		SteleFrame frame = build(ORIGIN, Direction.Axis.X);
		for (int column = 0; column < SteleFrame.WIDTH; column++) {
			cells.remove(frame.at(column, SteleFrame.HEIGHT - 1));
		}
		assertFalse(find(ORIGIN).isPresent());
	}

	@Test
	void aTallerSlabGivesTheLowestFrameThatHoldsTheStone() {
		SteleFrame frame = build(ORIGIN, Direction.Axis.X);
		for (int column : new int[]{0, 1, 3, 4}) {
			cells.put(frame.at(column, SteleFrame.HEIGHT), SteleFrame.Cell.STONE);
		}
		assertEquals(ORIGIN, find(frame.at(0, 0)).orElseThrow().origin());
		assertEquals(ORIGIN, find(frame.at(4, 3)).orElseThrow().origin());
		// The top stone is only in the frame one block up.
		assertEquals(ORIGIN.up(), find(frame.at(4, SteleFrame.HEIGHT)).orElseThrow().origin());
	}
}
