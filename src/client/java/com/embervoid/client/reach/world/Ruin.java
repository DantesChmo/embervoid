package com.embervoid.client.reach.world;

import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.Random;

/** Drawing helpers for ruined templates (PLAN 9.7–9.8) on top of {@link TemplateBuilder}, with a seeded random. */
final class Ruin {
	static final BlockState AIR = Blocks.AIR.getDefaultState();

	final TemplateBuilder t;
	final Random random;

	Ruin(String name, int sizeX, int sizeY, int sizeZ) {
		this.t = new TemplateBuilder(sizeX, sizeY, sizeZ);
		this.random = ReachZoneKit.random(name);
	}

	Ruin set(int x, int y, int z, BlockState state) {
		t.set(x, y, z, state);
		return this;
	}

	/** Fills a box. */
	Ruin fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		t.fill(x0, y0, z0, x1, y1, z1, state);
		return this;
	}

	/** Fills a box, each block replaced by {@code alt} with the chance given. */
	Ruin fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state, BlockState alt, double altChance) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					t.set(x, y, z, random.nextDouble() < altChance ? alt : state);
				}
			}
		}
		return this;
	}

	/** Air in a box (rooms keep out the terrain). */
	Ruin clear(int x0, int y0, int z0, int x1, int y1, int z1) {
		return fill(x0, y0, z0, x1, y1, z1, AIR);
	}

	/**
	 * The walls around a box, y0–y1, more broken the higher they go: at the top a block is missing with chance
	 * {@code decay}. {@code alt} replaces a block now and then (cracked bricks).
	 */
	Ruin walls(int x0, int z0, int x1, int z1, int y0, int y1, BlockState state, BlockState alt, double decay) {
		for (int y = y0; y <= y1; y++) {
			double missing = y1 == y0 ? decay : decay * (y - y0) / (y1 - y0);
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if (x != x0 && x != x1 && z != z0 && z != z1) {
						continue;
					}
					if (random.nextDouble() < missing) {
						t.set(x, y, z, AIR);
					} else {
						t.set(x, y, z, random.nextInt(5) == 0 ? alt : state);
					}
				}
			}
		}
		return this;
	}

	/** A roof layer at y over a box, each block there with chance {@code coverage}, air otherwise. */
	Ruin roof(int x0, int z0, int x1, int z1, int y, BlockState state, double coverage) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				t.set(x, y, z, random.nextDouble() < coverage ? state : AIR);
			}
		}
		return this;
	}

	/** {@code count} blocks of {@code state} on the floor (at y) inside a box, on free spots. */
	Ruin scatter(int x0, int z0, int x1, int z1, int y, BlockState state, int count) {
		for (int i = 0; i < count; i++) {
			t.set(x0 + random.nextInt(x1 - x0 + 1), y, z0 + random.nextInt(z1 - z0 + 1), state);
		}
		return this;
	}

	Ruin chest(int x, int y, int z, Direction facing, Identifier table) {
		t.chest(x, y, z, facing, table);
		return this;
	}

	Ruin mob(int x, int y, int z, Identifier mob, Direction facing, String... tags) {
		ReachZoneKit.mob(t, x, y, z, mob, facing, tags);
		return this;
	}

	TemplateBuilder build() {
		return t;
	}
}
