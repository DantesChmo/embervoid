package com.embervoid.block.prop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/** A prop that hangs on a chain when set against the underside of a block, like a lantern (the iron cage). */
public class HangingPropBlock extends PropBlock {
	public static final BooleanProperty HANGING = Properties.HANGING;

	public HangingPropBlock(Settings settings, String name) {
		super(settings, name);
	}

	@Override
	public String model(BlockState state) {
		return state.get(HANGING) ? name + "_hanging" : name;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(HANGING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return super.getPlacementState(ctx).with(HANGING, ctx.getSide() == Direction.DOWN);
	}
}
