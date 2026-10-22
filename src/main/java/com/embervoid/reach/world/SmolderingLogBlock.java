package com.embervoid.reach.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Smoldering log: a beam glowing inside, smoke rising from it, crackling now and then (PLAN 9.5, R40.3a). */
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
		if (random.nextInt(30) == 0) {
			world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ReachBlockSounds.SMOLDERING_LOG_AMBIENT, SoundCategory.BLOCKS,
					0.5f + random.nextFloat() * 0.3f, 0.9f + random.nextFloat() * 0.2f, false);
		}
	}
}
