package com.embervoid.sanctum.datagen;

import com.embervoid.Embervoid;
import com.embervoid.block.prop.HangingPropBlock;
import com.embervoid.block.setpiece.SetPieceBlock;
import com.embervoid.block.setpiece.SetPieces;
import com.embervoid.block.prop.LongPropBlock;
import com.embervoid.block.prop.PropBlock;
import com.embervoid.block.prop.PropCandleBlock;
import com.embervoid.block.prop.Props;
import com.embervoid.block.prop.StackedPropBlock;
import com.embervoid.block.prop.TallPropBlock;
import com.embervoid.entity.mob.MobEntities;
import com.embervoid.hub.stele.SteleBlocks;
import com.embervoid.hub.stele.SteleFrame;
import com.embervoid.progress.ProgressManager;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.sanctum.SanctumArena;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumEntities;
import com.embervoid.sanctum.SanctumGuards;
import com.embervoid.sanctum.SanctumVariants;
import com.embervoid.sanctum.boss.SanctumBossArena;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.JigsawOrientation;
import net.minecraft.block.enums.SlabType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import static com.embervoid.sanctum.datagen.TemplateBuilder.stair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static com.embervoid.registry.ModBlocks.*;

/**
 * The Ruined Sanctum's jigsaw pieces, built in code (there is no in-game building in this workflow).
 *
 * <pre>
 * ruin     (surface, start) ─down─► descent          ─► gate_hall     ─┬─► arena (shrine, fog, boss)
 * chapel   (surface, start) ─down─► descent_chapel   ─► nave          ─┤
 * cloister (surface, start) ─down─► descent_cloister ─► cloister_walk ─┘
 *                                                          └─ sides ─► halls pool (corridor / chamber / crypt / storeroom)
 * </pre>
 * Three variants (PLAN 16.2): the start pool picks the surface ruin, and each ruin leads through its own stairwell to
 * its own guarded hall; all three halls lead on to the same boss wing and side halls, with the same guards and loot.
 * The path to the arena is a fixed chain, so every Sanctum has one; the side halls are random. The arena is one piece
 * with its shrine room, so it is placed in the same step as the side halls, like the core before it, and nothing
 * built later can take its place. The old core (Maiden, obelisks) is still written: Sanctums planned before 0.5.0
 * still place it, but no pool leads to it any more (PLAN 16.1).
 * Horizontal connectors sit in the middle of 3x3 doorways at feet level (y = 1).
 */
final class SanctumTemplates {
	static final Identifier POOL_DESCENT = Embervoid.id("sanctum/descent");
	static final Identifier POOL_GATE = Embervoid.id("sanctum/gate_hall");
	static final Identifier POOL_ARENA = Embervoid.id("sanctum/arena");
	static final Identifier POOL_HALLS = Embervoid.id("sanctum/halls");
	static final Identifier POOL_DESCENT_CHAPEL = Embervoid.id("sanctum/descent_chapel");
	static final Identifier POOL_NAVE = Embervoid.id("sanctum/nave");
	static final Identifier POOL_DESCENT_CLOISTER = Embervoid.id("sanctum/descent_cloister");
	static final Identifier POOL_CLOISTER_WALK = Embervoid.id("sanctum/cloister_walk");

	private static final Identifier RUIN_DOWN = Embervoid.id("sanctum_ruin_down");
	private static final Identifier DESCENT_TOP = Embervoid.id("sanctum_descent_top");
	private static final Identifier HALL_IN = Embervoid.id("sanctum_hall_in");
	private static final Identifier HALL_OUT = Embervoid.id("sanctum_hall_out");
	private static final Identifier CORE_IN = Embervoid.id("sanctum_core_in");
	private static final Identifier ARENA_IN = Embervoid.id("sanctum_arena_in");

	static final Identifier OUTER_HALL_LOOT = Embervoid.id("chests/sanctum_outer_hall");
	private static final String HOLLOW = Registries.ENTITY_TYPE.getId(MobEntities.HOLLOW).toString();
	private static final String SKELETON = "minecraft:skeleton";

	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final BlockState BRICKS = ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState MOSSY = MOSSY_ASH_BRICKS.getDefaultState();
	private static final BlockState SOOTY = SOOTY_ASH_BRICKS.getDefaultState();
	private static final BlockState COBBLE_BRICKS = ASH_COBBLE_BRICKS.getDefaultState();
	private static final BlockState COBBLE_BRICK_STAIRS = ASH_COBBLE_BRICK_STAIRS.getDefaultState();
	private static final BlockState COBBLE_BRICK_SLAB = ASH_COBBLE_BRICK_SLAB.getDefaultState();
	private static final BlockState POLISHED = POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState CHISELED = CHISELED_ASH_STONE.getDefaultState();
	private static final BlockState CARVED = CARVED_ASH_STONE.getDefaultState();
	private static final BlockState PILLAR = ASH_BRICK_PILLAR.getDefaultState();
	private static final BlockState TILES = ASH_TILES.getDefaultState();
	private static final BlockState CRACKED_TILES = CRACKED_ASH_TILES.getDefaultState();
	private static final BlockState RUNE = SANCTUM_RUNE_STONE.getDefaultState();
	private static final BlockState BRICK_STAIRS = ASH_BRICK_STAIRS.getDefaultState();
	private static final BlockState SLATE_SLAB = SLATE_TILE_SLAB.getDefaultState();
	private static final BlockState SLATE_STAIRS = SLATE_TILE_STAIRS.getDefaultState();
	private static final BlockState COBBLED = COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState MOSSY_COBBLED = MOSSY_COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState STONE = ASH_STONE.getDefaultState();
	private static final BlockState SOIL = ASH_SOIL.getDefaultState();
	private static final BlockState PATH = ReachBlocks.ASH_COBBLE_PATH.getDefaultState();
	private static final BlockState GRATE = ReachBlocks.GRAVE_IRON_GRATE.getDefaultState();
	private static final BlockState WALL = ASH_BRICK_WALL.getDefaultState();
	private static final BlockState FLAGSTONE = SanctumBlocks.SANCTUM_FLAGSTONE.getDefaultState();
	private static final BlockState LANTERN = EMBER_LANTERN.getDefaultState();
	private static final BlockState HANGING_LANTERN = LANTERN.with(LanternBlock.HANGING, true);

	/** All pieces by template id (data/embervoid/structures/&lt;id&gt;.nbt). */
	static Map<Identifier, TemplateBuilder> all() {
		Map<Identifier, TemplateBuilder> pieces = new LinkedHashMap<>();
		pieces.put(SanctumVariants.RUIN, ruin());
		pieces.put(Embervoid.id("sanctum/descent"), descent(POOL_GATE));
		pieces.put(Embervoid.id("sanctum/gate_hall"), gateHall());
		pieces.put(SanctumVariants.CHAPEL, chapel());
		pieces.put(Embervoid.id("sanctum/descent_chapel"), chapelDescent());
		pieces.put(Embervoid.id("sanctum/nave"), nave());
		pieces.put(SanctumVariants.CLOISTER, cloister());
		pieces.put(Embervoid.id("sanctum/descent_cloister"), cloisterDescent());
		pieces.put(Embervoid.id("sanctum/cloister_walk"), cloisterWalk());
		pieces.put(Embervoid.id("sanctum/core"), core());
		pieces.put(SanctumArena.TEMPLATE, arena());
		pieces.put(Embervoid.id("sanctum/hall_corridor"), hallCorridor());
		pieces.put(Embervoid.id("sanctum/hall_chamber"), hallChamber());
		pieces.put(Embervoid.id("sanctum/hall_crypt"), hallCrypt());
		pieces.put(Embervoid.id("sanctum/hall_storeroom"), hallStoreroom());
		pieces.put(Embervoid.id("sanctum/hall_cap"), hallCap());
		return pieces;
	}

	/** Solid layers under the ruin's floor, so it sits on ground even on a slope (the structure carves nothing). */
	static final int RUIN_FOUNDATION = 3;

