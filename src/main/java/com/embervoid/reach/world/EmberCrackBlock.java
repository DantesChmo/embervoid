package com.embervoid.reach.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.MagmaBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Ember crack: burns whoever stands on it like magma, and spits sparks and crackles. */
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
		if (random.nextInt(100) == 0) {
			world.playSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ReachBlockSounds.EMBER_CRACK_AMBIENT, SoundCategory.BLOCKS,
					0.5f + random.nextFloat() * 0.3f, 0.9f + random.nextFloat() * 0.2f, false);
		}
	}
}
