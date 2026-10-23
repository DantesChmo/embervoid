package com.embervoid.hub.stele;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The shape of a Cleft Stele (DESIGN §2): a flat slab {@link #WIDTH} wide and {@link #HEIGHT} tall, two columns of
 * Cleft Stone on each side and the middle column open, the crack. It stands along X or along Z.
 *
 * @param origin bottom block of the leftmost column (the lowest X or Z of the slab)
 * @param axis   the horizontal axis the slab runs along
 */
public record SteleFrame(BlockPos origin, Direction.Axis axis) {
	public static final int WIDTH = 5;
	public static final int HEIGHT = 6;
	/** Column of the crack, counted from {@link #origin}. */
	public static final int CRACK = 2;

	/** What a block in the frame's plane counts as. */
	public enum Cell {
		/** Cleft Stone, cold or kindled. */
		STONE,
		/** Free for the crack: air, a replaceable plant, or the crack itself. */
		OPEN,
		/** Anything else. */
		OTHER
	}

	/**
	 * Finds a frame that {@code pos} belongs to, trying X before Z and the lowest origin first. Pure: {@code cells}
	 * says what each position holds, so the shape can be checked without a world.
	 */
	public static Optional<SteleFrame> find(Function<BlockPos, Cell> cells, BlockPos pos) {
		for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
			for (int up = HEIGHT - 1; up >= 0; up--) {
				for (int along = WIDTH - 1; along >= 0; along--) {
					SteleFrame frame = new SteleFrame(pos.offset(axis, -along).down(up), axis);
					if (frame.matches(cells)) {
						return Optional.of(frame);
					}
				}
			}
		}
		return Optional.empty();
	}

	/** Whether the stones stand and the crack is open. */
	public boolean matches(Function<BlockPos, Cell> cells) {
		for (int column = 0; column < WIDTH; column++) {
			Cell wanted = column == CRACK ? Cell.OPEN : Cell.STONE;
			for (int y = 0; y < HEIGHT; y++) {
				if (cells.apply(at(column, y)) != wanted) {
					return false;
				}
			}
		}
		return true;
	}

	/** The block {@code column} along the axis and {@code y} up from {@link #origin}. */
	public BlockPos at(int column, int y) {
		return origin.offset(axis, column).up(y);
	}

	/** The {@code 4 × HEIGHT} stones, column by column from the bottom. */
	public List<BlockPos> stones() {
		List<BlockPos> stones = new ArrayList<>();
		for (int column = 0; column < WIDTH; column++) {
			if (column != CRACK) {
				for (int y = 0; y < HEIGHT; y++) {
					stones.add(at(column, y));
				}
			}
		}
		return stones;
	}

	/** The crack column, from the bottom. */
	public List<BlockPos> crack() {
		List<BlockPos> crack = new ArrayList<>();
		for (int y = 0; y < HEIGHT; y++) {
			crack.add(at(CRACK, y));
		}
		return crack;
	}
}
