package com.embervoid.hub.stele;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The shape of a Cleft Stele, one of two:
 * <ul>
 *   <li>{@link Shape#CIRCLE}, the ritual circle: a centre stone and four pillars {@link #PILLAR_HEIGHT} tall
 *   {@link #RADIUS} blocks out from it on each side, all Cleft Stone, the {@link #COLUMN} blocks over the centre open;
 *   kindled, a column of mist (the rift) rises there. {@link #origin} is the centre stone; {@link #axis} means
 *   nothing.</li>
 *   <li>{@link Shape#SLAB}, the older slab, still kindled and still working where it was built: a flat slab
 *   {@link #WIDTH} wide and {@link #HEIGHT} tall, two columns of Cleft Stone on each side and the middle column open,
 *   the crack. It stands along X or along Z; {@link #origin} is the bottom block of the leftmost column (the lowest X
 *   or Z of the slab).</li>
 * </ul>
 * The passage ({@link #crack()}) is the crack of a slab or the rift of a circle.
 */
public record SteleFrame(BlockPos origin, Direction.Axis axis, Shape shape) {
	public static final int WIDTH = 5;
	public static final int HEIGHT = 6;
	/** Column of the crack, counted from {@link #origin}. */
	public static final int CRACK = 2;
	/** Ritual circle: blocks from the centre stone to each pillar, the pillars' height and the rift's. */
	public static final int RADIUS = 2;
	public static final int PILLAR_HEIGHT = 3;
	public static final int COLUMN = 3;
	private static final Direction[] PILLARS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	public enum Shape {
		SLAB,
		CIRCLE
	}

	public enum Cell {
		/** Cleft Stone, cold or kindled. */
		STONE,
		/** Free for the passage: air, a replaceable plant, or the crack or rift itself. */
		OPEN,
		OTHER
	}

	/** A slab along {@code axis}. */
	public SteleFrame(BlockPos origin, Direction.Axis axis) {
		this(origin, axis, Shape.SLAB);
	}

	public static SteleFrame circle(BlockPos centre) {
		return new SteleFrame(centre, Direction.Axis.X, Shape.CIRCLE);
	}

	/**
	 * Finds a frame that the stone at {@code pos} belongs to: a circle first, then a slab, trying X before Z and the
	 * lowest origin first. Pure: {@code cells} says what each position holds, so the shape can be checked without a
	 * world.
	 */
	public static Optional<SteleFrame> find(Function<BlockPos, Cell> cells, BlockPos pos) {
		for (SteleFrame circle : circlesWith(pos)) {
			if (circle.matches(cells)) {
				return Optional.of(circle);
			}
		}
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

	/** Every ritual circle that has a stone at {@code pos}: {@code pos} as its centre, or in one of its pillars. */
	public static List<SteleFrame> circlesWith(BlockPos pos) {
		List<SteleFrame> circles = new ArrayList<>();
		circles.add(circle(pos));
		for (Direction side : PILLARS) {
			for (int up = 0; up < PILLAR_HEIGHT; up++) {
				circles.add(circle(pos.offset(side, -RADIUS).down(up)));
			}
		}
		return circles;
	}

	/** Whether the stones stand and the passage is open. */
	public boolean matches(Function<BlockPos, Cell> cells) {
		return stones().stream().allMatch(pos -> cells.apply(pos) == Cell.STONE)
				&& crack().stream().allMatch(pos -> cells.apply(pos) == Cell.OPEN);
	}

	/** A slab's block {@code column} along the axis and {@code y} up from {@link #origin}. */
	public BlockPos at(int column, int y) {
		return origin.offset(axis, column).up(y);
	}

	/** The stones: a slab's {@code 4 × HEIGHT} column by column from the bottom; a circle's centre, then each pillar from the bottom. */
	public List<BlockPos> stones() {
		List<BlockPos> stones = new ArrayList<>();
		if (shape == Shape.CIRCLE) {
			stones.add(origin);
			for (Direction side : PILLARS) {
				for (int y = 0; y < PILLAR_HEIGHT; y++) {
					stones.add(origin.offset(side, RADIUS).up(y));
				}
			}
			return stones;
		}
		for (int column = 0; column < WIDTH; column++) {
			if (column != CRACK) {
				for (int y = 0; y < HEIGHT; y++) {
					stones.add(at(column, y));
				}
			}
		}
		return stones;
	}

	/** The passage, from the bottom: a slab's crack column, or the rift over a circle's centre stone. */
	public List<BlockPos> crack() {
		List<BlockPos> crack = new ArrayList<>();
		if (shape == Shape.CIRCLE) {
			for (int y = 1; y <= COLUMN; y++) {
				crack.add(origin.up(y));
			}
			return crack;
		}
		for (int y = 0; y < HEIGHT; y++) {
			crack.add(at(CRACK, y));
		}
		return crack;
	}

	/** The middle of the passage, where its sounds and particles come from. */
	public Vec3d middle() {
		List<BlockPos> crack = crack();
		return Vec3d.ofCenter(crack.get(crack.size() / 2));
	}

	/** Which ways a player can step out of the passage: across a slab, or any side of a circle's rift. */
	public List<Direction> ways() {
		if (shape == Shape.CIRCLE) {
			return List.of(PILLARS);
		}
		Direction.Axis across = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		return List.of(Direction.from(across, Direction.AxisDirection.POSITIVE), Direction.from(across, Direction.AxisDirection.NEGATIVE));
	}
}
