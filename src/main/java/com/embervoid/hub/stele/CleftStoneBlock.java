package com.embervoid.hub.stele;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Cleft Stone, the stone of a Cleft Stele (PLAN 15.3). Cold as crafted; {@link #KINDLED} once a Crying Soul lights
 * its stele, with glowing cracks. Losing a kindled stone puts the whole stele out ({@link CleftStele#putOut}).
 */
public class CleftStoneBlock extends Block {
	public static final BooleanProperty KINDLED = BooleanProperty.of("kindled");

	public CleftStoneBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(KINDLED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(KINDLED);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		super.onStateReplaced(state, world, pos, newState, moved);
		// Turning cold (newState is Cleft Stone again) is the put-out itself, not a loss.
		if (state.get(KINDLED) && !newState.isOf(this) && world instanceof ServerWorld serverWorld) {
			CleftStele.putOut(serverWorld, pos);
		}
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (!state.get(KINDLED) || random.nextInt(6) != 0) {
			return;
		}
		// An ember from a crack on a free side.
		Direction side = Direction.random(random);
		if (side == Direction.DOWN || world.getBlockState(pos.offset(side)).isOpaqueFullCube(world, pos.offset(side))) {
			return;
		}
		double x = pos.getX() + 0.5 + side.getOffsetX() * 0.52 + (side.getOffsetX() == 0 ? random.nextDouble() - 0.5 : 0);
		double y = pos.getY() + 0.5 + side.getOffsetY() * 0.52 + (side.getOffsetY() == 0 ? random.nextDouble() - 0.5 : 0);
		double z = pos.getZ() + 0.5 + side.getOffsetZ() * 0.52 + (side.getOffsetZ() == 0 ? random.nextDouble() - 0.5 : 0);
		world.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.01, 0);
	}
}
