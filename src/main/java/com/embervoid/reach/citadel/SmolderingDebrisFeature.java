package com.embervoid.reach.citadel;

import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.Set;

/**
 * Feature {@code embervoid:smoldering_debris} (PLAN 13.1): what is left of the burnt houses of the lower city: a fallen
 * smouldering beam, a charred post still standing, or a heap of brick rubble. Placed on the natural ground only
 * ({@link #NATURAL_GROUND}), so it never lands on the landing or a Citadel piece. Smouldering logs generate only here.
 */
public class SmolderingDebrisFeature extends Feature<DefaultFeatureConfig> {
	/** The Burning City's surface blocks (the final zone's surface rule in noise settings {@code ashen_reach}). */
	public static final Set<BlockState> NATURAL_GROUND = Set.of(ModBlocks.ASH_SOIL.getDefaultState(),
			Blocks.BLACKSTONE.getDefaultState(), ModBlocks.COBBLED_ASH_STONE.getDefaultState(),
			ReachBlocks.EMBER_CRACK.getDefaultState());

	public SmolderingDebrisFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();
		if (!onGround(world, origin)) {
			return false;
		}
		int kind = random.nextInt(10);
		if (kind < 6) {
			return beam(world, origin, random);
		}
		if (kind < 8) {
			return post(world, origin, random);
		}
		return rubble(world, origin, random);
	}

	/** A beam of 3–6 smouldering logs lying along x or z; each log needs ground under it. */
	private static boolean beam(StructureWorldAccess world, BlockPos origin, Random random) {
		Direction direction = Direction.Type.HORIZONTAL.random(random);
		BlockState log = ReachBlocks.SMOLDERING_LOG.getDefaultState().with(PillarBlock.AXIS, direction.getAxis());
		int length = 3 + random.nextInt(4);
		int placed = 0;
		BlockPos.Mutable pos = origin.mutableCopy();
		for (int i = 0; i < length; i++) {
			if (!onGround(world, pos)) {
				break;
			}
			world.setBlockState(pos, log, 2);
			placed++;
			pos.move(direction);
		}
		return placed > 0;
	}

	/** A charred post 2–4 high, sometimes still smouldering at the top. */
	private static boolean post(StructureWorldAccess world, BlockPos origin, Random random) {
		int height = 2 + random.nextInt(3);
		for (int dy = 0; dy < height; dy++) {
			BlockPos pos = origin.up(dy);
			if (!world.isAir(pos)) {
				return dy > 0;
			}
			boolean top = dy == height - 1 && random.nextBoolean();
			world.setBlockState(pos, (top ? ReachBlocks.SMOLDERING_LOG : ModBlocks.DEAD_LOG).getDefaultState(), 2);
		}
		return true;
	}

	/** A low heap of ash bricks and cobbles, a few blocks across. */
	private static boolean rubble(StructureWorldAccess world, BlockPos origin, Random random) {
		BlockState[] blocks = {ModBlocks.ASH_BRICKS.getDefaultState(), ModBlocks.CRACKED_ASH_BRICKS.getDefaultState(),
				ModBlocks.COBBLED_ASH_STONE.getDefaultState(), ModBlocks.ASH_BRICK_SLAB.getDefaultState()};
		boolean placed = false;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos pos = origin.add(dx, 0, dz);
				if (random.nextInt(3) == 0 || !onGround(world, pos)) {
					continue;
				}
				world.setBlockState(pos, blocks[random.nextInt(blocks.length)], 2);
				if (dx == 0 && dz == 0 && random.nextBoolean() && world.isAir(pos.up())) {
					world.setBlockState(pos.up(), blocks[random.nextInt(blocks.length)], 2);
				}
				placed = true;
			}
		}
		return placed;
	}

	private static boolean onGround(StructureWorldAccess world, BlockPos pos) {
		return world.isAir(pos) && NATURAL_GROUND.contains(world.getBlockState(pos.down()));
	}
}
