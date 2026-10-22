package com.embervoid.sanctum.datagen;

import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.enums.JigsawOrientation;
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
