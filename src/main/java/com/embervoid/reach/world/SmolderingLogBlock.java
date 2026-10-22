package com.embervoid.reach.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Smoldering log: a beam glowing inside, smoke rising from it (PLAN 9.5). */
public class SmolderingLogBlock extends PillarBlock {
	public SmolderingLogBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(3) == 0 && world.getBlockState(pos.up()).isAir()) {
			world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.0,
					pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.03, 0);
		}
	}
}
