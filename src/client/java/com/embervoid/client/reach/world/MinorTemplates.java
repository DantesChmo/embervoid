package com.embervoid.client.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.block.prop.LongPropBlock;
import com.embervoid.block.prop.PropBlock;
import com.embervoid.block.prop.PropCandleBlock;
import com.embervoid.block.prop.Props;
import com.embervoid.block.prop.StackedPropBlock;
import com.embervoid.block.prop.TallPropBlock;
import com.embervoid.block.setpiece.SetPieceBlock;
import com.embervoid.block.setpiece.SetPieces;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BellBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.embervoid.sanctum.datagen.TemplateBuilder.slab;
import static com.embervoid.sanctum.datagen.TemplateBuilder.stair;

/**
 * Minor structures (DESIGN §4.5 "Zones and places"): {@code embervoid:ashen_reach/<zone>/<name>}, each a
 * single-piece jigsaw structure in its zone. y = 0 is the ground layer (except where a structure starts lower, see
 * {@link #START_DEPTH}); placed zone mobs and chests at the zone's table ids: treasure in the Fields granary vault and
 * the Woods hunting lodge.
 *
 * <p>Built to docs/art-direction.md §3 from the zone palettes in {@link Ruin}: a base course, walls that crack
 * upwards and go mossy at the foot, trim piers, a detail block, a roof of stairs, a blended floor, rubble where a
 * wall fell, soot over every fire, the zone's accent on a few spots.
 */
public final class MinorTemplates {
	/** Structures whose ground layer is not y = 0: the depth their template reaches below the ground. */
	public static final Map<String, Integer> START_DEPTH = Map.of("fields/granary_vault", 4, "necropolis/collapsed_tomb", 2);

