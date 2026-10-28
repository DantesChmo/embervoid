package com.embervoid.reach.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Wet crypt stone: water drips from its underside, now and then audibly. */
public class WetStoneBlock extends Block {
	public WetStoneBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(6) == 0 && world.getBlockState(pos.down()).isAir()) {
			world.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + random.nextDouble(), pos.getY() - 0.05,
					pos.getZ() + random.nextDouble(), 0, 0, 0);
			if (random.nextInt(8) == 0) {
				world.playSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ReachBlockSounds.WET_CRYPT_STONE_DRIP, SoundCategory.BLOCKS,
						0.5f + random.nextFloat() * 0.3f, 0.9f + random.nextFloat() * 0.2f, false);
			}
		}
	}
}
