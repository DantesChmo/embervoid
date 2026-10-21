package com.embervoid.sanctum.datagen;

import com.embervoid.Embervoid;
import com.embervoid.entity.mob.MobEntities;
import com.embervoid.progress.ProgressManager;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumEntities;
import com.embervoid.sanctum.SanctumGuards;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
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
 * The Ember Sanctum's jigsaw pieces, built in code (there is no in-game building in this workflow).
 *
 * <pre>
 * ruin (surface, start) ─down─► descent (spiral stairwell) ─► gate_hall (guards, loot) ─► core
 *                                                               └─ sides ─► halls pool (corridor / chamber / crypt)
 * </pre>
 * The path to the core is a fixed chain, so every Sanctum has one; the side halls are random.
 * Horizontal connectors sit in the middle of 3x3 doorways at feet level (y = 1).
 */
final class SanctumTemplates {
	static final Identifier POOL_DESCENT = Embervoid.id("sanctum/descent");
	static final Identifier POOL_GATE = Embervoid.id("sanctum/gate_hall");
	static final Identifier POOL_CORE = Embervoid.id("sanctum/core");
	static final Identifier POOL_HALLS = Embervoid.id("sanctum/halls");

	private static final Identifier RUIN_DOWN = Embervoid.id("sanctum_ruin_down");
	private static final Identifier DESCENT_TOP = Embervoid.id("sanctum_descent_top");
	private static final Identifier HALL_IN = Embervoid.id("sanctum_hall_in");
	private static final Identifier HALL_OUT = Embervoid.id("sanctum_hall_out");
	private static final Identifier CORE_IN = Embervoid.id("sanctum_core_in");

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
		pieces.put(Embervoid.id("sanctum/ruin"), ruin());
		pieces.put(Embervoid.id("sanctum/descent"), descent());
		pieces.put(Embervoid.id("sanctum/gate_hall"), gateHall());
		pieces.put(Embervoid.id("sanctum/core"), core());
		pieces.put(Embervoid.id("sanctum/hall_corridor"), hallCorridor());
		pieces.put(Embervoid.id("sanctum/hall_chamber"), hallChamber());
		pieces.put(Embervoid.id("sanctum/hall_crypt"), hallCrypt());
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
	 * and a shaft cut open by a ravine looks like a pillar of rock rather than a brick tower.
	 */
	private static TemplateBuilder descent() {
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
		t.door(center, 1, size - 1, Direction.SOUTH, HALL_OUT, POOL_GATE, HALL_IN);
		t.jigsaw(center, height - 1, center, JigsawOrientation.UP_SOUTH, DESCENT_TOP, null, null, AIR);
		return t;
	}

	/** Guarded hall between the descent and the core, 9x7x23, with loot and two side exits. */
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
		t.door(4, 1, 22, Direction.SOUTH, HALL_OUT, POOL_CORE, CORE_IN);
		t.door(0, 1, 11, Direction.WEST, HALL_OUT, POOL_HALLS, HALL_IN);
		t.door(8, 1, 11, Direction.EAST, HALL_OUT, POOL_HALLS, HALL_IN);
		// Chiseled frame around the way into the core.
		t.fill(2, 1, 21, 2, 4, 21, CHISELED).fill(6, 1, 21, 6, 4, 21, CHISELED).fill(3, 4, 21, 5, 4, 21, CHISELED);
		t.set(4, 5, 6, HANGING_LANTERN).set(4, 5, 16, HANGING_LANTERN);
		t.chest(2, 1, 2, Direction.EAST, OUTER_HALL_LOOT);
		t.chest(6, 1, 19, Direction.WEST, OUTER_HALL_LOOT);
		t.entity(4, 1, 12, guard(HOLLOW, null));
		t.entity(4, 1, 18, guard(SKELETON, null));
		return t;
	}

	/**
	 * The core, 21x11x21: a safe room floored with Sanctum Flagstone (unbreakable, spawn-proof, attunes), the
	 * Ember Shrine in the middle, four Void Obelisks in a ring and the Ashen Maiden.
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

	/** Brick plug for an exit where no hall fits (the halls pool's fallback). */
	private static TemplateBuilder hallCap() {
		TemplateBuilder t = new TemplateBuilder(5, 5, 1);
		t.fill(0, 0, 0, 4, 4, 0, BRICKS);
		t.jigsaw(2, 1, 0, JigsawOrientation.NORTH_UP, HALL_IN, null, null, BRICKS);
		return t;
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
