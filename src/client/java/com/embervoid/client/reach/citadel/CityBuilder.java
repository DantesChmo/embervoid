package com.embervoid.client.reach.citadel;

import com.embervoid.reach.citadel.CitadelLayout;
import com.embervoid.realm.placed.PlacedEnemies;
import com.embervoid.realm.placed.PlacedEnemyMarkerBlock;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Random;

/**
 * Draws one Citadel piece's template: y = 0 is the piece's floor (the template's own y = 0 is
 * {@link CitadelLayout#FOUNDATION} lower), x and z run from the piece's min corner, z = 0 its north side. Seeded per
 * piece, so datagen output never changes between runs.
 */
final class CityBuilder {
	static final BlockState AIR = Blocks.AIR.getDefaultState();

	final TemplateBuilder t;
	final Random random;
	final int sizeX;
	final int sizeZ;
	/** Highest y above the floor the template holds. */
	final int top;

	CityBuilder(CitadelLayout.Piece piece) {
		this.t = new TemplateBuilder(piece.sizeX(), piece.sizeY(), piece.sizeZ()).baseY(CitadelLayout.FOUNDATION);
		this.random = new Random(piece.name().hashCode());
		this.sizeX = piece.sizeX();
		this.sizeZ = piece.sizeZ();
		this.top = piece.sizeY() - CitadelLayout.FOUNDATION - 1;
	}

	CityBuilder set(int x, int y, int z, BlockState state) {
		t.set(x, y, z, state);
		return this;
	}

	CityBuilder fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		t.fill(x0, y0, z0, x1, y1, z1, state);
		return this;
	}

	/** Fills a box, each block replaced by {@code alt} with the chance given. */
	CityBuilder fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state, BlockState alt, double altChance) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					t.set(x, y, z, random.nextDouble() < altChance ? alt : state);
				}
			}
		}
		return this;
	}

	CityBuilder clear(int x0, int y0, int z0, int x1, int y1, int z1) {
		return fill(x0, y0, z0, x1, y1, z1, AIR);
	}

	/** Foundation under the whole piece (y −3 to −1), so no edge ever hangs over a dip. */
	CityBuilder foundation(BlockState state) {
		return fill(0, -CitadelLayout.FOUNDATION, 0, sizeX - 1, -1, sizeZ - 1, state);
	}

	/** A floor layer (y 0) over a box: {@code main}, with {@code alt} and {@code rare} mixed in. */
	CityBuilder floor(int x0, int z0, int x1, int z1, BlockState main, BlockState alt, double altChance, BlockState rare,
			double rareChance) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				double roll = random.nextDouble();
				t.set(x, 0, z, roll < rareChance ? rare : roll < rareChance + altChance ? alt : main);
			}
		}
		return this;
	}

	/**
	 * The walls around a box from y0 to y1, more broken the higher they go: at the top a block is missing with chance
	 * {@code broken}, none are missing at y0. {@code alt} replaces one block in five.
	 */
	CityBuilder walls(int x0, int z0, int x1, int z1, int y0, int y1, BlockState state, BlockState alt, double broken) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (x != x0 && x != x1 && z != z0 && z != z1) {
					continue;
				}
				for (int y = y0; y <= y1; y++) {
					double missing = y1 == y0 ? broken : broken * (y - y0) / (y1 - y0);
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

	/** A layer at y over a box, each block there with chance {@code coverage}, air otherwise. */
	CityBuilder roof(int x0, int z0, int x1, int z1, int y, BlockState state, double coverage) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				t.set(x, y, z, random.nextDouble() < coverage ? state : AIR);
			}
		}
		return this;
	}

	/** Merlons on a line at y: pairs of {@code state}, then a pair of gaps. */
	CityBuilder merlons(int x0, int z0, int x1, int z1, int y, BlockState state) {
		int i = 0;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (i++ % 4 < 2) {
					t.set(x, y, z, state);
				}
			}
		}
		return this;
	}

	/** A stair block climbing towards {@code facing}. */
	static BlockState stairs(BlockState stairs, Direction facing) {
		return stairs.with(StairsBlock.FACING, facing);
	}

	/** {@code count} blocks of {@code state} on free floor spots (y) inside a box. */
	CityBuilder scatter(int x0, int z0, int x1, int z1, int y, BlockState state, int count) {
		for (int i = 0; i < count; i++) {
			t.set(x0 + random.nextInt(x1 - x0 + 1), y, z0 + random.nextInt(z1 - z0 + 1), state);
		}
		return this;
	}

	CityBuilder chest(int x, int y, int z, Direction facing, Identifier table) {
		t.chest(x, y, z, facing, table);
		return this;
	}

	/** A placed enemy (reach-world's marker) standing at x, y, z. */
	CityBuilder mob(int x, int y, int z, Identifier mob, Direction facing) {
		t.set(x, y, z, PlacedEnemies.MARKER.getDefaultState().with(PlacedEnemyMarkerBlock.FACING, facing),
				PlacedEnemies.markerNbt(mob, null, List.of()));
		return this;
	}

	TemplateBuilder build() {
		return t;
	}
}
