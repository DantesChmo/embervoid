package com.embervoid.sanctum.datagen;

import com.embervoid.Embervoid;
import com.embervoid.block.prop.HangingPropBlock;
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
	private static final BlockState POLISHED = POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState CHISELED = CHISELED_ASH_STONE.getDefaultState();
	private static final BlockState COBBLED = COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState STONE = ASH_STONE.getDefaultState();
	private static final BlockState SOIL = ASH_SOIL.getDefaultState();
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
		t.fill(0, 0, 0, 14, RUIN_FOUNDATION - 1, 14, COBBLED);
		t.fill(5, 0, 5, 9, RUIN_FOUNDATION - 1, 9, AIR);
		t.jigsaw(7, 0, 7, JigsawOrientation.DOWN_SOUTH, RUIN_DOWN, POOL_DESCENT, DESCENT_TOP, AIR);
		t.baseY(RUIN_FOUNDATION);
		for (int x = 0; x < 15; x++) {
			for (int z = 0; z < 15; z++) {
				double r = random.nextDouble();
				t.set(x, 0, z, r < 0.55 ? COBBLED : r < 0.8 ? STONE : SOIL);
			}
		}
		// Clear terrain inside the ruin; the walls below replace some of this air.
		t.fill(0, 1, 0, 14, 7, 14, AIR);
		// Broken outer walls with a gap in the middle of each side.
		for (int i = 0; i < 15; i++) {
			if (i >= 6 && i <= 8) {
				continue;
			}
			for (int[] p : new int[][]{{i, 0}, {i, 14}, {0, i}, {14, i}}) {
				int height = random.nextInt(5);
				for (int y = 1; y <= height; y++) {
					t.set(p[0], y, p[1], random.nextDouble() < 0.3 ? CRACKED : BRICKS);
				}
				if (height > 0 && height < 4 && random.nextBoolean()) {
					t.set(p[0], height + 1, p[1], ASH_BRICK_SLAB.getDefaultState());
				}
			}
		}
		// Corner pillars; one has fallen.
		int[][] corners = {{0, 0}, {14, 0}, {0, 14}, {14, 14}};
		for (int c = 0; c < corners.length; c++) {
			int top = c == 3 ? 3 : 6;
			t.fill(corners[c][0], 1, corners[c][1], corners[c][0], top, corners[c][1], POLISHED);
			if (c != 3) {
				t.set(corners[c][0], 7, corners[c][1], CHISELED);
			}
		}
		// The fallen pillar and a dead log.
		t.fill(10, 1, 12, 12, 1, 12, POLISHED);
		t.set(2, 1, 11, DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z))
				.set(2, 1, 12, DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z))
				.set(2, 1, 13, DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z));

		// The shaft: rim of polished stone, a wall rail with an opening on the north side, open below.
		for (int x = 4; x <= 10; x++) {
			for (int z = 4; z <= 10; z++) {
				boolean rim = x == 4 || x == 10 || z == 4 || z == 10;
				if (rim) {
					t.set(x, 0, z, POLISHED);
					if (!(z == 4 && x >= 6 && x <= 8)) {
						t.set(x, 1, z, WALL);
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
		t.fill(in, 0, in, in + 4, 0, in + 4, POLISHED);
		for (int y = 0; y <= height - 2; y++) {
			BlockState block = y % 5 == 4 ? CHISELED : BRICKS;
			t.fill(in + 1, y, in + 1, in + 3, y, in + 3, block);
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
		t.set(in, 1, in, LANTERN);
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
		t.room(POLISHED, BRICKS, BRICKS);
		for (int z = 2; z <= 20; z += 4) {
			t.set(4, 0, z, CHISELED);
		}
		for (int z : new int[]{4, 8, 14, 18}) {
			t.fill(1, 1, z, 1, 5, z, POLISHED);
			t.fill(7, 1, z, 7, 5, z, POLISHED);
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
		room(t, 6, 0, 0, 14, 6, 9, POLISHED, BRICKS, BRICKS);
		doorway(t, 10, 0, Direction.NORTH);
		t.door(10, 1, 0, Direction.NORTH, ARENA_IN, null, null);
		for (int[] p : new int[][]{{7, 1}, {13, 1}, {7, 8}, {13, 8}}) {
			t.fill(p[0], 1, p[1], p[0], 5, p[1], POLISHED);
		}
		t.fill(9, 0, 3, 11, 0, 5, CHISELED);
		t.set(10, 1, 4, SanctumBlocks.EMBER_SHRINE.getDefaultState());
		t.set(7, 1, 3, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2).with(PropBlock.FACING, Direction.EAST))
				.set(13, 1, 6, Props.ASH_URNS.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(12, 1, 7, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH));

		// The arena: the core's hall without its obelisks and Maiden, its chiseled stone gone but for the door and dais.
		room(t, 0, 0, 9, 20, 10, 29, POLISHED, POLISHED, BRICKS);
		// The centre, where the Fallen Sister is summoned (PLAN 16.3).
		int cx = SanctumBossArena.TEMPLATE_CENTER.getX();
		int cz = SanctumBossArena.TEMPLATE_CENTER.getZ();
		for (int x = 1; x <= 19; x++) {
			for (int z = 10; z <= 28; z++) {
				double r = random.nextDouble();
				long ring = Math.round(Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)));
				t.set(x, 0, z, ring == 6 ? BRICKS : r < 0.15 ? CRACKED : r < 0.23 ? COBBLED : POLISHED);
			}
		}
		for (int i = 4; i <= 16; i += 4) {
			for (int[] p : new int[][]{{i, 9}, {i, 29}, {0, i + 9}, {20, i + 9}}) {
				t.fill(p[0], 1, p[1], p[0], 9, p[1], BRICKS);
			}
		}
		BlockState cornice = ASH_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
		t.fill(1, 9, 10, 19, 9, 10, cornice).fill(1, 9, 28, 19, 9, 28, cornice)
				.fill(1, 9, 10, 1, 9, 28, cornice).fill(19, 9, 10, 19, 9, 28, cornice);
		// A stretch of the cornice and a pilaster have come down.
		t.fill(14, 9, 28, 17, 9, 28, AIR).fill(20, 6, 21, 20, 9, 21, CRACKED);
		t.set(18, 1, 21, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(15, 1, 27, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH))
				.set(1, 1, 15, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST))
				.set(19, 1, 11, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.SOUTH))
				.set(5, 1, 27, CRACKED).set(5, 2, 27, ASH_BRICK_SLAB.getDefaultState());

		// The fog wall fills the doorway from the shrine room, in a chiseled frame.
		t.fill(9, 1, 9, 11, 3, 9, SanctumBlocks.FOG_WALL.getDefaultState());
		t.fill(8, 1, 9, 8, 4, 9, CHISELED).fill(12, 1, 9, 12, 4, 9, CHISELED).fill(9, 4, 9, 11, 4, 9, CHISELED);

		// Braziers on pedestals, unlit until the fight (PLAN 16.3 lights them as she weakens).
		BlockState brazier = Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false);
		for (int z : new int[]{13, 19, 24}) {
			t.set(3, 1, z, WALL).set(3, 2, z, brazier.with(CampfireBlock.FACING, Direction.EAST));
			t.set(17, 1, z, WALL).set(17, 2, z, brazier.with(CampfireBlock.FACING, Direction.WEST));
		}

		// The dais with the cold Cleft Stele: four columns of Cleft Stone, the crack open (SteleFrame, along X).
		t.fill(6, 1, 26, 14, 1, 28, POLISHED);
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
		t.fill(0, 0, 0, 16, RUIN_FOUNDATION - 1, 20, COBBLED);
		t.fill(sx - 2, 0, sz - 2, sx + 2, RUIN_FOUNDATION - 1, sz + 2, AIR);
		t.jigsaw(sx, 0, sz, JigsawOrientation.DOWN_SOUTH, RUIN_DOWN, POOL_DESCENT_CHAPEL, DESCENT_TOP, AIR);
		t.baseY(RUIN_FOUNDATION);
		for (int x = 0; x < 17; x++) {
			for (int z = 0; z < 21; z++) {
				double r = random.nextDouble();
				boolean nave = x >= 3 && x <= 13 && z >= 1 && z <= 19;
				t.set(x, 0, z, nave ? (x == sx ? BRICKS : r < 0.75 ? POLISHED : COBBLED) : r < 0.5 ? SOIL : r < 0.8 ? COBBLED : STONE);
			}
		}
		t.fill(0, 1, 0, 16, 9, 20, AIR);

		// Side walls, higher towards the apse, with window gaps.
		for (int z = 0; z <= 20; z++) {
			for (int x : new int[]{2, 14}) {
				int height = Math.min(8, 2 + random.nextInt(3) + (z >= 12 ? 3 : z >= 6 ? 1 : 0));
				for (int y = 1; y <= height; y++) {
					boolean window = z % 4 == 0 && z > 0 && z < 20 && (y == 3 || y == 4);
					t.set(x, y, z, window ? AIR : random.nextDouble() < 0.3 ? CRACKED : BRICKS);
				}
			}
		}
		// The facade with its doorway, broken down to a stump on one side; the apse wall whole, with a round window.
		for (int x = 3; x <= 13; x++) {
			int height = x <= 6 ? 3 + random.nextInt(2) : 7 - Math.abs(x - sx) / 2;
			for (int y = 1; y <= height; y++) {
				t.set(x, y, 0, random.nextDouble() < 0.3 ? CRACKED : BRICKS);
			}
			t.fill(x, 1, 20, x, 8, 20, BRICKS);
		}
		t.fill(sx - 1, 1, 0, sx + 1, 4, 0, AIR).fill(sx - 1, 5, 0, sx + 1, 5, 0, CHISELED);
		t.fill(sx, 5, 20, sx, 6, 20, AIR).set(sx - 1, 5, 20, CHISELED).set(sx + 1, 5, 20, CHISELED);
		// Buttresses at the corners, one broken.
		for (int[] c : new int[][]{{2, 0}, {14, 0}, {2, 20}, {14, 20}}) {
			t.fill(c[0], 1, c[1], c[0], c[0] == 14 && c[1] == 0 ? 4 : 9, c[1], POLISHED);
		}

		// Pillars down the nave; two have broken off.
		for (int z = 4; z <= 16; z += 4) {
			for (int x : new int[]{4, 12}) {
				int top = x == 4 && z == 8 ? 3 : x == 12 && z == 4 ? 2 : 6;
				t.fill(x, 1, z, x, top, z, POLISHED);
				if (top == 6) {
					t.set(x, 7, z, CHISELED);
				}
			}
		}
		t.set(12, 1, 5, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.WEST))
				.set(5, 1, 8, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST));
		// What is left of the roof: one whole beam, half of another, and one fallen into the nave.
		BlockState beamX = DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X);
		t.fill(3, 8, 16, 13, 8, 16, beamX).fill(3, 8, 12, 7, 8, 12, beamX);
		t.fill(13, 1, 10, 13, 1, 14, DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z));
		t.set(sx, 7, 16, Props.IRON_CAGE.getDefaultState().with(HangingPropBlock.HANGING, true));

		// Pews facing the altar; some are gone.
		BlockState pew = DEAD_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH);
		for (int z : new int[]{2, 4}) {
			for (int x = 5; x <= 11; x++) {
				if (x != sx && random.nextDouble() < 0.75) {
					t.set(x, 1, z, pew);
				}
			}
		}

		// The hole down: a rim of polished stone and a rail with an opening towards the door, as at the ruin's shaft.
		for (int x = sx - 3; x <= sx + 3; x++) {
			for (int z = sz - 3; z <= sz + 3; z++) {
				boolean rim = Math.abs(x - sx) == 3 || Math.abs(z - sz) == 3;
				if (rim) {
					t.set(x, 0, z, POLISHED);
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
		t.fill(5, 1, 17, 11, 1, 19, POLISHED);
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
		t.room(POLISHED, BRICKS, BRICKS);
		for (int z = 1; z <= 23; z++) {
			t.set(5, 0, z, z % 4 == 0 ? CHISELED : BRICKS);
		}
		for (int z : new int[]{4, 8, 16, 20}) {
			for (int x : new int[]{2, 8}) {
				t.fill(x, 1, z, x, 5, z, POLISHED).set(x, 6, z, CHISELED);
			}
		}
		BlockState cornice = ASH_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
		t.fill(1, 6, 1, 1, 6, 23, cornice).fill(9, 6, 1, 9, 6, 23, cornice);
		doorway(t, 5, 0, Direction.NORTH);
		doorway(t, 5, 24, Direction.SOUTH);
		doorway(t, 0, 12, Direction.WEST);
		doorway(t, 10, 12, Direction.EAST);
		t.door(5, 1, 0, Direction.NORTH, HALL_IN, null, null);
		t.door(5, 1, 24, Direction.SOUTH, HALL_OUT, POOL_ARENA, ARENA_IN);
		t.door(0, 1, 12, Direction.WEST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.door(10, 1, 12, Direction.EAST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.fill(3, 1, 23, 3, 4, 23, CHISELED).fill(7, 1, 23, 7, 4, 23, CHISELED).fill(4, 4, 23, 6, 4, 23, CHISELED);

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
		t.set(5, 6, 6, HANGING_LANTERN).set(5, 6, 18, HANGING_LANTERN)
				.set(3, 6, 12, Props.IRON_CAGE.getDefaultState().with(HangingPropBlock.HANGING, true));
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
		t.fill(0, 0, 0, size - 1, RUIN_FOUNDATION - 1, size - 1, COBBLED);
		t.baseY(RUIN_FOUNDATION);
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				double r = random.nextDouble();
				t.set(x, 0, z, r < 0.55 ? COBBLED : r < 0.8 ? STONE : SOIL);
			}
		}
		t.fill(0, 1, 0, size - 1, 7, size - 1, AIR);
		// The pit: a brick retaining wall around it, open above.
		t.fill(1, pit, 1, size - 2, 0, size - 2, BRICKS);
		t.fill(2, pit + 1, 2, size - 3, 0, size - 3, AIR);
		for (int x = 2; x <= size - 3; x++) {
			for (int z = 2; z <= size - 3; z++) {
				boolean court = x >= 5 && x <= size - 6 && z >= 5 && z <= size - 6;
				boolean soil = court && (x <= 6 || x >= size - 7 || z <= 6 || z >= size - 7);
				t.set(x, pit, z, soil ? SOIL : random.nextDouble() < 0.8 ? POLISHED : COBBLED);
			}
		}
		for (int i = 1; i <= size - 2; i++) {
			for (int[] p : new int[][]{{i, 1}, {i, size - 2}, {1, i}, {size - 2, i}}) {
				if (random.nextDouble() < 0.25) {
					t.set(p[0], random.nextInt(2) - 1, p[1], CRACKED);
				}
			}
		}

		// Columns round the court and the walk's low roof on them, partly fallen in.
		for (int i = 4; i <= size - 5; i += 2) {
			for (int[] p : new int[][]{{i, 4}, {i, size - 5}, {4, i}, {size - 5, i}}) {
				boolean fallen = random.nextDouble() < 0.15;
				t.fill(p[0], pit + 1, p[1], p[0], fallen ? pit + 1 : 0, p[1], POLISHED);
			}
		}
		BlockState roof = ASH_BRICK_SLAB.getDefaultState();
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
			t.fill(c - 2, 1, z, c - 2, z == 1 ? 4 : 3, z, POLISHED).fill(c + 2, 1, z, c + 2, z == 1 ? 4 : 2, z, POLISHED);
		}
		t.fill(c - 2, 5, 1, c, 5, 1, CHISELED);
		// Corner piers standing over the walls, one fallen.
		for (int[] p : new int[][]{{1, 1}, {1, size - 2}, {size - 2, size - 2}}) {
			int top = p[1] == 1 ? 6 : p[0] == 1 ? 4 : 1;
			t.fill(p[0], 1, p[1], p[0], top, p[1], POLISHED);
		}
		// The bell tower over the north-east corner of the walk, seen from afar; its bell is gone, the cage still hangs.
		int tx = size - 3;
		t.fill(tx - 1, pit + 1, 1, tx + 1, 7, 3, BRICKS);
		t.fill(tx, 1, 2, tx, 6, 2, AIR);
		for (int y = 5; y <= 6; y++) {
			t.set(tx, y, 1, AIR).set(tx, y, 3, AIR).set(tx - 1, y, 2, AIR).set(tx + 1, y, 2, AIR);
		}
		for (int[] p : new int[][]{{tx - 1, 1}, {tx + 1, 1}, {tx - 1, 3}, {tx + 1, 3}}) {
			t.fill(p[0], 1, p[1], p[0], 7, p[1], POLISHED);
		}
		t.set(tx + 1, 7, 3, AIR).set(tx, 7, 3, CRACKED);
		t.set(tx, 6, 2, Props.IRON_CAGE.getDefaultState().with(HangingPropBlock.HANGING, true));

		// The well in the court's middle: the way down. A low curb with an opening to the north, lanterns on its corners.
		for (int x = c - 3; x <= c + 3; x++) {
			for (int z = c - 3; z <= c + 3; z++) {
				boolean rim = Math.abs(x - c) == 3 || Math.abs(z - c) == 3;
				if (rim) {
					t.set(x, pit, z, POLISHED);
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
		t.room(POLISHED, BRICKS, BRICKS);
		t.fill(5, 0, 6, 9, 0, 14, SOIL);
		for (int z = 5; z <= 15; z += 2) {
			t.fill(4, 1, z, 4, 4, z, POLISHED).fill(10, 1, z, 10, 4, z, POLISHED);
		}
		for (int x = 6; x <= 8; x += 2) {
			t.fill(x, 1, 5, x, 4, 5, POLISHED).fill(x, 1, 15, x, 4, 15, POLISHED);
		}
		// Lintels over the columns, an arcade round the court.
		t.fill(4, 5, 5, 4, 5, 15, BRICKS).fill(10, 5, 5, 10, 5, 15, BRICKS).fill(4, 5, 5, 10, 5, 5, BRICKS).fill(4, 5, 15, 10, 5, 15, BRICKS);
		doorway(t, 7, 0, Direction.NORTH);
		doorway(t, 7, 20, Direction.SOUTH);
		doorway(t, 0, 10, Direction.WEST);
		doorway(t, 14, 10, Direction.EAST);
		t.door(7, 1, 0, Direction.NORTH, HALL_IN, null, null);
		t.door(7, 1, 20, Direction.SOUTH, HALL_OUT, POOL_ARENA, ARENA_IN);
		t.door(0, 1, 10, Direction.WEST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.door(14, 1, 10, Direction.EAST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.fill(5, 1, 19, 5, 4, 19, CHISELED).fill(9, 1, 19, 9, 4, 19, CHISELED).fill(6, 4, 19, 8, 4, 19, CHISELED);

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
		t.room(COBBLED, BRICKS, BRICKS);
		for (int z = 3; z <= 9; z += 3) {
			t.fill(0, 1, z, 0, 3, z, POLISHED).fill(4, 1, z, 4, 3, z, POLISHED).fill(1, 4, z, 3, 4, z, POLISHED);
		}
		doorway(t, 2, 0, Direction.NORTH);
		doorway(t, 2, 12, Direction.SOUTH);
		t.door(2, 1, 0, Direction.NORTH, HALL_IN, null, null);
		t.door(2, 1, 12, Direction.SOUTH, HALL_OUT, POOL_HALLS, HALL_IN);
		t.set(1, 3, 6, HANGING_LANTERN);
		t.entity(2, 1, 7, guard(HOLLOW, SanctumGuards.ROLL_HALL));
		return t;
	}

	/** Crossroads chamber, 13x7x13, three exits, two chests, a Hollow spot and an archer spot. */
	private static TemplateBuilder hallChamber() {
		TemplateBuilder t = new TemplateBuilder(13, 7, 13);
		t.room(POLISHED, BRICKS, BRICKS);
		t.fill(5, 1, 5, 7, 5, 7, BRICKS).fill(5, 1, 5, 5, 5, 5, CHISELED).fill(7, 1, 7, 7, 5, 7, CHISELED);
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
		}
		t.entity(3, 1, 4, guard(HOLLOW, SanctumGuards.ROLL_HALL));
		t.entity(9, 1, 9, guard(SKELETON, SanctumGuards.ROLL_ARCHER));
		return t;
	}

	/** Dead-end crypt, 9x6x9: coffins along the walls, a chest at the back, a Hollow spot. */
	private static TemplateBuilder hallCrypt() {
		TemplateBuilder t = new TemplateBuilder(9, 6, 9);
		t.room(COBBLED, BRICKS, BRICKS);
		BlockState coffin = DEAD_SLAB.getDefaultState();
		for (int z = 2; z <= 6; z += 2) {
			t.fill(1, 1, z, 2, 1, z, coffin);
			t.fill(6, 1, z, 7, 1, z, coffin);
		}
		doorway(t, 4, 0, Direction.NORTH);
		t.door(4, 1, 0, Direction.NORTH, HALL_IN, null, null);
		t.chest(4, 1, 7, Direction.NORTH, OUTER_HALL_LOOT);
		t.set(4, 4, 4, HANGING_LANTERN);
		t.entity(4, 1, 5, guard(HOLLOW, SanctumGuards.ROLL_HALL));
		return t;
	}

	/** Dead-end storeroom, 9x6x11 (PLAN 16.2): sacks, barrels, a broken cart and a weapon rack, a chest, a Hollow spot. */
	private static TemplateBuilder hallStoreroom() {
		TemplateBuilder t = new TemplateBuilder(9, 6, 11);
		t.room(COBBLED, BRICKS, BRICKS);
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
		t.entity(4, 1, 5, guard(HOLLOW, SanctumGuards.ROLL_HALL));
		return t;
	}

	/** Brick plug for an exit where no hall fits (the halls pool's fallback). */
	private static TemplateBuilder hallCap() {
		TemplateBuilder t = new TemplateBuilder(5, 5, 1);
		t.fill(0, 0, 0, 4, 4, 0, BRICKS);
		t.jigsaw(2, 1, 0, JigsawOrientation.NORTH_UP, HALL_IN, null, null, BRICKS);
		return t;
	}

	/** {@link TemplateBuilder#room} for a box inside the template: walls, floor, ceiling, air inside. */
	private static void room(TemplateBuilder t, int x0, int y0, int z0, int x1, int y1, int z1, BlockState floor,
			BlockState walls, BlockState ceiling) {
		t.fill(x0, y0, z0, x1, y1, z1, walls);
		t.fill(x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1, AIR);
		t.fill(x0 + 1, y0, z0 + 1, x1 - 1, y0, z1 - 1, floor);
		t.fill(x0 + 1, y1, z0 + 1, x1 - 1, y1, z1 - 1, ceiling);
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

	/** Opens a 3-wide, 3-high doorway in the wall at (x, z), centered on the connector. */
	private static void doorway(TemplateBuilder t, int x, int z, Direction facing) {
		if (facing.getAxis() == Direction.Axis.Z) {
			t.fill(x - 1, 1, z, x + 1, 3, z, AIR);
		} else {
			t.fill(x, 1, z - 1, x, 3, z + 1, AIR);
		}
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
