package com.embervoid.reach.world;

import com.embervoid.realm.EdgeLayout;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmLayout;
import com.embervoid.realm.Realms;
import com.embervoid.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.List;

/**
 * Feature {@code embervoid:realm_edge} (PLAN 9.9), once per chunk of a realm's open zones: the kingdom's border — a
 * broken wall on a ring {@value #WALL_INSET} blocks inside the realm radius, with towers, and {@value #BRIDGES}
 * bridges that run out from the cliffs over the abyss and break off ({@link EdgeLayout}).
 */
public class EdgeFeature extends Feature<DefaultFeatureConfig> {
	public static final int WALL_INSET = 95;
	/** Bridges start where the ground begins to fall towards the cliffs. */
	public static final int BRIDGE_INSET = 58;
	public static final int BRIDGES = 6;
	private static final int TOWER_WIDTH = 5;
	private static final int PILLAR_SPACING = 12;
	private static final int PILLAR_LENGTH = 14;

	public EdgeFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	public static double wallRadius(RealmDefinition realm) {
		return realm.radius() - WALL_INSET;
	}

	public static List<EdgeLayout.Bridge> bridges(ServerWorld world, RealmDefinition realm) {
		return EdgeLayout.bridges(world.getSeed(), BRIDGES, realm.radius() - BRIDGE_INSET);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		ServerWorld server = world.toServerWorld();
		RealmDefinition realm = Realms.byWorld(server.getRegistryKey());
		if (realm == null || !realm.world().equals(server.getRegistryKey())) {
			return false;
		}
		ChunkPos chunk = new ChunkPos(context.getOrigin());
		double centreDistance = Math.hypot(chunk.getCenterX(), chunk.getCenterZ());
		double wall = wallRadius(realm);
		if (centreDistance < wall - 24) {
			return false;
		}
		boolean changed = buildWall(world, chunk, wall);
		for (EdgeLayout.Bridge bridge : bridges(server, realm)) {
			changed |= buildBridge(context, server, chunk, bridge);
		}
		return changed;
	}

	private static boolean buildWall(StructureWorldAccess world, ChunkPos chunk, double radius) {
		long seed = world.getSeed();
		boolean changed = false;
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int x = chunk.getStartX(); x <= chunk.getEndX(); x++) {
			for (int z = chunk.getStartZ(); z <= chunk.getEndZ(); z++) {
				double off = Math.abs(Math.hypot(x + 0.5, z + 0.5) - radius);
				double angle = RealmLayout.angle(x + 0.5, z + 0.5);
				boolean tower = EdgeLayout.isTower(angle, radius, TOWER_WIDTH) && off <= 2.5;
				if (off > 1.0 && !tower) {
					continue;
				}
				int ground = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z) - 1;
				if (ground <= world.getBottomY() + 1) {
					continue;
				}
				int height = EdgeLayout.wallHeight(seed, angle, radius);
				if (tower) {
					height = Math.max(height, 3) + 4;
				}
				world.setBlockState(pos.set(x, ground, z), ModBlocks.COBBLED_ASH_STONE.getDefaultState(), 2);
				for (int y = 1; y <= height; y++) {
					world.setBlockState(pos.set(x, ground + y, z), masonry(x, ground + y, z), 2);
				}
				changed = true;
			}
		}
		return changed;
	}

	private static boolean buildBridge(FeatureContext<DefaultFeatureConfig> context, ServerWorld server, ChunkPos chunk,
			EdgeLayout.Bridge bridge) {
		StructureWorldAccess world = context.getWorld();
		boolean changed = false;
		Integer deck = null;
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int x = chunk.getStartX(); x <= chunk.getEndX(); x++) {
			for (int z = chunk.getStartZ(); z <= chunk.getEndZ(); z++) {
				double[] at = EdgeLayout.bridgeCoordinates(bridge, x + 0.5, z + 0.5);
				if (at == null || at[0] < bridge.from() || at[0] > bridge.to() || Math.abs(at[1]) > bridge.width() / 2.0) {
					continue;
				}
				if (deck == null) {
					deck = deckHeight(context, server, bridge);
				}
				long hash = MathHelper.hashCode(x, deck, z);
				// The far end crumbles: more holes over the last eight blocks.
				double broken = Math.max(0, (at[0] - (bridge.to() - 8)) / 8);
				if (Math.floorMod(hash, 100) < broken * 70) {
					continue;
				}
				boolean edge = Math.abs(at[1]) > 1.5;
				world.setBlockState(pos.set(x, deck, z), edge ? ModBlocks.COBBLED_ASH_STONE.getDefaultState() : masonry(x, deck, z), 2);
				for (int y = 1; y <= 3; y++) {
					world.setBlockState(pos.set(x, deck + y, z), Blocks.AIR.getDefaultState(), 2);
				}
				if (edge && Math.floorMod(hash >> 7, 4) != 0) {
					world.setBlockState(pos.set(x, deck + 1, z), ModBlocks.ASH_BRICK_WALL.getDefaultState(), 2);
				}
				double fromStart = at[0] - bridge.from();
				if (Math.abs(at[1]) <= 1.0 && fromStart > 4 && fromStart % PILLAR_SPACING < 2) {
					for (int y = 1; y <= PILLAR_LENGTH; y++) {
						pos.set(x, deck - y, z);
						if (!world.getBlockState(pos).isAir()) {
							break;
						}
						world.setBlockState(pos, masonry(x, deck - y, z), 2);
					}
				}
				changed = true;
			}
		}
		return changed;
	}

	/** The deck's height: the ground a little before the bridge starts, from the noise (the same for every chunk). */
	private static int deckHeight(FeatureContext<DefaultFeatureConfig> context, ServerWorld server, EdgeLayout.Bridge bridge) {
		double root = bridge.from() - 12;
		int x = (int) Math.floor(Math.cos(bridge.angle()) * root);
		int z = (int) Math.floor(Math.sin(bridge.angle()) * root);
		return context.getGenerator().getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, context.getWorld(),
				server.getChunkManager().getNoiseConfig());
	}

	private static BlockState masonry(int x, int y, int z) {
		return Math.floorMod(MathHelper.hashCode(x, y, z), 5) == 0 ? ModBlocks.CRACKED_ASH_BRICKS.getDefaultState()
				: ModBlocks.ASH_BRICKS.getDefaultState();
	}
}
