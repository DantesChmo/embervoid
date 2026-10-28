package com.embervoid.block.prop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

import java.util.IdentityHashMap;
import java.util.Map;

/** A one-block prop that faces whoever placed it and can stand in water (a flooded crypt). */
public class PropBlock extends HorizontalFacingBlock implements Prop, Waterloggable {
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

	protected final String name;
	private final Map<BlockState, VoxelShape> shapes = new IdentityHashMap<>();

	public PropBlock(Settings settings, String name) {
		super(settings);
		this.name = name;
		setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(WATERLOGGED, false));
		for (BlockState state : getStateManager().getStates()) {
			shapes.put(state, PropShapes.get(model(state)).get(state.get(FACING)));
		}
	}

	@Override
	public String model(BlockState state) {
		return name;
	}

	@Override
	public String itemModel() {
		return model(getDefaultState());
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, placementFacing(ctx))
				.with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER);
	}

	protected Direction placementFacing(ItemPlacementContext ctx) {
		return ctx.getHorizontalPlayerFacing().getOpposite();
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapes.get(state);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
			WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		if (state.get(WATERLOGGED)) {
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}
		return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
	}
}
