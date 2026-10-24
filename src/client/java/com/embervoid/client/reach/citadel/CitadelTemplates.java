package com.embervoid.client.reach.citadel;

import com.embervoid.Embervoid;
import com.embervoid.block.setpiece.SetPieceBlock;
import com.embervoid.block.setpiece.SetPieces;
import com.embervoid.reach.citadel.CitadelLayout;
import com.embervoid.reach.citadel.CitadelLayout.Piece;
import com.embervoid.reach.gear.GearLootTables;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.embervoid.client.reach.citadel.CityBuilder.AIR;
import static com.embervoid.client.reach.citadel.CityBuilder.stairs;
import static com.embervoid.sanctum.datagen.TemplateBuilder.stair;

/**
 * The Smoldering Citadel's pieces (PLAN 13.2, DESIGN §4.5): {@code embervoid:ashen_reach/citadel/<name>}, one per
 * {@link CitadelLayout.Piece}, drawn for the layout's fixed orientation (the route enters each lower-city piece from
 * the south, +Z, and leaves north). Placed Citadel mobs per the bestiary: Sparking Marksmen on the walls and the
 * gatehouse, Furnace Golems in the inner ring, Smoldering Knights in the towers and the keep; Cinder Hounds spawn
 * naturally. Chests {@code citadel_common}, the keep's treasury {@code citadel_treasure}.
 */
public final class CitadelTemplates {
	static final Identifier MARKSMAN = Embervoid.id("sparking_marksman");
	static final Identifier GOLEM = Embervoid.id("furnace_golem");
	static final Identifier KNIGHT = Embervoid.id("smoldering_knight");