	private static final BlockState BRICKS = ModBlocks.ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = ModBlocks.CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState SOOTY = ModBlocks.SOOTY_ASH_BRICKS.getDefaultState();
	private static final BlockState MOSSY = ModBlocks.MOSSY_ASH_BRICKS.getDefaultState();
	private static final BlockState COBBLES = ModBlocks.COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState MOSSY_COBBLES = ModBlocks.MOSSY_COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState COBBLE_BRICKS = ModBlocks.ASH_COBBLE_BRICKS.getDefaultState();
	private static final BlockState COBBLE_BRICK_STAIRS = ModBlocks.ASH_COBBLE_BRICK_STAIRS.getDefaultState();
	private static final BlockState COBBLE_BRICK_WALL = ModBlocks.ASH_COBBLE_BRICK_WALL.getDefaultState();
	private static final BlockState POLISHED = ModBlocks.POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState POLISHED_STAIRS = ModBlocks.POLISHED_ASH_STONE_STAIRS.getDefaultState();
	private static final BlockState POLISHED_SLAB = ModBlocks.POLISHED_ASH_STONE_SLAB.getDefaultState();
	private static final BlockState CHISELED = ModBlocks.CHISELED_ASH_STONE.getDefaultState();
	private static final BlockState CARVED = ModBlocks.CARVED_ASH_STONE.getDefaultState();
	private static final BlockState PILLAR = ModBlocks.ASH_BRICK_PILLAR.getDefaultState();
	private static final BlockState TILES = ModBlocks.ASH_TILES.getDefaultState();
	private static final BlockState CRACKED_TILES = ModBlocks.CRACKED_ASH_TILES.getDefaultState();
	private static final BlockState BRICK_STAIRS = ModBlocks.ASH_BRICK_STAIRS.getDefaultState();
	private static final BlockState BRICK_SLAB = ModBlocks.ASH_BRICK_SLAB.getDefaultState();
	private static final BlockState BRICK_WALL = ModBlocks.ASH_BRICK_WALL.getDefaultState();
	private static final BlockState MOSSY_WALL = ModBlocks.MOSSY_ASH_BRICK_WALL.getDefaultState();
	private static final BlockState SLATE_STAIRS = ModBlocks.SLATE_TILE_STAIRS.getDefaultState();
	private static final BlockState SLATE_SLAB = ModBlocks.SLATE_TILE_SLAB.getDefaultState();
	private static final BlockState PLANKS = ModBlocks.DEAD_PLANKS.getDefaultState();
	private static final BlockState PLANK_STAIRS = ModBlocks.DEAD_STAIRS.getDefaultState();
	private static final BlockState PLANK_SLAB = ModBlocks.DEAD_SLAB.getDefaultState();
	private static final BlockState FENCE = ModBlocks.DEAD_FENCE.getDefaultState();
	private static final BlockState LOG = ModBlocks.DEAD_LOG.getDefaultState();
	private static final BlockState STRIPPED = ModBlocks.STRIPPED_DEAD_LOG.getDefaultState();
	private static final BlockState CHARRED = ReachBlocks.CHARRED_PLANKS.getDefaultState();
	private static final BlockState CHARRED_LOG = ReachBlocks.CHARRED_LOG.getDefaultState();
	private static final BlockState LANTERN = ModBlocks.EMBER_LANTERN.getDefaultState();
	private static final BlockState STRAW = ReachBlocks.BURNT_STRAW.getDefaultState();
	private static final BlockState THATCH = ReachBlocks.BURNT_THATCH.getDefaultState();
	private static final BlockState WET = ReachBlocks.WET_CRYPT_STONE.getDefaultState();
	private static final BlockState CRYPT_TILES = ReachBlocks.CRYPT_STONE_TILES.getDefaultState();
	private static final BlockState BONE_BRICKS = ReachBlocks.BONE_BRICKS.getDefaultState();
	private static final BlockState BONE_WALL = ReachBlocks.BONE_BRICK_WALL.getDefaultState();
	private static final BlockState BONE_PILLAR = ReachBlocks.BONE_PILLAR.getDefaultState();
	private static final BlockState GRATE = ReachBlocks.GRAVE_IRON_GRATE.getDefaultState();
	private static final BlockState BRASS = ReachBlocks.RUSTED_BRASS_BLOCK.getDefaultState();
	private static final BlockState CUT_BRASS = ReachBlocks.CUT_BRASS.getDefaultState();
	private static final BlockState AMBER = ReachBlocks.AMBER_BLOCK.getDefaultState();
	private static final BlockState PATH = ReachBlocks.ASH_COBBLE_PATH.getDefaultState();
	private static final BlockState SOIL = ModBlocks.ASH_SOIL.getDefaultState();
	private static final BlockState COARSE = Blocks.COARSE_DIRT.getDefaultState();
	private static final BlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
	private static final BlockState BONES = Blocks.BONE_BLOCK.getDefaultState();
	private static final BlockState SKULL = Blocks.SKELETON_SKULL.getDefaultState();
	private static final BlockState BARS = Blocks.IRON_BARS.getDefaultState();
	private static final BlockState CHAIN = Blocks.CHAIN.getDefaultState().with(ChainBlock.AXIS, Direction.Axis.Y);
	private static final BlockState WOOL = Blocks.BROWN_WOOL.getDefaultState();
	private static final BlockState CANDLE = Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true);
	private static final BlockState BLUE_CANDLE = Ruin.BLUE_CANDLE;
	private static final BlockState COLD_FIRE = Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false);

	public static Map<Identifier, TemplateBuilder> all() {
		Map<Identifier, TemplateBuilder> t = new LinkedHashMap<>();
		ReachZoneKit f = ReachZoneKit.FIELDS;
		put(t, "fields/farmstead", farmstead(f));
		put(t, "fields/barn", barn(f));
		put(t, "fields/windmill_skeleton", windmill(f));
		put(t, "fields/scarecrow_row", scarecrows(f));
		put(t, "fields/granary_vault", granaryVault(f));
		put(t, "fields/pilgrim_camp", pilgrimCamp(f));
		put(t, "fields/gallows", gallows(f));
		put(t, "fields/village_altar", villageAltar(f));
		ReachZoneKit w = ReachZoneKit.WOODS;
		put(t, "woods/park_wall", parkWall(w));
		put(t, "woods/hunting_lodge", huntingLodge(w));
		put(t, "woods/kennels", kennels(w));
		put(t, "woods/hunters_hide", huntersHide(w));
		put(t, "woods/hanging_cages", hangingCages(w));
		put(t, "woods/lantern_row", lanternRow(w));
		put(t, "woods/glade", glade(w));
		ReachZoneKit n = ReachZoneKit.NECROPOLIS;
		put(t, "necropolis/mausoleum_small", mausoleum(n, false));
		put(t, "necropolis/mausoleum_large", mausoleum(n, true));
		put(t, "necropolis/grave_rows", graveRows(n));
		put(t, "necropolis/chapel", chapel(n));
		put(t, "necropolis/ossuary_pit", ossuaryPit(n));
		put(t, "necropolis/collapsed_tomb", collapsedTomb(n));
		return t;
	}

	private static void put(Map<Identifier, TemplateBuilder> t, String name, TemplateBuilder template) {
		t.put(Embervoid.id("ashen_reach/" + name), template);
	}

	private static BlockState log(Direction.Axis axis) {
		return LOG.with(PillarBlock.AXIS, axis);
	}

	private static BlockState charredLog(Direction.Axis axis) {
		return CHARRED_LOG.with(PillarBlock.AXIS, axis);
	}

	private static void tall(Ruin r, int x, int y, int z, TallPropBlock block, Direction facing) {
		BlockState lower = block.getDefaultState().with(PropBlock.FACING, facing);
		r.set(x, y, z, lower.with(TallPropBlock.HALF, DoubleBlockHalf.LOWER)).set(x, y + 1, z, lower.with(TallPropBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** A two-block-long prop: its foot at (x, y, z), its head one block on in {@code facing}. */
	private static void longProp(Ruin r, int x, int y, int z, LongPropBlock block, Direction facing) {
		BlockState foot = block.getDefaultState().with(PropBlock.FACING, facing);
		r.set(x, y, z, foot.with(LongPropBlock.PART, BedPart.FOOT))
				.set(x + facing.getOffsetX(), y, z + facing.getOffsetZ(), foot.with(LongPropBlock.PART, BedPart.HEAD));
	}

	/** Timber-frame walls: a plank wall with log studs every third block, the top courses burnt and broken. */
	private static void timberWalls(Ruin r, int x0, int z0, int x1, int z1, int y0, int y1, double decay) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (x != x0 && x != x1 && z != z0 && z != z1) {
					continue;
				}
				boolean stud = (x == x0 || x == x1) ? (z - z0) % 3 == 0 : (x - x0) % 3 == 0;
				boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
				int broken = 0;
				for (int y = y0; y <= y1; y++) {
					double t = y1 == y0 ? 1 : (double) (y - y0) / (y1 - y0);
					if (broken > 0 || r.random.nextDouble() < decay * t * t * (stud ? 0.4 : 1)) {
						r.set(x, y, z, Ruin.AIR);
						broken++;
						continue;
					}
					if (y == y0) {
						r.set(x, y, z, COBBLE_BRICKS);
					} else if (stud || corner) {
						r.set(x, y, z, t > 0.6 && r.random.nextInt(3) == 0 ? charredLog(Direction.Axis.Y) : log(Direction.Axis.Y));
					} else {
						r.set(x, y, z, t > 0.5 && r.random.nextInt(2) == 0 ? CHARRED : PLANKS);
					}
				}
			}
		}
	}

	// --- Ashen Fields -------------------------------------------------------------------------------------------

	/** A burnt farmhouse: timber-frame walls on a cobble course, half the thatch gone, a brass pot on the hearth. */
	static TemplateBuilder farmstead(ReachZoneKit z) {
		Ruin r = new Ruin("farmstead", 13, 9, 11);
		r.t.blendedFloor(0, 0, 12, 10, 0, r.random, SOIL, 5, PATH, 2, COARSE, 2);
		r.t.blendedFloor(1, 1, 9, 7, 0, r.random, PLANKS, 5, CHARRED, 3, COBBLES, 1);
		r.clear(2, 1, 2, 8, 7, 6);
		timberWalls(r, 1, 1, 9, 7, 1, 4, 0.55);
		r.clear(5, 1, 1, 5, 2, 1);
		r.set(4, 1, 1, log(Direction.Axis.Y)).set(6, 1, 1, log(Direction.Axis.Y)).set(5, 3, 1, log(Direction.Axis.X));
		// A thatched roof, pitched along x; its north half burnt through.
		r.t.pitchedRoof(1, 1, 9, 7, 5, Direction.Axis.X, PLANK_STAIRS, PLANK_SLAB, r.random, 0.45);
		for (int x = 2; x <= 8; x++) {
			for (int zz : new int[]{6, 7}) {
				if (r.random.nextDouble() < 0.6) {
					r.set(x, 5 + (7 - zz), zz, THATCH.with(PillarBlock.AXIS, Direction.Axis.X));
				}
			}
		}
		r.fill(5, 5, 4, 5, 7, 4, charredLog(Direction.Axis.Y)).set(5, 8, 4, CHARRED_LOG);  // the ridge post, still smoking black
		// Inside: hearth with a brass pot, the table, the stores.
		r.set(3, 1, 3, COBBLE_BRICKS).set(3, 2, 3, COLD_FIRE).set(3, 3, 3, CUT_BRASS);
		r.t.soot(3, 3, 3, 2, SOOTY, r.random);
		r.set(2, 1, 3, COBBLE_BRICKS).set(2, 2, 3, COBBLE_BRICK_WALL);
		r.chest(2, 1, 6, Direction.EAST, z.commonChest());
		r.set(8, 1, 6, Blocks.CRAFTING_TABLE.getDefaultState());
		r.set(8, 1, 2, Props.GRAIN_SACKS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.WEST));
		r.set(7, 1, 2, Props.BURNT_BARREL.getDefaultState());
		// Outside: bales, a broken cart, the furrows the stalker hides in.
		r.set(11, 1, 2, STRAW).set(11, 2, 2, STRAW).set(10, 1, 9, STRAW.with(PillarBlock.AXIS, Direction.Axis.X));
		longProp(r, 11, 1, 4, Props.BROKEN_CART, Direction.SOUTH);
		r.fill(10, 0, 5, 12, 0, 10, COARSE, SOIL, 0.4);
		r.rubbleAlong(1, 1, 9, 7, 1, 3);
		r.mob(4, 1, 4, z.fodder(), Direction.NORTH).mob(7, 1, 5, z.fodder(), Direction.WEST);
		r.mob(11, 1, 7, z.tank(), Direction.NORTH);
		return r.build();
	}

	/** A plank barn on log posts, the thatched roof mostly gone, burnt bales stacked inside, a brass winch. */
	static TemplateBuilder barn(ReachZoneKit z) {
		Ruin r = new Ruin("barn", 15, 10, 11);
		r.t.blendedFloor(0, 0, 14, 10, 0, r.random, COARSE, 4, SOIL, 3, PATH, 2, CHARRED, 1);
		r.clear(1, 1, 1, 13, 8, 9);
		timberWalls(r, 0, 0, 14, 10, 1, 6, 0.6);
		for (int x : new int[]{0, 7, 14}) {
			r.fill(x, 1, 0, x, 6, 0, log(Direction.Axis.Y));
			r.fill(x, 1, 10, x, 6, 10, log(Direction.Axis.Y));
		}
		r.clear(6, 1, 0, 8, 4, 0);
		r.set(5, 1, 0, log(Direction.Axis.Y)).set(9, 1, 0, log(Direction.Axis.Y)).fill(5, 5, 0, 9, 5, 0, log(Direction.Axis.X));
		// Tie beams across, two fallen; a thatched roof pitched along x with the middle burnt out.
		for (int x = 3; x <= 11; x += 4) {
			r.fill(x, 6, 1, x, 6, 9, x == 7 ? charredLog(Direction.Axis.Z) : log(Direction.Axis.Z));
		}
		r.fill(2, 1, 4, 2, 1, 8, log(Direction.Axis.Z));
		r.t.pitchedRoof(0, 0, 14, 10, 7, Direction.Axis.X, PLANK_STAIRS, PLANK_SLAB, r.random, 0.3);
		for (int x = 0; x <= 14; x++) {
			for (int zz : new int[]{0, 10}) {
				if (r.random.nextDouble() < 0.5) {
					r.set(x, 7, zz, THATCH.with(PillarBlock.AXIS, Direction.Axis.X));
				}
			}
		}
		// A hoist over the barn door, its crate swaying.
		r.set(7, 6, 0, SetPieces.GRANARY_HOIST.getDefaultState().with(SetPieceBlock.FACING, Direction.NORTH));
		r.fill(1, 1, 7, 3, 2, 9, STRAW);
		r.fill(11, 1, 7, 13, 3, 9, STRAW, Ruin.AIR, 0.3);
		r.fill(5, 1, 8, 6, 1, 9, THATCH.with(PillarBlock.AXIS, Direction.Axis.Z));
		// The winch: a brass drum on a frame by the door, a chain down.
		r.set(10, 4, 1, BRASS).set(10, 3, 1, CHAIN).set(10, 2, 1, Props.IRON_CAGE.getDefaultState());
		r.chest(12, 1, 2, Direction.WEST, z.commonChest());
		r.set(1, 1, 2, Props.GRAIN_SACKS.getDefaultState().with(StackedPropBlock.COUNT, 3).with(PropBlock.FACING, Direction.EAST));
		r.set(1, 1, 3, Props.BURNT_BARREL.getDefaultState());
		r.rubbleAlong(0, 0, 14, 10, 1, 2);
		r.mob(4, 1, 3, z.fodder(), Direction.NORTH).mob(9, 1, 5, z.fodder(), Direction.NORTH)
				.mob(7, 1, 8, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/**
	 * The windmill's stone base, its platform and the turning sail wheel; a carrion perched on the platform.
	 */
	static TemplateBuilder windmill(ReachZoneKit z) {
		Ruin r = new Ruin("windmill", 9, 18, 9);
		Ruin.Palette p = Ruin.FIELDS;
		r.t.blendedFloor(1, 1, 7, 7, 0, r.random, PATH, 3, SOIL, 2, COBBLES, 1);
		r.fill(2, 0, 2, 6, 0, 6, COBBLE_BRICKS);
		r.clear(3, 1, 3, 5, 7, 5);
		r.facade(p, 2, 2, 6, 6, 1, 7, 0.25);
		r.cornerPiers(p, 2, 2, 6, 6, 1, 7, 0.0);
		r.t.stringCourse(2, 2, 6, 6, 4, ModBlocks.ASH_COBBLE_BRICK_SLAB.getDefaultState());
		r.clear(4, 1, 2, 4, 2, 2);
		r.set(4, 3, 2, stair(COBBLE_BRICK_STAIRS, Direction.SOUTH, true));
		r.set(2, 1, 4, GRATE).set(6, 2, 4, GRATE);
		// Inside: the millstones, a brass gear on the shaft, sacks.
		r.set(4, 1, 4, CUT_BRASS).set(4, 2, 4, Blocks.GRINDSTONE.getDefaultState());
		r.set(3, 1, 3, Props.GRAIN_SACKS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.SOUTH));
		// The platform: planks over a cornice, a railing, the shaft and the sail frame.
		r.cornice(2, 2, 6, 6, 8, COBBLE_BRICK_STAIRS, COBBLE_BRICKS);
		r.roof(2, 2, 6, 6, 8, PLANKS, 0.7);
		r.set(4, 8, 4, PLANKS);
		for (int i = 2; i <= 6; i++) {
			r.t.trySet(i, 9, 2, i == 4 ? Ruin.AIR : FENCE).trySet(i, 9, 6, FENCE).trySet(2, 9, i, FENCE).trySet(6, 9, i, FENCE);
		}
		r.set(4, 9, 4, BRASS).fill(4, 9, 1, 4, 9, 3, log(Direction.Axis.Z));
		r.set(4, 10, 4, PLANK_SLAB);
		r.fill(4, 4, 0, 4, 16, 0, FENCE);
		r.fill(0, 10, 0, 8, 10, 0, FENCE);
		r.set(4, 10, 0, BRASS);
		// The sail wheel: the fence frame goes, the turning wheel hangs on the axle's end.
		r.clear(4, 4, 0, 4, 16, 0).clear(0, 10, 0, 8, 10, 0);
		r.set(4, 9, 0, SetPieces.WINDMILL_SAILS.getDefaultState().with(SetPieceBlock.FACING, Direction.NORTH));
		r.chest(3, 1, 5, Direction.EAST, z.commonChest());
		r.rubbleAlong(2, 2, 6, 6, 1, 2);
		r.mob(4, 9, 4, z.ranged(), Direction.NORTH).mob(4, 1, 4, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** Furrows with four scarecrows of burnt straw; stalkers under the furrows, a carrion on a scarecrow. */
	static TemplateBuilder scarecrows(ReachZoneKit z) {
		Ruin r = new Ruin("scarecrows", 17, 6, 7);
		for (int zz = 0; zz < 7; zz++) {
			r.t.blendedFloor(0, zz, 16, zz, 0, r.random, zz % 2 == 0 ? COARSE : SOIL, 7, zz % 2 == 0 ? SOIL : PATH, 2, Blocks.DIRT.getDefaultState(), 1);
		}
		for (int x : new int[]{2, 6, 10, 14}) {
			if (x == 10) {
				tall(r, x, 1, 3, Props.SCARECROW, Direction.NORTH);
				continue;
			}
			r.set(x, 1, 3, FENCE).set(x, 2, 3, STRAW);
			r.set(x - 1, 2, 3, FENCE).set(x + 1, 2, 3, FENCE);
			r.set(x, 3, 3, Blocks.CARVED_PUMPKIN.getDefaultState().with(CarvedPumpkinBlock.FACING, Direction.NORTH));
		}
		r.scatter(0, 0, 16, 6, 1, Blocks.DEAD_BUSH.getDefaultState(), 5);
		r.set(0, 1, 0, STRAW.with(PillarBlock.AXIS, Direction.Axis.X)).set(16, 1, 6, STRAW);
		r.mob(6, 4, 3, z.ranged(), Direction.NORTH);
		r.mob(4, 1, 1, z.tank(), Direction.NORTH).mob(12, 1, 5, z.tank(), Direction.NORTH);
		return r.build();
	}

	/** A burnt granary over a stone vault; the Fields' treasure chest waits below. Ground layer at y = 4. */
	static TemplateBuilder granaryVault(ReachZoneKit z) {
		Ruin r = new Ruin("granary_vault", 11, 13, 11);
		Ruin.Palette p = Ruin.FIELDS;
		r.fill(1, 0, 1, 9, 4, 9, COBBLE_BRICKS, BRICKS, 0.3);
		r.t.blendedFloor(2, 2, 8, 8, 0, r.random, TILES, 5, CRACKED_TILES, 2, POLISHED, 2);
		r.clear(2, 1, 2, 8, 3, 8);
		// The vault: a ribbed ceiling of hung stairs, piers of pillar stone in the corners.
		r.t.innerCornice(1, 1, 9, 9, 3, BRICK_STAIRS);
		for (int[] c : new int[][]{{2, 2}, {8, 2}, {2, 8}, {8, 8}}) {
			r.fill(c[0], 1, c[1], c[0], 2, c[1], PILLAR).set(c[0], 3, c[1], CARVED);
		}
		for (int y = 1; y <= 4; y++) {
			r.set(2, y, 5, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.EAST));
		}
		r.chest(5, 1, 8, Direction.NORTH, z.treasureChest());
		r.set(4, 1, 8, CHISELED).set(6, 1, 8, CHISELED);
		r.set(4, 2, 8, Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true))
				.set(6, 2, 8, Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
		r.set(7, 1, 3, Props.GRAIN_SACKS.getDefaultState().with(StackedPropBlock.COUNT, 3).with(PropBlock.FACING, Direction.WEST));
		r.set(3, 1, 7, Props.BURNT_BARREL.getDefaultState());
		// The granary above: a brick base course, timber over it, the roof burnt off, one corner tower standing.
		r.clear(2, 5, 2, 8, 12, 8);
		r.t.blendedFloor(1, 1, 9, 9, 4, r.random, COBBLE_BRICKS, 4, BRICKS, 2, SOOTY, 2, CHARRED, 1);
		r.t.weatheredWalls(1, 1, 9, 9, 5, 10, r.random, 0.65, COBBLE_BRICKS, 2, BRICKS, CRACKED, SOOTY, p.rubble());
		r.fill(9, 5, 9, 9, 9, 9, BRICKS).set(9, 10, 9, CARVED);
		r.fill(1, 5, 1, 1, 7, 1, PILLAR).set(1, 8, 1, CARVED);
		r.t.stringCourse(1, 1, 9, 9, 7, ModBlocks.ASH_COBBLE_BRICK_SLAB.getDefaultState());
		r.scatter(2, 2, 8, 8, 5, STRAW, 4).scatter(2, 2, 8, 8, 5, THATCH, 3);
		r.fill(3, 5, 5, 7, 5, 5, charredLog(Direction.Axis.X));
		r.clear(5, 5, 1, 5, 6, 1);
		r.set(4, 5, 1, log(Direction.Axis.Y)).set(6, 5, 1, log(Direction.Axis.Y)).set(5, 7, 1, stair(COBBLE_BRICK_STAIRS, Direction.SOUTH, true));
		r.t.soot(5, 8, 1, 2, SOOTY, r.random);
		r.set(5, 8, 1, SetPieces.GRANARY_HOIST.getDefaultState().with(SetPieceBlock.FACING, Direction.NORTH)); // the granary's hoist
		r.mob(4, 1, 3, z.fodder(), Direction.SOUTH).mob(6, 1, 5, z.fodder(), Direction.SOUTH);
		r.mob(9, 10, 9, z.elite(), Direction.NORTH);
		return r.build();
	}

	/** Pilgrims who came for the flame: a cold fire in a cobble ring, bedrolls, a wool tent on dead-wood poles. */
	static TemplateBuilder pilgrimCamp(ReachZoneKit z) {
		Ruin r = new Ruin("pilgrim_camp", 11, 5, 11);
		r.t.blendedFloor(3, 3, 7, 7, 0, r.random, PATH, 4, SOIL, 2, COARSE, 1);
		r.set(5, 0, 5, COBBLE_BRICKS).set(5, 1, 5, COLD_FIRE);
		for (int[] c : new int[][]{{4, 4}, {6, 4}, {4, 6}, {6, 6}}) {
			r.set(c[0], 1, c[1], COBBLE_BRICK_WALL);
		}
		for (int zz = 6; zz <= 9; zz++) {
			r.set(1, 1, zz, WOOL).set(3, 1, zz, WOOL).set(2, 2, zz, WOOL).set(2, 1, zz, Ruin.AIR);
		}
		r.fill(2, 1, 6, 2, 2, 6, FENCE).set(2, 3, 6, LANTERN.with(LanternBlock.HANGING, false));
		r.set(1, 1, 9, PLANK_SLAB).set(3, 1, 9, PLANK_SLAB);
		r.set(7, 1, 7, Blocks.BROWN_CARPET.getDefaultState()).set(8, 1, 7, Blocks.BROWN_CARPET.getDefaultState());
		r.set(3, 1, 3, Blocks.BROWN_CARPET.getDefaultState()).set(3, 1, 2, Blocks.BROWN_CARPET.getDefaultState());
		r.chest(8, 1, 2, Direction.WEST, z.commonChest());
		r.set(9, 1, 3, Props.BURNT_BARREL.getDefaultState()).set(9, 1, 2, Props.GRAIN_SACKS.getDefaultState().with(PropBlock.FACING, Direction.WEST));
		tall(r, 1, 1, 1, Props.TATTERED_STANDARD, Direction.SOUTH);
		r.set(8, 1, 9, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2));
		r.mob(5, 1, 3, z.fodder(), Direction.SOUTH).mob(4, 1, 7, z.fodder(), Direction.EAST)
				.mob(7, 1, 5, z.fodder(), Direction.WEST);
		return r.build();
	}

	/** Gallows for the "hiders of the flame": a plank scaffold on a cobble plinth, chains from the beam. */
	static TemplateBuilder gallows(ReachZoneKit z) {
		Ruin r = new Ruin("gallows", 9, 8, 7);
		r.fill(1, 0, 2, 7, 0, 4, COBBLE_BRICKS, MOSSY_COBBLES, 0.3);
		r.fill(1, 1, 2, 7, 1, 4, PLANKS, CHARRED, 0.3);
		r.set(3, 1, 3, Ruin.AIR).set(3, 1, 3, Blocks.OAK_TRAPDOOR.getDefaultState().with(TrapdoorBlock.OPEN, true));
		r.set(3, 1, 3, ModBlocks.DEAD_TRAPDOOR.getDefaultState().with(TrapdoorBlock.OPEN, true).with(TrapdoorBlock.FACING, Direction.WEST));
		r.fill(2, 0, 3, 2, 6, 3, log(Direction.Axis.Y));
		r.fill(6, 0, 3, 6, 6, 3, log(Direction.Axis.Y));
		r.fill(2, 7, 3, 6, 7, 3, STRIPPED.with(PillarBlock.AXIS, Direction.Axis.X));
		r.set(3, 6, 3, stair(PLANK_STAIRS, Direction.WEST, true)).set(5, 6, 3, stair(PLANK_STAIRS, Direction.EAST, true));
		r.fill(3, 5, 3, 3, 5, 3, CHAIN).set(5, 6, 3, CHAIN).set(5, 5, 3, CHAIN);
		r.set(4, 1, 1, stair(PLANK_STAIRS, Direction.SOUTH, false));
		r.set(1, 1, 1, FENCE).set(7, 1, 1, FENCE).set(1, 1, 5, FENCE).set(7, 1, 5, FENCE);
		r.set(1, 2, 5, LANTERN.with(LanternBlock.HANGING, false));
		r.set(7, 1, 6, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST));
		r.set(5, 6, 3, SetPieces.HANGING_CAGE.getDefaultState().with(SetPieceBlock.FACING, Direction.NORTH)); // a gibbet cage swinging from the beam
		r.mob(4, 2, 3, z.fodder(), Direction.NORTH).mob(4, 1, 6, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** An empty village altar: the flame was taken from here. Tiles round a carved plinth, soot where it burned. */
	static TemplateBuilder villageAltar(ReachZoneKit z) {
		Ruin r = new Ruin("village_altar", 7, 6, 7);
		r.t.blendedFloor(1, 1, 5, 5, 0, r.random, TILES, 5, CRACKED_TILES, 3, POLISHED, 2);
		r.set(0, 0, 0, PATH).set(6, 0, 6, PATH).set(0, 0, 6, PATH).set(6, 0, 0, PATH);
		r.set(3, 1, 3, CARVED).set(3, 2, 3, slab(POLISHED_SLAB, false));
		r.set(3, 3, 3, Props.ASH_URNS.getDefaultState());
		for (int[] c : new int[][]{{2, 2}, {4, 2}, {2, 4}, {4, 4}}) {
			r.set(c[0], 1, c[1], Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 2));
		}
		r.fill(1, 1, 1, 1, 2, 1, PILLAR).set(1, 3, 1, LANTERN.with(LanternBlock.HANGING, false));
		r.fill(5, 1, 1, 5, 2, 1, PILLAR).set(5, 3, 1, CARVED);
		r.set(1, 1, 5, COBBLE_BRICK_WALL).set(5, 1, 5, COBBLE_BRICK_WALL);
		r.set(2, 1, 5, SOOTY).set(2, 2, 5, slab(ModBlocks.ASH_COBBLE_BRICK_SLAB.getDefaultState(), false));
		r.mob(3, 1, 5, z.fodder(), Direction.NORTH);
		return r.build();
	}

	// --- Royal Woods --------------------------------------------------------------------------------------------

	/** A stretch of the broken royal park wall with a bent gate; a Bark Hollow stands by it like a dead tree. */
	static TemplateBuilder parkWall(ReachZoneKit z) {
		Ruin r = new Ruin("park_wall", 21, 7, 5);
		Ruin.Palette p = Ruin.WOODS;
		r.t.blendedFloor(0, 1, 20, 3, 0, r.random, MOSSY_COBBLES, 4, Blocks.PODZOL.getDefaultState(), 2, COBBLES, 2);
		r.t.weatheredWalls(0, 2, 20, 2, 1, 5, r.random, 0.6, MOSSY_COBBLES, 1, BRICKS, CRACKED, MOSSY, null);
		for (int x = 0; x <= 20; x += 5) {
			r.fill(x, 1, 2, x, 5, 2, PILLAR).set(x, 6, 2, CARVED);
			r.t.trySet(x, 6, 1, stair(BRICK_STAIRS, Direction.SOUTH, true)).trySet(x, 6, 3, stair(BRICK_STAIRS, Direction.NORTH, true));
		}
		r.set(15, 6, 2, LANTERN.with(LanternBlock.HANGING, false)).set(20, 6, 2, stair(BRICK_STAIRS, Direction.EAST, false));
		// The wall's coping and the gate: an arch of pillar stone with a grille hanging bent in it.
		for (int x = 1; x <= 19; x++) {
			if (x % 5 != 0 && r.t.isSet(x, 5, 2) && !r.t.isAir(x, 5, 2)) {
				r.set(x, 5, 2, slab(ModBlocks.MOSSY_ASH_BRICK_SLAB.getDefaultState(), false));
			}
		}
		r.t.arch(10, 1, 2, Direction.Axis.X, 3, 4, BRICK_STAIRS, CARVED, PILLAR);
		r.set(9, 1, 2, GRATE).set(9, 2, 2, GRATE).set(11, 1, 2, GRATE);
		r.set(9, 3, 2, stair(BRICK_STAIRS, Direction.WEST, true));
		r.rubbleAlong(0, 2, 20, 2, 1, 3);
		r.set(3, 1, 0, Blocks.MOSS_CARPET.getDefaultState()).set(17, 1, 4, Blocks.MOSS_CARPET.getDefaultState());
		r.mob(13, 1, 4, z.tank(), Direction.NORTH).mob(6, 1, 0, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** The king's hunting lodge: a log hall on a stone plinth with a slate roof and a loft; the Woods' treasure in the loft. */
	static TemplateBuilder huntingLodge(ReachZoneKit z) {
		Ruin r = new Ruin("hunting_lodge", 15, 11, 13);
		Ruin.Palette p = Ruin.WOODS;
		r.t.blendedFloor(1, 1, 13, 11, 0, r.random, PLANKS, 6, CHARRED, 1, MOSSY_COBBLES, 1);
		r.clear(2, 1, 2, 12, 7, 10);
		// Walls: a mossy stone plinth, then logs laid along the walls, the upper courses broken.
		for (int x = 1; x <= 13; x++) {
			for (int zz = 1; zz <= 11; zz++) {
				if (x != 1 && x != 13 && zz != 1 && zz != 11) {
					continue;
				}
				boolean alongX = zz == 1 || zz == 11;
				int broken = 0;
				for (int y = 1; y <= 6; y++) {
					double t = (y - 1) / 5.0;
					if (broken > 0 || r.random.nextDouble() < 0.25 * t * t) {
						r.set(x, y, zz, Ruin.AIR);
						broken++;
					} else if (y == 1) {
						r.set(x, y, zz, r.random.nextInt(4) == 0 ? MOSSY_COBBLES : COBBLE_BRICKS);
					} else {
						r.set(x, y, zz, log(alongX ? Direction.Axis.X : Direction.Axis.Z));
					}
				}
			}
		}
		for (int[] c : new int[][]{{1, 1}, {13, 1}, {1, 11}, {13, 11}}) {
			r.fill(c[0], 1, c[1], c[0], 6, c[1], log(Direction.Axis.Y));
		}
		r.clear(7, 1, 1, 7, 2, 1);
		r.set(6, 1, 1, STRIPPED).set(8, 1, 1, STRIPPED).set(7, 3, 1, STRIPPED.with(PillarBlock.AXIS, Direction.Axis.X));
		r.set(6, 3, 1, LANTERN.with(LanternBlock.HANGING, false));
		// Amber windows either side of the door and in the gable, with grilles.
		r.set(4, 2, 1, GRATE).set(10, 2, 1, GRATE).set(4, 3, 1, AMBER).set(10, 3, 1, AMBER);
		r.set(1, 3, 6, GRATE).set(13, 3, 6, GRATE);
		// The loft over the back bays, its ladder, the slate roof pitched along x and half fallen in.
		r.fill(2, 4, 8, 12, 4, 10, PLANKS, CHARRED, 0.2);
		r.fill(7, 1, 8, 7, 3, 8, log(Direction.Axis.Y));
		r.fill(2, 4, 7, 12, 4, 7, FENCE, Ruin.AIR, 0.3).set(7, 4, 7, Ruin.AIR);
		for (int y = 1; y <= 4; y++) {
			r.set(7, y, 7, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
		}
		r.t.pitchedRoof(1, 1, 13, 11, 7, Direction.Axis.X, SLATE_STAIRS, SLATE_SLAB, r.random, 0.55);
		r.t.gable(1, 1, 11, 7, Direction.Axis.X, PLANKS, r.random, 0.5);
		r.t.gable(13, 1, 11, 7, Direction.Axis.X, PLANKS, r.random, 0.5);
		r.fill(2, 7, 6, 12, 7, 6, log(Direction.Axis.X), charredLog(Direction.Axis.X), 0.3);  // the tie beam under the ridge
		// The hearth on the east wall, soot up the chimney; trophies, barrels, the rack.
		r.set(12, 0, 5, COBBLE_BRICKS).set(12, 1, 5, COLD_FIRE);
		r.fill(13, 1, 4, 13, 7, 6, COBBLE_BRICKS, MOSSY_COBBLES, 0.2);
		r.t.soot(13, 2, 5, 5, SOOTY, r.random).set(13, 8, 5, COBBLE_BRICK_WALL);
		r.set(3, 1, 3, Props.BURNT_BARREL.getDefaultState()).set(3, 2, 3, SKULL);
		r.set(11, 1, 3, Props.BURNT_BARREL.getDefaultState()).set(11, 2, 3, SKULL);
		r.set(2, 1, 6, Props.WEAPON_RACK.getDefaultState().with(PropBlock.FACING, Direction.EAST));
		r.set(2, 2, 2, SKULL).set(12, 2, 10, Props.IRON_CAGE.getDefaultState());
		tall(r, 2, 1, 10, Props.TATTERED_STANDARD, Direction.EAST);
		r.chest(10, 5, 10, Direction.NORTH, z.treasureChest());
		r.set(9, 5, 10, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2));
		r.rubbleAlong(1, 1, 13, 11, 1, 3);
		r.mob(5, 1, 5, z.fodder(), Direction.NORTH).mob(9, 1, 6, z.fodder(), Direction.NORTH);
		r.mob(4, 5, 9, z.ranged(), Direction.NORTH);
		r.mob(13, 1, 12, z.tank(), Direction.NORTH);
		return r.build();
	}

	/** The royal kennels: fenced pens on a mossy court, three slate-roofed huts, gnawed bones. */
	static TemplateBuilder kennels(ReachZoneKit z) {
		Ruin r = new Ruin("kennels", 13, 5, 9);
		r.t.blendedFloor(0, 0, 12, 8, 0, r.random, Blocks.PODZOL.getDefaultState(), 4, MOSSY_COBBLES, 3, PATH, 2);
		r.walls(0, 0, 12, 8, 1, 1, FENCE, FENCE, 0.25);
		r.set(0, 1, 0, log(Direction.Axis.Y)).set(12, 1, 0, log(Direction.Axis.Y)).set(0, 1, 8, log(Direction.Axis.Y)).set(12, 1, 8, log(Direction.Axis.Y));
		r.set(6, 1, 0, Ruin.AIR).set(5, 1, 0, log(Direction.Axis.Y)).set(7, 1, 0, log(Direction.Axis.Y)).set(5, 2, 0, LANTERN.with(LanternBlock.HANGING, false));
		for (int x : new int[]{1, 5, 9}) {
			r.fill(x, 0, 5, x + 2, 0, 7, COBBLE_BRICKS);
			r.walls(x, 5, x + 2, 7, 1, 2, PLANKS, log(Direction.Axis.Y), 0.1);
			r.set(x, 1, 5, log(Direction.Axis.Y)).set(x + 2, 1, 5, log(Direction.Axis.Y));
			r.set(x + 1, 1, 6, Ruin.AIR).set(x + 1, 2, 6, Ruin.AIR).set(x + 1, 1, 5, Ruin.AIR);
			r.t.pitchedRoof(x, 5, x + 2, 7, 3, Direction.Axis.X, SLATE_STAIRS, SLATE_SLAB, r.random, x == 5 ? 0.5 : 0.9);
		}
		r.set(2, 1, 6, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH));
		r.scatter(1, 1, 11, 4, 1, BONES, 2).scatter(1, 1, 11, 4, 1, SKULL, 2);
		r.set(11, 1, 2, Blocks.MOSS_CARPET.getDefaultState());
		r.chest(6, 1, 2, Direction.NORTH, z.commonChest());
		r.mob(3, 1, 3, z.fodder(), Direction.NORTH).mob(9, 1, 2, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** A hunter's hide on log stilts, a slate lean-to over it; a Lantern Hag keeps watch from the platform. */
	static TemplateBuilder huntersHide(ReachZoneKit z) {
		Ruin r = new Ruin("hunters_hide", 7, 9, 8);
		for (int[] c : new int[][]{{1, 1}, {5, 1}, {1, 5}, {5, 5}, {3, 5}}) {
			r.fill(c[0], 0, c[1], c[0], 4, c[1], log(Direction.Axis.Y));
			r.set(c[0], 0, c[1], MOSSY_COBBLES);
		}
		r.fill(1, 4, 1, 5, 4, 5, log(Direction.Axis.X));
		r.fill(1, 5, 1, 5, 5, 5, PLANKS, CHARRED, 0.15);
		r.walls(1, 1, 5, 5, 6, 6, FENCE, FENCE, 0.2);
		r.set(3, 6, 5, Ruin.AIR);
		r.set(1, 6, 1, log(Direction.Axis.Y)).set(5, 6, 1, log(Direction.Axis.Y)).fill(1, 7, 1, 1, 7, 1, log(Direction.Axis.Y)).set(5, 7, 1, log(Direction.Axis.Y));
		for (int x = 1; x <= 5; x++) {
			r.t.trySet(x, 8, 1, stair(SLATE_STAIRS, Direction.NORTH, false)).trySet(x, 8, 2, slab(SLATE_SLAB, false));
		}
		for (int y = 1; y <= 5; y++) {
			r.set(3, y, 6, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH));
		}
		r.set(5, 6, 5, LANTERN.with(LanternBlock.HANGING, false)).set(1, 6, 5, AMBER);
		r.chest(4, 6, 2, Direction.WEST, z.commonChest());
		r.set(2, 6, 2, Props.WEAPON_RACK.getDefaultState().with(PropBlock.FACING, Direction.SOUTH));
		r.mob(2, 6, 3, z.ranged(), Direction.NORTH);
		return r.build();
	}

	/** A dead oak with cages on its branches, a Withered Thrall in each (they drop on whoever passes under). */
	static TemplateBuilder hangingCages(ReachZoneKit z) {
		Ruin r = new Ruin("hanging_cages", 11, 13, 11);
		r.t.blendedFloor(3, 3, 7, 7, 0, r.random, Blocks.PODZOL.getDefaultState(), 4, MOSSY_COBBLES, 2, Blocks.MOSS_BLOCK.getDefaultState(), 2);
		r.fill(5, 0, 5, 5, 10, 5, log(Direction.Axis.Y));
		r.fill(4, 1, 5, 4, 1, 5, log(Direction.Axis.Y)).fill(6, 1, 5, 6, 1, 5, log(Direction.Axis.Y)).set(5, 1, 4, log(Direction.Axis.Y));
		r.fill(1, 9, 5, 9, 9, 5, log(Direction.Axis.X));
		r.fill(5, 8, 1, 5, 8, 9, log(Direction.Axis.Z));
		r.set(1, 8, 5, FENCE).set(9, 10, 5, FENCE).set(5, 7, 1, FENCE).set(5, 9, 9, FENCE).set(5, 11, 5, FENCE);
		cage(r, z, 2, 5, 3, 9);
		cage(r, z, 8, 5, 3, 9);
		cage(r, z, 5, 2, 3, 7);
		r.set(5, 7, 9, LANTERN.with(LanternBlock.HANGING, true));
		r.set(9, 8, 5, LANTERN.with(LanternBlock.HANGING, true));
		r.set(5, 11, 5, Blocks.MOSS_CARPET.getDefaultState()).set(3, 1, 7, Props.BONE_PILE.getDefaultState());
		r.set(5, 7, 7, SetPieces.HANGING_CAGE.getDefaultState().with(SetPieceBlock.FACING, Direction.NORTH)); // one more cage, swinging
		return r.build();
	}

	/** A 3 × 3 cage centred on cx, cz: trapdoor floor at y0, grave-iron grilles around, hung by a chain up to the branch. */
	private static void cage(Ruin r, ReachZoneKit z, int cx, int cz, int y0, int branchY) {
		BlockState floor = ModBlocks.DEAD_TRAPDOOR.getDefaultState().with(TrapdoorBlock.HALF, BlockHalf.TOP);
		r.fill(cx - 1, y0, cz - 1, cx + 1, y0, cz + 1, floor);
		r.walls(cx - 1, cz - 1, cx + 1, cz + 1, y0 + 1, y0 + 2, GRATE, GRATE, 0);
		r.clear(cx, y0 + 1, cz, cx, y0 + 2, cz);
		r.fill(cx - 1, y0 + 3, cz - 1, cx + 1, y0 + 3, cz + 1, GRATE);
		r.fill(cx, y0 + 4, cz, cx, branchY - 1, cz, CHAIN);
		r.mob(cx, y0 + 1, cz, z.fodder(), Direction.NORTH, "embervoid.placed.cage");
	}

	/** Old lantern posts along a mossy path, most of them dark. */
	static TemplateBuilder lanternRow(ReachZoneKit z) {
		Ruin r = new Ruin("lantern_row", 15, 6, 5);
		r.t.blendedFloor(0, 1, 14, 3, 0, r.random, PATH, 4, GRAVEL, 2, MOSSY_COBBLES, 2, Blocks.PODZOL.getDefaultState(), 1);
		for (int x = 1; x <= 13; x += 4) {
			int side = (x / 4) % 2 == 0 ? 1 : 3;
			r.set(x, 0, side, MOSSY_COBBLES);
			r.fill(x, 1, side, x, 2, side, r.random.nextInt(3) == 0 ? MOSSY_WALL : BRICK_WALL);
			if (r.random.nextInt(3) == 0) {
				r.set(x, 3, side, LANTERN);
			} else if (r.random.nextBoolean()) {
				r.set(x, 3, side, Blocks.MOSS_CARPET.getDefaultState());
			}
		}
		r.set(7, 1, 2, Blocks.MOSS_CARPET.getDefaultState());
		r.mob(7, 1, 2, z.ranged(), Direction.NORTH);
		return r.build();
	}

	/** A mossy glade with fallen logs, where two Bark Hollows stand still among the dead trees. */
	static TemplateBuilder glade(ReachZoneKit z) {
		Ruin r = new Ruin("glade", 11, 4, 11);
		r.t.blendedFloor(0, 0, 10, 0, 10, r.random, Blocks.MOSS_BLOCK.getDefaultState(), 5, Blocks.PODZOL.getDefaultState(), 3, MOSSY_COBBLES, 1);
		r.fill(2, 1, 8, 6, 1, 8, log(Direction.Axis.X));
		r.set(8, 1, 2, log(Direction.Axis.Y)).set(8, 2, 2, log(Direction.Axis.Y)).set(7, 1, 2, STRIPPED.with(PillarBlock.AXIS, Direction.Axis.X));
		r.set(1, 1, 1, AMBER);
		r.scatter(0, 0, 10, 10, 1, Blocks.MOSS_CARPET.getDefaultState(), 8);
		r.scatter(0, 0, 10, 10, 1, Blocks.BROWN_MUSHROOM.getDefaultState(), 3);
		r.mob(3, 1, 3, z.tank(), Direction.SOUTH).mob(8, 1, 6, z.tank(), Direction.WEST);
		return r.build();
	}

	// --- Sunken Necropolis --------------------------------------------------------------------------------------

	/** A mausoleum: a wet-stone tomb on a cobble course with bone-brick quoins and a slate roof; Sentinels at the door. */
	static TemplateBuilder mausoleum(ReachZoneKit z, boolean large) {
		int sx = large ? 11 : 7;
		int sz = large ? 13 : 9;
		Ruin r = new Ruin(large ? "mausoleum_large" : "mausoleum_small", sx, large ? 10 : 8, sz);
		Ruin.Palette p = Ruin.NECROPOLIS;
		int top = large ? 7 : 5;
		r.floor(p, 0, 0, sx - 1, sz - 1, 0);
		r.clear(1, 1, 1, sx - 2, top, sz - 2);
		r.facade(p, 0, 0, sx - 1, sz - 1, 1, top, 0.15);
		r.cornerPiers(p, 0, 0, sx - 1, sz - 1, 1, top, 0.0);
		r.t.stringCourse(0, 0, sx - 1, sz - 1, 1, ModBlocks.ASH_COBBLE_BRICK_SLAB.getDefaultState());
		// The roof: a cornice of hung stairs and a pitched slate roof along z, a bone finial.
		r.t.innerCornice(-1, -1, sx, sz, top, BRICK_STAIRS);
		r.t.pitchedRoof(0, 0, sx - 1, sz - 1, top + 1, Direction.Axis.Z, SLATE_STAIRS, SLATE_SLAB, r.random, large ? 0.9 : 0.8);
		int door = sx / 2;
		r.t.arch(door, 1, 0, Direction.Axis.X, large ? 3 : 1, large ? 3 : 2, BRICK_STAIRS, CARVED, large ? BONE_PILLAR : null);
		if (!large) {
			r.set(door - 1, 1, 0, BONE_PILLAR).set(door + 1, 1, 0, BONE_PILLAR).set(door - 1, 2, 0, BONE_PILLAR).set(door + 1, 2, 0, BONE_PILLAR);
		}
		r.set(door - 2, 2, 0, GRATE).set(door + 2, 2, 0, GRATE);
		// Inside: the sarcophagus on a dais, candles, a bone niche.
		int lid = sz - 4;
		r.fill(door - 1, 0, lid - 1, door + 1, 0, lid + 2, TILES);
		longProp(r, door, 1, lid, Props.COFFIN, Direction.SOUTH);
		r.set(door - 1, 1, lid, Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true).with(PropCandleBlock.FACING, Direction.EAST));
		r.set(door + 1, 1, lid, BLUE_CANDLE.with(CandleBlock.CANDLES, 2));
		r.set(door, 2, sz - 2, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH)).set(door, 1, sz - 2, BONE_BRICKS);
		r.chest(sx - 2, 1, sz - 2, Direction.WEST, z.commonChest());
		r.set(1, 1, sz - 2, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, large ? 3 : 1).with(PropBlock.FACING, Direction.EAST));
		r.rubbleAlong(0, 0, sx - 1, sz - 1, 1, 2);
		if (large) {
			r.set(door, top, 3, SetPieces.GREAT_BELL.getDefaultState().with(SetPieceBlock.FACING, Direction.NORTH)); // the tomb's bell under the ceiling
		}
		if (large) {
			r.mob(door - 1, 1, 2, z.tank(), Direction.NORTH).mob(door + 1, 1, 2, z.tank(), Direction.NORTH);
			r.mob(2, 1, 6, z.fodder(), Direction.EAST).mob(sx - 3, 1, 7, z.fodder(), Direction.WEST);
		} else {
			r.mob(door, 1, 1, z.tank(), Direction.NORTH);
		}
		return r.build();
	}

	/** Rows of gravestones on gravel, blue candles left by no one; bones under the rows. */
	static TemplateBuilder graveRows(ReachZoneKit z) {
		Ruin r = new Ruin("grave_rows", 15, 3, 13);
		r.t.blendedFloor(0, 0, 14, 12, 0, r.random, GRAVEL, 5, Blocks.TUFF.getDefaultState(), 1, COBBLES, 1, PATH, 1);
		for (int row = 2; row <= 11; row += 3) {
			for (int x = 1; x <= 13; x += 2) {
				int roll = r.random.nextInt(12);
				BlockState stone = roll < 5 ? ReachBlocks.GRAVESTONE.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH)
						: roll < 7 ? Props.LEANING_GRAVESTONE.getDefaultState().with(PropBlock.FACING, Direction.NORTH)
						: roll < 9 ? Props.GRAVE_CROSS.getDefaultState().with(PropBlock.FACING, Direction.NORTH)
						: roll < 10 ? ModBlocks.COBBLED_ASH_STONE_SLAB.getDefaultState() : Ruin.AIR;
				r.set(x, 1, row, stone);
				if (roll < 9 && r.random.nextInt(3) == 0) {
					r.set(x, 0, row + 1, Blocks.COARSE_DIRT.getDefaultState());
				}
			}
		}
		r.set(0, 1, 6, BONE_WALL).set(14, 1, 6, BONE_WALL).set(0, 2, 6, BLUE_CANDLE).set(14, 2, 6, BLUE_CANDLE);
		r.scatter(0, 0, 14, 12, 1, BLUE_CANDLE, 4).scatter(0, 0, 14, 12, 1, CANDLE, 2);
		r.mob(4, 1, 3, z.fodder(), Direction.NORTH).mob(10, 1, 6, z.fodder(), Direction.NORTH)
				.mob(6, 1, 9, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** A chapel on a sinkhole rim: buttressed walls, grilled windows, pews, an altar of bone and the bell its Grave Warden rings. */
	static TemplateBuilder chapel(ReachZoneKit z) {
		Ruin r = new Ruin("chapel", 11, 13, 17);
		Ruin.Palette p = Ruin.NECROPOLIS;
		r.floor(p, 1, 1, 9, 15, 0);
		r.clear(2, 1, 2, 8, 10, 14);
		r.facade(p, 1, 1, 9, 15, 1, 8, 0.5);
		r.cornerPiers(p, 1, 1, 9, 15, 1, 8, 0.25);
		for (int zz = 4; zz <= 12; zz += 4) {
			r.t.buttress(1, 1, zz, Direction.WEST, 4, COBBLE_BRICKS, COBBLE_BRICK_STAIRS);
			r.t.buttress(9, 1, zz, Direction.EAST, 4, COBBLE_BRICKS, COBBLE_BRICK_STAIRS);
			if (r.t.isSet(1, 4, zz) && !r.t.isAir(1, 4, zz)) {
				r.set(1, 3, zz, GRATE).set(1, 4, zz, GRATE);
			}
			if (r.t.isSet(9, 4, zz) && !r.t.isAir(9, 4, zz)) {
				r.set(9, 3, zz, GRATE).set(9, 4, zz, GRATE);
			}
		}
		r.t.arch(5, 1, 1, Direction.Axis.X, 3, 3, BRICK_STAIRS, CARVED, BONE_PILLAR);
		r.set(5, 5, 1, GRATE).set(5, 6, 1, GRATE);
		r.t.pitchedRoof(1, 1, 9, 15, 9, Direction.Axis.Z, SLATE_STAIRS, SLATE_SLAB, r.random, 0.35);
		for (int zz = 4; zz <= 10; zz += 2) {
			for (int x : new int[]{2, 3, 7, 8}) {
				if (r.random.nextInt(4) != 0) {
					r.set(x, 1, zz, stair(PLANK_STAIRS, Direction.SOUTH, false));
				}
			}
		}
		r.fill(4, 1, 13, 6, 1, 13, BONE_BRICKS).fill(3, 0, 12, 7, 0, 14, TILES);
		r.set(4, 2, 13, BLUE_CANDLE.with(CandleBlock.CANDLES, 2)).set(6, 2, 13, BLUE_CANDLE.with(CandleBlock.CANDLES, 2))
				.set(5, 2, 13, Props.ASH_URNS.getDefaultState());
		r.set(5, 1, 12, Blocks.BELL.getDefaultState().with(BellBlock.ATTACHMENT, Attachment.FLOOR).with(BellBlock.FACING, Direction.NORTH));
		tall(r, 3, 1, 14, Props.TATTERED_STANDARD, Direction.NORTH);
		tall(r, 7, 1, 14, Props.TATTERED_STANDARD, Direction.NORTH);
		r.set(2, 1, 2, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST));
		// The great bell on a beam across the nave: a slow sway, a toll when a Grave Warden rings below it.
		r.fill(1, 9, 8, 9, 9, 8, LOG.with(PillarBlock.AXIS, Direction.Axis.X));
		r.set(5, 8, 8, SetPieces.GREAT_BELL.getDefaultState().with(SetPieceBlock.FACING, Direction.NORTH));
		r.chest(8, 1, 14, Direction.WEST, z.commonChest());
		r.rubbleAlong(1, 1, 9, 15, 1, 4);
		r.mob(5, 1, 10, z.elite(), Direction.NORTH);
		r.mob(3, 1, 7, z.fodder(), Direction.NORTH).mob(7, 1, 5, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** A mound of bones in a bone-brick ring, skulls on top; bones and moths around it. */
	static TemplateBuilder ossuaryPit(ReachZoneKit z) {
		Ruin r = new Ruin("ossuary_pit", 9, 6, 9);
		r.t.blendedFloor(0, 0, 8, 0, 8, r.random, GRAVEL, 5, COBBLES, 2, Blocks.TUFF.getDefaultState(), 1);
		for (int x = 1; x <= 7; x++) {
			for (int zz = 1; zz <= 7; zz++) {
				if ((x == 1 || x == 7 || zz == 1 || zz == 7) && r.random.nextInt(4) != 0) {
					r.set(x, 0, zz, BONE_BRICKS);
				}
			}
		}
		r.set(1, 1, 1, BONE_WALL).set(7, 1, 7, BONE_WALL).set(1, 2, 1, BLUE_CANDLE).set(7, 2, 7, BLUE_CANDLE);
		r.fill(2, 1, 2, 6, 1, 6, BONES, GRAVEL, 0.3);
		r.fill(3, 2, 3, 5, 2, 5, BONES, Ruin.AIR, 0.3);
		r.set(4, 3, 4, BONE_PILLAR).set(4, 4, 4, SKULL);
		r.scatter(1, 1, 7, 7, 2, SKULL, 2);
		r.set(7, 1, 2, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST));
		r.mob(1, 1, 1, z.fodder(), Direction.SOUTH).mob(7, 1, 7, z.fodder(), Direction.NORTH);
		r.mob(1, 2, 7, z.ranged(), Direction.EAST).mob(7, 2, 1, z.ranged(), Direction.WEST);
		return r.build();
	}

	/** A tomb half sunk into the ground, its roof fallen in; a Sentinel still stands inside. Ground layer at y = 2. */
	static TemplateBuilder collapsedTomb(ReachZoneKit z) {
		Ruin r = new Ruin("collapsed_tomb", 9, 7, 9);
		Ruin.Palette p = Ruin.NECROPOLIS;
		r.floor(p, 0, 0, 8, 8, 0);
		r.clear(1, 1, 1, 7, 5, 7);
		r.t.weatheredWalls(0, 0, 8, 8, 1, 4, r.random, 0.3, COBBLE_BRICKS, 1, WET, CRACKED, MOSSY, p.rubble());
		r.cornerPiers(p, 0, 0, 8, 8, 1, 4, 0.3);
		r.t.pitchedRoof(0, 0, 8, 8, 5, Direction.Axis.X, SLATE_STAIRS, SLATE_SLAB, r.random, 0.35);
		r.fill(3, 1, 3, 5, 1, 5, CRACKED, SLATE_SLAB, 0.4).set(4, 2, 4, slab(SLATE_SLAB, false));
		r.fill(4, 1, 0, 4, 3, 0, Ruin.AIR);
		r.set(4, 3, 0, stair(BRICK_STAIRS, Direction.SOUTH, true)).set(3, 1, 0, BONE_PILLAR).set(5, 1, 0, BONE_PILLAR);
		r.set(1, 1, 7, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST)).set(7, 1, 1, BLUE_CANDLE);
		r.chest(6, 1, 6, Direction.WEST, z.commonChest());
		r.mob(4, 1, 4, z.tank(), Direction.NORTH);
		return r.build();
	}

	private MinorTemplates() {}
}
