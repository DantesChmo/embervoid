package com.embervoid.client.reach.world;

import com.embervoid.Embervoid;
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
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minor structures (PLAN 9.7, DESIGN §4.5 "Zones and places"): {@code embervoid:ashen_reach/<zone>/<name>}, each a
 * single-piece jigsaw structure in its zone. y = 0 is the ground layer (except where a structure starts lower, see
 * {@link #START_DEPTH}); placed zone mobs and chests at the zone's table ids: treasure in the Fields granary vault and
 * the Woods hunting lodge.
 */
public final class MinorTemplates {
	/** Structures whose ground layer is not y = 0: the depth their template reaches below the ground. */
	public static final Map<String, Integer> START_DEPTH = Map.of("fields/granary_vault", 4, "necropolis/collapsed_tomb", 2);

	private static final BlockState BRICKS = ModBlocks.ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = ModBlocks.CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState COBBLES = ModBlocks.COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState POLISHED = ModBlocks.POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState CHISELED = ModBlocks.CHISELED_ASH_STONE.getDefaultState();
	private static final BlockState BRICK_SLAB = ModBlocks.ASH_BRICK_SLAB.getDefaultState();
	private static final BlockState BRICK_WALL = ModBlocks.ASH_BRICK_WALL.getDefaultState();
	private static final BlockState PLANKS = ModBlocks.DEAD_PLANKS.getDefaultState();
	private static final BlockState PLANK_SLAB = ModBlocks.DEAD_SLAB.getDefaultState();
	private static final BlockState FENCE = ModBlocks.DEAD_FENCE.getDefaultState();
	private static final BlockState LOG = ModBlocks.DEAD_LOG.getDefaultState();
	private static final BlockState LANTERN = ModBlocks.EMBER_LANTERN.getDefaultState();
	private static final BlockState STRAW = ReachBlocks.BURNT_STRAW.getDefaultState();
	private static final BlockState WET = ReachBlocks.WET_CRYPT_STONE.getDefaultState();
	private static final BlockState SOIL = ModBlocks.ASH_SOIL.getDefaultState();
	private static final BlockState COARSE = Blocks.COARSE_DIRT.getDefaultState();
	private static final BlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
	private static final BlockState BONES = Blocks.BONE_BLOCK.getDefaultState();
	private static final BlockState SKULL = Blocks.SKELETON_SKULL.getDefaultState();
	private static final BlockState BARS = Blocks.IRON_BARS.getDefaultState();
	private static final BlockState CHAIN = Blocks.CHAIN.getDefaultState().with(ChainBlock.AXIS, Direction.Axis.Y);
	private static final BlockState WOOL = Blocks.BROWN_WOOL.getDefaultState();

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

	// --- Ashen Fields -------------------------------------------------------------------------------------------

	/** A burnt farmhouse: half its plank walls, a scrap of roof, straw bales and a stalker in the furrow. */
	static TemplateBuilder farmstead(ReachZoneKit z) {
		Ruin r = new Ruin("farmstead", 13, 9, 11);
		r.fill(1, 0, 1, 9, 0, 7, COBBLES, SOIL, 0.2);
		r.clear(2, 1, 2, 8, 6, 6);
		r.walls(1, 1, 9, 7, 1, 4, PLANKS, LOG.with(PillarBlock.AXIS, Direction.Axis.Y), 0.55);
		for (int[] c : new int[][]{{1, 1}, {9, 1}, {1, 7}, {9, 7}}) {
			r.fill(c[0], 1, c[1], c[0], 3, c[1], LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
		}
		r.clear(5, 1, 1, 5, 2, 1);
		r.roof(1, 1, 9, 7, 5, PLANK_SLAB, 0.35);
		r.chest(2, 1, 6, Direction.EAST, z.commonChest());
		r.set(8, 1, 6, Blocks.CRAFTING_TABLE.getDefaultState());
		r.set(11, 1, 2, STRAW).set(11, 2, 2, STRAW).set(10, 1, 9, STRAW.with(PillarBlock.AXIS, Direction.Axis.X));
		r.fill(10, 0, 5, 12, 0, 10, COARSE, SOIL, 0.4);
		r.mob(4, 1, 4, z.fodder(), Direction.NORTH).mob(7, 1, 5, z.fodder(), Direction.WEST);
		r.mob(11, 1, 7, z.tank(), Direction.NORTH);
		return r.build();
	}

	/** A plank barn, roof mostly gone, burnt bales stacked inside. */
	static TemplateBuilder barn(ReachZoneKit z) {
		Ruin r = new Ruin("barn", 15, 10, 11);
		r.fill(0, 0, 0, 14, 0, 10, COARSE, SOIL, 0.3);
		r.clear(1, 1, 1, 13, 8, 9);
		r.walls(0, 0, 14, 10, 1, 6, PLANKS, LOG.with(PillarBlock.AXIS, Direction.Axis.Y), 0.6);
		for (int x : new int[]{0, 7, 14}) {
			r.fill(x, 1, 0, x, 6, 0, LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
			r.fill(x, 1, 10, x, 6, 10, LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
		}
		r.clear(6, 1, 0, 8, 4, 0);
		r.roof(0, 0, 14, 10, 7, PLANK_SLAB, 0.3);
		r.fill(1, 1, 7, 3, 2, 9, STRAW);
		r.fill(11, 1, 7, 13, 3, 9, STRAW, Ruin.AIR, 0.3);
		r.chest(12, 1, 2, Direction.WEST, z.commonChest());
		r.mob(4, 1, 3, z.fodder(), Direction.NORTH).mob(9, 1, 5, z.fodder(), Direction.NORTH)
				.mob(7, 1, 8, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** The stone base of a windmill with the bare frame of its sails; a carrion perched on the platform. */
	static TemplateBuilder windmill(ReachZoneKit z) {
		Ruin r = new Ruin("windmill", 9, 18, 9);
		r.fill(2, 0, 2, 6, 0, 6, COBBLES);
		r.clear(3, 1, 3, 5, 7, 5);
		r.walls(2, 2, 6, 6, 1, 7, COBBLES, BRICKS, 0.25);
		r.clear(4, 1, 2, 4, 2, 2);
		r.roof(2, 2, 6, 6, 8, PLANKS, 0.7);
		r.set(4, 8, 4, PLANKS);
		r.fill(4, 9, 1, 4, 9, 3, LOG.with(PillarBlock.AXIS, Direction.Axis.Z));
		r.fill(4, 4, 0, 4, 16, 0, FENCE);
		r.fill(0, 10, 0, 8, 10, 0, FENCE);
		r.chest(3, 1, 5, Direction.EAST, z.commonChest());
		r.mob(4, 9, 4, z.ranged(), Direction.NORTH).mob(4, 1, 4, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** Furrows with four scarecrows of burnt straw; stalkers under the furrows, a carrion on a scarecrow. */
	static TemplateBuilder scarecrows(ReachZoneKit z) {
		Ruin r = new Ruin("scarecrows", 17, 6, 7);
		for (int zz = 0; zz < 7; zz++) {
			r.fill(0, 0, zz, 16, 0, zz, zz % 2 == 0 ? COARSE : SOIL);
		}
		for (int x : new int[]{2, 6, 10, 14}) {
			r.set(x, 1, 3, FENCE).set(x, 2, 3, STRAW);
			r.set(x - 1, 2, 3, FENCE).set(x + 1, 2, 3, FENCE);
			r.set(x, 3, 3, Blocks.CARVED_PUMPKIN.getDefaultState().with(CarvedPumpkinBlock.FACING, Direction.NORTH));
		}
		r.scatter(0, 0, 16, 6, 1, Blocks.DEAD_BUSH.getDefaultState(), 5);
		r.mob(6, 4, 3, z.ranged(), Direction.NORTH);
		r.mob(4, 1, 1, z.tank(), Direction.NORTH).mob(12, 1, 5, z.tank(), Direction.NORTH);
		return r.build();
	}

	/** A burnt granary over a stone vault; the Fields' treasure chest waits below. Ground layer at y = 4. */
	static TemplateBuilder granaryVault(ReachZoneKit z) {
		Ruin r = new Ruin("granary_vault", 11, 13, 11);
		r.fill(1, 0, 1, 9, 4, 9, COBBLES, BRICKS, 0.3);
		r.fill(2, 0, 2, 8, 0, 8, POLISHED);
		r.clear(2, 1, 2, 8, 3, 8);
		for (int y = 1; y <= 4; y++) {
			r.set(2, y, 5, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.EAST));
		}
		r.chest(5, 1, 8, Direction.NORTH, z.treasureChest());
		r.set(4, 1, 8, CHISELED).set(6, 1, 8, CHISELED);
		r.clear(2, 5, 2, 8, 12, 8);
		r.walls(1, 1, 9, 9, 5, 10, BRICKS, CRACKED, 0.6);
		r.fill(9, 5, 9, 9, 9, 9, BRICKS);
		r.scatter(2, 2, 8, 8, 5, STRAW, 6);
		r.clear(5, 5, 1, 5, 6, 1);
		r.mob(4, 1, 3, z.fodder(), Direction.SOUTH).mob(6, 1, 5, z.fodder(), Direction.SOUTH);
		r.mob(9, 10, 9, z.elite(), Direction.NORTH);
		return r.build();
	}

	/** Pilgrims who came for the flame: a cold fire, bedrolls, a wool tent. */
	static TemplateBuilder pilgrimCamp(ReachZoneKit z) {
		Ruin r = new Ruin("pilgrim_camp", 11, 5, 11);
		r.set(5, 0, 5, COBBLES).set(5, 1, 5, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));
		for (int zz = 6; zz <= 9; zz++) {
			r.set(1, 1, zz, WOOL).set(3, 1, zz, WOOL).set(2, 2, zz, WOOL).set(2, 1, zz, Ruin.AIR);
		}
		r.set(7, 1, 7, Blocks.BROWN_CARPET.getDefaultState()).set(8, 1, 7, Blocks.BROWN_CARPET.getDefaultState());
		r.set(3, 1, 3, Blocks.BROWN_CARPET.getDefaultState()).set(3, 1, 2, Blocks.BROWN_CARPET.getDefaultState());
		r.chest(8, 1, 2, Direction.WEST, z.commonChest());
		r.set(9, 1, 3, Blocks.BARREL.getDefaultState());
		r.mob(5, 1, 3, z.fodder(), Direction.SOUTH).mob(4, 1, 7, z.fodder(), Direction.EAST)
				.mob(7, 1, 5, z.fodder(), Direction.WEST);
		return r.build();
	}

	/** Gallows for the "hiders of the flame". */
	static TemplateBuilder gallows(ReachZoneKit z) {
		Ruin r = new Ruin("gallows", 9, 8, 7);
		r.fill(1, 1, 2, 7, 1, 4, PLANKS, Ruin.AIR, 0.15);
		r.fill(2, 0, 3, 2, 6, 3, LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
		r.fill(6, 0, 3, 6, 6, 3, LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
		r.fill(2, 7, 3, 6, 7, 3, LOG.with(PillarBlock.AXIS, Direction.Axis.X));
		r.fill(3, 5, 3, 3, 6, 3, CHAIN).set(5, 6, 3, CHAIN);
		r.set(4, 1, 1, ModBlocks.DEAD_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
		r.mob(4, 2, 3, z.fodder(), Direction.NORTH).mob(4, 1, 6, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** An empty village altar: the flame was taken from here. */
	static TemplateBuilder villageAltar(ReachZoneKit z) {
		Ruin r = new Ruin("village_altar", 7, 6, 7);
		r.fill(1, 0, 1, 5, 0, 5, POLISHED, CRACKED, 0.25);
		r.set(3, 1, 3, CHISELED).set(3, 2, 3, BRICK_SLAB);
		for (int[] c : new int[][]{{2, 2}, {4, 2}, {2, 4}, {4, 4}}) {
			r.set(c[0], 1, c[1], Blocks.CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2));
		}
		r.fill(1, 1, 1, 1, 2, 1, BRICK_WALL).fill(5, 1, 1, 5, 2, 1, BRICK_WALL);
		r.mob(3, 1, 5, z.fodder(), Direction.NORTH);
		return r.build();
	}

	// --- Royal Woods --------------------------------------------------------------------------------------------

	/** A stretch of the broken royal park wall with a bent gate; a Bark Hollow stands by it like a dead tree. */
	static TemplateBuilder parkWall(ReachZoneKit z) {
		Ruin r = new Ruin("park_wall", 21, 7, 5);
		r.fill(0, 0, 2, 20, 0, 2, COBBLES);
		r.walls(0, 2, 20, 2, 1, 5, BRICKS, CRACKED, 0.6);
		for (int x = 0; x <= 20; x += 5) {
			r.fill(x, 1, 2, x, 6, 2, CHISELED);
		}
		r.clear(9, 1, 2, 11, 5, 2);
		r.set(9, 1, 2, BARS).set(9, 2, 2, BARS).set(11, 1, 2, BARS);
		r.mob(13, 1, 4, z.tank(), Direction.NORTH).mob(6, 1, 0, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** The king's hunting lodge: a log hall with a loft; the Woods' treasure chest in the loft. */
	static TemplateBuilder huntingLodge(ReachZoneKit z) {
		Ruin r = new Ruin("hunting_lodge", 15, 11, 13);
		r.fill(1, 0, 1, 13, 0, 11, PLANKS);
		r.clear(2, 1, 2, 12, 7, 10);
		r.walls(1, 1, 13, 11, 1, 6, PLANKS, LOG.with(PillarBlock.AXIS, Direction.Axis.Y), 0.25);
		for (int[] c : new int[][]{{1, 1}, {13, 1}, {1, 11}, {13, 11}}) {
			r.fill(c[0], 1, c[1], c[0], 6, c[1], LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
		}
		r.clear(7, 1, 1, 7, 2, 1);
		r.fill(2, 4, 8, 12, 4, 10, PLANKS);
		r.fill(7, 1, 8, 7, 3, 8, LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
		for (int y = 1; y <= 4; y++) {
			r.set(7, y, 7, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
		}
		r.roof(1, 1, 13, 11, 7, PLANK_SLAB, 0.5);
		r.set(12, 0, 5, COBBLES).set(12, 1, 5, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));
		r.fill(13, 1, 4, 13, 4, 6, COBBLES);
		r.set(3, 1, 3, Blocks.BARREL.getDefaultState()).set(3, 2, 3, SKULL);
		r.set(11, 1, 3, Blocks.BARREL.getDefaultState()).set(11, 2, 3, SKULL);
		r.chest(10, 5, 10, Direction.NORTH, z.treasureChest());
		r.mob(5, 1, 5, z.fodder(), Direction.NORTH).mob(9, 1, 6, z.fodder(), Direction.NORTH);
		r.mob(4, 5, 9, z.ranged(), Direction.NORTH);
		r.mob(13, 1, 12, z.tank(), Direction.NORTH);
		return r.build();
	}

	/** The royal kennels: fenced pens, three small huts, gnawed bones. */
	static TemplateBuilder kennels(ReachZoneKit z) {
		Ruin r = new Ruin("kennels", 13, 5, 9);
		r.walls(0, 0, 12, 8, 1, 1, FENCE, FENCE, 0.25);
		for (int x : new int[]{1, 5, 9}) {
			r.fill(x, 0, 5, x + 2, 0, 7, PLANKS);
			r.walls(x, 5, x + 2, 7, 1, 2, PLANKS, PLANKS, 0.1);
			r.set(x + 1, 1, 6, Ruin.AIR).set(x + 1, 2, 6, Ruin.AIR).set(x + 1, 1, 5, Ruin.AIR);
			r.roof(x, 5, x + 2, 7, 3, PLANK_SLAB, 0.85);
		}
		r.scatter(1, 1, 11, 4, 1, BONES, 3).scatter(1, 1, 11, 4, 1, SKULL, 2);
		r.chest(6, 1, 2, Direction.NORTH, z.commonChest());
		r.mob(3, 1, 3, z.fodder(), Direction.NORTH).mob(9, 1, 2, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** A hunter's hide on log stilts; a Lantern Hag keeps watch from the platform. */
	static TemplateBuilder huntersHide(ReachZoneKit z) {
		Ruin r = new Ruin("hunters_hide", 7, 9, 8);
		for (int[] c : new int[][]{{1, 1}, {5, 1}, {1, 5}, {5, 5}, {3, 5}}) {
			r.fill(c[0], 0, c[1], c[0], 4, c[1], LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
		}
		r.fill(1, 5, 1, 5, 5, 5, PLANKS);
		r.walls(1, 1, 5, 5, 6, 6, FENCE, FENCE, 0.2);
		r.set(3, 6, 5, Ruin.AIR);
		for (int y = 1; y <= 5; y++) {
			r.set(3, y, 6, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH));
		}
		r.chest(4, 6, 2, Direction.WEST, z.commonChest());
		r.mob(2, 6, 3, z.ranged(), Direction.NORTH);
		return r.build();
	}

	/** A dead oak with cages on its branches, a Withered Thrall in each (they drop on whoever passes under). */
	static TemplateBuilder hangingCages(ReachZoneKit z) {
		Ruin r = new Ruin("hanging_cages", 11, 13, 11);
		r.fill(5, 0, 5, 5, 10, 5, LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
		r.fill(1, 9, 5, 9, 9, 5, LOG.with(PillarBlock.AXIS, Direction.Axis.X));
		r.fill(5, 8, 1, 5, 8, 9, LOG.with(PillarBlock.AXIS, Direction.Axis.Z));
		cage(r, z, 2, 5, 3, 9);
		cage(r, z, 8, 5, 3, 9);
		cage(r, z, 5, 2, 3, 7);
		r.set(5, 7, 9, LANTERN.with(LanternBlock.HANGING, true));
		r.set(9, 8, 5, LANTERN.with(LanternBlock.HANGING, true));
		return r.build();
	}

	/** A 3 × 3 cage centred on cx, cz: trapdoor floor at y0, bars around, hung by a chain up to the branch. */
	private static void cage(Ruin r, ReachZoneKit z, int cx, int cz, int y0, int branchY) {
		BlockState floor = ModBlocks.DEAD_TRAPDOOR.getDefaultState().with(TrapdoorBlock.HALF, BlockHalf.TOP);
		r.fill(cx - 1, y0, cz - 1, cx + 1, y0, cz + 1, floor);
		r.walls(cx - 1, cz - 1, cx + 1, cz + 1, y0 + 1, y0 + 2, BARS, BARS, 0);
		r.clear(cx, y0 + 1, cz, cx, y0 + 2, cz);
		r.fill(cx - 1, y0 + 3, cz - 1, cx + 1, y0 + 3, cz + 1, BARS);
		r.fill(cx, y0 + 4, cz, cx, branchY - 1, cz, CHAIN);
		r.mob(cx, y0 + 1, cz, z.fodder(), Direction.NORTH, "embervoid.placed.cage");
	}

	/** Old lantern posts along a path, most of them dark. */
	static TemplateBuilder lanternRow(ReachZoneKit z) {
		Ruin r = new Ruin("lantern_row", 15, 6, 5);
		r.fill(0, 0, 2, 14, 0, 2, GRAVEL, COARSE, 0.3);
		for (int x = 1; x <= 13; x += 4) {
			int side = (x / 4) % 2 == 0 ? 1 : 3;
			r.fill(x, 1, side, x, 2, side, BRICK_WALL);
			if (r.random.nextInt(3) == 0) {
				r.set(x, 3, side, LANTERN);
			}
		}
		r.mob(7, 1, 2, z.ranged(), Direction.NORTH);
		return r.build();
	}

	/** A mossy glade with fallen logs, where two Bark Hollows stand still among the dead trees. */
	static TemplateBuilder glade(ReachZoneKit z) {
		Ruin r = new Ruin("glade", 11, 4, 11);
		r.fill(0, 0, 0, 10, 0, 10, Blocks.MOSS_BLOCK.getDefaultState(), Blocks.PODZOL.getDefaultState(), 0.35);
		r.fill(2, 1, 8, 6, 1, 8, LOG.with(PillarBlock.AXIS, Direction.Axis.X));
		r.set(8, 1, 2, LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
		r.scatter(0, 0, 10, 10, 1, Blocks.MOSS_CARPET.getDefaultState(), 8);
		r.scatter(0, 0, 10, 10, 1, Blocks.BROWN_MUSHROOM.getDefaultState(), 3);
		r.mob(3, 1, 3, z.tank(), Direction.SOUTH).mob(8, 1, 6, z.tank(), Direction.WEST);
		return r.build();
	}

	// --- Sunken Necropolis --------------------------------------------------------------------------------------

	/** A mausoleum: a stone tomb with a sarcophagus; Hollow Sentinels guard the door. */
	static TemplateBuilder mausoleum(ReachZoneKit z, boolean large) {
		int sx = large ? 11 : 7;
		int sz = large ? 13 : 9;
		Ruin r = new Ruin(large ? "mausoleum_large" : "mausoleum_small", sx, large ? 10 : 8, sz);
		int top = large ? 7 : 5;
		r.fill(0, 0, 0, sx - 1, 0, sz - 1, POLISHED, CRACKED, 0.2);
		r.clear(1, 1, 1, sx - 2, top, sz - 2);
		r.walls(0, 0, sx - 1, sz - 1, 1, top, WET, BRICKS, 0.15);
		r.roof(0, 0, sx - 1, sz - 1, top + 1, BRICK_SLAB, 0.8);
		int door = sx / 2;
		r.clear(door, 1, 0, door, large ? 3 : 2, 0);
		if (large) {
			r.fill(1, 1, 0, 1, 7, 0, CHISELED).fill(sx - 2, 1, 0, sx - 2, 7, 0, CHISELED);
		}
		int lid = sz - 4;
		r.fill(door, 1, lid, door, 1, lid + 1, CHISELED);
		r.set(door - 1, 1, lid, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 3));
		r.chest(sx - 2, 1, sz - 2, Direction.WEST, z.commonChest());
		if (large) {
			r.mob(door - 1, 1, 2, z.tank(), Direction.NORTH).mob(door + 1, 1, 2, z.tank(), Direction.NORTH);
			r.mob(2, 1, 6, z.fodder(), Direction.EAST).mob(sx - 3, 1, 7, z.fodder(), Direction.WEST);
		} else {
			r.mob(door, 1, 1, z.tank(), Direction.NORTH);
		}
		return r.build();
	}

	/** Rows of gravestones on gravel, candles left by no one; bones under the rows. */
	static TemplateBuilder graveRows(ReachZoneKit z) {
		Ruin r = new Ruin("grave_rows", 15, 3, 13);
		r.fill(0, 0, 0, 14, 0, 12, GRAVEL, Blocks.TUFF.getDefaultState(), 0.15);
		for (int row = 2; row <= 11; row += 3) {
			for (int x = 1; x <= 13; x += 2) {
				int roll = r.random.nextInt(10);
				BlockState stone = roll < 7 ? ReachBlocks.GRAVESTONE.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH)
						: roll < 9 ? ModBlocks.COBBLED_ASH_STONE_SLAB.getDefaultState() : Ruin.AIR;
				r.set(x, 1, row, stone);
			}
		}
		r.scatter(0, 0, 14, 12, 1, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true), 5);
		r.mob(4, 1, 3, z.fodder(), Direction.NORTH).mob(10, 1, 6, z.fodder(), Direction.NORTH)
				.mob(6, 1, 9, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** A chapel on a sinkhole rim: pews, an altar and the bell its Grave Warden rings. */
	static TemplateBuilder chapel(ReachZoneKit z) {
		Ruin r = new Ruin("chapel", 11, 13, 17);
		r.fill(1, 0, 1, 9, 0, 15, POLISHED, WET, 0.3);
		r.clear(2, 1, 2, 8, 10, 14);
		r.walls(1, 1, 9, 15, 1, 8, WET, BRICKS, 0.5);
		r.clear(5, 1, 1, 5, 3, 1);
		for (int zz = 4; zz <= 10; zz += 2) {
			for (int x : new int[]{2, 3, 7, 8}) {
				if (r.random.nextInt(4) != 0) {
					r.set(x, 1, zz, ModBlocks.DEAD_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
				}
			}
		}
		r.fill(4, 1, 13, 6, 1, 13, CHISELED);
		r.set(4, 2, 13, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true))
				.set(6, 2, 13, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true));
		r.set(5, 1, 12, Blocks.BELL.getDefaultState().with(BellBlock.ATTACHMENT, Attachment.FLOOR).with(BellBlock.FACING, Direction.NORTH));
		r.chest(8, 1, 14, Direction.WEST, z.commonChest());
		r.mob(5, 1, 10, z.elite(), Direction.NORTH);
		r.mob(3, 1, 7, z.fodder(), Direction.NORTH).mob(7, 1, 5, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** A mound of bones, skulls on top; bones and moths around it. */
	static TemplateBuilder ossuaryPit(ReachZoneKit z) {
		Ruin r = new Ruin("ossuary_pit", 9, 6, 9);
		r.fill(0, 0, 0, 8, 0, 8, GRAVEL);
		r.fill(2, 1, 2, 6, 1, 6, BONES, GRAVEL, 0.3);
		r.fill(3, 2, 3, 5, 2, 5, BONES, Ruin.AIR, 0.3);
		r.set(4, 3, 4, BONES).set(4, 4, 4, SKULL);
		r.scatter(1, 1, 7, 7, 2, SKULL, 2);
		r.mob(1, 1, 1, z.fodder(), Direction.SOUTH).mob(7, 1, 7, z.fodder(), Direction.NORTH);
		r.mob(1, 2, 7, z.ranged(), Direction.EAST).mob(7, 2, 1, z.ranged(), Direction.WEST);
		return r.build();
	}

	/** A tomb half sunk into the ground, its roof fallen in; a Sentinel still stands inside. Ground layer at y = 2. */
	static TemplateBuilder collapsedTomb(ReachZoneKit z) {
		Ruin r = new Ruin("collapsed_tomb", 9, 7, 9);
		r.fill(0, 0, 0, 8, 0, 8, WET);
		r.clear(1, 1, 1, 7, 5, 7);
		r.walls(0, 0, 8, 8, 1, 4, WET, CRACKED, 0.3);
		r.roof(0, 0, 8, 8, 5, BRICK_SLAB.with(SlabBlock.TYPE, SlabType.BOTTOM), 0.4);
		r.fill(4, 1, 0, 4, 3, 0, Ruin.AIR);
		r.set(4, 2, 0, ModBlocks.ASH_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
		r.chest(6, 1, 6, Direction.WEST, z.commonChest());
		r.mob(4, 1, 4, z.tank(), Direction.NORTH);
		return r.build();
	}

	private MinorTemplates() {}
}
