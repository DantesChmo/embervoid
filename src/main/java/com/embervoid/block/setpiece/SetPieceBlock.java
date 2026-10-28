package com.embervoid.block.setpiece;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * A moving set piece: a block whose look is a GeckoLib model on its {@link SetPieceBlockEntity}. Faces
 * the player who places it, stands in water, has the hitbox its {@link SetPieceKind} gives it and drops itself.
 * Nothing ticks on the server: an ALWAYS piece loops on the client, a TRIGGERED one changes state through
 * {@link SetPieces#trigger} and a timed state falls back through a scheduled block tick.
 */
public class SetPieceBlock extends BlockWithEntity implements Waterloggable, BlockEntityProvider {
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

	private final SetPieceKind kind;

	public SetPieceBlock(Settings settings, SetPieceKind kind) {
		super(settings);
		this.kind = kind;
		setDefaultState(defaultStateOf(getStateManager().getDefaultState()));
	}

	protected BlockState defaultStateOf(BlockState state) {
		return state.with(FACING, Direction.NORTH).with(WATERLOGGED, false);
	}

	public SetPieceKind kind() {
		return kind;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	@Nullable
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		World world = ctx.getWorld();
		if (!world.isClient && SetPieceCap.isFull(world, ctx.getBlockPos())) {
			PlayerEntity player = ctx.getPlayer();
			if (player != null) {
				player.sendMessage(Text.translatable("embervoid.setpiece.chunk_full"), true);
			}
			return null;
		}
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
				.with(WATERLOGGED, world.getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return kind.shape(state.get(FACING));
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
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

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
		super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
		if (kind.followsCampfire() && !world.isClient && sourcePos.equals(pos.down())
				&& world.getBlockEntity(pos) instanceof SetPieceBlockEntity piece) {
			piece.followCampfire();
		}
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onBlockAdded(state, world, pos, oldState, notify);
		if (kind.followsCampfire() && !world.isClient && world.getBlockEntity(pos) instanceof SetPieceBlockEntity piece) {
			piece.followCampfire();
		}
	}

	/** A timed state ran out: back to the kind's default state. */
	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getBlockEntity(pos) instanceof SetPieceBlockEntity piece) {
			piece.timedStateOver();
		}
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new SetPieceBlockEntity(pos, state);
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (!world.isClient || kind.puffs().isEmpty() || type != SetPieces.BLOCK_ENTITY) {
			return null;
		}
		return (w, p, s, be) -> ((SetPieceBlockEntity) be).clientTick();
	}
}
