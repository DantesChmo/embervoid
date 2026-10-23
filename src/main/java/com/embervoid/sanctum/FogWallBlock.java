package com.embervoid.sanctum;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The fog wall in the doorway of a Ruined Sanctum's boss arena (PLAN 16.1): a curtain of drifting ash in the same
 * particles as a boss fight's fog wall ({@link com.embervoid.boss.BossFight}). Nothing to see or hit but the ash: it
 * has no model, no outline and no collision, so players walk through it. Unbreakable, like the core blocks.
 */
public class FogWallBlock extends Block {
	public FogWallBlock(Settings settings) {
		super(settings);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		for (int i = 0; i < 6; i++) {
			world.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
					pos.getZ() + random.nextDouble(), 0, 0, 0);
		}
		if (random.nextInt(3) == 0) {
			world.addParticle(ParticleTypes.ASH, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
					pos.getZ() + random.nextDouble(), 0, 0.01, 0);
		}
	}
}
