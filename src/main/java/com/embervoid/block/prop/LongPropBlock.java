package com.embervoid.block.prop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * A prop two blocks long, laid like a bed (coffin, cart): the foot is placed at the cursor, the head one block on in
 * the facing, away from the player, so a north-facing prop has its head to the north.
 */
public class LongPropBlock extends TwoBlockPropBlock {
	public static final EnumProperty<BedPart> PART = Properties.BED_PART;

	public LongPropBlock(Settings settings, String name) {
		super(settings, name);
		setDefaultState(getDefaultState().with(PART, BedPart.FOOT));
	}

	@Override
	public String model(BlockState state) {
		return name + (state.get(PART) == BedPart.HEAD ? "_head" : "_foot");
	}

	@Override
	public String itemModel() {
		return name + "_item";
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(PART);
	}

	@Override
	protected Direction placementFacing(ItemPlacementContext ctx) {
		return ctx.getHorizontalPlayerFacing();
	}

	@Override
	public boolean isPrimary(BlockState state) {
		return state.get(PART) == BedPart.FOOT;
	}

	@Override
	public Direction toPartner(BlockState state) {
		Direction facing = state.get(FACING);
		return isPrimary(state) ? facing : facing.getOpposite();
	}

	@Override
	public BlockState partner(BlockState state) {
		return state.with(PART, isPrimary(state) ? BedPart.HEAD : BedPart.FOOT);
	}
}
