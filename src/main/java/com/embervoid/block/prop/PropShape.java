package com.embervoid.block.prop;

import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * A prop's hitbox (outline and collision): boxes in pixels with the prop facing north, turned for the other facings
 * the way its blockstate turns the model (y 90 per step clockwise, seen from above).
 */
public final class PropShape {
	/** Boxes {x1, y1, z1, x2, y2, z2} facing north. */
	private final double[][] boxes;
	/** By horizontal index (south, west, north, east). */
	private final VoxelShape[] shapes = new VoxelShape[4];

	private PropShape(double[][] boxes) {
		this.boxes = boxes;
		for (Direction facing : Direction.Type.HORIZONTAL) {
			VoxelShape shape = VoxelShapes.empty();
			for (double[] b : boxes(facing)) {
				shape = VoxelShapes.union(shape, VoxelShapes.cuboid(b[0] / 16, b[1] / 16, b[2] / 16, b[3] / 16, b[4] / 16, b[5] / 16));
			}
			shapes[facing.getHorizontal()] = shape.simplify();
		}
	}

	/** Groups of six numbers, each a box x1, y1, z1, x2, y2, z2 in pixels facing north. */
	public static PropShape of(double... coords) {
		if (coords.length == 0 || coords.length % 6 != 0) {
			throw new IllegalArgumentException("boxes need six coordinates each");
		}
		double[][] boxes = new double[coords.length / 6][];
		for (int i = 0; i < boxes.length; i++) {
			boxes[i] = new double[6];
			System.arraycopy(coords, i * 6, boxes[i], 0, 6);
		}
		return new PropShape(boxes);
	}

	public VoxelShape get(Direction facing) {
		return shapes[facing.getHorizontal()];
	}

	/** The boxes turned to face this way, in pixels. */
	public double[][] boxes(Direction facing) {
		double[][] out = new double[boxes.length][];
		for (int i = 0; i < boxes.length; i++) {
			out[i] = turn(boxes[i], quarterTurns(facing));
		}
		return out;
	}

	/** The blockstate's y rotation of a prop facing this way, in quarter turns: north 0, east 1, south 2, west 3. */
	public static int quarterTurns(Direction facing) {
		return switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
	}

	/** A box turned clockwise seen from above by quarter turns about the block's centre: (x, z) to (16 - z, x). */
	public static double[] turn(double[] box, int quarterTurns) {
		double[] b = box.clone();
		for (int i = 0; i < quarterTurns; i++) {
			b = new double[]{16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
		}
		return b;
	}
}
