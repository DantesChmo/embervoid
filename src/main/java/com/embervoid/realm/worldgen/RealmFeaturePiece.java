package com.embervoid.realm.worldgen;

import com.embervoid.realm.RealmBlocks;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.SanctumBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Placeholder landing platform / boss arena, built from code. Replaced by templates once they are built in game;
 * the positions it exposes ({@link #center}, {@link #shrinePos}) stay the contract for the rest of the realm code.
 */
public class RealmFeaturePiece extends StructurePiece {
	public static final int LANDING_RADIUS = 9;
	/** Air cleared above the floor. */
	private static final int HEADROOM = 14;
	/** Deepest a floor column is extended down to reach the ground. */
	private static final int MAX_FOUNDATION = 24;
	/** How far outside the fog wall an arena's shrine stands. */
	private static final int SHRINE_DISTANCE = 4;

	private final RealmFeatureStructure.Kind kind;
	private final BlockPos center;
	private final int radius;

	public RealmFeaturePiece(RealmFeatureStructure.Kind kind, BlockPos center, int radius) {
		super(RealmWorldgen.FEATURE_PIECE, 0, box(center, radius));
		this.kind = kind;
		this.center = center;
		this.radius = radius;
	}

	public RealmFeaturePiece(NbtCompound nbt) {
		super(RealmWorldgen.FEATURE_PIECE, nbt);
		this.kind = RealmFeatureStructure.Kind.valueOf(nbt.getString("Kind"));
		this.center = BlockPos.fromLong(nbt.getLong("Center"));
		this.radius = nbt.getInt("Radius");
	}

	private static BlockBox box(BlockPos center, int radius) {
		int r = radius + SHRINE_DISTANCE + 3;
		return new BlockBox(center.getX() - r, center.getY() - 1, center.getZ() - r,
				center.getX() + r, center.getY() + HEADROOM, center.getZ() + r);
	}

	@Override
	protected void writeNbt(StructureContext context, NbtCompound nbt) {
		nbt.putString("Kind", kind.name());
		nbt.putLong("Center", center.asLong());
		nbt.putInt("Radius", radius);
	}

	public RealmFeatureStructure.Kind kind() {
		return kind;
	}

	/** Floor center (the floor block itself). */
	public BlockPos center() {
		return center;
	}

	public int radius() {
		return radius;
	}

	/** Where the feature's Ember Shrine stands: the landing's center, or outside an arena towards the landing. */
	public BlockPos shrinePos() {
		if (kind == RealmFeatureStructure.Kind.LANDING) {
			return center.up();
		}
		double length = Math.sqrt((double) center.getX() * center.getX() + (double) center.getZ() * center.getZ());
		double dx = length < 1 ? 1 : -center.getX() / length;
		double dz = length < 1 ? 0 : -center.getZ() / length;
		int d = radius + SHRINE_DISTANCE;
		return center.add((int) Math.round(dx * d), 1, (int) Math.round(dz * d));
	}

	@Override
	public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
			Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
		if (kind == RealmFeatureStructure.Kind.LANDING) {
			generateLanding(world, chunkBox);
		} else {
			generateArena(world, chunkBox);
		}
	}

	private void generateLanding(StructureWorldAccess world, BlockBox chunkBox) {
		disc(world, chunkBox, center, radius, ModBlocks.POLISHED_ASH_STONE.getDefaultState(), ModBlocks.ASH_BRICKS.getDefaultState());
		put(world, chunkBox, shrinePos(), SanctumBlocks.EMBER_SHRINE.getDefaultState());

		// Return obelisk to the north, Sealed Gate to the south, lanterns on the diagonals.
		BlockPos obelisk = center.add(0, 1, -6);
		put(world, chunkBox, obelisk, RealmBlocks.RETURN_OBELISK.getDefaultState());
		put(world, chunkBox, obelisk.up(), RealmBlocks.RETURN_OBELISK.getDefaultState());
		for (int dy = 1; dy <= 3; dy++) {
			put(world, chunkBox, center.add(0, dy, 6), RealmBlocks.SEALED_GATE.getDefaultState());
			put(world, chunkBox, center.add(-1, dy, 6), ModBlocks.ASH_BRICKS.getDefaultState());
			put(world, chunkBox, center.add(1, dy, 6), ModBlocks.ASH_BRICKS.getDefaultState());
		}
		put(world, chunkBox, center.add(-1, 4, 6), ModBlocks.ASH_BRICK_SLAB.getDefaultState());
		put(world, chunkBox, center.add(0, 4, 6), ModBlocks.ASH_BRICK_SLAB.getDefaultState());
		put(world, chunkBox, center.add(1, 4, 6), ModBlocks.ASH_BRICK_SLAB.getDefaultState());
		for (int[] d : new int[][]{{5, 5}, {-5, 5}, {5, -5}, {-5, -5}}) {
			put(world, chunkBox, center.add(d[0], 1, d[1]), ModBlocks.ASH_BRICK_WALL.getDefaultState());
			put(world, chunkBox, center.add(d[0], 2, d[1]), ModBlocks.EMBER_LANTERN.getDefaultState());
		}
	}

	private void generateArena(StructureWorldAccess world, BlockBox chunkBox) {
		disc(world, chunkBox, center, radius, ModBlocks.POLISHED_ASH_STONE.getDefaultState(), ModBlocks.ASH_BRICKS.getDefaultState());
		put(world, chunkBox, center, ModBlocks.CHISELED_ASH_STONE.getDefaultState());
		// Six pillars just inside the fog wall.
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			BlockPos base = center.add((int) Math.round(Math.cos(a) * (radius - 3)), 0, (int) Math.round(Math.sin(a) * (radius - 3)));
			for (int dy = 1; dy <= 5; dy++) {
				put(world, chunkBox, base.up(dy), (dy == 5 ? ModBlocks.CHISELED_ASH_STONE : ModBlocks.ASH_BRICKS).getDefaultState());
			}
		}

		// Path out to the shrine and a small platform under it. The shrine is dormant until the boss is beaten (§10.8).
		BlockPos shrine = shrinePos();
		int steps = radius + SHRINE_DISTANCE;
		for (int s = radius - 1; s <= steps; s++) {
			double t = (double) s / steps;
			BlockPos step = new BlockPos((int) Math.round(center.getX() + (shrine.getX() - center.getX()) * t), center.getY(),
					(int) Math.round(center.getZ() + (shrine.getZ() - center.getZ()) * t));
			disc(world, chunkBox, step, 1, ModBlocks.POLISHED_ASH_STONE.getDefaultState(), ModBlocks.POLISHED_ASH_STONE.getDefaultState());
		}
		disc(world, chunkBox, shrine.down(), 2, ModBlocks.POLISHED_ASH_STONE.getDefaultState(), ModBlocks.ASH_BRICKS.getDefaultState());
		put(world, chunkBox, shrine, SanctumBlocks.EMBER_SHRINE.getDefaultState());
	}

	/** Floor disc with a rim, a foundation down to the ground and cleared air above. */
	private static void disc(StructureWorldAccess world, BlockBox chunkBox, BlockPos at, int r, BlockState floor, BlockState rim) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > r + 0.5) {
					continue;
				}
				int x = at.getX() + dx;
				int z = at.getZ() + dz;
				if (!chunkBox.contains(x, at.getY(), z)) {
					continue;
				}
				world.setBlockState(pos.set(x, at.getY(), z), d > r - 0.5 ? rim : floor, 2);
				for (int y = at.getY() + 1; y <= at.getY() + HEADROOM; y++) {
					world.setBlockState(pos.set(x, y, z), Blocks.AIR.getDefaultState(), 2);
				}
				for (int y = at.getY() - 1; y >= at.getY() - MAX_FOUNDATION && y > world.getBottomY(); y--) {
					pos.set(x, y, z);
					if (!world.getBlockState(pos).isAir() && world.getFluidState(pos).isEmpty()) {
						break;
					}
					world.setBlockState(pos, ModBlocks.ASH_STONE.getDefaultState(), 2);
				}
			}
		}
	}

	private static void put(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
		if (chunkBox.contains(pos)) {
			world.setBlockState(pos, state, 2);
		}
	}
}
