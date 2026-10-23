package com.embervoid.block.prop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/** A prop that piles up like sea pickles: using its item on it adds one more, up to three (sacks, urns). */
public class StackedPropBlock extends PropBlock {
	public static final int MAX = 3;
	public static final IntProperty COUNT = IntProperty.of("count", 1, MAX);

	public StackedPropBlock(Settings settings, String name) {
		super(settings, name);
	}

	@Override
	public String model(BlockState state) {
		return name + "_" + state.get(COUNT);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(COUNT);
	}

	@Override
	public boolean canReplace(BlockState state, ItemPlacementContext context) {
		return !context.shouldCancelInteraction() && context.getStack().isOf(asItem()) && state.get(COUNT) < MAX
				|| super.canReplace(state, context);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState there = ctx.getWorld().getBlockState(ctx.getBlockPos());
		if (there.isOf(this)) {
			return there.with(COUNT, Math.min(MAX, there.get(COUNT) + 1));
		}
		return super.getPlacementState(ctx);
	}
}
