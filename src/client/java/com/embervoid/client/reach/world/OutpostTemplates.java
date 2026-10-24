package com.embervoid.client.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.block.prop.PropBlock;
import com.embervoid.block.prop.Props;
import com.embervoid.block.prop.StackedPropBlock;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static com.embervoid.sanctum.datagen.TemplateBuilder.slab;
import static com.embervoid.sanctum.datagen.TemplateBuilder.stair;

/**
 * Road outposts (PLAN 9.6), one per kind and zone: {@code embervoid:ashen_reach/outpost/<toll_gate|watchtower>_<zone>}.
 * The road runs the whole length of each template along +Z (north = the landing side), centred on its middle column
 * (the road feature centres the template on the road); y = 0 is the ground. Placed fodder and ranged mobs of the zone,
 * a common chest.
 *
 * <p>0.5.0 art pass: the towers are cobble-brick base courses under weathered ash brick with pillar quoins, a string
 * course, a cornice and the zone's accent (brass, amber, bone); the watchtower has a slate hood over its platform.
 */
public final class OutpostTemplates {
	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final BlockState BRICKS = ModBlocks.ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = ModBlocks.CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState COBBLE_BRICKS = ModBlocks.ASH_COBBLE_BRICKS.getDefaultState();
	private static final BlockState COBBLE_BRICK_STAIRS = ModBlocks.ASH_COBBLE_BRICK_STAIRS.getDefaultState();
	private static final BlockState COBBLE_BRICK_SLAB = ModBlocks.ASH_COBBLE_BRICK_SLAB.getDefaultState();
	private static final BlockState COBBLE_BRICK_WALL = ModBlocks.ASH_COBBLE_BRICK_WALL.getDefaultState();
	private static final BlockState COBBLES = ModBlocks.COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState FLAGSTONES = ReachBlocks.KINGSROAD_FLAGSTONES.getDefaultState();
	private static final BlockState KERB = ReachBlocks.KINGSROAD_KERB.getDefaultState();
	private static final BlockState POLISHED = ModBlocks.POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState TILES = ModBlocks.ASH_TILES.getDefaultState();
	private static final BlockState CRACKED_TILES = ModBlocks.CRACKED_ASH_TILES.getDefaultState();
	private static final BlockState PILLAR = ModBlocks.ASH_BRICK_PILLAR.getDefaultState();
	private static final BlockState CARVED = ModBlocks.CARVED_ASH_STONE.getDefaultState();
	private static final BlockState BRICK_STAIRS = ModBlocks.ASH_BRICK_STAIRS.getDefaultState();
	private static final BlockState BRICK_SLAB = ModBlocks.ASH_BRICK_SLAB.getDefaultState();
	private static final BlockState SLATE_STAIRS = ModBlocks.SLATE_TILE_STAIRS.getDefaultState();
	private static final BlockState SLATE_SLAB = ModBlocks.SLATE_TILE_SLAB.getDefaultState();
	private static final BlockState PLANKS = ModBlocks.DEAD_PLANKS.getDefaultState();
	private static final BlockState FENCE = ModBlocks.DEAD_FENCE.getDefaultState();
	private static final BlockState LOG = ModBlocks.DEAD_LOG.getDefaultState();
	private static final BlockState LANTERN = ModBlocks.EMBER_LANTERN.getDefaultState();
	private static final BlockState GRATE = ReachBlocks.GRAVE_IRON_GRATE.getDefaultState();
	private static final BlockState PATH = ReachBlocks.ASH_COBBLE_PATH.getDefaultState();

	public static Map<Identifier, TemplateBuilder> all() {
		Map<Identifier, TemplateBuilder> templates = new LinkedHashMap<>();
		for (ReachZoneKit zone : ReachZoneKit.ALL) {
			templates.put(Embervoid.id("ashen_reach/outpost/toll_gate_" + zone.name()), tollGate(zone));
			templates.put(Embervoid.id("ashen_reach/outpost/watchtower_" + zone.name()), watchtower(zone));
		}
		return templates;
	}

