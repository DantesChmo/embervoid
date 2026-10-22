package com.embervoid.boss;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Collections;
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
		int perTick = (int) Math.ceil((double) pending.size() / Math.max(1, ticks - age));
		for (int i = 0; i < perTick && !pending.isEmpty(); i++) {
			BlockPos pos = pending.remove(pending.size() - 1);
			if (world.getBlockState(pos).isAir() || world.getBlockState(pos).isOf(Blocks.WATER)) {
				fight.blocks().set(pos, Blocks.WATER.getDefaultState());
			}
		}
		age++;
		return !pending.isEmpty();
	}

	/** Every air block from the floor up to the water surface over solid floor, in random order (so it rises evenly). */
	private List<BlockPos> columns(BossFight fight, Random random) {
		ServerWorld world = fight.world();
		BlockPos center = fight.center();
		List<BlockPos> result = new ArrayList<>();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) {
					continue;
				}
				pos.set(center.getX() + dx, center.getY(), center.getZ() + dz);
				BlockState floor = world.getBlockState(pos);
				if (!floor.isSideSolidFullSquare(world, pos, Direction.UP)) {
					continue;
				}
				for (int y = 1; y <= depth; y++) {
					BlockPos at = pos.up(y).toImmutable();
					BlockState state = world.getBlockState(at);
					if (state.isOf(Blocks.WATER)) {
						continue;
					}
					if (!state.isAir()) {
						break; // something stands here: raised ground keeps it dry
					}
					result.add(at);
				}
			}
		}
		// Shuffle, lowest layer last in the list so it is placed first.
		Collections.shuffle(result, new java.util.Random(random.nextLong()));
		result.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
		return result;
	}
}
