package com.embervoid.sanctum.datagen;

import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.JigsawOrientation;
import net.minecraft.block.enums.SlabType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Builds a structure template ({@code .nbt}, the format structure blocks save) in code. Positions not set are
 * left out of the template, so the world keeps whatever is there. Also used for the boss arenas (track reach-bosses).
 */
public final class TemplateBuilder {
	private record Entry(BlockState state, @Nullable NbtCompound nbt) {}

	public final int sizeX;
	public final int sizeY;
	public final int sizeZ;
	private final Map<BlockPos, Entry> blocks = new LinkedHashMap<>();
	private final NbtList entities = new NbtList();
	/** Added to every y given to the methods below, so a piece can be drawn on top of a foundation. */
	private int baseY;

	public TemplateBuilder(int sizeX, int sizeY, int sizeZ) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
	}

	public TemplateBuilder set(int x, int y, int z, BlockState state) {
		return set(x, y, z, state, null);
	}

	/** From now on, y = 0 means {@code baseY} in the template. */
	public TemplateBuilder baseY(int baseY) {
		this.baseY = baseY;
		return this;
	}

	public TemplateBuilder set(int x, int y, int z, BlockState state, @Nullable NbtCompound nbt) {
		y += baseY;
		if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
			throw new IllegalArgumentException("Outside the template: " + x + ", " + y + ", " + z);
		}
		blocks.put(new BlockPos(x, y, z), new Entry(state, nbt));
		return this;
	}

	/** Like {@link #set}, but a position outside the template is skipped: for mouldings that may run off the edge. */
	public TemplateBuilder trySet(int x, int y, int z, BlockState state) {
		if (inside(x, y, z)) {
			set(x, y, z, state);
		}
		return this;
	}

	public boolean inside(int x, int y, int z) {
		int yy = y + baseY;
		return x >= 0 && yy >= 0 && z >= 0 && x < sizeX && yy < sizeY && z < sizeZ;
	}

	/** Whether something was set at the position (air counts; a position never touched is left to the world). */
	public boolean isSet(int x, int y, int z) {
		return blocks.containsKey(new BlockPos(x, y + baseY, z));
	}

	public boolean isAir(int x, int y, int z) {
		Entry entry = blocks.get(new BlockPos(x, y + baseY, z));
		return entry != null && entry.state.isAir();
	}

	public BlockState get(int x, int y, int z) {
		Entry entry = blocks.get(new BlockPos(x, y + baseY, z));
		return entry != null ? entry.state : Blocks.STRUCTURE_VOID.getDefaultState();
	}

	/** Fills the inclusive box. */
	public TemplateBuilder fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					set(x, y, z, state);
				}
			}
		}
		return this;
	}

	/** Fills the inclusive box, each block replaced by {@code alt} with the chance given. */
	public TemplateBuilder fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state, BlockState alt, double altChance, Random random) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					set(x, y, z, random.nextDouble() < altChance ? alt : state);
				}
			}
		}
		return this;
	}

	/**
	 * A closed room filling the whole template: floor, ceiling and walls of the given blocks, air inside.
	 */
	public TemplateBuilder room(BlockState floor, BlockState walls, BlockState ceiling) {
		fill(0, 0, 0, sizeX - 1, sizeY - 1, sizeZ - 1, walls);
		fill(1, 1, 1, sizeX - 2, sizeY - 2, sizeZ - 2, Blocks.AIR.getDefaultState());
		fill(1, 0, 1, sizeX - 2, 0, sizeZ - 2, floor);
		fill(1, sizeY - 1, 1, sizeX - 2, sizeY - 1, sizeZ - 2, ceiling);
		return this;
	}

	// --- Masonry (the 0.5.0 art pass, docs/art-direction.md §3) --------------------------------------------------
	// Mouldings, roofs, arches and floors every template builder shares. States are passed in, so a Sanctum wall and
	// a Citadel wall use the same shapes in their own materials. Positions outside the template are skipped.

	/** A stair block: {@code facing} is the side its full-height half stands against; {@code top} hangs it. */
	public static BlockState stair(BlockState stairs, Direction facing, boolean top) {
		return stairs.with(StairsBlock.FACING, facing).with(StairsBlock.HALF, top ? BlockHalf.TOP : BlockHalf.BOTTOM);
	}

	public static BlockState slab(BlockState slab, boolean top) {
		return slab.with(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	/**
	 * A cornice: hung stairs on the ring just outside the box x0..x1, z0..z1 at height y, their tall half against the
	 * wall, so the moulding projects outwards. Corners get the block given (null: nothing).
	 */
	public TemplateBuilder cornice(int x0, int z0, int x1, int z1, int y, BlockState stairs, @Nullable BlockState corner) {
		for (int x = x0; x <= x1; x++) {
			trySet(x, y, z0 - 1, stair(stairs, Direction.SOUTH, true));
			trySet(x, y, z1 + 1, stair(stairs, Direction.NORTH, true));
		}
		for (int z = z0; z <= z1; z++) {
			trySet(x0 - 1, y, z, stair(stairs, Direction.EAST, true));
			trySet(x1 + 1, y, z, stair(stairs, Direction.WEST, true));
		}
		if (corner != null) {
			trySet(x0 - 1, y, z0 - 1, corner).trySet(x1 + 1, y, z0 - 1, corner)
					.trySet(x0 - 1, y, z1 + 1, corner).trySet(x1 + 1, y, z1 + 1, corner);
		}
		return this;
	}

	/** A cornice inside a room: hung stairs along the inner face of the walls of the box at height y. */
	public TemplateBuilder innerCornice(int x0, int z0, int x1, int z1, int y, BlockState stairs) {
		for (int x = x0 + 1; x <= x1 - 1; x++) {
			trySet(x, y, z0 + 1, stair(stairs, Direction.NORTH, true));
			trySet(x, y, z1 - 1, stair(stairs, Direction.SOUTH, true));
		}
		for (int z = z0 + 1; z <= z1 - 1; z++) {
			trySet(x0 + 1, y, z, stair(stairs, Direction.WEST, true));
			trySet(x1 - 1, y, z, stair(stairs, Direction.EAST, true));
		}
		return this;
	}

	/** A string course: a ring of top slabs just outside the box at height y (a sill line or a plinth edge). */
	public TemplateBuilder stringCourse(int x0, int z0, int x1, int z1, int y, BlockState slab) {
		BlockState top = slab(slab, true);
		for (int x = x0 - 1; x <= x1 + 1; x++) {
			trySet(x, y, z0 - 1, top).trySet(x, y, z1 + 1, top);
		}
		for (int z = z0; z <= z1; z++) {
			trySet(x0 - 1, y, z, top).trySet(x1 + 1, y, z, top);
		}
		return this;
	}

	/**
	 * A pitched roof over the box x0..x1, z0..z1 with its ridge along {@code axis}, eaves at height y: courses of
	 * stairs climbing towards the ridge from both sides, the ridge a slab (odd span) or two stairs back to back (even).
	 * {@code coverage} below 1 burns the roof away from the ridge down, in runs along the ridge (so what is left
	 * stands on the eaves and nothing floats): at 0.5 about half the courses are gone on average.
	 */
	public TemplateBuilder pitchedRoof(int x0, int z0, int x1, int z1, int y, Direction.Axis axis, BlockState stairs, BlockState slab,
			Random random, double coverage) {
		int lo = axis == Direction.Axis.X ? z0 : x0;
		int hi = axis == Direction.Axis.X ? z1 : x1;
		int a0 = axis == Direction.Axis.X ? x0 : z0;
		int a1 = axis == Direction.Axis.X ? x1 : z1;
		int courses = ridgeHeight(hi - lo + 1) + 1;
		int[][] standing = new int[2][a1 - a0 + 1];
		for (int side = 0; side < 2; side++) {
			double run = random.nextDouble();
			for (int i = 0; i <= a1 - a0; i++) {
				run = 0.65 * run + 0.35 * random.nextDouble();
				double c = coverage >= 1 ? 1 : Math.max(0, Math.min(1, coverage + (run - 0.5) * 0.9));
				standing[side][i] = (int) Math.round(courses * c);
			}
		}
		// A roof column stands only where its eave has a wall under it: over a gap the whole column is gone.
		for (int i = 0; i <= a1 - a0; i++) {
			for (int side = 0; side < 2; side++) {
				int across = side == 0 ? lo : hi;
				int bx = axis == Direction.Axis.X ? a0 + i : across;
				int bz = axis == Direction.Axis.X ? across : a0 + i;
				if (!(inside(bx, y - 1, bz) && isSet(bx, y - 1, bz) && !isAir(bx, y - 1, bz))) {
					standing[side][i] = 0;
				}
			}
		}
		Direction towardsRidge = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		for (int k = 0; lo + k <= hi - k; k++) {
			int near = lo + k;
			int far = hi - k;
			for (int a = a0; a <= a1; a++) {
				boolean keepNear = k < standing[0][a - a0];
				boolean keepFar = k < standing[1][a - a0];
				if (near == far) {
					putRoof(axis, a, y + k, near, keepNear && keepFar ? slab(slab, false) : Blocks.AIR.getDefaultState());
				} else {
					putRoof(axis, a, y + k, near, keepNear ? stair(stairs, towardsRidge, false) : Blocks.AIR.getDefaultState());
					putRoof(axis, a, y + k, far, keepFar ? stair(stairs, towardsRidge.getOpposite(), false) : Blocks.AIR.getDefaultState());
				}
			}
		}
		return this;
	}

	private void putRoof(Direction.Axis axis, int along, int y, int across, BlockState state) {
		if (axis == Direction.Axis.X) {
			trySet(along, y, across, state);
		} else {
			trySet(across, y, along, state);
		}
	}

	/** The height of a pitched roof's ridge above its eaves for a span across. */
	public static int ridgeHeight(int span) {
		return (span - 1) / 2;
	}

	/**
	 * A gable: the triangle of wall under a pitched roof at the end {@code along} (an x for a ridge along x), filled
	 * with the state from the eaves at y up to the ridge.
	 */
	public TemplateBuilder gable(int along, int lo, int hi, int y, Direction.Axis axis, BlockState state, Random random, double decay) {
		for (int k = 0; lo + k <= hi - k; k++) {
			for (int c = lo + k + 1; c <= hi - k - 1; c++) {
				if (random.nextDouble() >= decay * k / Math.max(1, ridgeHeight(hi - lo + 1))) {
					putRoof(axis, along, y + k, c, state);
				}
			}
		}
		return this;
	}

	/**
	 * An arched opening {@code width} wide (odd) and {@code height} high in a wall running along {@code axis}, centred
	 * on (x, z) at floor y: the opening cleared, hung stairs at the top corners as the arch's shoulders, a keystone
	 * above the middle (null: none), jambs of {@code jamb} either side (null: none).
	 */
	public TemplateBuilder arch(int x, int y, int z, Direction.Axis axis, int width, int height, BlockState stairs,
			@Nullable BlockState keystone, @Nullable BlockState jamb) {
		int half = width / 2;
		for (int d = -half; d <= half; d++) {
			int ax = axis == Direction.Axis.X ? x + d : x;
			int az = axis == Direction.Axis.X ? z : z + d;
			for (int yy = y; yy < y + height; yy++) {
				trySet(ax, yy, az, Blocks.AIR.getDefaultState());
			}
			if (d == -half || d == half) {
				Direction inwards = axis == Direction.Axis.X ? (d < 0 ? Direction.WEST : Direction.EAST) : (d < 0 ? Direction.NORTH : Direction.SOUTH);
				trySet(ax, y + height - 1, az, stair(stairs, inwards, true));
				if (jamb != null) {
					int jx = axis == Direction.Axis.X ? x + d + (d < 0 ? -1 : 1) : x;
					int jz = axis == Direction.Axis.X ? z : z + d + (d < 0 ? -1 : 1);
					for (int yy = y; yy < y + height; yy++) {
						trySet(jx, yy, jz, jamb);
					}
				}
			}
		}
		if (keystone != null) {
			trySet(x, y + height, z, keystone);
		}
		return this;
	}

	/**
	 * A buttress against a wall: a pier of {@code pier} standing one block out from the wall at (x, z) on the side
	 * {@code out}, {@code height} high from y, with a sloping stair cap leaning onto the wall.
	 */
	public TemplateBuilder buttress(int x, int y, int z, Direction out, int height, BlockState pier, BlockState stairs) {
		int bx = x + out.getOffsetX();
		int bz = z + out.getOffsetZ();
		for (int yy = y; yy < y + height; yy++) {
			trySet(bx, yy, bz, pier);
		}
		trySet(bx, y + height, bz, stair(stairs, out.getOpposite(), false));
		return this;
	}

	/** A window grille: the opening w wide and h high in a wall along {@code axis}, filled with {@code pane}. */
	public TemplateBuilder grille(int x, int y, int z, Direction.Axis axis, int w, int h, BlockState pane) {
		for (int d = 0; d < w; d++) {
			for (int yy = y; yy < y + h; yy++) {
				trySet(axis == Direction.Axis.X ? x + d : x, yy, axis == Direction.Axis.X ? z : z + d, pane);
			}
		}
		return this;
	}

	/** A floor over the box at y blended from weighted states: {@code entries} alternate state, weight. */
	public TemplateBuilder blendedFloor(int x0, int z0, int x1, int z1, int y, Random random, Object... entries) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
				trySet(x, y, z, pick(random, entries));
			}
		}
		return this;
	}

	/** One of the weighted states: {@code entries} alternate BlockState, Number. */
	public static BlockState pick(Random random, Object... entries) {
		double total = 0;
		for (int i = 1; i < entries.length; i += 2) {
			total += ((Number) entries[i]).doubleValue();
		}
		double roll = random.nextDouble() * total;
		for (int i = 0; i < entries.length; i += 2) {
			roll -= ((Number) entries[i + 1]).doubleValue();
			if (roll < 0) {
				return (BlockState) entries[i];
			}
		}
		return (BlockState) entries[entries.length - 2];
	}

	/**
	 * A wall block for a course: a base course of {@code base} at the lowest {@code baseRows} rows, then the main
	 * state, with the cracked variant growing with height and towards the ends of the wall and the mossy variant at
	 * the foot. {@code t} is the height fraction 0 (bottom) to 1 (top), {@code edge} 0 (middle) to 1 (a corner).
	 */
	public static BlockState weathered(Random random, double t, double edge, BlockState base, int baseRow, int row,
			BlockState main, @Nullable BlockState cracked, @Nullable BlockState mossy) {
		if (row < baseRow) {
			return random.nextDouble() < 0.15 && mossy != null ? mossy : base;
		}
		double roll = random.nextDouble();
		if (cracked != null && roll < 0.08 + 0.3 * t + 0.2 * edge) {
			return cracked;
		}
		if (mossy != null && roll > 0.92 - 0.25 * (1 - t)) {
			return mossy;
		}
		return main;
	}

	/**
	 * Walls round the box from y0 to y1 with the weathering above, broken towards the top with chance {@code decay}
	 * (more at the corners), rubble ({@code rubble}, may be null) dropped on the ground just outside where the top
	 * of the wall is gone. Returns this.
	 */
	public TemplateBuilder weatheredWalls(int x0, int z0, int x1, int z1, int y0, int y1, Random random, double decay,
			BlockState base, int baseRows, BlockState main, @Nullable BlockState cracked, @Nullable BlockState mossy,
			@Nullable BlockState rubble) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (x != x0 && x != x1 && z != z0 && z != z1) {
					continue;
				}
				double edge = Math.max(1 - Math.min(x - x0, x1 - x) / 3.0, 1 - Math.min(z - z0, z1 - z) / 3.0);
				edge = Math.max(0, Math.min(1, edge));
				int broken = 0;
				for (int y = y0; y <= y1; y++) {
					double t = y1 == y0 ? 1 : (double) (y - y0) / (y1 - y0);
					double missing = decay * t * t * (0.6 + 0.6 * edge);
					if (random.nextDouble() < missing) {
						set(x, y, z, Blocks.AIR.getDefaultState());
						broken++;
					} else if (broken > 0) {
						// Nothing stands on a gap: the courses above a hole are gone too.
						set(x, y, z, Blocks.AIR.getDefaultState());
					} else {
						set(x, y, z, weathered(random, t, edge, base, y0 + baseRows, y, main, cracked, mossy));
					}
				}
				if (rubble != null && broken > 1) {
					int ox = x == x0 ? x - 1 : x == x1 ? x + 1 : x;
					int oz = z == z0 ? z - 1 : z == z1 ? z + 1 : z;
					if (random.nextDouble() < 0.6 && inside(ox, y0, oz) && !isSet(ox, y0, oz)) {
						set(ox, y0, oz, rubble);
					}
				}
			}
		}
		return this;
	}

	/** Soot over a fire or a doorway: the blocks at y above (x, z) up {@code height} replaced with {@code sooty}. */
	public TemplateBuilder soot(int x, int y, int z, int height, BlockState sooty, Random random) {
		for (int yy = y; yy < y + height; yy++) {
			if (inside(x, yy, z) && isSet(x, yy, z) && !isAir(x, yy, z) && random.nextDouble() < 0.8) {
				set(x, yy, z, sooty);
			}
		}
		return this;
	}

	/**
	 * Jigsaw block. {@code name} is what other pieces target; {@code pool}/{@code target} say what to attach here
	 * (may be null for a receiving-only connector). After generation the block becomes {@code finalState}.
	 */
	public TemplateBuilder jigsaw(int x, int y, int z, JigsawOrientation orientation, Identifier name,
			@Nullable Identifier pool, @Nullable Identifier target, BlockState finalState) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", "minecraft:jigsaw");
		nbt.putString("name", name.toString());
		nbt.putString("target", target != null ? target.toString() : "minecraft:empty");
		nbt.putString("pool", pool != null ? pool.toString() : "minecraft:empty");
		nbt.putString("joint", orientation.getFacing().getAxis().isVertical() ? "rollable" : "aligned");
		nbt.putString("final_state", stateString(finalState));
		return set(x, y, z, Blocks.JIGSAW.getDefaultState().with(JigsawBlock.ORIENTATION, orientation), nbt);
	}

	/** A horizontal doorway jigsaw on a wall, facing out of the piece; becomes air. */
	public TemplateBuilder door(int x, int y, int z, Direction facing, Identifier name, @Nullable Identifier pool, @Nullable Identifier target) {
		return jigsaw(x, y, z, JigsawOrientation.byDirections(facing, Direction.UP), name, pool, target,
				Blocks.AIR.getDefaultState());
	}

	public TemplateBuilder chest(int x, int y, int z, Direction facing, Identifier lootTable) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", "minecraft:chest");
		nbt.putString("LootTable", lootTable.toString());
		return set(x, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing), nbt);
	}

	/** A block entity block with extra NBT (the id is added). */
	public TemplateBuilder blockEntity(int x, int y, int z, BlockState state, String blockEntityId, NbtCompound data) {
		NbtCompound nbt = data.copy();
		nbt.putString("id", blockEntityId);
		return set(x, y, z, state, nbt);
	}

	/** An entity standing on the block at (x, y, z); {@code nbt} must contain its "id". */
	public TemplateBuilder entity(int x, int y, int z, NbtCompound nbt) {
		NbtCompound entry = new NbtCompound();
		NbtList pos = new NbtList();
		pos.add(NbtDouble.of(x + 0.5));
		pos.add(NbtDouble.of(y + baseY));
		pos.add(NbtDouble.of(z + 0.5));
		entry.put("pos", pos);
		entry.put("blockPos", intList(x, y + baseY, z));
		entry.put("nbt", nbt);
		entities.add(entry);
		return this;
	}

	public NbtCompound toNbt() {
		NbtCompound root = new NbtCompound();
		Map<BlockState, Integer> paletteIndex = new HashMap<>();
		NbtList palette = new NbtList();
		NbtList blockList = new NbtList();
		List<Map.Entry<BlockPos, Entry>> sorted = new ArrayList<>(blocks.entrySet());
		// Same order as vanilla saves: by y, then z, then x. Keeps the output stable.
		sorted.sort((a, b) -> {
			BlockPos p = a.getKey();
			BlockPos q = b.getKey();
			if (p.getY() != q.getY()) return Integer.compare(p.getY(), q.getY());
			if (p.getZ() != q.getZ()) return Integer.compare(p.getZ(), q.getZ());
			return Integer.compare(p.getX(), q.getX());
		});
		for (Map.Entry<BlockPos, Entry> e : sorted) {
			BlockState state = e.getValue().state;
			int index = paletteIndex.computeIfAbsent(state, s -> {
				palette.add(NbtHelper.fromBlockState(s));
				return palette.size() - 1;
			});
			NbtCompound block = new NbtCompound();
			block.put("pos", intList(e.getKey().getX(), e.getKey().getY(), e.getKey().getZ()));
			block.putInt("state", index);
			if (e.getValue().nbt != null) {
				block.put("nbt", e.getValue().nbt);
			}
			blockList.add(block);
		}
		root.put("size", intList(sizeX, sizeY, sizeZ));
		root.put("palette", palette);
		root.put("blocks", blockList);
		root.put("entities", entities);
		root.putInt("DataVersion", SharedConstants.getGameVersion().getSaveVersion().getId());
		return root;
	}

	/** "namespace:block[prop=value,...]", the format jigsaw final states use. */
	public static String stateString(BlockState state) {
		String s = state.toString();
		// BlockState.toString is "Block{namespace:id}[props]".
		int open = s.indexOf('{');
		int close = s.indexOf('}');
		return s.substring(open + 1, close) + s.substring(close + 1);
	}

	private static NbtList intList(int... values) {
		NbtList list = new NbtList();
		for (int v : values) {
			list.add(NbtInt.of(v));
		}
		return list;
	}
}
