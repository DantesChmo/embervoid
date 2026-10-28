package com.embervoid.client.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.block.prop.LongPropBlock;
import com.embervoid.block.prop.PropBlock;
import com.embervoid.block.prop.PropCandleBlock;
import com.embervoid.block.prop.Props;
import com.embervoid.block.prop.StackedPropBlock;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BellBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.BedPart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.embervoid.sanctum.datagen.TemplateBuilder.slab;
import static com.embervoid.sanctum.datagen.TemplateBuilder.stair;

/**
 * The Necropolis crypt: a jigsaw of {@code embervoid:ashen_reach/necropolis/crypt/<piece>}. The entrance
 * stairwell (start, 9 below the ground) leads to a junction; the junction's side doors open into random halls, its far
 * door into the descent and the lower crypt with the Necropolis treasure. Doors are 3 wide, their jigsaw in the middle
 * at floor + 1; a piece is entered through its {@link #IN} door, the others lead out.
 *
 * <p>Built from the Necropolis palette: wet crypt stone over a cobble-brick base course, crypt tiles
 * and ash tiles underfoot, bone-brick trim, carved capitals, grave-iron grilles, blue candles; a ribbed ceiling of
 * hung stairs in every room.
 */
public final class CryptTemplates {
	public static final Identifier IN = Embervoid.id("crypt_in");
	public static final Identifier OUT = Embervoid.id("crypt_out");
	public static final Identifier POOL_START_HALL = pool("start_hall");
	public static final Identifier POOL_HALLS = pool("halls");
	public static final Identifier POOL_DESCENT = pool("descent");
	public static final Identifier POOL_LOWER = pool("lower");

