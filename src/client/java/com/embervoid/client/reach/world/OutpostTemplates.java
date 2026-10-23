package com.embervoid.client.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Road outposts (PLAN 9.6), one per kind and zone: {@code embervoid:ashen_reach/outpost/<toll_gate|watchtower>_<zone>}.
 * The road runs the whole length of each template along +Z (north = the landing side), centred on its middle column
 * (the road feature centres the template on the road); y = 0 is the ground. Placed fodder and ranged mobs of the zone,
 * a common chest.
 */
public final class OutpostTemplates {
	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final BlockState BRICKS = ModBlocks.ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = ModBlocks.CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState COBBLES = ModBlocks.COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState FLAGSTONES = ReachBlocks.KINGSROAD_FLAGSTONES.getDefaultState();
	private static final BlockState KERB = ReachBlocks.KINGSROAD_KERB.getDefaultState();
	private static final BlockState POLISHED = ModBlocks.POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState PLANKS = ModBlocks.DEAD_PLANKS.getDefaultState();
	private static final BlockState FENCE = ModBlocks.DEAD_FENCE.getDefaultState();
	private static final BlockState LOG = ModBlocks.DEAD_LOG.getDefaultState();
	private static final BlockState LANTERN = ModBlocks.EMBER_LANTERN.getDefaultState();

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
		TemplateBuilder t = new TemplateBuilder(11, 9, 9);
		road(t, 3, 7, 9);
		// Towers: left 6 high, right broken at 4.
		tower(t, random, 0, 3, 6);
		tower(t, random, 8, 3, 4);
		t.set(9, 5, 4, LANTERN);
		// What is left of the arch: a stub from the left tower.
		t.set(3, 5, 4, ModBlocks.ASH_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
		t.set(4, 5, 4, ModBlocks.ASH_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
		// The barrier: fence pieces across half the road.
		t.set(3, 1, 4, FENCE);
		t.set(4, 1, 4, FENCE);
		t.set(7, 1, 4, FENCE);
		t.chest(1, 1, 6, Direction.EAST, zone.commonChest());
		t.set(0, 1, 6, POLISHED);
		accent(t, zone, 9, 1, 7);
		ReachZoneKit.mob(t, 5, 1, 2, zone.fodder(), Direction.NORTH);
		ReachZoneKit.mob(t, 4, 1, 6, zone.fodder(), Direction.NORTH);
		ReachZoneKit.mob(t, 1, 7, 4, zone.ranged(), Direction.NORTH);
		return t;
	}

	/** A five-block square tower beside the road, a ladder to a fenced platform at the top. */
	static TemplateBuilder watchtower(ReachZoneKit zone) {
		Random random = ReachZoneKit.random("watchtower_" + zone.name());
		TemplateBuilder t = new TemplateBuilder(21, 14, 9);
		road(t, 8, 12, 9);
		// Walls x 15–19, z 2–6, dead-log corners, a doorway towards the road.
		for (int y = 0; y <= 9; y++) {
			for (int x = 15; x <= 19; x++) {
				for (int z = 2; z <= 6; z++) {
					boolean edgeX = x == 15 || x == 19;
					boolean edgeZ = z == 2 || z == 6;
					if (y == 0) {
						t.set(x, y, z, POLISHED);
					} else if (edgeX && edgeZ) {
						t.set(x, y, z, LOG.with(PillarBlock.AXIS, Direction.Axis.Y));
					} else if (edgeX || edgeZ) {
						t.set(x, y, z, wall(random, y));
					} else {
						t.set(x, y, z, AIR);
					}
				}
			}
		}
		t.set(15, 1, 4, AIR);
		t.set(15, 2, 4, AIR);
		t.set(14, 0, 4, COBBLES);
		t.set(13, 0, 4, COBBLES);
		// Ladder on the inner east wall up through the platform.
		for (int y = 1; y <= 10; y++) {
			t.set(18, y, 4, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.WEST));
		}
		for (int x = 15; x <= 19; x++) {
			for (int z = 2; z <= 6; z++) {
				if (!(x == 18 && z == 4)) {
					t.set(x, 10, z, PLANKS);
				}
				if (x == 15 || x == 19 || z == 2 || z == 6) {
					t.set(x, 11, z, random.nextInt(5) == 0 ? AIR : FENCE);
				} else {
					t.set(x, 11, z, AIR);
				}
				t.fill(x, 12, z, x, 13, z, AIR);
			}
		}
		t.set(19, 12, 6, LANTERN.with(LanternBlock.HANGING, false));
		t.set(19, 11, 6, FENCE);
		t.chest(16, 1, 5, Direction.EAST, zone.commonChest());
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

	/** A solid 3 × 3 gate tower at x0, z0, {@code height} high, its top course broken. */
	private static void tower(TemplateBuilder t, Random random, int x0, int z0, int height) {
		for (int x = x0; x < x0 + 3; x++) {
			for (int z = z0; z < z0 + 3; z++) {
				t.set(x, 0, z, POLISHED);
				for (int y = 1; y <= height; y++) {
					boolean top = y == height;
					t.set(x, y, z, top && random.nextInt(3) == 0 ? AIR : wall(random, y));
				}
			}
		}
	}

	private static BlockState wall(Random random, int y) {
		int roll = random.nextInt(10);
		return roll < 2 ? CRACKED : (roll == 2 && y > 3 ? COBBLES : BRICKS);
	}

	/** A small touch of the zone next to the outpost. */
	private static void accent(TemplateBuilder t, ReachZoneKit zone, int x, int y, int z) {
		switch (zone.name()) {
			case "fields" -> {
				t.set(x, y, z, ReachBlocks.BURNT_STRAW.getDefaultState());
				t.set(x, y + 1, z, ReachBlocks.BURNT_STRAW.getDefaultState());
			}
			case "woods" -> t.set(x, y, z, LOG.with(PillarBlock.AXIS, Direction.Axis.Z));
			default -> t.set(x, y, z, ReachBlocks.GRAVESTONE.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
		}
	}

	private OutpostTemplates() {}
}