	private static final BlockState BLACK_BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
	private static final BlockState BLACK_CRACKED = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
	private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.getDefaultState();
	private static final BlockState BLACK_POLISHED = Blocks.POLISHED_BLACKSTONE.getDefaultState();
	private static final BlockState BLACK_CHISELED = Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
	private static final BlockState GILDED = ReachBlocks.GILDED_ASH_STONE.getDefaultState();
	private static final BlockState EMBER_GLASS = ReachBlocks.EMBER_GLASS.getDefaultState();
	private static final BlockState CHARRED = ReachBlocks.CHARRED_PLANKS.getDefaultState();
	private static final BlockState CHARRED_SLAB = ReachBlocks.CHARRED_SLAB.getDefaultState();
	private static final BlockState CHARRED_STAIRS = ReachBlocks.CHARRED_STAIRS.getDefaultState();
	private static final BlockState SLATE_STAIRS = ModBlocks.SLATE_TILE_STAIRS.getDefaultState();
	private static final BlockState SLATE_SLAB = ModBlocks.SLATE_TILE_SLAB.getDefaultState();
	private static final BlockState TILES = ModBlocks.ASH_TILES.getDefaultState();
	private static final BlockState CRACKED_TILES = ModBlocks.CRACKED_ASH_TILES.getDefaultState();
	private static final BlockState SOOTY = ModBlocks.SOOTY_ASH_BRICKS.getDefaultState();
	private static final BlockState COBBLE_BRICKS = ModBlocks.ASH_COBBLE_BRICKS.getDefaultState();
	private static final BlockState CARVED = ModBlocks.CARVED_ASH_STONE.getDefaultState();
	private static final BlockState PILLAR = ModBlocks.ASH_BRICK_PILLAR.getDefaultState();
	private static final BlockState TAR = ReachBlocks.TAR_BLOCK.getDefaultState();
	private static final BlockState IRON_PLATE = ReachBlocks.GRAVE_IRON_PLATE.getDefaultState();
	private static final BlockState GRATE = ReachBlocks.GRAVE_IRON_GRATE.getDefaultState();
	private static final BlockState CUT_BRASS = ReachBlocks.CUT_BRASS.getDefaultState();
	private static final BlockState BRASS = ReachBlocks.RUSTED_BRASS_BLOCK.getDefaultState();
	private static final BlockState PATH = ReachBlocks.ASH_COBBLE_PATH.getDefaultState();
	private static final BlockState BLACK_WALL = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState();
	private static final BlockState BLACK_STAIRS = Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.getDefaultState();
	private static final BlockState BLACK_SLAB = Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState();
	private static final BlockState BRICKS = ModBlocks.ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = ModBlocks.CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState BRICK_SLAB = ModBlocks.ASH_BRICK_SLAB.getDefaultState();
	private static final BlockState BRICK_WALL = ModBlocks.ASH_BRICK_WALL.getDefaultState();
	private static final BlockState COBBLES = ModBlocks.COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState COBBLE_STAIRS = ModBlocks.COBBLED_ASH_STONE_STAIRS.getDefaultState();
	private static final BlockState STONE = ModBlocks.ASH_STONE.getDefaultState();
	private static final BlockState POLISHED = ModBlocks.POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState CHISELED = ModBlocks.CHISELED_ASH_STONE.getDefaultState();
	private static final BlockState SOIL = ModBlocks.ASH_SOIL.getDefaultState();
	private static final BlockState PLANKS = ModBlocks.DEAD_PLANKS.getDefaultState();
	private static final BlockState PLANK_SLAB = ModBlocks.DEAD_SLAB.getDefaultState();
	private static final BlockState FENCE = ModBlocks.DEAD_FENCE.getDefaultState();
	private static final BlockState LOG = ModBlocks.DEAD_LOG.getDefaultState();
	private static final BlockState LANTERN = ModBlocks.EMBER_LANTERN.getDefaultState();
	private static final BlockState HANGING_LANTERN = LANTERN.with(LanternBlock.HANGING, true);
	private static final BlockState CRACK = ReachBlocks.EMBER_CRACK.getDefaultState();
	private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.getDefaultState();
	private static final BlockState CAMPFIRE = Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true);
	private static final BlockState BARS = Blocks.IRON_BARS.getDefaultState();
	private static final BlockState CHAIN = Blocks.CHAIN.getDefaultState().with(ChainBlock.AXIS, Direction.Axis.Y);
	private static final BlockState BARREL = Blocks.BARREL.getDefaultState();
	private static final BlockState CARPET = Blocks.BLACK_CARPET.getDefaultState();
	private static final BlockState RED_CARPET = Blocks.RED_CARPET.getDefaultState();

	public static Map<Identifier, TemplateBuilder> all() {
		Map<Identifier, TemplateBuilder> t = new LinkedHashMap<>();
		t.put(CitadelLayout.OUTSKIRTS.template(), outskirts(CitadelLayout.OUTSKIRTS));
		t.put(CitadelLayout.STREET.template(), street(CitadelLayout.STREET));
		t.put(CitadelLayout.MARKET.template(), market(CitadelLayout.MARKET));
		t.put(CitadelLayout.GATEHOUSE.template(), gatehouse(CitadelLayout.GATEHOUSE));
		t.put(CitadelLayout.WALL_WEST.template(), wall(CitadelLayout.WALL_WEST, false));
		t.put(CitadelLayout.WALL_EAST.template(), wall(CitadelLayout.WALL_EAST, true));
		t.put(CitadelLayout.TOWER_WEST.template(), tower(CitadelLayout.TOWER_WEST, false));
		t.put(CitadelLayout.TOWER_EAST.template(), tower(CitadelLayout.TOWER_EAST, true));
		t.put(CitadelLayout.INNER_RING_WEST.template(), innerRing(CitadelLayout.INNER_RING_WEST, false));
		t.put(CitadelLayout.INNER_RING_EAST.template(), innerRing(CitadelLayout.INNER_RING_EAST, true));
		t.put(CitadelLayout.KEEP.template(), keep(CitadelLayout.KEEP));
		return t;
	}

	static BlockState log(Direction.Axis axis) {
		return LOG.with(PillarBlock.AXIS, axis);
	}

	static BlockState smoldering(Direction.Axis axis) {
		return ReachBlocks.SMOLDERING_LOG.getDefaultState().with(PillarBlock.AXIS, axis);
	}

	// --- Lower city ---------------------------------------------------------------------------------------------

	/**
	 * The outskirts beside the landing: a road north between burnt shacks, a broken "city limits" arch. One step up
	 * from the landing at the south edge.
	 */
	static TemplateBuilder outskirts(Piece p) {
		CityBuilder c = new CityBuilder(p);
		c.foundation(STONE);
		c.floor(0, 0, 32, 32, SOIL, PATH, 0.3, TAR, 0.08);
		c.floor(13, 0, 19, 32, COBBLE_BRICKS, BRICKS, 0.35, CRACK, 0.05);
		for (int x = 13; x <= 19; x++) {
			c.set(x, 0, 32, stairs(COBBLE_STAIRS, Direction.NORTH));
		}
		// The arch: two pillars, a lintel with its middle fallen onto the road.
		c.fill(10, 1, 3, 12, 8, 4, BLACK_BRICKS, BLACK_CRACKED, 0.25);
		c.fill(20, 1, 3, 22, 8, 4, BLACK_BRICKS, BLACK_CRACKED, 0.25);
		c.pillar(10, 3, 1, 8).pillar(12, 3, 1, 8).pillar(20, 3, 1, 8).pillar(22, 3, 1, 8);
		c.fill(10, 9, 3, 13, 9, 4, BLACK_BRICKS).fill(19, 9, 3, 22, 9, 4, BLACK_BRICKS);
		c.set(13, 8, 3, stair(BLACK_STAIRS, Direction.WEST, true)).set(19, 8, 3, stair(BLACK_STAIRS, Direction.EAST, true));
		c.set(11, 10, 3, GILDED).set(21, 10, 3, GILDED).set(11, 11, 3, BLACK_WALL).set(21, 11, 3, BLACK_WALL);
		c.set(11, 5, 3, EMBER_GLASS).set(21, 5, 3, EMBER_GLASS);
		c.fill(14, 1, 6, 17, 1, 7, BLACK_BRICKS, BLACK_CRACKED, 0.5).set(15, 2, 6, BLACK_CRACKED);
		c.set(9, 1, 5, FENCE).set(9, 2, 5, LANTERN).set(23, 1, 5, FENCE).set(23, 2, 5, LANTERN);
		// Shacks: brick on the west, burnt plank on the east, open to the sky.
		shack(c, 1, 20, 8, 28, 4, BRICKS, CRACKED, Direction.EAST);
		c.set(4, 1, 24, CAMPFIRE).chest(2, 1, 27, Direction.EAST, GearLootTables.CITADEL_COMMON);
		c.sootOver(1, 1, 24, 3);
		c.fill(1, 4, 23, 8, 4, 23, smoldering(Direction.Axis.X), AIR, 0.35);
		c.t.pitchedRoof(1, 20, 8, 28, 5, Direction.Axis.X, SLATE_STAIRS, SLATE_SLAB, c.random, 0.3);
		shack(c, 1, 6, 9, 14, 5, BRICKS, CRACKED, Direction.EAST);
		c.clear(1, 1, 6, 1, 5, 14).scatter(2, 7, 8, 13, 1, CRACKED, 5);
		shack(c, 23, 18, 31, 27, 3, CHARRED, log(Direction.Axis.Y), Direction.WEST);
		c.fill(24, 1, 21, 29, 1, 21, smoldering(Direction.Axis.X)).set(27, 1, 25, MAGMA);
		c.t.pitchedRoof(23, 18, 31, 27, 4, Direction.Axis.Z, CHARRED_STAIRS, CHARRED_SLAB, c.random, 0.35);
		shack(c, 23, 4, 30, 12, 3, CHARRED, log(Direction.Axis.Y), Direction.WEST);
		c.fill(25, 1, 7, 25, 1, 10, smoldering(Direction.Axis.Z));
		// Beams and cinders along the road.
		c.fill(20, 1, 15, 20, 1, 18, smoldering(Direction.Axis.Z)).fill(11, 1, 26, 11, 2, 26, log(Direction.Axis.Y));
		c.set(12, 1, 12, CAMPFIRE);
		return c.build();
	}

	/** A small roofless house: walls, a doorway on one side, a little rubble. */
	private static void shack(CityBuilder c, int x0, int z0, int x1, int z1, int height, BlockState wall, BlockState alt,
			Direction door) {
		if (wall == CHARRED) {
			c.walls(x0, z0, x1, z1, 1, height, wall, alt, 0.5);
			c.fill(x0, 1, z0, x1, 1, z1, COBBLE_BRICKS, AIR, 0).fill(x0 + 1, 1, z0 + 1, x1 - 1, 1, z1 - 1, AIR);
			c.floor(x0 + 1, z0 + 1, x1 - 1, z1 - 1, CHARRED, SOIL, 0.4, TAR, 0.1);
		} else {
			c.facade(x0, z0, x1, z1, 1, height, 0.5);
			c.floor(x0 + 1, z0 + 1, x1 - 1, z1 - 1, TILES, CRACKED_TILES, 0.4, SOOTY, 0.1);
		}
		for (int[] corner : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
			c.fill(corner[0], 1, corner[1], corner[0], height, corner[1], wall == CHARRED ? log(Direction.Axis.Y) : PILLAR);
		}
		int mid = (z0 + z1) / 2;
		int x = door == Direction.EAST ? x1 : x0;
		c.clear(x, 1, mid, x, 3, mid + 1);
		c.set(x, 2, mid - 2, wall == CHARRED ? GRATE : EMBER_GLASS).set(x, 2, mid + 3, GRATE);
		c.scatter(x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, CRACKED, 2);
	}

	/**
	 * A burning street: a paved road between two rows of burnt townhouses (stone ground floors, plank upper storeys
	 * mostly gone), fallen beams across the road. Climbs two blocks from the outskirts at its south edge.
	 */
	static TemplateBuilder street(Piece p) {
		CityBuilder c = new CityBuilder(p);
		c.foundation(STONE);
		c.floor(0, 0, 32, 31, BRICKS, CRACKED, 0.3, TILES, 0.15);
		c.floor(12, 0, 20, 31, BLACK_POLISHED, BLACKSTONE, 0.3, CRACK, 0.06);
		c.floor(11, 0, 11, 31, IRON_PLATE, BLACK_POLISHED, 0.5, TAR, 0.1).floor(21, 0, 21, 31, IRON_PLATE, BLACK_POLISHED, 0.5, TAR, 0.1);
		// The two-block climb from the outskirts.
		for (int x = 0; x <= 32; x++) {
			c.set(x, 0, 32, AIR).set(x, -1, 32, stairs(COBBLE_STAIRS, Direction.NORTH));
			c.set(x, 0, 31, stairs(COBBLE_STAIRS, Direction.NORTH));
		}
		for (int[] row : new int[][]{{1, 9}, {11, 19}, {21, 29}}) {
			townhouse(c, 0, row[0], 10, row[1], Direction.EAST);
			townhouse(c, 22, row[0], 32, row[1], Direction.WEST);
		}
		// One house collapsed into the street.
		c.clear(22, 1, 11, 32, 9, 19).fill(23, 1, 12, 31, 1, 18, CRACKED, BRICKS, 0.4)
				.scatter(23, 12, 31, 18, 2, BLACK_CRACKED, 12).fill(21, 1, 13, 21, 1, 16, CRACKED);
		c.fill(12, 1, 15, 18, 1, 15, smoldering(Direction.Axis.X));
		c.fill(15, 1, 24, 20, 1, 24, smoldering(Direction.Axis.X)).set(14, 1, 24, CAMPFIRE);
		for (int z = 4; z <= 28; z += 8) {
			c.set(11, 1, z, FENCE).set(11, 2, z, FENCE).set(11, 3, z, LANTERN);
			if (z + 4 < 31) {
				c.set(21, 1, z + 4, FENCE).set(21, 2, z + 4, LANTERN);
			}
		}
		c.chest(2, 1, 15, Direction.EAST, GearLootTables.CITADEL_COMMON);
		return c.build();
	}

	/** A two-storey townhouse facing the road: blackstone ground floor with a door and windows, burnt plank storey. */
	private static void townhouse(CityBuilder c, int x0, int z0, int x1, int z1, Direction front) {
		c.facade(x0, z0, x1, z1, 1, 4, 0.15);
		c.floor(x0 + 1, z0 + 1, x1 - 1, z1 - 1, TILES, CRACKED_TILES, 0.3, BLACK_POLISHED, 0.2);
		c.t.stringCourse(x0, z0, x1, z1, 4, BLACK_SLAB);
		// The upper storey: charred timber frame, mostly gone; what is left of a slate roof over it.
		c.walls(x0, z0, x1, z1, 5, 9, CHARRED, log(Direction.Axis.Y), 0.75);
		for (int[] corner : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
			c.fill(corner[0], 5, corner[1], corner[0], 5 + c.random.nextInt(4), corner[1], ReachBlocks.CHARRED_LOG.getDefaultState());
		}
		c.roof(x0 + 1, z0 + 1, x1 - 1, z1 - 1, 4, CHARRED_SLAB.with(SlabBlock.TYPE, SlabType.TOP), 0.5);
		c.t.pitchedRoof(x0, z0, x1, z1, 10, Direction.Axis.Z, SLATE_STAIRS, SLATE_SLAB, c.random, 0.25);
		int fx = front == Direction.EAST ? x1 : x0;
		int mid = (z0 + z1) / 2;
		c.clear(fx, 1, mid, fx, 3, mid + 1);
		c.set(fx, 4, mid, stair(BLACK_STAIRS, front.getOpposite(), true)).set(fx, 4, mid + 1, stair(BLACK_STAIRS, front.getOpposite(), true));
		c.fill(fx, 2, z0 + 1, fx, 3, z0 + 2, EMBER_GLASS).fill(fx, 2, z1 - 2, fx, 3, z1 - 1, GRATE);
		c.scatter(x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, MAGMA, 2);
		c.set(fx == x1 ? x0 + 2 : x1 - 2, 1, mid, CAMPFIRE);
		c.sootOver(fx == x1 ? x0 : x1, 1, mid, 3);
	}

	/**
	 * The market square before the hill: stalls, a dry fountain glowing with embers, a toppled statue. Climbs two
	 * blocks from the street at its south edge; the gatehouse stairs start at its north edge.
	 */
	static TemplateBuilder market(Piece p) {
		CityBuilder c = new CityBuilder(p);
		c.foundation(STONE);
		c.floor(0, 0, 48, 23, BRICKS, TILES, 0.3, CRACK, 0.05);
		c.floor(10, 6, 38, 18, TILES, CRACKED_TILES, 0.3, BRICKS, 0.2);
		for (int x = 0; x <= 48; x++) {
			c.set(x, 0, 24, AIR).set(x, -1, 24, stairs(COBBLE_STAIRS, Direction.NORTH));
			c.set(x, 0, 23, stairs(COBBLE_STAIRS, Direction.NORTH));
		}
		// Fountain: a blackstone ring around ember-lit magma, a chiselled column with a fire on top.
		c.walls(21, 9, 27, 15, 1, 1, CUT_BRASS, BRASS, 0.3);
		c.fill(22, 0, 10, 26, 0, 14, MAGMA);
		c.pillar(24, 12, 1, 3).set(24, 4, 12, CAMPFIRE).set(24, 3, 12, GILDED);
		for (int[] s : new int[][]{{5, 3}, {13, 3}, {33, 3}, {41, 3}, {5, 16}, {41, 16}}) {
			stall(c, s[0], s[1]);
		}
		// The toppled statue of the king.
		c.fill(30, 1, 17, 34, 1, 18, PILLAR.with(PillarBlock.AXIS, Direction.Axis.X), CARVED, 0.3).set(35, 1, 17, GILDED).fill(29, 1, 16, 29, 2, 16, CHISELED).set(29, 3, 16, CARVED);
		// Low broken house fronts on both sides.
		c.facade(0, 0, 2, 22, 1, 4, 0.6);
		c.facade(46, 0, 48, 22, 1, 4, 0.6);
		c.pillar(2, 0, 1, 4).pillar(2, 22, 1, 4).pillar(46, 0, 1, 4).pillar(46, 22, 1, 4);
		c.fill(8, 1, 20, 12, 1, 20, smoldering(Direction.Axis.X));
		c.chest(43, 1, 18, Direction.WEST, GearLootTables.CITADEL_COMMON);
		return c.build();
	}

	private static void stall(CityBuilder c, int x, int z) {
		for (int[] d : new int[][]{{0, 0}, {3, 0}, {0, 2}, {3, 2}}) {
			c.set(x + d[0], 1, z + d[1], FENCE).set(x + d[0], 2, z + d[1], FENCE);
		}
		c.roof(x, z, x + 3, z + 2, 3, c.random.nextBoolean() ? CHARRED_SLAB : PLANK_SLAB, 0.55);
		c.set(x + 1, 1, z + 1, BARREL).set(x + 2, 1, z + 1, CARPET);
	}

	// --- City wall --------------------------------------------------------------------------------------------

	/**
	 * The gatehouse in the city wall, cut into the hill: an approach from the market between two barbican towers, a
	 * gate with a raised portcullis, a stair tunnel climbing to the plateau (y 14 = the plateau's ground), a small plaza
	 * before the courtyard, a roof level with the wall walk (y 22) and three Sparking Marksmen on it.
	 */
	static TemplateBuilder gatehouse(Piece p) {
		CityBuilder c = new CityBuilder(p);
		int plateau = CitadelLayout.PLATEAU_Y - p.floorY();
		int roof = plateau + 8;
		// The body inside the hill and under the wall line (z 8–27), solid; the plaza's ground (z 0–7).
		c.fill(0, -3, 8, 24, roof, 27, BLACK_BRICKS, BLACK_CRACKED, 0.2);
		c.fill(0, -3, 0, 24, plateau - 1, 7, STONE);
		c.fill(0, plateau, 0, 24, plateau, 7, TILES, CRACKED_TILES, 0.25);
		// Gilded string courses and ember-glass slits on the faces towards the market and the city.
		for (int x = 0; x <= 24; x += 4) {
			c.set(x, roof - 4, 27, GILDED).set(x, roof - 4, 8, GILDED);
			if (x % 8 == 0 && x + 2 <= 24) {
				c.set(x + 2, roof - 2, 27, EMBER_GLASS).set(x + 2, roof - 2, 8, EMBER_GLASS);
			}
		}
		c.clear(0, plateau + 1, 0, 24, c.top, 7);
		// Barbican towers flanking the approach (z 28–34), the approach itself climbing from the market.
		c.fill(0, -3, 28, 5, roof, 34, BLACK_BRICKS, BLACK_CRACKED, 0.2);
		c.fill(19, -3, 28, 24, roof, 34, BLACK_BRICKS, BLACK_CRACKED, 0.2);
		c.fill(6, -3, 28, 18, -1, 34, STONE);
		c.floor(6, 28, 18, 31, BLACK_POLISHED, BLACKSTONE, 0.3, CRACK, 0.04);
		c.clear(6, 1, 28, 18, c.top, 34);
		for (int x = 6; x <= 18; x++) {
			c.set(x, 0, 32, stairs(BLACK_STAIRS, Direction.NORTH));
			c.set(x, 0, 33, AIR).set(x, -1, 33, stairs(BLACK_STAIRS, Direction.NORTH));
			c.set(x, 0, 34, AIR).set(x, -1, 34, AIR).set(x, -2, 34, stairs(BLACK_STAIRS, Direction.NORTH));
		}
		// The gate through the wall line, portcullis raised.
		c.clear(9, 1, 22, 15, 6, 27);
		c.floor(9, 22, 15, 27, BLACK_POLISHED, BLACKSTONE, 0.3, BLACK_POLISHED, 0);
		c.fill(9, 5, 27, 15, 6, 27, GRATE);
		c.pillar(8, 27, 1, 6).pillar(16, 27, 1, 6).fill(9, 7, 27, 15, 7, 27, GILDED);
		c.set(12, 7, 27, SetPieces.PORTCULLIS.getDefaultState().with(SetPieceBlock.FACING, Direction.SOUTH)); // the gate's portcullis in the lintel, its gears on the face (PLAN 20.2)
		c.set(6, 1, 29, BLACK_WALL).set(6, 2, 29, LANTERN).set(18, 1, 29, BLACK_WALL).set(18, 2, 29, LANTERN);
		// The stair tunnel: one step up per row from z 21 to the plaza, headroom 5, ember cracks in the walls.
		for (int i = 0; i < plateau; i++) {
			int z = 21 - i;
			int y = 1 + i;
			for (int x = 9; x <= 15; x++) {
				c.set(x, y, z, stairs(BLACK_STAIRS, Direction.NORTH));
				c.clear(x, y + 1, z, x, y + 5, z);
			}
			if (i % 3 == 1) {
				c.set(8, y + 2, z, CRACK).set(16, y + 2, z, CRACK);
			}
		}
		c.clear(9, plateau + 1, 8, 15, plateau + 5, 8);
		// The roof: merlons on the outer edges, a parapet towards the city, a ladder down to the plaza.
		c.merlons(0, 34, 5, 34, roof + 1, BLACK_WALL).merlons(19, 34, 24, 34, roof + 1, BLACK_WALL);
		c.merlons(6, 27, 18, 27, roof + 1, BLACK_WALL);
		c.merlons(0, 28, 0, 33, roof + 1, BLACK_WALL).merlons(24, 28, 24, 33, roof + 1, BLACK_WALL);
		c.fill(5, roof + 1, 8, 19, roof + 1, 8, BLACK_WALL);
		for (int y = plateau + 1; y <= roof; y++) {
			c.set(3, y, 7, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
		}
		c.set(12, roof + 1, 20, CAMPFIRE).set(2, roof + 1, 20, LANTERN).set(22, roof + 1, 20, LANTERN);
		c.fill(10, roof + 1, 19, 14, roof + 1, 21, IRON_PLATE, TAR, 0.2).set(12, roof + 1, 20, CAMPFIRE);
		c.mob(3, roof + 1, 31, MARKSMAN, Direction.SOUTH).mob(21, roof + 1, 31, MARKSMAN, Direction.SOUTH)
				.mob(12, roof + 1, 25, MARKSMAN, Direction.SOUTH);
		// The plaza: lanterns, a brazier pair at the courtyard side.
		c.set(4, plateau + 1, 1, BLACK_WALL).set(4, plateau + 2, 1, LANTERN).set(20, plateau + 1, 1, BLACK_WALL)
				.set(20, plateau + 2, 1, LANTERN);
		return c.build();
	}

	/**
	 * A stretch of the city wall on the hill's south rim: 5 thick, walk on top (y 8, level with the gatehouse roof),
	 * merlons on the outer side, a stair up from the inner side, one breach, two Sparking Marksmen.
	 */
	static TemplateBuilder wall(Piece p, boolean east) {
		CityBuilder c = new CityBuilder(p);
		int last = p.sizeX() - 1;
		c.foundation(STONE);
		c.floor(0, 0, last, 0, BLACK_POLISHED, BLACKSTONE, 0.3, CRACK, 0.03);
		c.fill(0, 0, 1, last, 8, 5, BLACK_BRICKS, BLACK_CRACKED, 0.2);
		c.fill(0, 0, 5, last, 0, 5, BLACK_POLISHED).fill(0, 1, 5, last, 1, 5, SOOTY, BLACK_BRICKS, 0.5);
		c.fill(0, 8, 1, last, 8, 5, TILES, BLACK_POLISHED, 0.4);
		c.fill(0, 9, 5, last, 9, 5, BLACK_WALL);
		for (int x = 2; x <= last; x += 6) {
			c.set(x, 4, 5, EMBER_GLASS);
			c.set(x, 7, 5, stair(BLACK_STAIRS, Direction.NORTH, true));
		}
		c.merlons(0, 5, last, 5, 10, BLACK_WALL);
		// Stair from the city up to the walk, climbing away from the gatehouse.
		for (int i = 0; i < 8; i++) {
			int x = east ? 30 - i : 21 + i;
			if (i > 0) {
				c.fill(x, 1, 0, x, i, 0, BLACK_BRICKS);
			}
			c.set(x, 1 + i, 0, stairs(BLACK_STAIRS, east ? Direction.WEST : Direction.EAST));
		}
		// The breach: a two-block gap in the walk.
		int breach = east ? last - 16 : 15;
		c.clear(breach, 6, 1, breach + 1, 10, 5).fill(breach, 6, 1, breach + 1, 6, 5, BLACK_CRACKED, AIR, 0.4);
		for (int x = 5; x <= last; x += 20) {
			c.set(x, 10, 5, BLACK_WALL).set(x, 11, 5, LANTERN);
		}
		c.mob(east ? last - 10 : 10, 9, 3, MARKSMAN, Direction.SOUTH).mob(east ? last - 40 : 40, 9, 3, MARKSMAN, Direction.SOUTH);
		return c.build();
	}

	/**
	 * A corner tower of the wall: three floors (0, 8 = the wall walk's level, 15), roof at 21, a ladder up the inside,
	 * a door into the city on the north side, a doorway onto the wall walk. A Smoldering Knight on the top floor.
	 */
	static TemplateBuilder tower(Piece p, boolean east) {
		CityBuilder c = new CityBuilder(p);
		c.foundation(STONE);
		c.facade(0, 0, 11, 11, 0, 21, 0);
		c.pillar(0, 0, 0, 21).pillar(11, 0, 0, 21).pillar(0, 11, 0, 21).pillar(11, 11, 0, 21);
		for (int y : new int[]{0, 8, 15, 21}) {
			c.fill(1, y, 1, 10, y, 10, y == 21 ? BLACK_POLISHED : TILES, y == 21 ? TILES : CRACKED_TILES, 0.25);
		}
		c.t.stringCourse(0, 0, 11, 11, 8, BLACK_SLAB).stringCourse(0, 0, 11, 11, 15, BLACK_SLAB);
		c.t.cornice(0, 0, 11, 11, 21, BLACK_STAIRS, BLACK_POLISHED);
		for (int y : new int[]{5, 12, 19}) {
			c.set(3, y, 0, EMBER_GLASS).set(8, y, 0, EMBER_GLASS).set(3, y, 11, GRATE).set(8, y, 11, GRATE);
		}
		c.merlons(0, 0, 11, 0, 22, BLACK_WALL).merlons(0, 11, 11, 11, 22, BLACK_WALL)
				.merlons(0, 1, 0, 10, 22, BLACK_WALL).merlons(11, 1, 11, 10, 22, BLACK_WALL);
		// Ladder on the inside wall, holes in the floors above it.
		int lx = east ? 10 : 1;
		Direction ladderFacing = east ? Direction.WEST : Direction.EAST;
		for (int y = 1; y <= 21; y++) {
			c.set(lx, y, 3, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, ladderFacing));
		}
		c.set(lx, 22, 3, AIR);
		c.clear(5, 1, 0, 6, 3, 0); // door into the city
		int side = east ? 0 : 11;
		c.clear(side, 9, 7, side, 10, 9); // doorway onto the wall walk
		for (int y : new int[]{4, 11, 18}) {
			c.set(5, y, 11, AIR).set(east ? 11 : 0, y, 5, AIR);
		}
		c.set(east ? 2 : 9, 1, 9, CAMPFIRE).set(5, 9, 5, LANTERN).set(5, 16, 9, LANTERN);
		c.chest(east ? 2 : 9, 9, 9, east ? Direction.EAST : Direction.WEST, GearLootTables.CITADEL_COMMON);
		c.mob(6, 16, 6, KNIGHT, Direction.SOUTH);
		return c.build();
	}

	// --- Inner ring and keep -----------------------------------------------------------------------------------

	/**
	 * The inner ring beside the courtyard: a ring road along the courtyard side (east side for the west ring), a big hall
	 * (the royal foundry in the west, the barracks in the east) with Furnace Golems, burnt mansions at both ends.
	 */
	static TemplateBuilder innerRing(Piece p, boolean east) {
		CityBuilder c = new CityBuilder(p);
		int w = p.sizeX() - 1;
		int last = p.sizeZ() - 1;
		c.foundation(STONE);
		c.floor(0, 0, w, last, SOIL, PATH, 0.3, TAR, 0.1);
		int road0 = east ? 0 : w - 7;
		int road1 = road0 + 7;
		c.floor(road0, 0, road1, last, BLACK_BRICKS, BLACK_CRACKED, 0.3, CRACK, 0.04);
		int post = east ? road1 + 1 : road0 - 1;
		for (int z = 6; z <= last; z += 12) {
			c.set(post, 1, z, FENCE).set(post, 2, z, FENCE).set(post, 3, z, LANTERN);
		}
		// The hall (mirrored for the east ring): x 4–27 measured from the outer side.
		int h0 = east ? w - 27 : 4;
		int h1 = east ? w - 4 : 27;
		int door = east ? h0 : h1;
		c.floor(h0, 30, h1, 58, east ? TILES : IRON_PLATE, east ? CRACKED_TILES : BLACK_POLISHED, 0.3, MAGMA, 0.03);
		c.facade(h0, 30, h1, 58, 1, 10, 0.35);
		for (int z = 30; z <= 58; z += 7) {
			c.pillar(h0, z, 1, 10).pillar(h1, z, 1, 10);
			if (z > 30 && z < 58) {
				c.set(h0, 5, z - 3, EMBER_GLASS).set(h1, 5, z - 3, EMBER_GLASS).set(h0, 6, z - 3, EMBER_GLASS).set(h1, 6, z - 3, EMBER_GLASS);
			}
		}
		c.t.stringCourse(h0, 30, h1, 58, 7, BLACK_SLAB);
		for (int z = 34; z <= 54; z += 6) {
			c.fill(h0 + 1, 10, z, h1 - 1, 10, z, smoldering(Direction.Axis.X), AIR, 0.3);
			c.set((h0 + h1) / 2, 9, z, CHAIN).set((h0 + h1) / 2, 8, z, HANGING_LANTERN);
		}
		c.t.pitchedRoof(h0, 30, h1, 58, 11, Direction.Axis.Z, SLATE_STAIRS, SLATE_SLAB, c.random, 0.3);
		c.clear(door, 1, 42, door, 4, 45); // onto the ring road
		c.clear((h0 + h1) / 2 - 1, 1, 30, (h0 + h1) / 2 + 1, 4, 30); // north door
		int inner = east ? h1 - 1 : h0 + 1;
		Direction intoHall = east ? Direction.WEST : Direction.EAST;
		if (!east) {
			// The royal foundry: furnaces along the outer wall, anvils, a hearth of magma, lava cauldrons.
			for (int z = 33; z <= 55; z += 3) {
				c.set(inner, 1, z, Blocks.BLAST_FURNACE.getDefaultState().with(AbstractFurnaceBlock.FACING, intoHall)
						.with(AbstractFurnaceBlock.LIT, z % 2 == 1));
			}
			c.walls(13, 43, 17, 47, 1, 1, CUT_BRASS, BRASS, 0.4).fill(14, 0, 44, 16, 0, 46, MAGMA).set(15, 1, 45, CAMPFIRE);
			c.fill(13, 0, 43, 17, 0, 47, IRON_PLATE).fill(14, 0, 44, 16, 0, 46, MAGMA);
			for (int z = 33; z <= 55; z += 3) {
				c.sootOver(inner, 1, z, 4);
			}
			c.set(9, 1, 36, Blocks.DAMAGED_ANVIL.getDefaultState().with(AnvilBlock.FACING, Direction.NORTH))
					.set(9, 1, 52, Blocks.CHIPPED_ANVIL.getDefaultState().with(AnvilBlock.FACING, Direction.NORTH))
					.set(21, 1, 34, Blocks.LAVA_CAULDRON.getDefaultState()).set(22, 1, 34, Blocks.LAVA_CAULDRON.getDefaultState());
			c.chest(inner + 1, 1, 56, intoHall, GearLootTables.CITADEL_COMMON);
		} else {
			// The barracks: plank bunks along the outer wall, weapon racks, an armoury corner.
			for (int z = 33; z <= 55; z += 4) {
				c.fill(inner - 2, 1, z, inner, 1, z + 1, PLANK_SLAB.with(SlabBlock.TYPE, SlabType.TOP));
				c.fill(inner - 2, 2, z, inner, 2, z + 1, Blocks.GRAY_CARPET.getDefaultState());
			}
			c.fill(h0 + 6, 1, 32, h1 - 6, 2, 32, FENCE).fill(h0 + 6, 3, 32, h1 - 6, 3, 32, GRATE);
			c.set(h0 + 8, 1, 50, Blocks.DAMAGED_ANVIL.getDefaultState().with(AnvilBlock.FACING, Direction.EAST))
					.set((h0 + h1) / 2, 1, 44, CAMPFIRE);
			c.chest(inner - 1, 1, 56, intoHall, GearLootTables.CITADEL_COMMON);
		}
		int g0 = (h0 + h1) / 2;
		c.mob(g0 - 4, 1, 40, GOLEM, intoHall).mob(g0 + 4, 1, 51, GOLEM, intoHall);
		if (!east) {
			// The foundry breathes: bellows at the hearth, a winch on a beam over the floor (PLAN 20.2).
			c.set(15, 1, 42, SetPieces.FORGE_BELLOWS.getDefaultState().with(SetPieceBlock.FACING, Direction.SOUTH));
			c.set(h0 + 6, 10, 46, smoldering(Direction.Axis.X)).set(h0 + 6, 9, 46, SetPieces.CHAIN_WINCH.getDefaultState().with(SetPieceBlock.FACING, Direction.EAST));
		}
		// Burnt mansions at both ends.
		int m0 = east ? w - 24 : 3;
		int m1 = east ? w - 3 : 24;
		mansion(c, m0, 4, m1, 22);
		mansion(c, m0, 66, m1, 84);
		if (!east) {
			c.mob(road0 + 3, 1, 70, GOLEM, Direction.NORTH);
		}
		c.fill(road0 + 1, 1, 20, road0 + 5, 1, 20, smoldering(Direction.Axis.X));
		return c.build();
	}

	/** A burnt noble house: stone walls 7 high, an inner wall, half the upper floor fallen, a fire inside. */
	private static void mansion(CityBuilder c, int x0, int z0, int x1, int z1) {
		c.floor(x0, z0, x1, z1, TILES, CRACKED_TILES, 0.3, CRACK, 0.03);
		c.facade(x0, z0, x1, z1, 1, 7, 0.45);
		c.pillar(x0, z0, 1, 7).pillar(x1, z0, 1, 7).pillar(x0, z1, 1, 7).pillar(x1, z1, 1, 7);
		c.t.stringCourse(x0, z0, x1, z1, 4, BLACK_SLAB);
		int mid = (x0 + x1) / 2;
		c.walls(x0, z0, mid, z1, 1, 5, BRICKS, CRACKED, 0.4);
		int zm = (z0 + z1) / 2;
		c.clear(mid, 1, zm, mid, 3, zm + 1);
		c.clear(x0, 1, zm, x0, 3, zm + 1).clear(x1, 1, zm, x1, 3, zm + 1);
		c.set(x0, 4, zm, GILDED).set(x1, 4, zm, GILDED).set(x0, 4, zm + 1, GILDED).set(x1, 4, zm + 1, GILDED);
		c.set(x0, 2, zm - 3, EMBER_GLASS).set(x0, 3, zm - 3, EMBER_GLASS).set(x1, 2, zm + 4, EMBER_GLASS).set(x1, 3, zm + 4, EMBER_GLASS);
		c.roof(x0 + 1, z0 + 1, x1 - 1, z1 - 1, 4, CHARRED, 0.35);
		c.t.pitchedRoof(x0, z0, x1, z1, 8, Direction.Axis.Z, SLATE_STAIRS, SLATE_SLAB, c.random, 0.2);
		c.set(mid + 3, 1, z0 + 3, CAMPFIRE);
		c.sootOver(mid + 3, 2, z0 + 3, 2);
		c.scatter(x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, CRACKED, 6);
	}

	/**
	 * The keep behind the courtyard: a forecourt with braziers (its south edge meets the keep wall's gate at x 26–30),
	 * the great hall (19 high, two rows of pillars, ember lanterns on chains, a red runner), the king's empty throne on a
	 * dais, the treasury behind it with the {@code citadel_treasure} chest, corner turrets, two Smoldering Knights.
	 */
	static TemplateBuilder keep(Piece p) {
		CityBuilder c = new CityBuilder(p);
		int w = p.sizeX() - 1;
		int last = p.sizeZ() - 1;
		int mid = w / 2;
		c.foundation(STONE);
		c.floor(0, 0, w, last, BLACK_BRICKS, BLACK_CRACKED, 0.25, TILES, 0.15);
		// Curtain walls on the flanks.
		c.walls(0, 0, 1, last, 1, 6, BLACK_BRICKS, BLACK_CRACKED, 0.5);
		c.walls(w - 1, 0, w, last, 1, 6, BLACK_BRICKS, BLACK_CRACKED, 0.5);
		// The keep: walls 2 thick from y 0 to 20, ceiling at 20, merlons on top.
		int k0 = 6;
		int k1 = w - 6;
		c.fill(k0, 1, 4, k1, 20, 35, BLACK_BRICKS, BLACK_CRACKED, 0.15);
		c.fill(k0, 1, 4, k1, 1, 35, BLACK_POLISHED).fill(k0, 2, 4, k1, 2, 35, SOOTY, BLACK_BRICKS, 0.4);
		c.clear(k0 + 2, 1, 6, k1 - 2, 19, 33);
		c.fill(k0, 20, 4, k1, 20, 35, BLACK_POLISHED, BLACK_BRICKS, 0.3);
		c.roof(k0 + 3, 7, k1 - 3, 32, 20, TILES, 0.88);
		// Gilded string courses round the keep, ember-glass windows high in the flanks.
		c.t.stringCourse(k0, 4, k1, 35, 8, BLACK_SLAB).stringCourse(k0, 4, k1, 35, 16, BLACK_SLAB);
		for (int z = 7; z <= 32; z += 5) {
			c.set(k0, 8, z, GILDED).set(k1, 8, z, GILDED).set(k0, 16, z, GILDED).set(k1, 16, z, GILDED);
		}
		for (int x = k0 + 4; x <= k1 - 4; x += 6) {
			c.set(x, 17, 35, EMBER_GLASS).set(x, 18, 35, EMBER_GLASS).set(x, 17, 4, EMBER_GLASS);
		}
		c.merlons(k0, 4, k1, 4, 21, BLACK_WALL).merlons(k0, 35, k1, 35, 21, BLACK_WALL)
				.merlons(k0, 5, k0, 34, 21, BLACK_WALL).merlons(k1, 5, k1, 34, 21, BLACK_WALL);
		for (int[] t : new int[][]{{k0 - 2, 2}, {k1 - 2, 2}, {k0 - 2, 33}, {k1 - 2, 33}}) {
			c.fill(t[0], 1, t[1], t[0] + 4, 26, t[1] + 4, BLACK_BRICKS, BLACK_CRACKED, 0.2);
			c.merlons(t[0], t[1], t[0] + 4, t[1], 27, BLACK_WALL).merlons(t[0], t[1] + 4, t[0] + 4, t[1] + 4, 27, BLACK_WALL);
		}
		// Entrance in the south wall.
		c.clear(mid - 2, 1, 34, mid + 2, 6, 35).fill(mid - 2, 6, 35, mid + 2, 6, 35, GRATE);
		c.pillar(mid - 3, 35, 1, 7).pillar(mid + 3, 35, 1, 7).fill(mid - 2, 7, 35, mid + 2, 7, 35, GILDED).set(mid, 8, 35, CARVED);
		// Great hall: pillars, chandeliers, the runner, windows high up.
		c.floor(k0 + 2, 6, k1 - 2, 33, TILES, BLACK_POLISHED, 0.3, GILDED, 0.01);
		for (int z = 10; z <= 30; z += 5) {
			for (int x : new int[]{mid - 12, mid + 11}) {
				c.fill(x, 1, z, x + 1, 18, z + 1, BLACK_BRICKS, BLACK_CRACKED, 0.15).fill(x, 19, z, x + 1, 19, z + 1, BLACK_CHISELED);
				c.pillar(x, z, 1, 19).pillar(x + 1, z + 1, 1, 19);
				c.set(x, 2, z + 1, GILDED).set(x + 1, 2, z, GILDED).set(x, 18, z + 1, GILDED).set(x + 1, 18, z, GILDED);
			}
			c.fill(mid, 15, z, mid, 19, z, CHAIN).set(mid, 14, z, HANGING_LANTERN);
			c.t.soot(mid, 20, z, 1, SOOTY, c.random);
		}
		c.fill(mid - 1, 1, 15, mid + 1, 1, 33, RED_CARPET);
		c.fill(mid - 2, 0, 15, mid - 2, 0, 33, IRON_PLATE).fill(mid + 2, 0, 15, mid + 2, 0, 33, IRON_PLATE);
		for (int z = 9; z <= 30; z += 7) {
			c.fill(k0, 12, z, k0 + 1, 14, z, EMBER_GLASS).fill(k1 - 1, 12, z, k1, 14, z, EMBER_GLASS);
		}
		// The dais and the empty throne.
		c.fill(mid - 6, 1, 10, mid + 6, 1, 14, BLACK_POLISHED);
		c.fill(mid - 6, 1, 14, mid + 6, 1, 14, stairs(BLACK_STAIRS, Direction.NORTH));
		c.fill(mid - 3, 2, 10, mid + 3, 2, 12, GILDED);
		c.set(mid, 3, 11, stairs(BLACK_STAIRS, Direction.NORTH)).fill(mid, 4, 10, mid, 6, 10, GILDED);
		c.set(mid - 1, 3, 11, BLACK_WALL).set(mid + 1, 3, 11, BLACK_WALL);
		c.set(mid - 6, 2, 13, CAMPFIRE).set(mid + 6, 2, 13, CAMPFIRE);
		// The treasury behind the throne: the hoarded flame and the king's chest.
		c.walls(mid - 4, 6, mid + 4, 9, 1, 6, BLACK_BRICKS, GILDED, 0);
		c.fill(mid - 3, 6, 7, mid + 3, 6, 8, BLACK_POLISHED);
		c.clear(mid + 4, 1, 7, mid + 4, 2, 8).fill(mid + 4, 3, 7, mid + 4, 3, 8, GRATE);
		c.fill(mid - 3, 0, 7, mid - 2, 0, 8, MAGMA).set(mid - 3, 1, 8, CAMPFIRE).set(mid + 2, 1, 7, GILDED);
		c.chest(mid, 1, 7, Direction.SOUTH, GearLootTables.CITADEL_TREASURE);
		c.set(mid - 1, 1, 8, SetPieces.FORGE_BELLOWS.getDefaultState().with(SetPieceBlock.FACING, Direction.WEST)); // bellows at the treasury's hearth (PLAN 20.2)
		c.set(mid - 6, 3, 13, SetPieces.GREAT_BRAZIER.getDefaultState()).set(mid + 6, 3, 13, SetPieces.GREAT_BRAZIER.getDefaultState()); // great braziers over the dais fires
		// Side alcove with a common chest, braziers in the forecourt.
		c.chest(k0 + 3, 1, 31, Direction.EAST, GearLootTables.CITADEL_COMMON);
		for (int x : new int[]{mid - 6, mid + 6}) {
			c.set(x, 1, 40, BLACK_CHISELED).set(x, 2, 40, CAMPFIRE);
			c.fill(x - 1, 0, 39, x + 1, 0, 41, IRON_PLATE).set(x, 0, 40, TAR);
		}
		c.floor(mid - 10, 36, mid + 10, 43, TILES, CRACKED_TILES, 0.3, CRACK, 0.04);
		for (int x : new int[]{mid - 6, mid + 6}) {
			c.set(x, 3, 40, SetPieces.GREAT_BRAZIER.getDefaultState()); // over the forecourt fires (PLAN 20.2)
		}
		// A winch on a beam over the keep's door, its bucket hanging before the wall (PLAN 20.2).
		c.set(mid, 20, 36, smoldering(Direction.Axis.Z)).set(mid, 21, 36, SetPieces.CHAIN_WINCH.getDefaultState().with(SetPieceBlock.FACING, Direction.SOUTH));
		c.mob(mid - 7, 1, 20, KNIGHT, Direction.SOUTH).mob(mid + 7, 1, 20, KNIGHT, Direction.SOUTH);
		return c.build();
	}

	private CitadelTemplates() {}
}
