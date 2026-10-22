package com.embervoid.client.reach.boss.datagen;

import com.embervoid.reach.boss.arena.ReachArenas;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.reach.boss.arena.ReachArenas.Arena;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static com.embervoid.registry.ModBlocks.*;

/**
 * The three zone arena templates (PLAN 12.2), built in code from the layouts in {@link ReachArenas}. Coordinates in
 * the helpers are offsets from the arena centre (x east, z south, north = towards the landing); the template is the
 * square of side 2·site + 1 around it, y = 0 the floor. The arena structure (track reach-world) clears the site,
 * places the template and puts the Ember Shrine on the platform left for it.
 */
final class ArenaTemplates {
	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final BlockState BRICKS = ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState POLISHED = POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState CHISELED = CHISELED_ASH_STONE.getDefaultState();
	private static final BlockState COBBLED = COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState SOIL = ASH_SOIL.getDefaultState();
	private static final BlockState LOG = DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y);
	private static final BlockState WALL = ASH_BRICK_WALL.getDefaultState();
	private static final BlockState LANTERN = EMBER_LANTERN.getDefaultState();
	private static final BlockState SOUL_LANTERN = Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true);

	static Map<Identifier, TemplateBuilder> all() {
		Map<Identifier, TemplateBuilder> templates = new LinkedHashMap<>();
		templates.put(ReachArenas.FIELDS.template(), fields());
		templates.put(ReachArenas.WOODS.template(), woods());
		templates.put(ReachArenas.NECROPOLIS.template(), necropolis());
		templates.put(ReachArenas.CITADEL.template(), citadel());
		return templates;
	}

	/** Builder addressed by offsets from the arena centre. */
	private static final class Canvas {
		final Arena arena;
		final TemplateBuilder builder;
		final Random random;

		Canvas(Arena arena, long seed) {
			this.arena = arena;
			this.builder = new TemplateBuilder(arena.size(), arena.height(), arena.size());
			this.random = new Random(seed);
		}

		void set(int x, int y, int z, BlockState state) {
			builder.set(x + arena.site(), y, z + arena.site(), state);
		}

		void column(int x, int z, int y0, int y1, BlockState state) {
			for (int y = y0; y <= y1; y++) {
				set(x, y, z, state);
			}
		}

		boolean inSite(int x, int z) {
			return x * x + z * z <= (arena.site() + 0.5) * (arena.site() + 0.5);
		}

		/** Floor platform for the shrine and the arena-side chest beside it (the shrine block itself is placed by the arena structure). */
		void shrinePlatform(BlockState floor) {
			BlockPos shrine = arena.shrine();
			for (int dx = -2; dx <= 3; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					set(shrine.getX() + dx, 0, shrine.getZ() + dz, floor);
				}
			}
			set(shrine.getX(), 0, shrine.getZ(), CHISELED);
			if (!arena.hasChest()) {
				return;
			}
			BlockPos chest = arena.chest();
			builder.chest(chest.getX() + arena.site(), chest.getY(), chest.getZ() + arena.site(), Direction.NORTH, arena.chestLoot());
		}

		<T> T pick(T[] options, int[] weights) {
			int total = 0;
			for (int w : weights) {
				total += w;
			}
			int roll = random.nextInt(total);
			for (int i = 0; i < options.length; i++) {
				roll -= weights[i];
				if (roll < 0) {
					return options[i];
				}
			}
			return options[options.length - 1];
		}
	}

	private static double radius(int x, int z) {
		return Math.sqrt(x * x + z * z);
	}

	// --- Ashen Fields: market square sunk into a crater ---

	private static TemplateBuilder fields() {
		Arena arena = ReachArenas.FIELDS;
		Canvas c = new Canvas(arena, 0xF1E1D5L);
		BlockState[] paving = {POLISHED, COBBLED, BRICKS, CRACKED};
		int[] pavingWeights = {50, 25, 15, 10};
		for (int x = -arena.site(); x <= arena.site(); x++) {
			for (int z = -arena.site(); z <= arena.site(); z++) {
				if (!c.inSite(x, z)) {
					continue;
				}
				double r = radius(x, z);
				BlockState floor;
				if (r <= 3.5) {
					floor = CHISELED;
				} else if (r <= arena.radius() - 0.5) {
					floor = c.pick(paving, pavingWeights);
				} else if (r <= arena.radius() + 0.5) {
					floor = BRICKS; // curb at the fog wall
				} else {
					floor = c.random.nextInt(6) == 0 ? COBBLED : SOIL;
				}
				c.set(x, 0, z, floor);
			}
		}
		// Path from the market out to the shrine.
		for (int z = -arena.radius(); z >= -arena.radius() - ReachArenas.SHRINE_GAP; z--) {
			for (int x = -1; x <= 1; x++) {
				c.set(x, 0, z, POLISHED);
			}
		}
		// Burnt-granary pillars: dead log corners, brick sides, a chiseled cap.
		for (BlockPos pillar : ReachArenas.pillars()) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					boolean corner = dx != 0 && dz != 0;
					for (int y = 1; y <= ReachArenas.PILLAR_HEIGHT; y++) {
						BlockState state = y == ReachArenas.PILLAR_HEIGHT ? CHISELED
								: corner ? LOG : (c.random.nextInt(4) == 0 ? CRACKED : BRICKS);
						c.set(pillar.getX() + dx, y, pillar.getZ() + dz, state);
					}
					c.set(pillar.getX() + dx, 0, pillar.getZ() + dz, CHISELED);
				}
			}
		}
		// Ruined market stalls between the pillars: fence posts and a sagging slab roof.
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * 2 * i / 8 + 0.2;
			int sx = (int) Math.round(Math.sin(angle) * 14);
			int sz = (int) Math.round(-Math.cos(angle) * 14);
			boolean collapsed = i % 3 == 2;
			for (int[] post : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
				int height = collapsed && post[0] > 0 ? 1 : 2;
				c.column(sx + post[0], sz + post[1], 1, height, DEAD_FENCE.getDefaultState());
			}
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (!collapsed || dx <= 0) {
						c.set(sx + dx, 3, sz + dz, DEAD_SLAB.getDefaultState());
					}
				}
			}
		}
		// Rubble around the crater floor.
		for (int i = 0; i < 70; i++) {
			double angle = c.random.nextDouble() * Math.PI * 2;
			double r = 30 + c.random.nextDouble() * 17;
			int x = (int) Math.round(Math.sin(angle) * r);
			int z = (int) Math.round(-Math.cos(angle) * r);
			if (Math.abs(x) <= 3 && z < -arena.radius() + 2) {
				continue; // keep the path to the shrine clear
			}
			BlockState rubble = switch (c.random.nextInt(4)) {
				case 0 -> COBBLED_ASH_STONE_SLAB.getDefaultState();
				case 1 -> COBBLED;
				case 2 -> ASH_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.fromHorizontal(c.random.nextInt(4)));
				default -> CRACKED;
			};
			c.set(x, 1, z, rubble);
		}
		c.shrinePlatform(POLISHED);
		return c.builder;
	}

	// --- Royal Woods: the walled Royal Grove ---

	private static TemplateBuilder woods() {
		Arena arena = ReachArenas.WOODS;
		Canvas c = new Canvas(arena, 0x9207EL);
		BlockState podzol = Blocks.PODZOL.getDefaultState();
		BlockState[] ground = {podzol, Blocks.MOSS_BLOCK.getDefaultState(), Blocks.COARSE_DIRT.getDefaultState(),
				Blocks.ROOTED_DIRT.getDefaultState()};
		int[] groundWeights = {50, 20, 20, 10};
		BlockState gravel = Blocks.GRAVEL.getDefaultState();
		BlockState hedge = Blocks.DARK_OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
		int wall = ReachArenas.GROVE_WALL;
		for (int x = -arena.site(); x <= arena.site(); x++) {
			for (int z = -arena.site(); z <= arena.site(); z++) {
				if (!c.inSite(x, z)) {
					continue;
				}
				double r = radius(x, z);
				boolean gate = z < 0 && Math.abs(x) <= ReachArenas.GROVE_GATE_HALF_WIDTH;
				boolean path = Math.abs(x) <= 1 || Math.abs(z) <= 1 || (r >= 24 && r <= 26);
				if (r >= wall && r < wall + 2 && !gate) {
					// Garden wall: brick, cracked in places, capped with a wall.
					c.set(x, 0, z, BRICKS);
					for (int y = 1; y <= ReachArenas.GROVE_WALL_HEIGHT; y++) {
						c.set(x, y, z, y == ReachArenas.GROVE_WALL_HEIGHT ? WALL : (c.random.nextInt(5) == 0 ? CRACKED : BRICKS));
					}
					continue;
				}
				if (r <= 6) {
					c.set(x, 0, z, c.random.nextBoolean() ? Blocks.ROOTED_DIRT.getDefaultState() : Blocks.MOSS_BLOCK.getDefaultState());
				} else if ((path && r < wall) || gate) {
					c.set(x, 0, z, c.random.nextInt(4) == 0 ? Blocks.COARSE_DIRT.getDefaultState() : gravel);
				} else {
					c.set(x, 0, z, c.pick(ground, groundWeights));
				}
				// Hedge rows between the paths, with gaps.
				if (r >= 36 && r <= 37.2 && !path && !gate && (int) Math.floor(Math.toDegrees(Math.atan2(z, x)) / 12) % 3 != 0) {
					c.column(x, z, 1, 2, hedge);
				}
			}
		}
		// Gate posts with lanterns.
		for (int side : new int[]{-1, 1}) {
			int x = side * (ReachArenas.GROVE_GATE_HALF_WIDTH + 1);
			int z = -(int) Math.round(Math.sqrt((wall + 0.5) * (wall + 0.5) - x * x));
			c.column(x, z, 1, ReachArenas.GROVE_WALL_HEIGHT + 1, CHISELED);
			c.set(x, ReachArenas.GROVE_WALL_HEIGHT + 2, z, LANTERN);
		}
		// Lantern posts along the ring path, benches facing the middle.
		for (int i = 0; i < 12; i++) {
			double angle = Math.PI * 2 * (i + 0.5) / 12;
			int x = (int) Math.round(Math.sin(angle) * 27);
			int z = (int) Math.round(-Math.cos(angle) * 27);
			c.column(x, z, 1, 2, WALL);
			c.set(x, 3, z, LANTERN);
			if (i % 2 == 0) {
				int bx = (int) Math.round(Math.sin(angle + 0.12) * 22);
				int bz = (int) Math.round(-Math.cos(angle + 0.12) * 22);
				// The seat faces the middle of the grove.
				Direction facing = Direction.getFacing((float) bx, 0, (float) bz);
				c.set(bx, 1, bz, DEAD_STAIRS.getDefaultState().with(StairsBlock.FACING, facing));
			}
		}
		c.shrinePlatform(POLISHED);
		return c.builder;
	}

	// --- Sunken Necropolis: sinkhole, switchback stair, flooded ossuary ---

	private static TemplateBuilder necropolis() {
		Arena arena = ReachArenas.NECROPOLIS;
		Canvas c = new Canvas(arena, 0x05505AL);
		BlockState[] floor = {Blocks.TUFF.getDefaultState(), CRACKED, Blocks.MOSSY_COBBLESTONE.getDefaultState(), Blocks.GRAVEL.getDefaultState()};
		int[] floorWeights = {40, 30, 20, 10};
		BlockState bone = Blocks.BONE_BLOCK.getDefaultState();
		BlockState roof = Blocks.DEEPSLATE_TILES.getDefaultState();
		for (int x = -arena.site(); x <= arena.site(); x++) {
			for (int z = -arena.site(); z <= arena.site(); z++) {
				if (!c.inSite(x, z)) {
					continue;
				}
				double r = radius(x, z);
				if (ReachArenas.isWalkway(x, z)) {
					c.set(x, 0, z, COBBLED);
					c.set(x, 1, z, COBBLED);
					c.set(x, ReachArenas.WALKWAY_TOP, z, r <= ReachArenas.DAIS_RADIUS + 0.5 ? CHISELED : POLISHED);
				} else {
					c.set(x, 0, z, c.pick(floor, floorWeights));
				}
				// The ossuary roof over the ring, fallen in here and there.
				if (r >= ReachArenas.ROOF_INNER && r <= ReachArenas.ROOF_OUTER && c.random.nextInt(7) != 0) {
					c.set(x, ReachArenas.ROOF_Y, z, roof);
				}
			}
		}
		// Bone columns holding the roof, on its inner and outer edge.
		for (int[] ring : new int[][]{{ReachArenas.ROOF_INNER, 12}, {ReachArenas.ROOF_OUTER - 1, 24}}) {
			for (int i = 0; i < ring[1]; i++) {
				double angle = Math.PI * 2 * (i + 0.5) / ring[1];
				int x = (int) Math.round(Math.sin(angle) * ring[0]);
				int z = (int) Math.round(-Math.cos(angle) * ring[0]);
				if (ReachArenas.isWalkway(x, z)) {
					continue;
				}
				c.column(x, z, 1, ReachArenas.ROOF_Y - 1, bone);
				c.set(x, ReachArenas.ROOF_Y, z, roof);
				if (i % 3 == 0) {
					c.set(x + 1, ReachArenas.ROOF_Y - 1, z, Blocks.CHAIN.getDefaultState());
					c.set(x + 1, ReachArenas.ROOF_Y - 2, z, SOUL_LANTERN);
				}
			}
		}
		// Unlit braziers on the pedestals (the Crypt Keeper's light, PLAN 12.3), candles on the dais.
		for (BlockPos brazier : ReachArenas.braziers()) {
			c.set(brazier.getX(), brazier.getY(), brazier.getZ(), Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));
		}
		for (int[] candle : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
			c.set(candle[0], ReachArenas.WALKWAY_TOP + 1, candle[1], Blocks.CANDLE.getDefaultState());
		}
		// Bones and skulls on the lower floor.
		for (int i = 0; i < 40; i++) {
			double angle = c.random.nextDouble() * Math.PI * 2;
			double r = 15 + c.random.nextDouble() * 15;
			int x = (int) Math.round(Math.sin(angle) * r);
			int z = (int) Math.round(-Math.cos(angle) * r);
			if (ReachArenas.isWalkway(x, z) || c.builder.get(x + arena.site(), 1, z + arena.site()).isOf(Blocks.BONE_BLOCK)) {
				continue;
			}
			c.set(x, 1, z, c.random.nextInt(3) == 0
					? Blocks.SKELETON_SKULL.getDefaultState().with(SkullBlock.ROTATION, c.random.nextInt(16))
					: Blocks.BONE_BLOCK.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z));
		}
		switchbackStair(c);
		c.shrinePlatform(POLISHED);
		return c.builder;
	}

	/**
	 * The stair from the rim down the pit wall: an outer flight from the north going east, a landing, and an inner
	 * flight back towards the north ending by the shrine. Half-block steps (slabs), so it is walked without jumping.
	 */
	private static void switchbackStair(Canvas c) {
		int depth = ReachArenas.PIT_DEPTH;
		int landing = 9;
		flight(c, 36.5, 39.5, -100, -66, depth, landing);
		flight(c, 33.5, 39.5, -66, -50, landing, landing);
		flight(c, 33.5, 36.2, -50, -84, landing, 1);
	}

	/** A curved flight between two radii from angle {@code from} to {@code to} (degrees, north = −90), surface {@code high} → {@code low}. */
	private static void flight(Canvas c, double inner, double outer, double from, double to, int high, int low) {
		int site = c.arena.site();
		for (int x = -site; x <= site; x++) {
			for (int z = -site; z <= site; z++) {
				double r = radius(x, z);
				if (r < inner || r > outer) {
					continue;
				}
				double angle = Math.toDegrees(Math.atan2(z, x));
				double t = (angle - from) / (to - from);
				if (t < 0 || t > 1) {
					continue;
				}
				// Surface in half blocks, falling evenly from high to low.
				int halves = (int) Math.round((high + (low - high) * t) * 2);
				int full = halves / 2;
				c.column(x, z, 1, full - 1, COBBLED);
				if (halves % 2 == 1) {
					c.set(x, full, z, COBBLED_ASH_STONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM));
				} else if (full >= 1) {
					c.set(x, full - 1, z, POLISHED);
				}
			}
		}
	}

	// --- Smoldering Citadel: the courtyard below the keep wall ---

	private static TemplateBuilder citadel() {
		Arena arena = ReachArenas.CITADEL;
		Canvas c = new Canvas(arena, 0xC17ADE1L);
		BlockState[] paving = {BRICKS, POLISHED, CRACKED, COBBLED};
		int[] pavingWeights = {45, 25, 20, 10};
		BlockState ember = ReachBlocks.EMBER_CRACK.getDefaultState();
		BlockState smoldering = ReachBlocks.SMOLDERING_LOG.getDefaultState();
		int wallZ = ReachArenas.KEEP_WALL_Z;
		int wallTop = ReachArenas.KEEP_WALL_HEIGHT;
		for (int x = -arena.site(); x <= arena.site(); x++) {
			for (int z = -arena.site(); z <= arena.site(); z++) {
				if (!c.inSite(x, z)) {
					continue;
				}
				double r = radius(x, z);
				BlockState floor = r <= 3.5 ? CHISELED : (r > 6 && c.random.nextInt(50) == 0 ? ember : c.pick(paving, pavingWeights));
				c.set(x, 0, z, floor);
				if (z >= wallZ && z < wallZ + ReachArenas.KEEP_WALL_DEPTH) {
					boolean gate = Math.abs(x) <= ReachArenas.KEEP_GATE_HALF_WIDTH;
					for (int y = 1; y <= wallTop; y++) {
						if (gate && y <= ReachArenas.KEEP_GATE_HEIGHT) {
							continue;
						}
						c.set(x, y, z, y == wallTop ? POLISHED : (c.random.nextInt(5) == 0 ? CRACKED : BRICKS));
					}
					// Crenellations along the wall's inner edge.
					if (z == wallZ && Math.abs(x) % 2 == 0 && Math.abs(x) > 2) {
						c.set(x, wallTop + 1, z, BRICKS);
					}
				}
			}
		}
		// Burning beams fallen across the courtyard.
		for (int i = 0; i < 6; i++) {
			double angle = c.random.nextDouble() * Math.PI * 2;
			double r = 8 + c.random.nextDouble() * 12;
			int x = (int) Math.round(Math.sin(angle) * r);
			int z = (int) Math.round(-Math.cos(angle) * r);
			if (z >= wallZ - 2) {
				continue;
			}
			boolean alongX = c.random.nextBoolean();
			for (int d = -2; d <= 2; d++) {
				c.set(alongX ? x + d : x, 1, alongX ? z : z + d,
						smoldering.with(PillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z));
			}
		}
		// Signal braziers on pedestals: lighting all three brings the Lord down from the wall.
		for (BlockPos brazier : ReachArenas.signalBraziers()) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					c.column(brazier.getX() + dx, brazier.getZ() + dz, 1, brazier.getY() - 1, CHISELED);
				}
			}
			c.set(brazier.getX(), brazier.getY(), brazier.getZ(), Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));
		}
		// Way in from the north edge (where the inner ring road meets the square, x −3…3) past the shrine.
		for (int z = -arena.radius(); z >= -arena.site(); z--) {
			for (int x = -3; x <= 3; x++) {
				c.set(x, 0, z, POLISHED);
			}
		}
		c.shrinePlatform(POLISHED);
		return c.builder;
	}

	private ArenaTemplates() {}
}
