package com.embervoid.block.prop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fat tallow candles, 1–4 like vanilla candles (lit with flint and steel, put out by hand or water, light 3 a candle),
 * with their own model per count and a facing to turn the cluster. Not in the candles tag: a cake would take them as
 * a vanilla candle.
 */
public class PropCandleBlock extends CandleBlock implements Prop {
	public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
	/** Each candle's x, z and height in pixels facing north, by count (tools/prop_art.py CANDLES). */
	private static final double[][][] WICKS = {
			{{8, 8, 8}},
			{{6.5, 7, 8}, {10.5, 9.5, 5}},
			{{6.5, 6.5, 8}, {10.5, 8, 6}, {7, 11, 4}},
			{{6, 6, 8}, {10, 6.5, 6}, {6.5, 10.5, 4}, {10.5, 10.5, 5}}};

	private final String name;
	private final Map<BlockState, VoxelShape> shapes = new IdentityHashMap<>();
	private final Map<BlockState, List<Vec3d>> flames = new IdentityHashMap<>();

	public PropCandleBlock(Settings settings, String name) {
		super(settings);
		this.name = name;
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
		for (BlockState state : getStateManager().getStates()) {
			Direction facing = state.get(FACING);
			shapes.put(state, PropShapes.get(model(state)).get(facing));
			List<Vec3d> points = new ArrayList<>();
			for (double[] c : WICKS[state.get(CANDLES) - 1]) {
				// The flame a pixel above the wick, turned with the cluster.
				double[] p = PropShape.turn(new double[]{c[0], c[2] + 2.5, c[1], c[0], c[2] + 2.5, c[1]}, PropShape.quarterTurns(facing));
				points.add(new Vec3d(p[0] / 16, p[1] / 16, p[2] / 16));
			}
			flames.put(state, List.copyOf(points));
		}
	}

	@Override
	public String model(BlockState state) {
		return name + "_" + state.get(CANDLES) + (state.get(LIT) ? "_lit" : "");
	}

	@Override
	public String itemModel() {
		return name + "_1";
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState state = super.getPlacementState(ctx);
		if (state == null || ctx.getWorld().getBlockState(ctx.getBlockPos()).isOf(this)) {
			return state;
		}
		return state.with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapes.get(state);
	}

	@Override
	protected Iterable<Vec3d> getParticleOffsets(BlockState state) {
		return flames.get(state);
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}
}
