package com.embervoid.block.prop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/** A prop two blocks tall (scarecrow, standard): the lower half is placed, the upper one grows on top. */
public class TallPropBlock extends TwoBlockPropBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

	public TallPropBlock(Settings settings, String name) {
		super(settings, name);
		setDefaultState(getDefaultState().with(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	public String model(BlockState state) {
		return name + (state.get(HALF) == DoubleBlockHalf.LOWER ? "_lower" : "_upper");
	}

	@Override
	public String itemModel() {
		return name + "_item";
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(HALF);
	}

	@Override
	public boolean isPrimary(BlockState state) {
		return state.get(HALF) == DoubleBlockHalf.LOWER;
	}

	@Override
	public Direction toPartner(BlockState state) {
		return isPrimary(state) ? Direction.UP : Direction.DOWN;
	}

	@Override
	public BlockState partner(BlockState state) {
		return state.with(HALF, isPrimary(state) ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
	}
}
