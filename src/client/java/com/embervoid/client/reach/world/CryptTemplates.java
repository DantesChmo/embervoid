package com.embervoid.client.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BellBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.Attachment;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Necropolis crypt (PLAN 9.8): a jigsaw of {@code embervoid:ashen_reach/necropolis/crypt/<piece>}. The entrance
 * stairwell (start, 9 below the ground) leads to a junction; the junction's side doors open into random halls, its far
 * door into the descent and the lower crypt with the Necropolis treasure. Doors are 3 wide, their jigsaw in the middle
 * at floor + 1; a piece is entered through its {@link #IN} door, the others lead out.
 */
public final class CryptTemplates {
	public static final Identifier IN = Embervoid.id("crypt_in");
	public static final Identifier OUT = Embervoid.id("crypt_out");
	public static final Identifier POOL_START_HALL = pool("start_hall");
	public static final Identifier POOL_HALLS = pool("halls");
	public static final Identifier POOL_DESCENT = pool("descent");
	public static final Identifier POOL_LOWER = pool("lower");

	private static final BlockState WET = ReachBlocks.WET_CRYPT_STONE.getDefaultState();
	private static final BlockState BRICKS = ModBlocks.ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = ModBlocks.CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState POLISHED = ModBlocks.POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState CHISELED = ModBlocks.CHISELED_ASH_STONE.getDefaultState();
	private static final BlockState BONES = Blocks.BONE_BLOCK.getDefaultState();
	private static final BlockState SKULL = Blocks.SKELETON_SKULL.getDefaultState();
	private static final BlockState WATER = Blocks.WATER.getDefaultState();
	private static final BlockState CANDLE = Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true);

	public static Map<Identifier, TemplateBuilder> all() {
		ReachZoneKit z = ReachZoneKit.NECROPOLIS;
		Map<Identifier, TemplateBuilder> t = new LinkedHashMap<>();
		t.put(piece("entrance"), entrance());
		t.put(piece("junction"), junction(z));
		t.put(piece("corridor"), corridor(z));
		t.put(piece("burial_hall"), burialHall(z));
		t.put(piece("flooded_hall"), floodedHall(z));
		t.put(piece("warden_chamber"), wardenChamber(z));
		t.put(piece("dead_end"), deadEnd());
		t.put(piece("descent"), descent());
		t.put(piece("lower_crypt"), lowerCrypt(z));
		return t;
	}

	public static Identifier piece(String name) {
		return Embervoid.id("ashen_reach/necropolis/crypt/" + name);
	}

	private static Identifier pool(String name) {
		return Embervoid.id("ashen_reach/necropolis/crypt/" + name);
	}

	/** A closed stone box: floor y = 0, ceiling at the top, air inside. */
	private static Ruin room(String name, int sx, int sy, int sz) {
		Ruin r = new Ruin("crypt_" + name, sx, sy, sz);
		r.fill(0, 0, 0, sx - 1, sy - 1, sz - 1, WET, CRACKED, 0.2);
		r.fill(1, 0, 1, sx - 2, 0, sz - 2, POLISHED, WET, 0.3);
		r.clear(1, 1, 1, sx - 2, sy - 2, sz - 2);
		return r;
	}

	/** A 3-wide, 3-high doorway centred on (x, z) in a wall, the jigsaw in its middle at y = 1. */
	private static void door(Ruin r, int x, int z, Direction facing, Identifier name, Identifier pool, Identifier target) {
		boolean alongX = facing.getAxis() == Direction.Axis.Z;
		for (int d = -1; d <= 1; d++) {
			for (int y = 1; y <= 3; y++) {
				r.set(alongX ? x + d : x, y, alongX ? z : z + d, Ruin.AIR);
			}
		}
		r.t.door(x, 1, z, facing, name, pool, target);
	}

	private static void in(Ruin r, int x, int z, Direction facing) {
		door(r, x, z, facing, IN, null, null);
	}

	private static void out(Ruin r, int x, int z, Direction facing, Identifier pool) {
		door(r, x, z, facing, OUT, pool, IN);
	}

	/** Entrance: a porch on the ground (y = 9), stairs down south to a landing and the door into the crypt. */
	static TemplateBuilder entrance() {
		Ruin r = new Ruin("crypt_entrance", 7, 14, 15);
		r.fill(1, 0, 3, 5, 12, 14, WET, CRACKED, 0.15);
		r.fill(1, 9, 0, 5, 9, 3, POLISHED);
		r.clear(2, 1, 4, 4, 12, 12);
		r.clear(1, 10, 0, 5, 12, 2);
		for (int k = 0; k <= 7; k++) {
			int y = 8 - k;
			int z = 4 + k;
			r.fill(2, y, z, 4, y, z, ModBlocks.ASH_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
			if (y > 0) {
				r.fill(2, 0, z, 4, y - 1, z, WET);
			}
		}
		r.fill(2, 0, 12, 4, 0, 13, POLISHED);
		r.clear(2, 1, 12, 4, 4, 13);
		r.fill(1, 13, 3, 5, 13, 14, WET);
		r.fill(1, 10, 0, 1, 12, 0, CHISELED).fill(5, 10, 0, 5, 12, 0, CHISELED);
		r.fill(1, 13, 0, 5, 13, 2, ModBlocks.ASH_BRICK_SLAB.getDefaultState());
		r.clear(2, 10, 3, 4, 12, 3);
		r.set(1, 10, 1, CANDLE).set(5, 10, 1, CANDLE);
		out(r, 3, 14, Direction.SOUTH, POOL_START_HALL);
		return r.build();
	}

	/** Junction: a pillared room; halls to the sides, the way down straight ahead. */
	static TemplateBuilder junction(ReachZoneKit z) {
		Ruin r = room("junction", 11, 6, 11);
		for (int[] c : new int[][]{{3, 3}, {7, 3}, {3, 7}, {7, 7}}) {
			r.fill(c[0], 1, c[1], c[0], 4, c[1], CHISELED);
		}
		r.set(5, 1, 5, CANDLE.with(CandleBlock.CANDLES, 3));
		in(r, 5, 0, Direction.NORTH);
		out(r, 10, 5, Direction.EAST, POOL_HALLS);
		out(r, 0, 5, Direction.WEST, POOL_HALLS);
		out(r, 5, 10, Direction.SOUTH, POOL_DESCENT);
		r.mob(5, 1, 7, z.fodder(), Direction.NORTH);
		return r.build();
	}

	static TemplateBuilder corridor(ReachZoneKit z) {
		Ruin r = room("corridor", 5, 5, 9);
		in(r, 2, 0, Direction.NORTH);
		out(r, 2, 8, Direction.SOUTH, POOL_HALLS);
		r.scatter(1, 1, 3, 7, 1, BONES, 1);
		r.mob(2, 1, 4, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** Burial hall: bone-filled niches in the walls; bones and a Sentinel. */
	static TemplateBuilder burialHall(ReachZoneKit z) {
		Ruin r = room("burial_hall", 9, 6, 13);
		for (int zz = 2; zz <= 10; zz += 2) {
			r.set(1, 2, zz, BONES).set(7, 2, zz, BONES);
			r.set(1, 3, zz, SKULL).set(7, 3, zz, SKULL);
		}
		in(r, 4, 0, Direction.NORTH);
		out(r, 4, 12, Direction.SOUTH, POOL_HALLS);
		r.chest(7, 1, 6, Direction.WEST, z.commonChest());
		r.mob(3, 1, 4, z.fodder(), Direction.NORTH).mob(5, 1, 8, z.fodder(), Direction.NORTH);
		r.mob(4, 1, 10, z.tank(), Direction.NORTH);
		return r.build();
	}

	/** Flooded hall: a foot of water over the whole floor; moths over it. */
	static TemplateBuilder floodedHall(ReachZoneKit z) {
		Ruin r = room("flooded_hall", 9, 6, 11);
		r.fill(1, 1, 1, 7, 1, 9, WATER);
		in(r, 4, 0, Direction.NORTH);
		out(r, 4, 10, Direction.SOUTH, POOL_HALLS);
		r.mob(4, 1, 5, z.fodder(), Direction.NORTH);
		r.mob(2, 3, 3, z.ranged(), Direction.EAST).mob(6, 3, 7, z.ranged(), Direction.WEST);
		return r.build();
	}

	/** A chapel chamber with its Grave Warden and bell: a dead end. */
	static TemplateBuilder wardenChamber(ReachZoneKit z) {
		Ruin r = room("warden_chamber", 11, 7, 11);
		r.fill(4, 1, 8, 6, 1, 8, CHISELED);
		r.set(5, 1, 7, Blocks.BELL.getDefaultState().with(BellBlock.ATTACHMENT, Attachment.FLOOR).with(BellBlock.FACING, Direction.NORTH));
		r.set(4, 2, 8, CANDLE).set(6, 2, 8, CANDLE);
		in(r, 5, 0, Direction.NORTH);
		r.chest(9, 1, 9, Direction.WEST, z.commonChest());
		r.mob(5, 1, 5, z.elite(), Direction.NORTH);
		r.mob(2, 1, 3, z.fodder(), Direction.NORTH).mob(8, 1, 3, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** A collapsed passage: rubble, no way on. Also the fallback that closes every door the crypt runs out of. */
	static TemplateBuilder deadEnd() {
		Ruin r = room("dead_end", 5, 5, 4);
		r.fill(1, 1, 2, 3, 3, 2, ModBlocks.COBBLED_ASH_STONE.getDefaultState(), Blocks.GRAVEL.getDefaultState(), 0.4);
		r.set(2, 1, 1, Blocks.GRAVEL.getDefaultState());
		in(r, 2, 0, Direction.NORTH);
		return r.build();
	}

	/** Descent: stairs six blocks down to the lower crypt. */
	static TemplateBuilder descent() {
		Ruin r = new Ruin("crypt_descent", 5, 11, 10);
		r.fill(0, 0, 0, 4, 10, 9, WET, CRACKED, 0.2);
		r.clear(1, 7, 0, 3, 9, 1);
		r.fill(1, 6, 0, 3, 6, 1, POLISHED);
		r.clear(1, 1, 2, 3, 9, 8);
		for (int k = 0; k < 6; k++) {
			int y = 5 - k;
			int zz = 2 + k;
			r.fill(1, y, zz, 3, y, zz, ModBlocks.ASH_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
			if (y > 0) {
				r.fill(1, 0, zz, 3, y - 1, zz, WET);
			}
		}
		r.fill(1, 0, 8, 3, 0, 8, POLISHED);
		r.clear(1, 1, 8, 3, 3, 8);
		// The top door is at y 7 (floor 6), the bottom one at y 1 (floor 0).
		for (int d = -1; d <= 1; d++) {
			for (int y = 7; y <= 9; y++) {
				r.set(2 + d, y, 0, Ruin.AIR);
			}
		}
		r.t.door(2, 7, 0, Direction.NORTH, IN, null, null);
		out(r, 2, 9, Direction.SOUTH, POOL_LOWER);
		return r.build();
	}

	/** Lower crypt: a flooded vault, sarcophagi along water channels, the Necropolis treasure on a dais. */
	static TemplateBuilder lowerCrypt(ReachZoneKit z) {
		Ruin r = room("lower_crypt", 13, 8, 15);
		r.fill(1, 0, 3, 1, 0, 13, WATER).fill(11, 0, 3, 11, 0, 13, WATER);
		for (int zz = 4; zz <= 12; zz += 4) {
			r.fill(3, 1, zz, 3, 1, zz + 1, CHISELED).fill(9, 1, zz, 9, 1, zz + 1, CHISELED);
			r.set(3, 2, zz, CANDLE);
		}
		for (int[] c : new int[][]{{5, 4}, {7, 4}, {5, 10}, {7, 10}}) {
			r.fill(c[0], 1, c[1], c[0], 6, c[1], BRICKS);
		}
		r.fill(5, 1, 12, 7, 1, 13, POLISHED);
		r.chest(6, 2, 13, Direction.NORTH, z.treasureChest());
		r.set(5, 2, 13, CANDLE.with(CandleBlock.CANDLES, 4)).set(7, 2, 13, CANDLE.with(CandleBlock.CANDLES, 4));
		in(r, 6, 0, Direction.NORTH);
		r.mob(6, 1, 11, z.elite(), Direction.NORTH);
		r.mob(4, 1, 3, z.tank(), Direction.NORTH).mob(8, 1, 3, z.tank(), Direction.NORTH);
		return r.build();
	}

	private CryptTemplates() {}
}
