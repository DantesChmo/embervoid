package com.embervoid.client.reach.world;

import com.embervoid.block.prop.PropBlock;
import com.embervoid.block.prop.Props;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.Random;

/**
 * Drawing helpers for ruined templates (PLAN 9.7–9.8) on top of {@link TemplateBuilder}, with a seeded random, and
 * the zone palettes of the 0.5.0 art pass (docs/art-direction.md §3): every façade is a base course, a wall with
 * cracks growing upwards and moss at the foot, a trim, a detail, a roof and a blended floor, with the zone's accent
 * on a few spots.
 */
final class Ruin {
	static final BlockState AIR = Blocks.AIR.getDefaultState();

	/**
	 * A zone's materials: the base course, the wall and its cracked and mossy variants, the trim (pillars, jambs,
	 * corners), the detail (carved stone, a grate), the roof's stairs and slab, three floor materials (main, alt,
	 * rare), the accent block and the rubble that lies at the foot of a broken wall.
	 */
	record Palette(BlockState base, BlockState wall, BlockState cracked, BlockState mossy, BlockState trim, BlockState detail,
			BlockState roofStairs, BlockState roofSlab, BlockState floor, BlockState floorAlt, BlockState floorRare,
			BlockState accent, BlockState rubble) {
	}

	static final BlockState RUBBLE = Props.RUBBLE_PILE.getDefaultState();
	static final BlockState BLUE_CANDLE = Blocks.LIGHT_BLUE_CANDLE.getDefaultState().with(CandleBlock.LIT, true);

	/** Ashen Fields: burnt farm country. Soot on the bricks, thatch and dead wood, brass on the machinery. */
	static final Palette FIELDS = new Palette(ModBlocks.ASH_COBBLE_BRICKS.getDefaultState(), ModBlocks.ASH_BRICKS.getDefaultState(),
			ModBlocks.CRACKED_ASH_BRICKS.getDefaultState(), ModBlocks.SOOTY_ASH_BRICKS.getDefaultState(),
			ModBlocks.POLISHED_ASH_STONE.getDefaultState(), ModBlocks.CARVED_ASH_STONE.getDefaultState(),
			ModBlocks.DEAD_STAIRS.getDefaultState(), ModBlocks.DEAD_SLAB.getDefaultState(),
			ModBlocks.COBBLED_ASH_STONE.getDefaultState(), ReachBlocks.ASH_COBBLE_PATH.getDefaultState(), ModBlocks.ASH_SOIL.getDefaultState(),
			ReachBlocks.RUSTED_BRASS_BLOCK.getDefaultState(), RUBBLE);
	/** Royal Woods: the king's park gone to moss. Amber in the lodge, moss on everything low. */
	static final Palette WOODS = new Palette(ModBlocks.MOSSY_COBBLED_ASH_STONE.getDefaultState(), ModBlocks.ASH_BRICKS.getDefaultState(),
			ModBlocks.CRACKED_ASH_BRICKS.getDefaultState(), ModBlocks.MOSSY_ASH_BRICKS.getDefaultState(),
			ModBlocks.POLISHED_ASH_STONE.getDefaultState(), ModBlocks.CARVED_ASH_STONE.getDefaultState(),
			ModBlocks.SLATE_TILE_STAIRS.getDefaultState(), ModBlocks.SLATE_TILE_SLAB.getDefaultState(),
			ModBlocks.MOSSY_COBBLED_ASH_STONE.getDefaultState(), Blocks.PODZOL.getDefaultState(), Blocks.MOSS_BLOCK.getDefaultState(),
			ReachBlocks.AMBER_BLOCK.getDefaultState(), RUBBLE);
	/** Sunken Necropolis: wet stone, bone and blue candle light. */
	static final Palette NECROPOLIS = new Palette(ModBlocks.ASH_COBBLE_BRICKS.getDefaultState(), ReachBlocks.WET_CRYPT_STONE.getDefaultState(),
			ModBlocks.CRACKED_ASH_BRICKS.getDefaultState(), ModBlocks.MOSSY_ASH_BRICKS.getDefaultState(),
			ReachBlocks.BONE_BRICKS.getDefaultState(), ModBlocks.CARVED_ASH_STONE.getDefaultState(),
			ModBlocks.SLATE_TILE_STAIRS.getDefaultState(), ModBlocks.SLATE_TILE_SLAB.getDefaultState(),
			ReachBlocks.CRYPT_STONE_TILES.getDefaultState(), ModBlocks.ASH_TILES.getDefaultState(), ModBlocks.CRACKED_ASH_TILES.getDefaultState(),
			BLUE_CANDLE, RUBBLE);

