package com.embervoid.block.prop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;

/**
 * A prop two blocks big, like a door or a bed: placing it sets both halves, losing one half breaks the other. Only the
 * primary half (the one placed at the cursor) drops the item; breaking the other in creative clears the primary
 * without a drop.
 */
public abstract class TwoBlockPropBlock extends PropBlock {
	protected TwoBlockPropBlock(Settings settings, String name) {
		super(settings, name);
	}

	/** Whether this is the half the player placed. */
	public abstract boolean isPrimary(BlockState state);

	/** Where the other half is. */
	public abstract Direction toPartner(BlockState state);

	/** The other half's state for this half. */
	public abstract BlockState partner(BlockState state);

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState state = super.getPlacementState(ctx);
		BlockPos other = ctx.getBlockPos().offset(toPartner(state));
		World world = ctx.getWorld();
		if (world.isOutOfHeightLimit(other) || !world.getBlockState(other).canReplace(ctx)) {
			return null;
		}
		return state;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		BlockPos other = pos.offset(toPartner(state));
		world.setBlockState(other, partner(state).with(WATERLOGGED, world.getFluidState(other).getFluid() == Fluids.WATER),
				Block.NOTIFY_ALL);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
			WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		if (direction == toPartner(state) && !(neighborState.isOf(this) && neighborState.get(FACING) == state.get(FACING)
				&& isPrimary(neighborState) != isPrimary(state))) {
			// Air, not water, even when waterlogged: only a change to air drops the primary half's item.
			return Blocks.AIR.getDefaultState();
		}
		return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient && player.isCreative() && !isPrimary(state)) {
			BlockPos primaryPos = pos.offset(toPartner(state));
			BlockState primary = world.getBlockState(primaryPos);
			if (primary.isOf(this) && isPrimary(primary)) {
				BlockState left = primary.get(WATERLOGGED) ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
				world.setBlockState(primaryPos, left, Block.NOTIFY_ALL | Block.SKIP_DROPS);
				world.syncWorldEvent(player, WorldEvents.BLOCK_BROKEN, primaryPos, Block.getRawIdFromState(primary));
			}
		}
		super.onBreak(world, pos, state, player);
	}
}
