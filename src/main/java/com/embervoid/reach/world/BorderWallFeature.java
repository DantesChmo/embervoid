package com.embervoid.reach.world;

import com.embervoid.realm.EdgeLayout;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.Realms;
import com.embervoid.realm.worldgen.RealmRoads;
import com.embervoid.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Feature {@code embervoid:realm_border_wall}, once per chunk near the final zone's borders: a burnt city
 * wall along both of them from just past the plaza to the abyss ({@link EdgeLayout#borderWallHeight}), three blocks
 * thick on the border line, with towers, crenellations, charred beams and ember-split stones, and gaps where it has
 * fallen. The gaps are no way in: the {@link com.embervoid.realm.FinalZoneLock} holds the region, not the blocks.
 */
public class BorderWallFeature extends Feature<DefaultFeatureConfig> {
	/** The wall starts this far from the landing centre (just past the plaza rim and the Citadel Gate). */
	public static final double FROM = RealmRoads.PLAZA_RADIUS + 2;
	/** Half the wall's thickness, and a tower's. */
	private static final double HALF = 1.5;
	private static final double TOWER_HALF = 3.5;
	/** How much taller than the wall a tower stands. */
	private static final int TOWER_RISE = 5;

	public BorderWallFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	/** The border angles of a realm's final zone: its sector's start, then its end. */
	public static double[] borders(RealmDefinition realm) {
		return ReachWorldRules.borders(realm.sectors());
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		ServerWorld server = world.toServerWorld();
		RealmDefinition realm = Realms.byWorld(server.getRegistryKey());
		if (realm == null || !realm.world().equals(server.getRegistryKey()) || !realm.sectors().hasFinal()) {
			return false;
		}
		ChunkPos chunk = new ChunkPos(context.getOrigin());
		double[] borders = borders(realm);
		boolean changed = false;
		for (int border = 0; border < borders.length; border++) {
			double[] centre = EdgeLayout.borderCoordinates(borders[border], chunk.getCenterX(), chunk.getCenterZ());
			// A chunk's columns lie within 11.4 blocks of its centre.
			if (Math.abs(centre[1]) > TOWER_HALF + 12 || centre[0] < FROM - 12 || centre[0] > realm.radius() + 12) {
				continue;
			}
			changed |= build(world, chunk, border, borders[border], realm.radius());
		}
		return changed;
	}

	private static boolean build(StructureWorldAccess world, ChunkPos chunk, int border, double angle, double radius) {
		long seed = world.getSeed();
		// Beams lie along the wall: on the axis nearer the border's direction.
		Direction.Axis along = Math.abs(Math.cos(angle)) > Math.abs(Math.sin(angle)) ? Direction.Axis.X : Direction.Axis.Z;
		boolean changed = false;
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int x = chunk.getStartX(); x <= chunk.getEndX(); x++) {
			for (int z = chunk.getStartZ(); z <= chunk.getEndZ(); z++) {
				double[] at = EdgeLayout.borderColumn(angle, x, z);
				if (at[0] < FROM || at[0] > radius) {
					continue;
				}
				boolean tower = EdgeLayout.isBorderTower(at[0]) && Math.abs(at[1]) <= TOWER_HALF;
				if (!tower && Math.abs(at[1]) > HALF) {
					continue;
				}
				int ground = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z) - 1;
				if (ground <= world.getBottomY() + 1) {
					continue; // over the abyss
				}
				int height = EdgeLayout.borderWallHeight(seed, border, at[0]);
				long hash = MathHelper.hashCode(x, border, z);
				world.setBlockState(pos.set(x, ground, z), ModBlocks.COBBLED_ASH_STONE.getDefaultState(), 2);
				if (tower) {
					height = Math.max(height, 4) + TOWER_RISE;
				} else if (height == 0) {
					// A gap: rubble where the wall fell.
					if (Math.floorMod(hash, 3) == 0) {
						world.setBlockState(pos.set(x, ground + 1, z), ModBlocks.COBBLED_ASH_STONE.getDefaultState(), 2);
					}
					changed = true;
					continue;
				}
				for (int y = 1; y <= height; y++) {
					world.setBlockState(pos.set(x, ground + y, z), masonry(x, ground + y, z), 2);
				}
				boolean rim = Math.abs(at[1]) > (tower ? TOWER_HALF - 1 : HALF - 1);
				if (rim && height >= 5 && Math.floorMod((long) Math.floor(at[0]), 2) == 0) {
					// Crenellations on the outer rows of a whole stretch or a tower.
					world.setBlockState(pos.set(x, ground + height + 1, z), ModBlocks.ASH_BRICK_WALL.getDefaultState(), 2);
				} else if (!rim && height >= 5 && Math.floorMod(hash >> 5, 7) == 0) {
					// A charred beam of the burnt walkway, still glowing.
					world.setBlockState(pos.set(x, ground + height + 1, z),
							ReachBlocks.SMOLDERING_LOG.getDefaultState().with(PillarBlock.AXIS, along), 2);
				}
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Whether the wall stands in the column at x, z: its brickwork within 16 below the top. For checks. Its ember
	 * cracks and charred beams do not count: the Burning City's ground has them too.
	 */
	public static boolean standsAt(World world, int x, int z) {
		int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int y = top; y > top - 16 && y > world.getBottomY(); y--) {
			BlockState state = world.getBlockState(pos.set(x, y, z));
			if (state.isOf(ModBlocks.ASH_BRICKS) || state.isOf(ModBlocks.CRACKED_ASH_BRICKS) || state.isOf(ModBlocks.ASH_BRICK_WALL)) {
				return true;
			}
		}
		return false;
	}

	private static BlockState masonry(int x, int y, int z) {
		int roll = Math.floorMod(MathHelper.hashCode(x, y, z), 40);
		if (roll == 0) {
			return ReachBlocks.EMBER_CRACK.getDefaultState();
		}
		return roll < 12 ? ModBlocks.CRACKED_ASH_BRICKS.getDefaultState() : ModBlocks.ASH_BRICKS.getDefaultState();
	}
}
