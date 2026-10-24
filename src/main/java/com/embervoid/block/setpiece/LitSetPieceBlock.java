package com.embervoid.block.setpiece;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;

/** A set piece with a lit state (brazier, censer): the {@code lit} blockstate follows the block entity's state and gives light. */
public class LitSetPieceBlock extends SetPieceBlock {
	public static final BooleanProperty LIT = Properties.LIT;

	public LitSetPieceBlock(Settings settings, SetPieceKind kind) {
		super(settings.luminance(state -> state.get(LIT) ? kind.luminance() : 0), kind);
	}

	@Override
	protected BlockState defaultStateOf(BlockState state) {
		return super.defaultStateOf(state).with(LIT, false);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(LIT);
	}
}
