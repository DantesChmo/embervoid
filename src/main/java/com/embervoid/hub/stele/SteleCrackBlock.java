package com.embervoid.hub.stele;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The crack of a lit Cleft Stele: the passage in its middle column, a thin sheet of mist like a nether
 * portal's. Walked through, not broken: it goes only when its stele is put out. Walking in is handed to
 * {@link CleftStele#onEnter}; a right click goes through its stele too ({@link SteleTravel#use}).
 */
public class SteleCrackBlock extends Block {
	public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;
	private static final VoxelShape ALONG_X = Block.createCuboidShape(0, 0, 6, 16, 16, 10);
	private static final VoxelShape ALONG_Z = Block.createCuboidShape(6, 0, 0, 10, 16, 16);

	public SteleCrackBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(AXIS, Direction.Axis.X));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(AXIS) == Direction.Axis.X ? ALONG_X : ALONG_Z;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (entity instanceof ServerPlayerEntity player && !player.hasVehicle()) {
			CleftStele.onEnter(player, pos);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		return use(world, pos, player);
	}

	/**
	 * A right click on a lit stele's stone or crack. The client cannot tell the world's stele from a dead one and takes
	 * every click as used; the server sends the player through if it is a working stele.
	 */
	static ActionResult use(World world, BlockPos pos, PlayerEntity player) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		return player instanceof ServerPlayerEntity serverPlayer && !serverPlayer.hasVehicle()
				&& SteleTravel.use(serverPlayer, pos) ? ActionResult.CONSUME : ActionResult.PASS;
	}

	@Override
	public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
		return ItemStack.EMPTY;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		boolean alongX = state.get(AXIS) == Direction.Axis.X;
		// Mist seeping out of the crack on both faces, ash and the odd ember rising through it.
		for (int i = 0; i < 2; i++) {
			double across = (random.nextBoolean() ? 1 : -1) * (0.15 + random.nextDouble() * 0.2);
			double along = random.nextDouble();
			double x = pos.getX() + (alongX ? along : 0.5 + across);
			double z = pos.getZ() + (alongX ? 0.5 + across : along);
			double y = pos.getY() + random.nextDouble();
			world.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0, 0, 0);
			if (random.nextInt(3) == 0) {
				world.addParticle(ParticleTypes.CLOUD, x, y, z, (alongX ? 0 : across) * 0.04, 0.005,
						(alongX ? across : 0) * 0.04);
			}
		}
		if (random.nextInt(5) == 0) {
			world.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + random.nextDouble(),
					pos.getZ() + 0.5, 0, 0.02, 0);
		}
		if (random.nextInt(120) == 0) {
			world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SteleSounds.AMBIENT,
					SoundCategory.BLOCKS, 0.4f, 0.8f + random.nextFloat() * 0.2f, false);
		}
	}
}
