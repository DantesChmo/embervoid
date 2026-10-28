package com.embervoid.reach.world;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.List;

/**
 * Feature {@code embervoid:sinkhole_pool}, once per chunk: if the lowest ground of the chunk lies deep (a
 * sinkhole bottom), water fills the hollow around it {@value #DEPTH} blocks deep. Only a hollow closed on every side
 * within the chunk and its neighbours is filled, so water never pours over a rim. The chunk holding a hollow's lowest
 * column fills it (a neighbour with the same low point fills the same water). Placed in the Necropolis only: the only
 * water of the realm besides its crypts and arena.
 */
public class SinkholePoolFeature extends Feature<DefaultFeatureConfig> {
	/** Only ground below this height counts as a sinkhole bottom (the Necropolis basin floor is ~58). */
	public static final int MAX_BOTTOM = 50;
	public static final int DEPTH = 2;
	/** Largest pool, in columns. */
	private static final int MAX_COLUMNS = 900;

	public SinkholePoolFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		ChunkPos chunk = new ChunkPos(context.getOrigin());
		int minX = chunk.getStartX() - 16;
		int minZ = chunk.getStartZ() - 16;
		int size = 48;
		int[][] ground = new int[size][size];
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				ground[x][z] = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, minX + x, minZ + z) - 1;
			}
		}
		int lowX = -1;
		int lowZ = -1;
		for (int x = 16; x < 32; x++) {
			for (int z = 16; z < 32; z++) {
				if (lowX < 0 || ground[x][z] < ground[lowX][lowZ]) {
					lowX = x;
					lowZ = z;
				}
			}
		}
		int bottom = ground[lowX][lowZ];
		if (bottom >= MAX_BOTTOM || bottom <= world.getBottomY() + 1) {
			return false;
		}
		List<int[]> pool = Hollows.fill(ground, lowX, lowZ, bottom + DEPTH, MAX_COLUMNS);
		if (pool == null) {
			return false;
		}
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int[] column : pool) {
			for (int y = ground[column[0]][column[1]] + 1; y <= bottom + DEPTH; y++) {
				world.setBlockState(pos.set(minX + column[0], y, minZ + column[1]), Blocks.WATER.getDefaultState(), 2);
			}
		}
		return true;
	}
}