	/** Two ruined gate towers either side of the road, a broken arch, a barrier half across. */
	static TemplateBuilder tollGate(ReachZoneKit zone) {
		Random random = ReachZoneKit.random("toll_gate_" + zone.name());
		Ruin.Palette p = Ruin.of(zone);
		TemplateBuilder t = new TemplateBuilder(11, 9, 9);
		road(t, 3, 7, 9);
		t.blendedFloor(0, 0, 2, 8, 0, random, PATH, 3, p.floor(), 2, p.floorRare(), 1);
		t.blendedFloor(8, 0, 10, 8, 0, random, PATH, 3, p.floor(), 2, p.floorRare(), 1);
		// Towers: left 6 high, right broken at 4.
		tower(t, random, p, 0, 3, 6);
		tower(t, random, p, 8, 3, 4);
		t.set(9, 5, 4, LANTERN);
		// What is left of the arch: a springer and two voussoirs from the left tower, a jamb on the right.
		t.set(3, 5, 4, stair(BRICK_STAIRS, Direction.WEST, true));
		t.set(4, 5, 4, slab(BRICK_SLAB, true));
		t.set(3, 6, 4, CARVED);
		t.set(7, 4, 4, stair(BRICK_STAIRS, Direction.EAST, true));
		// The barrier: a grate and fence pieces across half the road.
		t.set(3, 1, 4, GRATE);
		t.set(4, 1, 4, FENCE);
		t.set(7, 1, 4, FENCE);
		t.chest(1, 1, 6, Direction.EAST, zone.commonChest());
		t.set(0, 1, 6, TILES);
		t.set(1, 1, 1, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.SOUTH));
		t.set(9, 1, 1, Props.BURNT_BARREL.getDefaultState());
		accent(t, zone, 9, 1, 7);
		ReachZoneKit.mob(t, 5, 1, 2, zone.fodder(), Direction.NORTH);
		ReachZoneKit.mob(t, 4, 1, 6, zone.fodder(), Direction.NORTH);
		ReachZoneKit.mob(t, 1, 7, 4, zone.ranged(), Direction.NORTH);
		return t;
	}

	/** A five-block square tower beside the road, a ladder to a fenced platform under a slate hood. */
	static TemplateBuilder watchtower(ReachZoneKit zone) {
		Random random = ReachZoneKit.random("watchtower_" + zone.name());
		Ruin.Palette p = Ruin.of(zone);
		TemplateBuilder t = new TemplateBuilder(21, 14, 9);
		road(t, 8, 12, 9);
		t.blendedFloor(13, 0, 20, 8, 0, random, PATH, 3, p.floor(), 2, p.floorRare(), 1);
		// Walls x 15–19, z 2–6: a cobble-brick base course, weathered brick, pillar corners, a doorway towards the road.
		t.blendedFloor(15, 2, 19, 6, 0, random, TILES, 4, CRACKED_TILES, 2, POLISHED, 1);
		for (int y = 1; y <= 9; y++) {
			for (int x = 15; x <= 19; x++) {
				for (int z = 2; z <= 6; z++) {
					boolean edgeX = x == 15 || x == 19;
					boolean edgeZ = z == 2 || z == 6;
					if (edgeX && edgeZ) {
						t.set(x, y, z, y == 9 ? CARVED : PILLAR);
					} else if (edgeX || edgeZ) {
						t.set(x, y, z, y == 1 ? COBBLE_BRICKS : wall(random, p, y, 9));
					} else {
						t.set(x, y, z, AIR);
					}
				}
			}
		}
		t.stringCourse(15, 2, 19, 6, 5, COBBLE_BRICK_SLAB);
		t.set(15, 1, 4, AIR);
		t.set(15, 2, 4, AIR);
		t.set(15, 3, 4, stair(BRICK_STAIRS, Direction.EAST, true));
		t.set(14, 0, 4, COBBLES);
		t.set(13, 0, 4, COBBLES);
		t.set(17, 4, 2, GRATE).set(19, 7, 4, GRATE).set(17, 7, 6, GRATE);
		// Ladder on the inner east wall up through the platform.
		for (int y = 1; y <= 10; y++) {
			t.set(18, y, 4, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.WEST));
		}
		t.cornice(15, 2, 19, 6, 9, COBBLE_BRICK_STAIRS, COBBLE_BRICKS);
		for (int x = 15; x <= 19; x++) {
			for (int z = 2; z <= 6; z++) {
				if (!(x == 18 && z == 4)) {
					t.set(x, 10, z, random.nextInt(6) == 0 ? ReachBlocks.CHARRED_PLANKS.getDefaultState() : PLANKS);
				}
				if (x == 15 || x == 19 || z == 2 || z == 6) {
					boolean corner = (x == 15 || x == 19) && (z == 2 || z == 6);
					t.set(x, 11, z, corner ? LOG.with(PillarBlock.AXIS, Direction.Axis.Y) : random.nextInt(5) == 0 ? AIR : FENCE);
				} else {
					t.set(x, 11, z, AIR);
				}
				t.fill(x, 12, z, x, 13, z, AIR);
			}
		}
		// The hood: log posts on the corners carry a slate roof pitched along z, half of it gone.
		for (int[] c : new int[][]{{15, 2}, {19, 2}, {15, 6}, {19, 6}}) {
			t.set(c[0], 12, c[1], LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
		}
		t.pitchedRoof(15, 2, 19, 6, 13, Direction.Axis.Z, SLATE_STAIRS, SLATE_SLAB, random, 0.6);
		t.set(19, 12, 6, LANTERN.with(LanternBlock.HANGING, false));
		t.set(16, 1, 1, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH));
		t.chest(16, 1, 5, Direction.EAST, zone.commonChest());
		t.set(16, 1, 3, Props.GRAIN_SACKS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.EAST));
		t.set(17, 11, 5, Props.WEAPON_RACK.getDefaultState().with(PropBlock.FACING, Direction.NORTH));
		accent(t, zone, 14, 1, 8);
		ReachZoneKit.mob(t, 13, 1, 2, zone.fodder(), Direction.NORTH);
		ReachZoneKit.mob(t, 14, 1, 6, zone.fodder(), Direction.NORTH);
		ReachZoneKit.mob(t, 16, 11, 3, zone.ranged(), Direction.NORTH);
		return t;
	}

	/** The paved road through the template, as the road feature paves it (PLAN 14.2): flagstones, a kerb, air above. */
	private static void road(TemplateBuilder t, int x0, int x1, int length) {
		for (int x = x0; x <= x1; x++) {
			for (int z = 0; z < length; z++) {
				t.set(x, 0, z, x == x0 || x == x1 ? KERB : FLAGSTONES);
				t.fill(x, 1, z, x, 3, z, AIR);
			}
		}
	}

	/** A solid 3 × 3 gate tower at x0, z0, {@code height} high, pillar quoins, its top course broken into merlons. */
	private static void tower(TemplateBuilder t, Random random, Ruin.Palette p, int x0, int z0, int height) {
		for (int x = x0; x < x0 + 3; x++) {
			for (int z = z0; z < z0 + 3; z++) {
				boolean corner = (x == x0 || x == x0 + 2) && (z == z0 || z == z0 + 2);
				t.set(x, 0, z, TILES);
				for (int y = 1; y <= height; y++) {
					boolean top = y == height;
					if (top && random.nextInt(3) == 0) {
						t.set(x, y, z, AIR);
					} else if (corner) {
						t.set(x, y, z, top ? CARVED : y == 1 ? COBBLE_BRICKS : PILLAR);
					} else {
						t.set(x, y, z, y == 1 ? COBBLE_BRICKS : wall(random, p, y, height));
					}
				}
			}
		}
		t.stringCourse(x0, z0, x0 + 2, z0 + 2, 1, COBBLE_BRICK_SLAB);
		t.cornice(x0, z0, x0 + 2, z0 + 2, height, COBBLE_BRICK_STAIRS, null);
		for (int x = x0; x < x0 + 3; x++) {
			if (random.nextBoolean()) {
				t.trySet(x, height + 1, z0, COBBLE_BRICK_WALL);
			}
		}
	}

	/** A wall block: the zone's wall, cracked more with height, mossy at the foot. */
	private static BlockState wall(Random random, Ruin.Palette p, int y, int top) {
		double t = (double) y / top;
		int roll = random.nextInt(10);
		return roll < 1 + 2 * t ? p.cracked() : roll == 9 && t < 0.4 ? p.mossy() : BRICKS;
	}

	/** A small touch of the zone next to the outpost. */
	private static void accent(TemplateBuilder t, ReachZoneKit zone, int x, int y, int z) {
		switch (zone.name()) {
			case "fields" -> {
				t.set(x, y, z, ReachBlocks.BURNT_STRAW.getDefaultState());
				t.set(x, y + 1, z, ReachBlocks.RUSTED_BRASS_BLOCK.getDefaultState());
			}
			case "woods" -> {
				t.set(x, y, z, LOG.with(PillarBlock.AXIS, Direction.Axis.Z));
				t.set(x, y + 1, z, ReachBlocks.AMBER_BLOCK.getDefaultState());
			}
			default -> {
				t.set(x, y, z, ReachBlocks.GRAVESTONE.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
				t.set(x, y + 1, z, Ruin.BLUE_CANDLE);
			}
		}
	}

	private OutpostTemplates() {}
}