	/**
	 * Surface ruin, 15x11x15: a {@value #RUIN_FOUNDATION}-block foundation, then the ground layer (the structure
	 * starts that far below the surface, see worldgen/structure/ember_sanctum.json). A railed 5x5 shaft in the middle
	 * leads down through the foundation to the descent.
	 */
	private static TemplateBuilder ruin() {
		TemplateBuilder t = new TemplateBuilder(15, RUIN_FOUNDATION + 8, 15);
		Random random = new Random(1);
		t.fill(0, 0, 0, 14, RUIN_FOUNDATION - 1, 14, COBBLED, COBBLE_BRICKS, 0.3, random);
		t.fill(5, 0, 5, 9, RUIN_FOUNDATION - 1, 9, AIR);
		t.jigsaw(7, 0, 7, JigsawOrientation.DOWN_SOUTH, RUIN_DOWN, POOL_DESCENT, DESCENT_TOP, AIR);
		t.baseY(RUIN_FOUNDATION);
		// The floor: the old hall's tiles in the middle, cobbles and soil creeping in from the edges.
		for (int x = 0; x < 15; x++) {
			for (int z = 0; z < 15; z++) {
				double edge = Math.min(Math.min(x, 14 - x), Math.min(z, 14 - z)) / 4.0;
				double r = random.nextDouble();
				t.set(x, 0, z, r < 0.35 * edge ? TILES : r < 0.5 * edge ? CRACKED_TILES : r < 0.65 ? COBBLED : r < 0.8 ? MOSSY_COBBLED : r < 0.9 ? STONE : SOIL);
			}
		}
		// Clear terrain inside the ruin; the walls below replace some of this air.
		t.fill(0, 1, 0, 14, 7, 14, AIR);
		// Broken outer walls with a gap in the middle of each side: a cobble-brick base course, bricks cracking
		// upwards and mossy at the foot, a slab coping where the top is level, rubble inside where they fell.
		for (int i = 0; i < 15; i++) {
			if (i >= 6 && i <= 8) {
				continue;
			}
			for (int[] p : new int[][]{{i, 0}, {i, 14}, {0, i}, {14, i}}) {
				int height = random.nextInt(5);
				for (int y = 1; y <= height; y++) {
					t.set(p[0], y, p[1], TemplateBuilder.weathered(random, (y - 1) / 4.0, 0, COBBLE_BRICKS, 2, y, BRICKS, CRACKED, MOSSY));
				}
				if (height > 0 && height < 4 && random.nextBoolean()) {
					t.set(p[0], height + 1, p[1], ASH_BRICK_SLAB.getDefaultState());
				}
				if (height == 0 && random.nextInt(3) == 0) {
					int ix = p[0] == 0 ? 1 : p[0] == 14 ? 13 : p[0];
					int iz = p[1] == 0 ? 1 : p[1] == 14 ? 13 : p[1];
					t.set(ix, 1, iz, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, p[1] == 0 ? Direction.NORTH : p[1] == 14 ? Direction.SOUTH : p[0] == 0 ? Direction.WEST : Direction.EAST));
				}
			}
		}
		// Corner pillars with carved capitals; one has fallen.
		int[][] corners = {{0, 0}, {14, 0}, {0, 14}, {14, 14}};
		for (int c = 0; c < corners.length; c++) {
			if (c == 3) {
				brokenPillar(t, corners[c][0], corners[c][1], 1, 3);
			} else {
				pillar(t, corners[c][0], corners[c][1], 1, 7);
			}
		}
		// The gateposts of each gap, a lintel stump on the north one.
		for (int[] g : new int[][]{{5, 0}, {9, 0}, {5, 14}, {9, 14}, {0, 5}, {0, 9}, {14, 5}, {14, 9}}) {
			t.fill(g[0], 1, g[1], g[0], 2 + random.nextInt(2), g[1], PILLAR);
		}
		t.set(5, 4, 0, CARVED).set(6, 4, 0, stair(BRICK_STAIRS, Direction.WEST, true));
		// The fallen pillar and a dead log.
		t.fill(10, 1, 12, 12, 1, 12, PILLAR.with(PillarBlock.AXIS, Direction.Axis.X)).set(13, 1, 12, CARVED);
		t.set(2, 1, 11, DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z))
				.set(2, 1, 12, DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z))
				.set(2, 1, 13, DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z));
		t.set(12, 1, 2, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2)).set(2, 1, 3, Props.BONE_PILE.getDefaultState());

		// The shaft: rim of tiles with a rune at each corner, a wall rail with an opening on the north side, open below.
		for (int x = 4; x <= 10; x++) {
			for (int z = 4; z <= 10; z++) {
				boolean rim = x == 4 || x == 10 || z == 4 || z == 10;
				boolean corner = (x == 4 || x == 10) && (z == 4 || z == 10);
				if (rim) {
					t.set(x, 0, z, corner ? RUNE : random.nextInt(5) == 0 ? CRACKED_TILES : TILES);
					if (!(z == 4 && x >= 6 && x <= 8)) {
						t.set(x, 1, z, corner ? COBBLE_BRICKS : WALL);
					}
				} else {
					t.set(x, 0, z, AIR);
				}
			}
		}
		t.set(4, 2, 4, LANTERN).set(10, 2, 4, LANTERN).set(4, 2, 10, LANTERN).set(10, 2, 10, LANTERN);
		return t;
	}

	/**
	 * Spiral stairwell, 11x20x11: stairs wind down around a 3x3 column on the outer ring of the 5x5 inside and end at
	 * a doorway in the south wall. The top layer joins the ruin's shaft. The wall is three thick: bricks inside, then
	 * solid rock, then a ragged rock layer with gaps where the world keeps its own blocks (rock where that is water or lava), so caves never break in
	 * and a shaft cut open by a ravine looks like a pillar of rock rather than a brick tower. The doorway leads to
	 * {@code gatePool}, the variant's guarded hall.
	 */
	private static TemplateBuilder descent(Identifier gatePool) {
		int height = 20;
		int wall = 3;
		int size = 5 + 2 * wall;
		int in = wall; // first inside x/z
		TemplateBuilder t = new TemplateBuilder(size, height, size);
		t.fill(0, 0, 0, size - 1, height - 1, size - 1, BRICKS);
		Random random = new Random(2);
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				int ring = Math.min(Math.min(x, z), Math.min(size - 1 - x, size - 1 - z));
				if (ring > 1) {
					continue;
				}
				for (int y = 0; y < height; y++) {
					if (ring == 0 && random.nextDouble() < 0.4) {
						// A gap: the world keeps its block, or gets rock if it is a fluid (CasingGapProcessor).
						t.set(x, y, z, Blocks.STRUCTURE_VOID.getDefaultState());
					} else {
						t.set(x, y, z, rock(random));
					}
				}
			}
		}
		t.fill(in, 1, in, in + 4, height - 1, in + 4, AIR);
		t.blendedFloor(in, in, in + 4, in + 4, 0, random, TILES, 4, CRACKED_TILES, 1, POLISHED, 2);
		for (int y = 0; y <= height - 2; y++) {
			BlockState block = y % 5 == 4 ? CARVED : y % 5 == 0 ? CHISELED : PILLAR;
			t.fill(in + 1, y, in + 1, in + 3, y, in + 3, block);
		}
		// The shaft's inner wall: a base course, bricks cracking downwards (the deep is wetter), mossy at the bottom.
		for (int x = in - 1; x <= in + 5; x++) {
			for (int z = in - 1; z <= in + 5; z++) {
				if (x != in - 1 && x != in + 5 && z != in - 1 && z != in + 5) {
					continue;
				}
				for (int y = 1; y < height - 1; y++) {
					double h = 1 - (double) y / height;
					t.set(x, y, z, TemplateBuilder.weathered(random, h, 0, COBBLE_BRICKS, 2, y, BRICKS, CRACKED, MOSSY));
				}
			}
		}

		// Outer ring of the 5x5 inside, clockwise from the north-west corner.
		int[][] ring = new int[16][];
		int n = 0;
		for (int x = 0; x <= 4; x++) ring[n++] = new int[]{in + x, in};
		for (int z = 1; z <= 4; z++) ring[n++] = new int[]{in + 4, in + z};
		for (int x = 3; x >= 0; x--) ring[n++] = new int[]{in + x, in + 4};
		for (int z = 3; z >= 1; z--) ring[n++] = new int[]{in, in + z};
		int doorIndex = 10; // middle of the south side, in front of the door
		int steps = height - 1;
		for (int k = 0; k < steps; k++) {
			int[] cell = ring[Math.floorMod(doorIndex - (steps - k), 16)];
			int[] up = ring[Math.floorMod(doorIndex - (steps - k) - 1, 16)];
			Direction facing = Direction.fromVector(up[0] - cell[0], 0, up[1] - cell[1]);
			t.set(cell[0], height - 1 - k, cell[1], POLISHED_ASH_STONE_STAIRS.getDefaultState().with(StairsBlock.FACING, facing));
		}
		int center = in + 2;
		// Doorway through the wall, with solid sides and top even where the outer layer is ragged.
		t.fill(center - 2, 0, in + 5, center + 2, 4, size - 1, BRICKS);
		t.fill(center - 1, 1, in + 5, center + 1, 3, size - 1, AIR);
		t.set(center - 2, 1, in + 5, PILLAR).set(center + 2, 1, in + 5, PILLAR).set(center, 4, in + 5, CARVED);
		t.set(in, 1, in, LANTERN);
		t.set(in + 4, 7, in, GRATE).set(in, 13, in + 4, GRATE);
		t.door(center, 1, size - 1, Direction.SOUTH, HALL_OUT, gatePool, HALL_IN);
		t.jigsaw(center, height - 1, center, JigsawOrientation.UP_SOUTH, DESCENT_TOP, null, null, AIR);
		return t;
	}

	/**
	 * Guarded hall between the descent and the arena, 9x7x23, with loot and two side exits. Its size and blocks stay
	 * as they were when it led to the core, so a Sanctum planned before 0.5.0 still fits around it.
	 */
	private static TemplateBuilder gateHall() {
		TemplateBuilder t = new TemplateBuilder(9, 7, 23);
		Random random = new Random(3);
		hall(t, random, 0, 0, 0, 8, 6, 22, true);
		for (int z = 2; z <= 20; z += 4) {
			t.set(4, 0, z, RUNE);
		}
		for (int z : new int[]{4, 8, 14, 18}) {
			pillar(t, 1, z, 1, 5);
			pillar(t, 7, z, 1, 5);
			t.set(0, 3, z + 2, GRATE).set(8, 3, z - 2, GRATE);
		}
		doorway(t, 4, 0, Direction.NORTH);
		doorway(t, 4, 22, Direction.SOUTH);
		doorway(t, 0, 11, Direction.WEST);
		doorway(t, 8, 11, Direction.EAST);
		t.door(4, 1, 0, Direction.NORTH, HALL_IN, null, null);
		t.door(4, 1, 22, Direction.SOUTH, HALL_OUT, POOL_ARENA, ARENA_IN);
		t.door(0, 1, 11, Direction.WEST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.door(8, 1, 11, Direction.EAST, HALL_OUT, POOL_HALLS, HALL_IN);
		// Chiseled frame around the way on to the arena.
		t.fill(2, 1, 21, 2, 4, 21, CHISELED).fill(6, 1, 21, 6, 4, 21, CHISELED).fill(3, 4, 21, 5, 4, 21, CHISELED);
		t.set(4, 5, 6, HANGING_LANTERN).set(4, 5, 16, HANGING_LANTERN);
		sootOver(t, random, 4, 5, 6);
		sootOver(t, random, 4, 5, 16);
		t.set(1, 1, 20, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.EAST))
				.set(7, 1, 2, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST));
		t.chest(2, 1, 2, Direction.EAST, OUTER_HALL_LOOT);
		t.chest(6, 1, 19, Direction.WEST, OUTER_HALL_LOOT);
		t.entity(4, 1, 12, guard(HOLLOW, null));
		t.entity(4, 1, 18, guard(SKELETON, null));
		return t;
	}

	/**
	 * The core of Sanctums before 0.5.0, 21x11x21: a safe room floored with Sanctum Flagstone (unbreakable,
	 * spawn-proof, attunes), the Ember Shrine in the middle, four Void Obelisks in a ring and the Ashen Maiden. No pool
	 * leads here any more; the template stays for Sanctums half generated in old worlds.
	 */
	private static TemplateBuilder core() {
		TemplateBuilder t = new TemplateBuilder(21, 11, 21);
		t.room(FLAGSTONE, POLISHED, BRICKS);
		for (int i = 4; i <= 16; i += 4) {
			for (int[] p : new int[][]{{i, 0}, {i, 20}, {0, i}, {20, i}}) {
				t.fill(p[0], 1, p[1], p[0], 9, p[1], CHISELED);
			}
		}
		// Cornice along the walls under the ceiling.
		BlockState cornice = ASH_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
		t.fill(1, 9, 1, 19, 9, 1, cornice).fill(1, 9, 19, 19, 9, 19, cornice)
				.fill(1, 9, 1, 1, 9, 19, cornice).fill(19, 9, 1, 19, 9, 19, cornice);
		doorway(t, 10, 0, Direction.NORTH);
		t.fill(9, 0, 0, 11, 0, 0, FLAGSTONE);
		t.door(10, 1, 0, Direction.NORTH, CORE_IN, null, null);

		t.set(10, 1, 10, SanctumBlocks.EMBER_SHRINE.getDefaultState());
		int[][] obelisks = {{5, 5}, {15, 5}, {15, 15}, {5, 15}};
		for (int i = 0; i < obelisks.length; i++) {
			NbtCompound data = new NbtCompound();
			Identifier realm = i == 0 ? ProgressManager.REALM_1 : Embervoid.id("realm_" + (i + 1));
			data.putString("Realm", realm.toString());
			t.blockEntity(obelisks[i][0], 1, obelisks[i][1], SanctumBlocks.VOID_OBELISK.getDefaultState(),
					"embervoid:void_obelisk", data);
		}
		// The Ashen Maiden stands south of the shrine, facing the entrance.
		NbtCompound maiden = new NbtCompound();
		maiden.putString("id", Registries.ENTITY_TYPE.getId(SanctumEntities.ASHEN_MAIDEN).toString());
		maiden.putBoolean("PersistenceRequired", true);
		NbtList rotation = new NbtList();
		rotation.add(NbtFloat.of(180));
		rotation.add(NbtFloat.of(0));
		maiden.put("Rotation", rotation);
		t.entity(10, 1, 15, maiden);
		for (int[] p : new int[][]{{10, 5}, {10, 15}, {5, 10}, {15, 10}, {3, 3}, {17, 3}, {3, 17}, {17, 17}}) {
			t.set(p[0], 9, p[1], HANGING_LANTERN);
		}
		return t;
	}

	/**
	 * The boss wing, 21x11x30 (PLAN 16.1, DESIGN §2): a 9x7x10 shrine room at the gate hall's door with an Ember
	 * Shrine (rest and flasks; overworld shrines set no respawn point), a fog wall in its far doorway, then the boss
	 * arena where the core was: the core's 21x21 hall, ruined and dim, with unlit braziers along the sides and a cold
	 * Cleft Stele on a dais at the far end ({@link SanctumArena} keeps it from being mined). The Fallen Sister is
	 * summoned at the arena's centre ({@link SanctumBossArena}, PLAN 16.3). Low candles and one high lantern keep every floor block at block
	 * light 1 or more, so nothing spawns in the arena.
	 */
	private static TemplateBuilder arena() {
		TemplateBuilder t = new TemplateBuilder(21, 11, 30);
		Random random = new Random(9);
		// The shrine room; the air above and beside it is left to the world.
		hall(t, random, 6, 0, 0, 14, 6, 9, true);
		doorway(t, 10, 0, Direction.NORTH);
		t.door(10, 1, 0, Direction.NORTH, ARENA_IN, null, null);
		for (int[] p : new int[][]{{7, 1}, {13, 1}, {7, 8}, {13, 8}}) {
			pillar(t, p[0], p[1], 1, 5);
		}
		t.fill(9, 0, 3, 11, 0, 5, RUNE).set(10, 0, 4, CHISELED);
		t.set(10, 1, 4, SanctumBlocks.EMBER_SHRINE.getDefaultState());
		t.set(7, 1, 3, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.EAST))
				.set(13, 1, 6, Props.ASH_URNS.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(12, 1, 7, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH));

		// The arena: the core's hall without its obelisks and Maiden, its chiseled stone gone but for the door and dais.
		hall(t, random, 0, 0, 9, 20, 10, 29, true);
		// The centre, where the Fallen Sister is summoned (PLAN 16.3).
		int cx = SanctumBossArena.TEMPLATE_CENTER.getX();
		int cz = SanctumBossArena.TEMPLATE_CENTER.getZ();
		for (int x = 1; x <= 19; x++) {
			for (int z = 10; z <= 28; z++) {
				double r = random.nextDouble();
				long ring = Math.round(Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)));
				t.set(x, 0, z, ring == 6 ? (random.nextInt(4) == 0 ? RUNE : BRICKS) : r < 0.15 ? CRACKED_TILES : r < 0.3 ? TILES : POLISHED);
			}
		}
		for (int i = 4; i <= 16; i += 4) {
			for (int[] p : new int[][]{{i, 9}, {i, 29}, {0, i + 9}, {20, i + 9}}) {
				t.set(p[0], 1, p[1], CHISELED).fill(p[0], 2, p[1], p[0], 8, p[1], PILLAR).set(p[0], 9, p[1], CARVED);
			}
		}
		// The cornice is the hall's; a stretch of it and a pilaster have come down.
		t.fill(14, 9, 28, 17, 9, 28, AIR).fill(20, 6, 21, 20, 9, 21, CRACKED);
		for (int z = 12; z <= 26; z += 7) {
			t.set(0, 5, z, GRATE).set(20, 5, z + 2, GRATE);
		}
		t.set(18, 1, 21, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(15, 1, 27, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH))
				.set(1, 1, 15, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST))
				.set(19, 1, 11, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.SOUTH))
				.set(5, 1, 27, CRACKED).set(5, 2, 27, ASH_BRICK_SLAB.getDefaultState());

		// The fog wall fills the doorway from the shrine room, in a chiseled frame.
		t.fill(9, 1, 9, 11, 3, 9, SanctumBlocks.FOG_WALL.getDefaultState());
		t.fill(8, 1, 9, 8, 3, 9, PILLAR).fill(12, 1, 9, 12, 3, 9, PILLAR).set(8, 4, 9, CARVED).set(12, 4, 9, CARVED)
				.fill(9, 4, 9, 11, 4, 9, CHISELED).set(10, 5, 9, RUNE);

		// Braziers on pedestals, unlit until the fight (PLAN 16.3 lights them as she weakens).
		BlockState brazier = Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false);
		for (int z : new int[]{13, 19, 24}) {
			t.set(3, 1, z, WALL).set(3, 2, z, brazier.with(CampfireBlock.FACING, Direction.EAST));
			t.set(17, 1, z, WALL).set(17, 2, z, brazier.with(CampfireBlock.FACING, Direction.WEST));
		}
		// Great braziers over the campfires follow them; censers under the cornice light at her phase (PLAN 20.2).
		for (int z : new int[]{13, 19, 24}) {
			t.set(3, 3, z, SetPieces.GREAT_BRAZIER.getDefaultState().with(SetPieceBlock.FACING, Direction.EAST));
			t.set(17, 3, z, SetPieces.GREAT_BRAZIER.getDefaultState().with(SetPieceBlock.FACING, Direction.WEST));
		}
		for (int z : new int[]{16, 22}) {
			t.set(1, 8, z, SetPieces.CENSER.getDefaultState().with(SetPieceBlock.FACING, Direction.EAST));
			t.set(19, 8, z, SetPieces.CENSER.getDefaultState().with(SetPieceBlock.FACING, Direction.WEST));
		}

		// The dais with the cold Cleft Stele: four columns of Cleft Stone, the crack open (SteleFrame, along X).
		t.blendedFloor(6, 26, 14, 28, 1, random, POLISHED, 4, TILES, 3, RUNE, 1);
		t.fill(6, 1, 25, 14, 1, 25, POLISHED_ASH_STONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
		BlockState cleft = SteleBlocks.CLEFT_STONE.getDefaultState();
		int steleZ = 27;
		for (int x : new int[]{8, 9, 11, 12}) {
			t.fill(x, 2, steleZ, x, 1 + SteleFrame.HEIGHT, steleZ, cleft);
		}
		t.fill(10, 2, steleZ, 10, 1 + SteleFrame.HEIGHT, steleZ, AIR);
		t.fill(7, 1, steleZ, 13, 1, steleZ, CHISELED);

		// Light: tallow candles low down and one ember lantern high over the centre.
		BlockState candles = Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true);
		for (int[] p : new int[][]{{4, 1, 13}, {16, 1, 13}, {4, 1, 24}, {16, 1, 24}, {7, 2, 26}, {13, 2, 26}, {2, 1, 10}, {18, 1, 10}}) {
			t.set(p[0], p[1], p[2], candles.with(PropCandleBlock.FACING, p[0] < cx ? Direction.EAST : Direction.WEST));
		}
		t.set(cx, 9, cz, HANGING_LANTERN);
		sootOver(t, random, cx, 9, cz);
		t.set(6, 9, 15, Props.IRON_CAGE.getDefaultState().with(HangingPropBlock.HANGING, true))
				.set(14, 9, 22, Props.IRON_CAGE.getDefaultState().with(HangingPropBlock.HANGING, true).with(PropBlock.FACING, Direction.EAST));

		return t;
	}

	/**
	 * Collapsed chapel, 17x13x21 (PLAN 16.2, the second start): a nave along z with its roof fallen in, the walls
	 * standing higher towards the apse, two rows of pillars, pews, an altar under two tattered standards at the south end
	 * and a few graves along both outer walls. The way down is a hole broken through the nave floor, railed like the
	 * ruin's shaft. Same foundation and ground layer as the ruin, so the structure's start height fits all three.
	 */
	private static TemplateBuilder chapel() {
		TemplateBuilder t = new TemplateBuilder(17, RUIN_FOUNDATION + 10, 21);
		Random random = new Random(11);
		int sx = 8;
		int sz = 9;
		t.fill(0, 0, 0, 16, RUIN_FOUNDATION - 1, 20, COBBLED, COBBLE_BRICKS, 0.3, random);
		t.fill(sx - 2, 0, sz - 2, sx + 2, RUIN_FOUNDATION - 1, sz + 2, AIR);
		t.jigsaw(sx, 0, sz, JigsawOrientation.DOWN_SOUTH, RUIN_DOWN, POOL_DESCENT_CHAPEL, DESCENT_TOP, AIR);
		t.baseY(RUIN_FOUNDATION);
		for (int x = 0; x < 17; x++) {
			for (int z = 0; z < 21; z++) {
				double r = random.nextDouble();
				boolean nave = x >= 3 && x <= 13 && z >= 1 && z <= 19;
				t.set(x, 0, z, nave ? (x == sx ? (z % 3 == 0 ? RUNE : TILES) : r < 0.55 ? TILES : r < 0.75 ? CRACKED_TILES : COBBLED)
						: r < 0.45 ? SOIL : r < 0.6 ? PATH : r < 0.8 ? MOSSY_COBBLED : STONE);
			}
		}
		t.fill(0, 1, 0, 16, 9, 20, AIR);

		// Side walls, higher towards the apse, with grilled window gaps; a base course, cracks growing upwards.
		for (int z = 0; z <= 20; z++) {
			for (int x : new int[]{2, 14}) {
				int height = Math.min(8, 2 + random.nextInt(3) + (z >= 12 ? 3 : z >= 6 ? 1 : 0));
				for (int y = 1; y <= height; y++) {
					boolean window = z % 4 == 0 && z > 0 && z < 20 && (y == 3 || y == 4);
					t.set(x, y, z, window ? (y == 3 ? GRATE : AIR) : TemplateBuilder.weathered(random, (y - 1) / 8.0, 0, COBBLE_BRICKS, 2, y, BRICKS, CRACKED, MOSSY));
				}
				if (z % 4 == 0 && z > 0 && z < 20 && height >= 5) {
					t.set(x, 5, z, stair(BRICK_STAIRS, x == 2 ? Direction.EAST : Direction.WEST, true));
				}
			}
		}
		// Buttresses on the outer walls between the windows, their caps sloping onto the wall.
		for (int z = 2; z <= 18; z += 4) {
			t.buttress(2, 1, z, Direction.WEST, 3, COBBLE_BRICKS, COBBLE_BRICK_STAIRS);
			t.buttress(14, 1, z, Direction.EAST, 3, COBBLE_BRICKS, COBBLE_BRICK_STAIRS);
		}
		// The facade with its doorway, broken down to a stump on one side; the apse wall whole, with a round window.
		for (int x = 3; x <= 13; x++) {
			int height = x <= 6 ? 3 + random.nextInt(2) : 7 - Math.abs(x - sx) / 2;
			for (int y = 1; y <= height; y++) {
				t.set(x, y, 0, TemplateBuilder.weathered(random, (y - 1) / 7.0, 0, COBBLE_BRICKS, 2, y, BRICKS, CRACKED, MOSSY));
			}
			for (int y = 1; y <= 8; y++) {
				t.set(x, y, 20, TemplateBuilder.weathered(random, (y - 1) / 8.0, 0, COBBLE_BRICKS, 2, y, BRICKS, CRACKED, MOSSY));
			}
		}
		t.arch(sx, 1, 0, Direction.Axis.X, 3, 4, BRICK_STAIRS, CARVED, PILLAR);
		t.fill(sx, 5, 20, sx, 6, 20, AIR).set(sx - 1, 5, 20, CARVED).set(sx + 1, 5, 20, CARVED)
				.set(sx, 7, 20, CARVED).set(sx, 4, 20, GRATE);
		// Corner piers, one broken.
		for (int[] c : new int[][]{{2, 0}, {14, 0}, {2, 20}, {14, 20}}) {
			if (c[0] == 14 && c[1] == 0) {
				brokenPillar(t, c[0], c[1], 1, 4);
			} else {
				pillar(t, c[0], c[1], 1, 9);
			}
		}

		// Pillars down the nave; two have broken off.
		for (int z = 4; z <= 16; z += 4) {
			for (int x : new int[]{4, 12}) {
				int top = x == 4 && z == 8 ? 3 : x == 12 && z == 4 ? 2 : 6;
				if (top == 6) {
					pillar(t, x, z, 1, 7);
				} else {
					brokenPillar(t, x, z, 1, top);
				}
			}
		}
		t.set(12, 1, 5, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(5, 1, 8, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST));
		// What is left of the roof: one whole beam, half of another, and one fallen into the nave.
		BlockState beamX = DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X);
		BlockState charredX = ReachBlocks.CHARRED_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X);
		t.fill(3, 8, 16, 13, 8, 16, beamX).fill(3, 8, 12, 7, 8, 12, charredX);
		// A few slates still hang on the beam over the apse.
		for (int x = 3; x <= 13; x++) {
			if (random.nextInt(3) == 0) {
				t.set(x, 9, 16, stair(SLATE_STAIRS, Direction.SOUTH, false));
			}
		}
		t.fill(13, 1, 10, 13, 1, 14, DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z));
		t.set(sx, 7, 16, Props.IRON_CAGE.getDefaultState().with(HangingPropBlock.HANGING, true));
		t.set(5, 7, 16, SetPieces.CENSER.getDefaultState()).set(11, 7, 16, SetPieces.CENSER.getDefaultState()); // censers swaying from the beam (PLAN 20.2)

		// Pews facing the altar; some are gone.
		BlockState pew = DEAD_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH);
		for (int z : new int[]{2, 4}) {
			for (int x = 5; x <= 11; x++) {
				if (x != sx && random.nextDouble() < 0.75) {
					t.set(x, 1, z, pew);
				}
			}
		}

		// The hole down: a rim of tiles and a rail with an opening towards the door, as at the ruin's shaft.
		for (int x = sx - 3; x <= sx + 3; x++) {
			for (int z = sz - 3; z <= sz + 3; z++) {
				boolean rim = Math.abs(x - sx) == 3 || Math.abs(z - sz) == 3;
				if (rim) {
					t.set(x, 0, z, Math.abs(x - sx) == 3 && Math.abs(z - sz) == 3 ? RUNE : TILES);
					if (!(z == sz - 3 && Math.abs(x - sx) <= 1)) {
						t.set(x, 1, z, WALL);
					}
				} else {
					t.set(x, 0, z, AIR);
				}
			}
		}
		t.set(sx - 3, 2, sz - 3, LANTERN).set(sx + 3, 2, sz - 3, LANTERN).set(sx - 3, 2, sz + 3, LANTERN).set(sx + 3, 2, sz + 3, LANTERN);

		// The altar on a dais under two standards, candles and urns on it.
		t.blendedFloor(5, 17, 11, 19, 1, random, POLISHED, 4, TILES, 3, RUNE, 1);
		t.fill(5, 1, 16, 11, 1, 16, POLISHED_ASH_STONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
		t.fill(sx - 1, 2, 18, sx + 1, 2, 18, CHISELED);
		BlockState candles = Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 4).with(CandleBlock.LIT, true);
		t.set(sx - 1, 3, 18, candles).set(sx + 1, 3, 18, candles.with(CandleBlock.CANDLES, 2))
				.set(sx, 3, 18, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2));
		tall(t, 5, 2, 18, Props.TATTERED_STANDARD, Direction.NORTH);
		tall(t, 11, 2, 18, Props.TATTERED_STANDARD, Direction.NORTH);
		t.set(10, 2, 19, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST));

		// Graves along the outer walls.
		for (int z = 3; z <= 15; z += 4) {
			t.set(0, 1, z, Props.GRAVE_CROSS.getDefaultState().with(PropBlock.FACING, Direction.WEST));
			t.set(16, 1, z + 2, Props.GRAVE_CROSS.getDefaultState().with(PropBlock.FACING, Direction.EAST));
		}
		t.set(1, 1, 5, Props.LEANING_GRAVESTONE.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(1, 1, 13, Props.LEANING_GRAVESTONE.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(15, 1, 9, Props.LEANING_GRAVESTONE.getDefaultState().with(PropBlock.FACING, Direction.EAST))
				.set(1, 1, 17, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.SOUTH));
		return t;
	}

	/** The chapel's stairwell: the ruin's, leading to the nave, with bones and a coffin at the bottom. */
	private static TemplateBuilder chapelDescent() {
		TemplateBuilder t = descent(POOL_NAVE);
		int in = 3;
		t.set(in, 1, in + 4, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH))
				.set(in, 1, in + 2, Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
		longProp(t, in + 1, 1, in, Props.COFFIN, Direction.EAST);
		return t;
	}

	/**
	 * The chapel's guarded hall, 11x8x25: a crypt nave with two rows of pillars, coffins in the bays between them,
	 * candles, standards flanking the way on to the arena, loot and two side exits; the same guards as the gate hall.
	 */
	private static TemplateBuilder nave() {
		TemplateBuilder t = new TemplateBuilder(11, 8, 25);
		Random random = new Random(13);
		hall(t, random, 0, 0, 0, 10, 7, 24, true);
		for (int z = 1; z <= 23; z++) {
			t.set(5, 0, z, z % 4 == 0 ? RUNE : TILES);
		}
		for (int z : new int[]{4, 8, 16, 20}) {
			for (int x : new int[]{2, 8}) {
				pillar(t, x, z, 1, 6);
			}
			// Ribs across the nave from the capitals.
			t.fill(3, 6, z, 7, 6, z, stair(BRICK_STAIRS, Direction.NORTH, true), stair(BRICK_STAIRS, Direction.SOUTH, true), 0.5, random);
		}
		t.set(0, 3, 6, GRATE).set(10, 3, 18, GRATE);
		doorway(t, 5, 0, Direction.NORTH);
		doorway(t, 5, 24, Direction.SOUTH);
		doorway(t, 0, 12, Direction.WEST);
		doorway(t, 10, 12, Direction.EAST);
		t.door(5, 1, 0, Direction.NORTH, HALL_IN, null, null);
		t.door(5, 1, 24, Direction.SOUTH, HALL_OUT, POOL_ARENA, ARENA_IN);
		t.door(0, 1, 12, Direction.WEST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.door(10, 1, 12, Direction.EAST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.fill(3, 1, 23, 3, 3, 23, PILLAR).fill(7, 1, 23, 7, 3, 23, PILLAR).set(3, 4, 23, CARVED).set(7, 4, 23, CARVED)
				.fill(4, 4, 23, 6, 4, 23, CHISELED);

		// Coffins in the bays along the walls, candles beside some.
		for (int z : new int[]{5, 9, 14, 17}) {
			longProp(t, 1, 1, z, Props.COFFIN, Direction.SOUTH);
			longProp(t, 9, 1, z, Props.COFFIN, Direction.SOUTH);
		}
		BlockState candles = Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true);
		t.set(1, 1, 7, candles.with(PropCandleBlock.FACING, Direction.EAST)).set(9, 1, 7, candles.with(PropCandleBlock.FACING, Direction.WEST))
				.set(1, 1, 19, candles.with(PropCandleBlock.FACING, Direction.EAST)).set(9, 1, 19, candles.with(PropCandleBlock.FACING, Direction.WEST));
		tall(t, 3, 1, 22, Props.TATTERED_STANDARD, Direction.NORTH);
		tall(t, 7, 1, 22, Props.TATTERED_STANDARD, Direction.NORTH);
		t.set(1, 1, 1, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.EAST))
				.set(1, 1, 3, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST))
				.set(9, 1, 22, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(7, 1, 9, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST));
		t.set(5, 6, 6, HANGING_LANTERN).set(5, 6, 18, HANGING_LANTERN);
		sootOver(t, random, 5, 6, 6);
		sootOver(t, random, 5, 6, 18);
		t.set(3, 6, 12, Props.IRON_CAGE.getDefaultState().with(HangingPropBlock.HANGING, true));
		t.chest(9, 1, 2, Direction.WEST, OUTER_HALL_LOOT);
		t.chest(1, 1, 21, Direction.EAST, OUTER_HALL_LOOT);
		t.entity(5, 1, 12, guard(HOLLOW, null));
		t.entity(5, 1, 20, guard(SKELETON, null));
		t.entity(5, 1, 6, guard(HOLLOW, SanctumGuards.ROLL_HALL));
		return t;
	}

	/**
	 * Sunken cloister, 21x11x21 (PLAN 16.2, the third start): a square court sunk three blocks into the ground, a covered
	 * walk around it under a low, partly fallen roof on columns, steps down from the north and the south, graves and a
	 * dead tree in the court, and a well in its middle that is the way down. Same foundation and ground layer as the
	 * ruin; the court's floor is the template's bottom layer, where the stairwell joins.
	 */
	private static TemplateBuilder cloister() {
		int size = 21;
		int c = size / 2;
		int pit = -RUIN_FOUNDATION; // the court's floor, below the ground layer
		TemplateBuilder t = new TemplateBuilder(size, RUIN_FOUNDATION + 8, size);
		Random random = new Random(12);
		t.fill(0, 0, 0, size - 1, RUIN_FOUNDATION - 1, size - 1, COBBLED, COBBLE_BRICKS, 0.3, random);
		t.baseY(RUIN_FOUNDATION);
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				double r = random.nextDouble();
				t.set(x, 0, z, r < 0.45 ? COBBLED : r < 0.6 ? MOSSY_COBBLED : r < 0.75 ? PATH : r < 0.9 ? STONE : SOIL);
			}
		}
		t.fill(0, 1, 0, size - 1, 7, size - 1, AIR);
		// The pit: a brick retaining wall around it with a cobble-brick base, mossy below, open above.
		t.fill(1, pit, 1, size - 2, 0, size - 2, BRICKS);
		for (int i = 1; i <= size - 2; i++) {
			for (int[] p : new int[][]{{i, 1}, {i, size - 2}, {1, i}, {size - 2, i}}) {
				for (int y = pit; y <= 0; y++) {
					t.set(p[0], y, p[1], TemplateBuilder.weathered(random, (double) (y - pit) / -pit, 0, COBBLE_BRICKS, pit + 1, y, BRICKS, CRACKED, MOSSY));
				}
			}
		}
		t.fill(2, pit + 1, 2, size - 3, 0, size - 3, AIR);
		for (int x = 2; x <= size - 3; x++) {
			for (int z = 2; z <= size - 3; z++) {
				boolean court = x >= 5 && x <= size - 6 && z >= 5 && z <= size - 6;
				boolean soil = court && (x <= 6 || x >= size - 7 || z <= 6 || z >= size - 7);
				double r = random.nextDouble();
				t.set(x, pit, z, soil ? (r < 0.7 ? SOIL : MOSSY_COBBLED) : r < 0.5 ? TILES : r < 0.7 ? POLISHED : r < 0.85 ? CRACKED_TILES : COBBLED);
			}
		}

		// Columns round the court with carved capitals, and the walk's low slate roof on them, partly fallen in.
		for (int i = 4; i <= size - 5; i += 2) {
			for (int[] p : new int[][]{{i, 4}, {i, size - 5}, {4, i}, {size - 5, i}}) {
				boolean fallen = random.nextDouble() < 0.15;
				if (fallen) {
					brokenPillar(t, p[0], p[1], pit + 1, pit + 1);
				} else {
					pillar(t, p[0], p[1], pit + 1, 0);
				}
			}
		}
		BlockState roof = SLATE_SLAB;
		for (int x = 1; x <= size - 2; x++) {
			for (int z = 1; z <= size - 2; z++) {
				boolean walk = x <= 4 || x >= size - 5 || z <= 4 || z >= size - 5;
				boolean collapsed = x >= size - 9 && z >= size - 9;
				if (walk && !(collapsed && random.nextDouble() < 0.8) && random.nextDouble() < 0.85) {
					t.set(x, 1, z, roof);
				}
			}
		}
		t.set(size - 4, pit + 1, size - 3, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(size - 3, pit + 1, size - 6, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH))
				.set(size - 6, pit + 1, size - 4, CRACKED).set(size - 6, pit + 2, size - 4, ASH_BRICK_SLAB.getDefaultState());

		// Steps down from the north and the south, through gaps in the roof, under broken gateways.
		for (int x = c - 1; x <= c + 1; x++) {
			t.set(x, pit + 2, 1, POLISHED_ASH_STONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH))
					.set(x, pit + 1, 2, POLISHED_ASH_STONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH))
					.set(x, pit + 2, size - 2, POLISHED_ASH_STONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH))
					.set(x, pit + 1, size - 3, POLISHED_ASH_STONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
			t.fill(x, 0, 1, x, 1, 3, AIR).fill(x, 0, size - 4, x, 1, size - 2, AIR);
		}
		for (int z : new int[]{1, size - 2}) {
			t.fill(c - 2, 1, z, c - 2, z == 1 ? 4 : 3, z, PILLAR).fill(c + 2, 1, z, c + 2, z == 1 ? 4 : 2, z, PILLAR);
		}
		t.fill(c - 2, 5, 1, c, 5, 1, CARVED).set(c - 1, 4, 1, stair(BRICK_STAIRS, Direction.WEST, true));
		// Corner piers standing over the walls, one fallen.
		for (int[] p : new int[][]{{1, 1}, {1, size - 2}, {size - 2, size - 2}}) {
			int top = p[1] == 1 ? 6 : p[0] == 1 ? 4 : 1;
			if (top > 2) {
				pillar(t, p[0], p[1], 1, top);
			} else {
				brokenPillar(t, p[0], p[1], 1, top);
			}
		}
		// The bell tower over the north-east corner of the walk, seen from afar; its bell is gone, the cage still hangs.
		int tx = size - 3;
		for (int x = tx - 1; x <= tx + 1; x++) {
			for (int z = 1; z <= 3; z++) {
				for (int y = pit + 1; y <= 7; y++) {
					t.set(x, y, z, TemplateBuilder.weathered(random, (double) (y - pit - 1) / (7 - pit - 1), 0, COBBLE_BRICKS, pit + 2, y, BRICKS, CRACKED, MOSSY));
				}
			}
		}
		t.fill(tx, 1, 2, tx, 6, 2, AIR);
		for (int y = 5; y <= 6; y++) {
			t.set(tx, y, 1, AIR).set(tx, y, 3, AIR).set(tx - 1, y, 2, AIR).set(tx + 1, y, 2, AIR);
		}
		t.set(tx, 7, 1, stair(BRICK_STAIRS, Direction.SOUTH, true)).set(tx - 1, 7, 2, stair(BRICK_STAIRS, Direction.EAST, true));
		t.set(tx, 3, 1, GRATE).set(tx + 1, 3, 2, GRATE);
		for (int[] p : new int[][]{{tx - 1, 1}, {tx + 1, 1}, {tx - 1, 3}, {tx + 1, 3}}) {
			pillar(t, p[0], p[1], 1, 7);
		}
		t.set(tx + 1, 7, 3, AIR).set(tx, 7, 3, CRACKED);
		t.set(tx, 6, 2, Props.IRON_CAGE.getDefaultState().with(HangingPropBlock.HANGING, true));

		// The well in the court's middle: the way down. A low curb with an opening to the north, lanterns on its corners.
		for (int x = c - 3; x <= c + 3; x++) {
			for (int z = c - 3; z <= c + 3; z++) {
				boolean rim = Math.abs(x - c) == 3 || Math.abs(z - c) == 3;
				if (rim) {
					t.set(x, pit, z, Math.abs(x - c) == 3 && Math.abs(z - c) == 3 ? RUNE : TILES);
					if (!(z == c - 3 && Math.abs(x - c) <= 1)) {
						t.set(x, pit + 1, z, WALL);
					}
				} else {
					t.set(x, pit, z, AIR);
				}
			}
		}
		t.jigsaw(c, pit, c, JigsawOrientation.DOWN_SOUTH, RUIN_DOWN, POOL_DESCENT_CLOISTER, DESCENT_TOP, AIR);
		for (int[] p : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
			t.set(c + p[0], pit + 2, c + p[1], LANTERN);
		}

		// The court: graves, bones and a dead tree on the soil around the well.
		BlockState log = DEAD_LOG.getDefaultState();
		t.fill(5, pit + 1, size - 6, 5, pit + 4, size - 6, log);
		t.set(6, pit + 4, size - 6, DEAD_FENCE.getDefaultState().with(FenceBlock.WEST, true))
				.set(5, pit + 3, size - 7, DEAD_FENCE.getDefaultState().with(FenceBlock.SOUTH, true));
		t.set(5, pit + 1, 6, Props.GRAVE_CROSS.getDefaultState().with(PropBlock.FACING, Direction.EAST))
				.set(size - 6, pit + 1, 6, Props.GRAVE_CROSS.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(size - 6, pit + 1, size - 7, Props.GRAVE_CROSS.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(5, pit + 1, c, Props.LEANING_GRAVESTONE.getDefaultState().with(PropBlock.FACING, Direction.EAST))
				.set(size - 6, pit + 1, c + 1, Props.LEANING_GRAVESTONE.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(c + 2, pit + 1, size - 6, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.SOUTH))
				.set(6, pit + 1, 5, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 3));

		// The walk: candles, a coffin and urns under the roof.
		BlockState candles = Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true);
		t.set(2, pit + 1, 2, candles).set(size - 3, pit + 1, size - 3, candles).set(2, pit + 1, size - 3, candles);
		longProp(t, 2, pit + 1, 7, Props.COFFIN, Direction.SOUTH);
		t.set(size - 3, pit + 1, c, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.WEST));
		return t;
	}

	/** The cloister's stairwell: the ruin's, leading to the cloister walk, with stores left at the bottom. */
	private static TemplateBuilder cloisterDescent() {
		TemplateBuilder t = descent(POOL_CLOISTER_WALK);
		int in = 3;
		t.set(in, 1, in + 4, Props.GRAIN_SACKS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.EAST))
				.set(in, 1, in + 3, Props.BURNT_BARREL.getDefaultState())
				.set(in + 2, 1, in, Props.ASH_URNS.getDefaultState().with(PropBlock.FACING, Direction.SOUTH));
		return t;
	}

	/**
	 * The cloister's guarded hall, 15x7x21: a walk round a buried court of columns (graves and a dead tree on ash soil
	 * inside), stores in one corner, standards at the way on to the arena, loot and two side exits; the same guards as
	 * the gate hall, plus a hall and an archer spot that are rolled.
	 */
	private static TemplateBuilder cloisterWalk() {
		TemplateBuilder t = new TemplateBuilder(15, 7, 21);
		Random random = new Random(14);
		hall(t, random, 0, 0, 0, 14, 6, 20, true);
		t.blendedFloor(5, 6, 9, 14, 0, random, SOIL, 5, MOSSY_COBBLED, 2, PATH, 1);
		for (int z = 5; z <= 15; z += 2) {
			pillar(t, 4, z, 1, 4);
			pillar(t, 10, z, 1, 4);
		}
		for (int x = 6; x <= 8; x += 2) {
			pillar(t, x, 5, 1, 4);
			pillar(t, x, 15, 1, 4);
		}
		// Lintels over the columns with hung-stair arches between them, an arcade round the court.
		t.fill(4, 5, 5, 4, 5, 15, BRICKS).fill(10, 5, 5, 10, 5, 15, BRICKS).fill(4, 5, 5, 10, 5, 5, BRICKS).fill(4, 5, 15, 10, 5, 15, BRICKS);
		for (int z = 6; z <= 14; z += 2) {
			t.set(4, 4, z, stair(BRICK_STAIRS, random.nextBoolean() ? Direction.NORTH : Direction.SOUTH, true));
			t.set(10, 4, z, stair(BRICK_STAIRS, random.nextBoolean() ? Direction.NORTH : Direction.SOUTH, true));
		}
		t.set(7, 4, 5, stair(BRICK_STAIRS, Direction.WEST, true)).set(7, 4, 15, stair(BRICK_STAIRS, Direction.EAST, true));
		t.fill(5, 5, 6, 9, 5, 14, MOSSY, BRICKS, 0.5, random);
		doorway(t, 7, 0, Direction.NORTH);
		doorway(t, 7, 20, Direction.SOUTH);
		doorway(t, 0, 10, Direction.WEST);
		doorway(t, 14, 10, Direction.EAST);
		t.door(7, 1, 0, Direction.NORTH, HALL_IN, null, null);
		t.door(7, 1, 20, Direction.SOUTH, HALL_OUT, POOL_ARENA, ARENA_IN);
		t.door(0, 1, 10, Direction.WEST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.door(14, 1, 10, Direction.EAST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.fill(5, 1, 19, 5, 3, 19, PILLAR).fill(9, 1, 19, 9, 3, 19, PILLAR).set(5, 4, 19, CARVED).set(9, 4, 19, CARVED)
				.fill(6, 4, 19, 8, 4, 19, CHISELED);

		// The court: a dead tree, graves, bones.
		t.fill(7, 1, 10, 7, 3, 10, DEAD_LOG.getDefaultState());
		t.set(6, 3, 10, DEAD_FENCE.getDefaultState().with(FenceBlock.EAST, true))
				.set(8, 3, 10, DEAD_FENCE.getDefaultState().with(FenceBlock.WEST, true));
		t.set(6, 1, 7, Props.GRAVE_CROSS.getDefaultState())
				.set(8, 1, 13, Props.GRAVE_CROSS.getDefaultState().with(PropBlock.FACING, Direction.SOUTH))
				.set(8, 1, 7, Props.LEANING_GRAVESTONE.getDefaultState())
				.set(6, 1, 13, Props.LEANING_GRAVESTONE.getDefaultState().with(PropBlock.FACING, Direction.SOUTH))
				.set(6, 1, 11, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST));

		// Stores in the north-east corner, a weapon rack and a broken cart along the walk.
		t.set(13, 1, 1, Props.GRAIN_SACKS.getDefaultState().with(StackedPropBlock.COUNT, 3).with(PropBlock.FACING, Direction.SOUTH))
				.set(12, 1, 1, Props.GRAIN_SACKS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.SOUTH))
				.set(13, 1, 2, Props.BURNT_BARREL.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(11, 1, 1, Props.BURNT_BARREL.getDefaultState().with(PropBlock.FACING, Direction.SOUTH))
				.set(1, 1, 6, Props.WEAPON_RACK.getDefaultState().with(PropBlock.FACING, Direction.EAST))
				.set(1, 1, 19, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.EAST))
				.set(2, 1, 13, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST));
		longProp(t, 13, 1, 13, Props.BROKEN_CART, Direction.SOUTH);
		tall(t, 4, 1, 18, Props.TATTERED_STANDARD, Direction.NORTH);
		tall(t, 10, 1, 18, Props.TATTERED_STANDARD, Direction.NORTH);
		BlockState candles = Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true);
		t.set(3, 1, 1, candles).set(11, 1, 19, candles.with(PropCandleBlock.FACING, Direction.WEST));
		for (int[] p : new int[][]{{2, 2}, {12, 2}, {2, 18}, {12, 18}}) {
			t.set(p[0], 5, p[1], HANGING_LANTERN);
			sootOver(t, random, p[0], 5, p[1]);
		}
		t.set(7, 5, 10, HANGING_LANTERN).set(12, 5, 10, Props.IRON_CAGE.getDefaultState().with(HangingPropBlock.HANGING, true));
		t.chest(1, 1, 1, Direction.EAST, OUTER_HALL_LOOT);
		t.chest(13, 1, 19, Direction.WEST, OUTER_HALL_LOOT);
		t.entity(7, 1, 3, guard(HOLLOW, null));
		t.entity(7, 1, 17, guard(SKELETON, null));
		t.entity(2, 1, 15, guard(HOLLOW, SanctumGuards.ROLL_HALL));
		t.entity(12, 1, 5, guard(SKELETON, SanctumGuards.ROLL_ARCHER));
		return t;
	}

	/** Straight corridor, 5x5x13. */
	private static TemplateBuilder hallCorridor() {
		TemplateBuilder t = new TemplateBuilder(5, 5, 13);
		Random random = new Random(21);
		hall(t, random, 0, 0, 0, 4, 4, 12, false);
		for (int z = 3; z <= 9; z += 3) {
			t.fill(0, 1, z, 0, 3, z, PILLAR).fill(4, 1, z, 4, 3, z, PILLAR).fill(1, 4, z, 3, 4, z, CARVED);
			t.set(1, 3, z, stair(BRICK_STAIRS, Direction.WEST, true)).set(3, 3, z, stair(BRICK_STAIRS, Direction.EAST, true));
		}
		doorway(t, 2, 0, Direction.NORTH);
		doorway(t, 2, 12, Direction.SOUTH);
		t.door(2, 1, 0, Direction.NORTH, HALL_IN, null, null);
		t.door(2, 1, 12, Direction.SOUTH, HALL_OUT, POOL_HALLS, HALL_IN);
		t.set(1, 3, 6, HANGING_LANTERN);
		sootOver(t, random, 1, 3, 6);
		t.set(3, 1, 10, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST));
		t.entity(2, 1, 7, guard(HOLLOW, SanctumGuards.ROLL_HALL));
		return t;
	}

	/** Crossroads chamber, 13x7x13, three exits, two chests, a Hollow spot and an archer spot. */
	private static TemplateBuilder hallChamber() {
		TemplateBuilder t = new TemplateBuilder(13, 7, 13);
		Random random = new Random(22);
		hall(t, random, 0, 0, 0, 12, 6, 12, true);
		t.fill(5, 1, 5, 7, 5, 7, BRICKS, CRACKED, 0.2, random);
		pillar(t, 5, 5, 1, 5);
		pillar(t, 7, 7, 1, 5);
		pillar(t, 7, 5, 1, 5);
		pillar(t, 5, 7, 1, 5);
		t.set(6, 0, 6, RUNE).set(6, 5, 6, SOOTY);
		doorway(t, 6, 0, Direction.NORTH);
		doorway(t, 0, 6, Direction.WEST);
		doorway(t, 12, 6, Direction.EAST);
		doorway(t, 6, 12, Direction.SOUTH);
		t.door(6, 1, 0, Direction.NORTH, HALL_IN, null, null);
		t.door(0, 1, 6, Direction.WEST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.door(12, 1, 6, Direction.EAST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.door(6, 1, 12, Direction.SOUTH, HALL_OUT, POOL_HALLS, HALL_IN);
		t.chest(1, 1, 10, Direction.EAST, OUTER_HALL_LOOT);
		t.chest(11, 1, 2, Direction.WEST, OUTER_HALL_LOOT);
		for (int[] p : new int[][]{{3, 3}, {9, 9}, {3, 9}, {9, 3}}) {
			t.set(p[0], 5, p[1], HANGING_LANTERN);
			sootOver(t, random, p[0], 5, p[1]);
		}
		t.set(1, 1, 1, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 3)).set(11, 1, 11, Props.BONE_PILE.getDefaultState());
		t.entity(3, 1, 4, guard(HOLLOW, SanctumGuards.ROLL_HALL));
		t.entity(9, 1, 9, guard(SKELETON, SanctumGuards.ROLL_ARCHER));
		return t;
	}

	/** Dead-end crypt, 9x6x9: coffins along the walls, a chest at the back, a Hollow spot. */
	private static TemplateBuilder hallCrypt() {
		TemplateBuilder t = new TemplateBuilder(9, 6, 9);
		Random random = new Random(23);
		hall(t, random, 0, 0, 0, 8, 5, 8, false);
		for (int z = 2; z <= 6; z += 2) {
			longProp(t, 1, 1, z, Props.COFFIN, Direction.EAST);
			longProp(t, 7, 1, z, Props.COFFIN, Direction.WEST);
			t.set(0, 2, z + 1, ReachBlocks.BONE_BRICKS.getDefaultState()).set(8, 2, z - 1, ReachBlocks.BONE_BRICKS.getDefaultState());
		}
		t.set(4, 0, 4, RUNE);
		doorway(t, 4, 0, Direction.NORTH);
		t.door(4, 1, 0, Direction.NORTH, HALL_IN, null, null);
		t.chest(4, 1, 7, Direction.NORTH, OUTER_HALL_LOOT);
		t.set(4, 4, 4, HANGING_LANTERN);
		sootOver(t, random, 4, 4, 4);
		t.set(1, 1, 7, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST));
		t.entity(4, 1, 5, guard(HOLLOW, SanctumGuards.ROLL_HALL));
		return t;
	}

	/** Dead-end storeroom, 9x6x11 (PLAN 16.2): sacks, barrels, a broken cart and a weapon rack, a chest, a Hollow spot. */
	private static TemplateBuilder hallStoreroom() {
		TemplateBuilder t = new TemplateBuilder(9, 6, 11);
		Random random = new Random(24);
		hall(t, random, 0, 0, 0, 8, 5, 10, false);
		t.fill(1, 0, 1, 7, 0, 3, DEAD_PLANKS.getDefaultState(), ReachBlocks.CHARRED_PLANKS.getDefaultState(), 0.3, random);
		doorway(t, 4, 0, Direction.NORTH);
		t.door(4, 1, 0, Direction.NORTH, HALL_IN, null, null);
		for (int z = 2; z <= 8; z++) {
			if (z != 5) {
				t.set(1, 1, z, Props.GRAIN_SACKS.getDefaultState().with(StackedPropBlock.COUNT, 1 + z % 3).with(PropBlock.FACING, Direction.EAST));
			}
		}
		t.set(7, 1, 2, Props.BURNT_BARREL.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(7, 1, 3, Props.BURNT_BARREL.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(7, 1, 6, Props.BURNT_BARREL.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(1, 1, 9, Props.WEAPON_RACK.getDefaultState().with(PropBlock.FACING, Direction.EAST))
				.set(3, 1, 6, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH));
		longProp(t, 5, 1, 9, Props.BROKEN_CART, Direction.EAST);
		t.chest(4, 1, 9, Direction.NORTH, OUTER_HALL_LOOT);
		t.set(4, 4, 5, HANGING_LANTERN);
		sootOver(t, random, 4, 4, 5);
		t.entity(4, 1, 5, guard(HOLLOW, SanctumGuards.ROLL_HALL));
		return t;
	}

	/** Brick plug for an exit where no hall fits (the halls pool's fallback). */
	private static TemplateBuilder hallCap() {
		TemplateBuilder t = new TemplateBuilder(5, 5, 1);
		t.fill(0, 0, 0, 4, 4, 0, BRICKS, CRACKED, 0.2, new Random(25));
		t.set(0, 0, 0, COBBLE_BRICKS).set(4, 0, 0, COBBLE_BRICKS);
		t.jigsaw(2, 1, 0, JigsawOrientation.NORTH_UP, HALL_IN, null, null, BRICKS);
		return t;
	}

	/**
	 * A dungeon hall in a box (docs/art-direction.md §3): a cobble-brick base course, ash-brick walls cracking
	 * upwards and mossy at the foot, a ceiling of bricks, a blended floor ({@code fine}: tiles over polished stone;
	 * else cobble bricks and cobbles), a ribbed cornice of hung stairs under the ceiling where the room is tall
	 * enough, air inside.
	 */
	private static void hall(TemplateBuilder t, Random random, int x0, int y0, int z0, int x1, int y1, int z1, boolean fine) {
		t.fill(x0, y0, z0, x1, y1, z1, BRICKS);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
				for (int y = y0 + 1; y < y1; y++) {
					if (!edge) {
						continue;
					}
					double h = (double) (y - y0 - 1) / Math.max(1, y1 - y0 - 2);
					t.set(x, y, z, TemplateBuilder.weathered(random, h, 0, COBBLE_BRICKS, y0 + 2, y, BRICKS, CRACKED, MOSSY));
				}
				if (random.nextInt(12) == 0) {
					t.set(x, y1, z, CRACKED);
				}
			}
		}
		t.fill(x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1, AIR);
		if (fine) {
			t.blendedFloor(x0 + 1, z0 + 1, x1 - 1, z1 - 1, y0, random, POLISHED, 5, TILES, 4, CRACKED_TILES, 1);
		} else {
			t.blendedFloor(x0 + 1, z0 + 1, x1 - 1, z1 - 1, y0, random, COBBLE_BRICKS, 5, COBBLED, 3, CRACKED_TILES, 1);
		}
		if (y1 - y0 >= 6) {
			t.innerCornice(x0, z0, x1, z1, y1 - 1, BRICK_STAIRS);
		}
	}

	/** A pillar from y0 to y1: a chiseled base, a brick-pillar shaft, a carved capital. */
	private static void pillar(TemplateBuilder t, int x, int z, int y0, int y1) {
		t.set(x, y0, z, CHISELED);
		if (y1 - y0 >= 2) {
			t.fill(x, y0 + 1, z, x, y1 - 1, z, PILLAR);
		}
		t.set(x, y1, z, CARVED);
	}

	/** A broken pillar: its shaft stops at {@code top}, no capital. */
	private static void brokenPillar(TemplateBuilder t, int x, int z, int y0, int top) {
		t.set(x, y0, z, CHISELED);
		if (top > y0) {
			t.fill(x, y0 + 1, z, x, top, z, PILLAR);
		}
	}

	/** Soot on the ceiling over a hanging lantern at (x, y, z). */
	private static void sootOver(TemplateBuilder t, Random random, int x, int y, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (t.inside(x + dx, y + 1, z + dz) && t.isSet(x + dx, y + 1, z + dz) && !t.isAir(x + dx, y + 1, z + dz)
						&& random.nextDouble() < (dx == 0 && dz == 0 ? 1 : 0.5)) {
					t.set(x + dx, y + 1, z + dz, SOOTY);
				}
			}
		}
	}

	/** A two-block-long prop (coffin, cart): its foot at (x, y, z), its head one block on in {@code facing}. */
	private static void longProp(TemplateBuilder t, int x, int y, int z, LongPropBlock block, Direction facing) {
		BlockState foot = block.getDefaultState().with(PropBlock.FACING, facing);
		t.set(x, y, z, foot.with(LongPropBlock.PART, BedPart.FOOT))
				.set(x + facing.getOffsetX(), y, z + facing.getOffsetZ(), foot.with(LongPropBlock.PART, BedPart.HEAD));
	}

	/** A two-block-tall prop (standard, scarecrow) standing at (x, y, z). */
	private static void tall(TemplateBuilder t, int x, int y, int z, TallPropBlock block, Direction facing) {
		BlockState lower = block.getDefaultState().with(PropBlock.FACING, facing);
		t.set(x, y, z, lower.with(TallPropBlock.HALF, DoubleBlockHalf.LOWER)).set(x, y + 1, z, lower.with(TallPropBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** Rough natural-looking rock for casings. */
	private static BlockState rock(Random random) {
		double r = random.nextDouble();
		return r < 0.4 ? STONE : r < 0.7 ? COBBLED : r < 0.9 ? Blocks.STONE.getDefaultState() : Blocks.TUFF.getDefaultState();
	}

	/**
	 * Opens a 3-wide, 3-high doorway in the wall at (x, z), centered on the connector: an arch with hung stairs as
	 * its shoulders, pillar jambs and a carved keystone where the wall has room.
	 */
	private static void doorway(TemplateBuilder t, int x, int z, Direction facing) {
		Direction.Axis along = facing.getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
		t.arch(x, 1, z, along, 3, 3, BRICK_STAIRS, CARVED, PILLAR);
	}

	/**
	 * An outer-hall guard that never despawns. Template mobs are initialized, so they get their usual gear.
	 * {@code rollTag} makes the spot optional (see {@link SanctumGuards}); null = always there.
	 */
	private static NbtCompound guard(String entityId, @Nullable String rollTag) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", entityId);
		nbt.putBoolean("PersistenceRequired", true);
		NbtList tags = new NbtList();
		tags.add(NbtString.of(SanctumGuards.GUARD));
		if (rollTag != null) {
			tags.add(NbtString.of(rollTag));
		}
		nbt.put("Tags", tags);
		return nbt;
	}

	private SanctumTemplates() {}
}
