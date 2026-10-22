package com.embervoid.reach.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.MagmaBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Ember crack: burns whoever stands on it like magma, and spits sparks (PLAN 9.5). */
public class EmberCrackBlock extends MagmaBlock {
	public EmberCrackBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(4) == 0 && world.getBlockState(pos.up()).isAir()) {
			world.addParticle(ParticleTypes.LAVA, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
					pos.getZ() + random.nextDouble(), 0, 0, 0);
		}
	}
}
