package com.embervoid.reach.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** A carved stone that faces the way it was set: road milestones and gravestones (PLAN 9.5). */
public class MarkerStoneBlock extends HorizontalFacingBlock {
	/** A square post, 10 × 14 × 10 pixels. */
	static final VoxelShape[] MILESTONE_SHAPE = shapes(3, 13, 14, 3, 13);
	/** A slab of stone 12 wide, 15 high, 4 thick, across the facing. */
	static final VoxelShape[] GRAVESTONE_SHAPE = shapes(2, 14, 15, 6, 10);

	/** Shapes for facing north / east / south / west. */
	private final VoxelShape[] shapes;

	public MarkerStoneBlock(Settings settings, VoxelShape[] shapes) {
		super(settings);
		this.shapes = shapes;
		setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapes[state.get(FACING).getHorizontal()];
	}

	/** Shapes by horizontal index (south, west, north, east) of a box across x [x0, x1], z [z0, z1] facing north. */
	private static VoxelShape[] shapes(int x0, int x1, int height, int z0, int z1) {
		VoxelShape northSouth = Block.createCuboidShape(x0, 0, z0, x1, height, z1);
		VoxelShape eastWest = Block.createCuboidShape(z0, 0, x0, z1, height, x1);
		return new VoxelShape[]{northSouth, eastWest, northSouth, eastWest};
	}
}