	private static final BlockState WET = ReachBlocks.WET_CRYPT_STONE.getDefaultState();
	private static final BlockState CRYPT_TILES = ReachBlocks.CRYPT_STONE_TILES.getDefaultState();
	private static final BlockState BRICKS = ModBlocks.ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = ModBlocks.CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState MOSSY = ModBlocks.MOSSY_ASH_BRICKS.getDefaultState();
	private static final BlockState COBBLE_BRICKS = ModBlocks.ASH_COBBLE_BRICKS.getDefaultState();
	private static final BlockState BRICK_STAIRS = ModBlocks.ASH_BRICK_STAIRS.getDefaultState();
	private static final BlockState BRICK_SLAB = ModBlocks.ASH_BRICK_SLAB.getDefaultState();
	private static final BlockState POLISHED = ModBlocks.POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState TILES = ModBlocks.ASH_TILES.getDefaultState();
	private static final BlockState CRACKED_TILES = ModBlocks.CRACKED_ASH_TILES.getDefaultState();
	private static final BlockState CHISELED = ModBlocks.CHISELED_ASH_STONE.getDefaultState();
	private static final BlockState CARVED = ModBlocks.CARVED_ASH_STONE.getDefaultState();
	private static final BlockState PILLAR = ModBlocks.ASH_BRICK_PILLAR.getDefaultState();
	private static final BlockState BONE_BRICKS = ReachBlocks.BONE_BRICKS.getDefaultState();
	private static final BlockState BONE_PILLAR = ReachBlocks.BONE_PILLAR.getDefaultState();
	private static final BlockState BONE_WALL = ReachBlocks.BONE_BRICK_WALL.getDefaultState();
	private static final BlockState GRATE = ReachBlocks.GRAVE_IRON_GRATE.getDefaultState();
	private static final BlockState IRON_PLATE = ReachBlocks.GRAVE_IRON_PLATE.getDefaultState();
	private static final BlockState BONES = Blocks.BONE_BLOCK.getDefaultState();
	private static final BlockState SKULL = Blocks.SKELETON_SKULL.getDefaultState();
	private static final BlockState WATER = Blocks.WATER.getDefaultState();
	private static final BlockState CANDLE = Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true);
	private static final BlockState BLUE_CANDLE = Ruin.BLUE_CANDLE;

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

	/** A wall block of the crypt: wet stone, cracked here and there, mossy near the floor. */
	private static BlockState wall(Ruin r, int y, int top) {
		double t = top <= 1 ? 1 : (double) (y - 1) / (top - 1);
		double roll = r.random.nextDouble();
		return roll < 0.1 + 0.15 * t ? CRACKED : roll > 0.9 - 0.2 * (1 - t) ? MOSSY : WET;
	}

	/**
	 * A closed stone box: a cobble-brick base course, wet-stone walls, a tiled floor, a ribbed ceiling of hung
	 * stairs under a brick vault, air inside.
	 */
	private static Ruin room(String name, int sx, int sy, int sz) {
		Ruin r = new Ruin("crypt_" + name, sx, sy, sz);
		r.fill(0, 0, 0, sx - 1, sy - 1, sz - 1, WET, CRACKED, 0.15);
		for (int x = 0; x < sx; x++) {
			for (int zz = 0; zz < sz; zz++) {
				if (x == 0 || x == sx - 1 || zz == 0 || zz == sz - 1) {
					for (int y = 1; y < sy - 1; y++) {
						r.set(x, y, zz, y == 1 ? COBBLE_BRICKS : wall(r, y, sy - 2));
					}
				}
			}
		}
		r.t.blendedFloor(1, 1, sx - 2, sz - 2, 0, r.random, CRYPT_TILES, 5, TILES, 3, CRACKED_TILES, 1, WET, 1);
		r.clear(1, 1, 1, sx - 2, sy - 2, sz - 2);
		r.t.innerCornice(0, 0, sx - 1, sz - 1, sy - 2, BRICK_STAIRS);
		return r;
	}

	/** A 3-wide, 3-high doorway centred on (x, z) in a wall, the jigsaw in its middle at y = 1, bone-pillar jambs. */
	private static void door(Ruin r, int x, int z, Direction facing, Identifier name, Identifier pool, Identifier target) {
		boolean alongX = facing.getAxis() == Direction.Axis.Z;
		for (int d = -1; d <= 1; d++) {
			for (int y = 1; y <= 3; y++) {
				r.set(alongX ? x + d : x, y, alongX ? z : z + d, Ruin.AIR);
			}
		}
		for (int d : new int[]{-2, 2}) {
			int jx = alongX ? x + d : x;
			int jz = alongX ? z : z + d;
			for (int y = 1; y <= 3; y++) {
				r.t.trySet(jx, y, jz, BONE_PILLAR);
			}
		}
		r.t.trySet(x, 4, z, CARVED);
		r.t.door(x, 1, z, facing, name, pool, target);
	}

	private static void in(Ruin r, int x, int z, Direction facing) {
		door(r, x, z, facing, IN, null, null);
	}

	private static void out(Ruin r, int x, int z, Direction facing, Identifier pool) {
		door(r, x, z, facing, OUT, pool, IN);
	}

	/** A pillar of the crypt: a brick-pillar shaft on a chiseled base with a carved capital. */
	private static void pillar(Ruin r, int x, int z, int y0, int y1) {
		r.set(x, y0, z, CHISELED);
		r.fill(x, y0 + 1, z, x, y1 - 1, z, PILLAR);
		r.set(x, y1, z, CARVED);
	}

	/** Entrance: a porch on the ground (y = 9), stairs down south to a landing and the door into the crypt. */
	static TemplateBuilder entrance() {
		Ruin r = new Ruin("crypt_entrance", 7, 14, 15);
		r.fill(1, 0, 3, 5, 12, 14, WET, CRACKED, 0.15);
		r.fill(1, 9, 0, 5, 9, 3, COBBLE_BRICKS, CRYPT_TILES, 0.4);
		r.clear(2, 1, 4, 4, 12, 12);
		r.clear(1, 10, 0, 5, 12, 2);
		for (int k = 0; k <= 7; k++) {
			int y = 8 - k;
			int z = 4 + k;
			r.fill(2, y, z, 4, y, z, stair(BRICK_STAIRS, Direction.NORTH, false));
			if (y > 0) {
				r.fill(2, 0, z, 4, y - 1, z, WET);
			}
			r.set(1, y + 1, z, y % 3 == 0 ? BLUE_CANDLE : GRATE).set(5, y + 2, z, k % 2 == 0 ? BONE_BRICKS : MOSSY);
		}
		r.fill(2, 0, 12, 4, 0, 13, CRYPT_TILES);
		r.clear(2, 1, 12, 4, 4, 13);
		r.fill(1, 13, 3, 5, 13, 14, WET);
		// The porch: bone-pillar posts, a carved lintel, a slab roof, lanterns of candle light.
		r.fill(1, 10, 0, 1, 12, 0, BONE_PILLAR).fill(5, 10, 0, 5, 12, 0, BONE_PILLAR);
		r.fill(1, 10, 2, 1, 12, 2, PILLAR).fill(5, 10, 2, 5, 12, 2, PILLAR);
		r.fill(2, 13, 0, 4, 13, 0, slab(BRICK_SLAB, false)).set(1, 13, 0, CARVED).set(5, 13, 0, CARVED);
		r.fill(1, 13, 1, 5, 13, 2, slab(BRICK_SLAB, false));
		r.t.arch(3, 10, 3, Direction.Axis.X, 3, 3, BRICK_STAIRS, CARVED, null);
		r.set(1, 10, 1, BLUE_CANDLE.with(CandleBlock.CANDLES, 2)).set(5, 10, 1, BLUE_CANDLE.with(CandleBlock.CANDLES, 2));
		r.set(2, 10, 3, BONE_WALL).set(4, 10, 3, BONE_WALL);
		r.set(3, 10, 0, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.SOUTH));
		out(r, 3, 14, Direction.SOUTH, POOL_START_HALL);
		return r.build();
	}

	/** Junction: a pillared room with a floor of iron plates in the middle; halls to the sides, the way down ahead. */
	static TemplateBuilder junction(ReachZoneKit z) {
		Ruin r = room("junction", 11, 6, 11);
		for (int[] c : new int[][]{{3, 3}, {7, 3}, {3, 7}, {7, 7}}) {
			pillar(r, c[0], c[1], 1, 4);
		}
		r.fill(4, 0, 4, 6, 0, 6, IRON_PLATE).set(5, 0, 5, CARVED);
		r.set(5, 1, 5, Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
		r.set(1, 1, 1, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2)).set(9, 1, 9, Props.BONE_PILE.getDefaultState());
		in(r, 5, 0, Direction.NORTH);
		out(r, 10, 5, Direction.EAST, POOL_HALLS);
		out(r, 0, 5, Direction.WEST, POOL_HALLS);
		out(r, 5, 10, Direction.SOUTH, POOL_DESCENT);
		r.mob(5, 1, 7, z.fodder(), Direction.NORTH);
		return r.build();
	}

	static TemplateBuilder corridor(ReachZoneKit z) {
		Ruin r = room("corridor", 5, 5, 9);
		r.set(0, 2, 4, GRATE).set(4, 2, 4, GRATE).set(0, 1, 4, BONE_BRICKS).set(4, 1, 4, BONE_BRICKS);
		r.fill(1, 3, 4, 3, 3, 4, stair(BRICK_STAIRS, Direction.NORTH, true));
		in(r, 2, 0, Direction.NORTH);
		out(r, 2, 8, Direction.SOUTH, POOL_HALLS);
		r.set(1, 1, 6, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST)).set(3, 1, 2, BLUE_CANDLE);
		r.mob(2, 1, 4, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** Burial hall: bone-filled niches in the walls behind grilles; coffins, bones and a Sentinel. */
	static TemplateBuilder burialHall(ReachZoneKit z) {
		Ruin r = room("burial_hall", 9, 6, 13);
		for (int zz = 2; zz <= 10; zz += 2) {
			r.set(1, 2, zz, BONES).set(7, 2, zz, BONES);
			r.set(1, 3, zz, SKULL).set(7, 3, zz, SKULL);
			r.set(1, 1, zz, BONE_BRICKS).set(7, 1, zz, BONE_BRICKS);
			r.set(1, 2, zz + 1, GRATE).set(7, 2, zz + 1, GRATE);
		}
		r.set(0, 2, 6, BLUE_CANDLE).set(1, 1, 6, BLUE_CANDLE);
		coffin(r, 2, 1, 9, Direction.SOUTH);
		coffin(r, 6, 1, 2, Direction.SOUTH);
		in(r, 4, 0, Direction.NORTH);
		out(r, 4, 12, Direction.SOUTH, POOL_HALLS);
		r.chest(7, 1, 6, Direction.WEST, z.commonChest());
		r.mob(3, 1, 4, z.fodder(), Direction.NORTH).mob(5, 1, 8, z.fodder(), Direction.NORTH);
		r.mob(4, 1, 10, z.tank(), Direction.NORTH);
		return r.build();
	}

	private static void coffin(Ruin r, int x, int y, int z, Direction facing) {
		BlockState foot = Props.COFFIN.getDefaultState().with(PropBlock.FACING, facing);
		r.set(x, y, z, foot.with(LongPropBlock.PART, BedPart.FOOT))
				.set(x + facing.getOffsetX(), y, z + facing.getOffsetZ(), foot.with(LongPropBlock.PART, BedPart.HEAD));
	}

	/** Flooded hall: a foot of water over the whole floor, a plate walkway down the middle; moths over it. */
	static TemplateBuilder floodedHall(ReachZoneKit z) {
		Ruin r = room("flooded_hall", 9, 6, 11);
		r.fill(1, 1, 1, 7, 1, 9, WATER);
		r.fill(4, 1, 1, 4, 1, 9, IRON_PLATE, slab(BRICK_SLAB, false), 0.4);
		r.fill(1, 0, 1, 7, 0, 9, CRYPT_TILES, WET, 0.3);
		r.set(1, 2, 3, BLUE_CANDLE).set(7, 2, 7, BLUE_CANDLE).set(0, 3, 5, GRATE).set(8, 3, 5, GRATE);
		in(r, 4, 0, Direction.NORTH);
		out(r, 4, 10, Direction.SOUTH, POOL_HALLS);
		r.mob(4, 1, 5, z.fodder(), Direction.NORTH);
		r.mob(2, 3, 3, z.ranged(), Direction.EAST).mob(6, 3, 7, z.ranged(), Direction.WEST);
		return r.build();
	}

	/** A chapel chamber with its Grave Warden and bell: a dead end. */
	static TemplateBuilder wardenChamber(ReachZoneKit z) {
		Ruin r = room("warden_chamber", 11, 7, 11);
		for (int[] c : new int[][]{{2, 2}, {8, 2}, {2, 8}, {8, 8}}) {
			pillar(r, c[0], c[1], 1, 5);
		}
		r.fill(3, 0, 7, 7, 0, 9, TILES).fill(4, 1, 8, 6, 1, 8, BONE_BRICKS);
		r.set(5, 1, 7, Blocks.BELL.getDefaultState().with(BellBlock.ATTACHMENT, Attachment.FLOOR).with(BellBlock.FACING, Direction.NORTH));
		r.set(4, 2, 8, BLUE_CANDLE.with(CandleBlock.CANDLES, 3)).set(6, 2, 8, BLUE_CANDLE.with(CandleBlock.CANDLES, 3))
				.set(5, 2, 8, Props.ASH_URNS.getDefaultState());
		r.set(5, 2, 9, SKULL).set(4, 1, 9, BONE_PILLAR).set(6, 1, 9, BONE_PILLAR);
		r.set(1, 1, 9, Props.BONE_PILE.getDefaultState().with(PropBlock.FACING, Direction.EAST));
		in(r, 5, 0, Direction.NORTH);
		r.chest(9, 1, 9, Direction.WEST, z.commonChest());
		r.mob(5, 1, 5, z.elite(), Direction.NORTH);
		r.mob(2, 1, 3, z.fodder(), Direction.NORTH).mob(8, 1, 3, z.fodder(), Direction.NORTH);
		return r.build();
	}

	/** A collapsed passage: rubble, no way on. Also the fallback that closes every door the crypt runs out of. */
	static TemplateBuilder deadEnd() {
		Ruin r = room("dead_end", 5, 5, 4);
		r.fill(1, 1, 2, 3, 3, 2, CRACKED, Blocks.GRAVEL.getDefaultState(), 0.4);
		r.set(2, 1, 1, Props.RUBBLE_PILE.getDefaultState().with(PropBlock.FACING, Direction.NORTH));
		r.set(1, 1, 1, Blocks.GRAVEL.getDefaultState()).set(3, 2, 1, slab(BRICK_SLAB, false));
		in(r, 2, 0, Direction.NORTH);
		return r.build();
	}

	/** Descent: stairs six blocks down to the lower crypt, candles on the landings. */
	static TemplateBuilder descent() {
		Ruin r = new Ruin("crypt_descent", 5, 11, 10);
		r.fill(0, 0, 0, 4, 10, 9, WET, CRACKED, 0.2);
		r.clear(1, 7, 0, 3, 9, 1);
		r.fill(1, 6, 0, 3, 6, 1, CRYPT_TILES);
		r.clear(1, 1, 2, 3, 9, 8);
		for (int k = 0; k < 6; k++) {
			int y = 5 - k;
			int zz = 2 + k;
			r.fill(1, y, zz, 3, y, zz, stair(BRICK_STAIRS, Direction.NORTH, false));
			if (y > 0) {
				r.fill(1, 0, zz, 3, y - 1, zz, WET);
			}
			r.set(0, y + 2, zz, k % 2 == 0 ? BONE_BRICKS : MOSSY).set(4, y + 1, zz, k % 3 == 1 ? BLUE_CANDLE : WET);
			r.set(4, y + 1, zz, k % 3 == 1 ? GRATE : WET);
		}
		r.fill(1, 0, 8, 3, 0, 8, CRYPT_TILES);
		r.clear(1, 1, 8, 3, 3, 8);
		r.set(1, 7, 1, BLUE_CANDLE).set(3, 1, 8, BLUE_CANDLE);
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

	/** Lower crypt: a flooded vault, sarcophagi between pillars along water channels, the Necropolis treasure on a dais. */
	static TemplateBuilder lowerCrypt(ReachZoneKit z) {
		Ruin r = room("lower_crypt", 13, 8, 15);
		r.fill(1, 0, 3, 1, 0, 13, WATER).fill(11, 0, 3, 11, 0, 13, WATER);
		r.fill(2, 0, 3, 2, 0, 13, IRON_PLATE, CRYPT_TILES, 0.5).fill(10, 0, 3, 10, 0, 13, IRON_PLATE, CRYPT_TILES, 0.5);
		for (int zz = 4; zz <= 12; zz += 4) {
			r.fill(3, 1, zz, 3, 1, zz + 1, BONE_BRICKS).fill(9, 1, zz, 9, 1, zz + 1, BONE_BRICKS);
			r.set(3, 2, zz, BLUE_CANDLE).set(9, 2, zz + 1, BLUE_CANDLE);
			coffin(r, 4, 1, zz, Direction.SOUTH);
			coffin(r, 8, 1, zz, Direction.SOUTH);
		}
		for (int[] c : new int[][]{{5, 4}, {7, 4}, {5, 10}, {7, 10}}) {
			pillar(r, c[0], c[1], 1, 6);
		}
		r.fill(5, 6, 5, 7, 6, 9, stair(BRICK_STAIRS, Direction.NORTH, true), stair(BRICK_STAIRS, Direction.SOUTH, true), 0.5);
		r.fill(4, 0, 12, 8, 0, 13, TILES).fill(5, 1, 12, 7, 1, 13, POLISHED);
		r.fill(5, 1, 11, 7, 1, 11, stair(ModBlocks.POLISHED_ASH_STONE_STAIRS.getDefaultState(), Direction.SOUTH, false));
		r.set(4, 1, 13, BONE_PILLAR).set(8, 1, 13, BONE_PILLAR).set(4, 2, 13, SKULL).set(8, 2, 13, SKULL);
		r.chest(6, 2, 13, Direction.NORTH, z.treasureChest());
		r.set(5, 2, 13, Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, 4).with(CandleBlock.LIT, true).with(PropCandleBlock.FACING, Direction.EAST))
				.set(7, 2, 13, BLUE_CANDLE.with(CandleBlock.CANDLES, 4));
		r.set(6, 2, 12, Props.ASH_URNS.getDefaultState().with(StackedPropBlock.COUNT, 2));
		in(r, 6, 0, Direction.NORTH);
		r.mob(6, 1, 11, z.elite(), Direction.NORTH);
		r.mob(4, 1, 3, z.tank(), Direction.NORTH).mob(8, 1, 3, z.tank(), Direction.NORTH);
		return r.build();
	}

	private CryptTemplates() {}
}
