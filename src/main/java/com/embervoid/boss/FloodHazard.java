package com.embervoid.boss;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Water rising in an arena (DESIGN §4.5, Crypt Keeper): over {@code ticks} ticks, every open floor column within
 * {@code radius} of the arena centre fills with water up to {@code depth} blocks above the floor, where nothing solid
 * stands. Raised ground (walkways, pedestals) stays dry. The water is real blocks through {@link BossFight#blocks},
 * so it drains when the fight ends.
 */
public class FloodHazard implements ArenaHazard {
	private final int radius;
	private final int depth;
	private final int ticks;
	private List<BlockPos> pending;
	private int age;

	/** {@code depth}: water surface in blocks above the floor block (1 or 2); {@code ticks}: how long the rise takes. */
	public FloodHazard(int radius, int depth, int ticks) {
		this.radius = radius;
		this.depth = depth;
		this.ticks = Math.max(1, ticks);
	}

	@Override
	public boolean tick(BossFight fight) {
		ServerWorld world = fight.world();
		if (pending == null) {
			pending = columns(fight, world.getRandom());
			world.playSound(null, fight.center(), SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.HOSTILE, 2.0f, 0.6f);
		}
		int perTick = FloodPlan.perTick(pending.size(), ticks - age);
		for (int i = 0; i < perTick && !pending.isEmpty(); i++) {
			BlockPos pos = pending.remove(pending.size() - 1);
			BlockState state = world.getBlockState(pos);
			if (state.isOf(Blocks.WATER)) {
				// Flowed here from this flood's own water: it was air before the fight.
				fight.blocks().rememberAs(pos, Blocks.AIR.getDefaultState());
			}
			if (state.isAir() || state.isOf(Blocks.WATER)) {
				fight.blocks().set(pos, Blocks.WATER.getDefaultState());
			}
		}
		age++;
		return !pending.isEmpty();
	}

	/**
	 * Ended before it finished (the fight is over): the spots it had not filled yet may hold water that spread there
	 * on its own; drain them too (the filled ones come back with the arena's blocks).
	 */
	@Override
	public void end(BossFight fight) {
		if (pending == null) {
			return;
		}
		for (BlockPos pos : pending) {
			if (fight.world().getBlockState(pos).isOf(Blocks.WATER)) {
				fight.world().setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
			}
		}
		pending.clear();
	}

	/** The flood's water spots in the arena now ({@link FloodPlan#columns}). */
	private List<BlockPos> columns(BossFight fight, Random random) {
		ServerWorld world = fight.world();
		FloodPlan.Ground ground = new FloodPlan.Ground() {
			@Override
			public boolean isSolidFloor(BlockPos pos) {
				return world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP);
			}

			@Override
			public FloodPlan.Space space(BlockPos pos) {
				BlockState state = world.getBlockState(pos);
				return state.isOf(Blocks.WATER) ? FloodPlan.Space.WATER : state.isAir() ? FloodPlan.Space.AIR : FloodPlan.Space.BLOCKED;
			}
		};
		return FloodPlan.columns(fight.center(), radius, depth, ground, random.nextLong());
	}
}