	static Palette of(ReachZoneKit zone) {
		return switch (zone.name()) {
			case "fields" -> FIELDS;
			case "woods" -> WOODS;
			default -> NECROPOLIS;
		};
	}

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

	/**
	 * Walls of the palette round a box from y0 to y1: a base course, the wall cracking upwards and mossy at the foot,
	 * broken towards the top with chance {@code decay}, rubble dropped outside where a stretch has fallen.
	 */
	Ruin facade(Palette p, int x0, int z0, int x1, int z1, int y0, int y1, double decay) {
		t.weatheredWalls(x0, z0, x1, z1, y0, y1, random, decay, p.base(), 1, p.wall(), p.cracked(), p.mossy(), p.rubble());
		return this;
	}

	/** Trim pillars on the four corners of a box from y0 to y1, a detail block on top of each whole one. */
	Ruin cornerPiers(Palette p, int x0, int z0, int x1, int z1, int y0, int y1, double broken) {
		for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
			int top = random.nextDouble() < broken ? y0 + 1 + random.nextInt(Math.max(1, y1 - y0)) : y1;
			fill(c[0], y0, c[1], c[0], top, c[1], p.trim());
			if (top == y1) {
				t.trySet(c[0], y1 + 1, c[1], p.detail());
			}
		}
		return this;
	}

	/** A blended floor of the palette over a box at y: the main stone, its alt, the rare one. */
	Ruin floor(Palette p, int x0, int z0, int x1, int z1, int y) {
		t.blendedFloor(x0, z0, x1, z1, y, random, p.floor(), 6, p.floorAlt(), 3, p.floorRare(), 1);
		return this;
	}

	/** A pitched roof of the palette's stairs over a box, its ridge along the axis, with the coverage given. */
	Ruin roof(Palette p, int x0, int z0, int x1, int z1, int y, Direction.Axis axis, double coverage) {
		t.pitchedRoof(x0, z0, x1, z1, y, axis, p.roofStairs(), p.roofSlab(), random, coverage);
		return this;
	}

	/** A cornice of the palette's trim stairs round the box at y (the stairs of the base course family). */
	Ruin cornice(int x0, int z0, int x1, int z1, int y, BlockState stairs, BlockState corner) {
		t.cornice(x0, z0, x1, z1, y, stairs, corner);
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

	/** A flat roof of the palette's slab over a box, rotting away from one corner: {@code coverage} at most. */
	Ruin flatRoof(Palette p, int x0, int z0, int x1, int z1, int y, double coverage) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				double edge = Math.min(Math.min(x - x0, x1 - x), Math.min(z - z0, z1 - z));
				t.set(x, y, z, random.nextDouble() < coverage * (edge == 0 ? 0.7 : 1) ? p.roofSlab() : AIR);
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

	/** Rubble piles at the foot of a wall's outside: {@code count} of them along the box's edges, facing outwards. */
	Ruin rubbleAlong(int x0, int z0, int x1, int z1, int y, int count) {
		for (int i = 0; i < count; i++) {
			int side = random.nextInt(4);
			int x = side < 2 ? x0 + random.nextInt(x1 - x0 + 1) : side == 2 ? x0 - 1 : x1 + 1;
			int z = side >= 2 ? z0 + random.nextInt(z1 - z0 + 1) : side == 0 ? z0 - 1 : z1 + 1;
			Direction facing = side == 0 ? Direction.NORTH : side == 1 ? Direction.SOUTH : side == 2 ? Direction.WEST : Direction.EAST;
			if (t.inside(x, y, z) && (!t.isSet(x, y, z) || t.isAir(x, y, z))) {
				t.set(x, y, z, RUBBLE.with(PropBlock.FACING, facing));
			}
		}
		return this;
	}

	/** A log with its axis. */
	static BlockState log(BlockState log, Direction.Axis axis) {
		return log.with(PillarBlock.AXIS, axis);
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
