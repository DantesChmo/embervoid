package com.embervoid.realm.placed;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;

/**
 * Marker block {@code embervoid:placed_enemy} in structure templates (PLAN 9.2): the {@link PlacedEnemyProcessor}
 * turns it into air and spawns the entity named in its NBT, facing the block's direction. Invisible; never left in
 * the world when the processor runs.
 */
public class PlacedEnemyMarkerBlock extends HorizontalFacingBlock {
	public PlacedEnemyMarkerBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
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
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}
}
