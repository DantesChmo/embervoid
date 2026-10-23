package com.embervoid.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.RoadLayout;
import com.embervoid.realm.placed.PlacedEnemyProcessor;
import com.embervoid.realm.worldgen.RealmRoads;
import com.embervoid.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feature {@code embervoid:realm_roads} (PLAN 9.6, 14.2), run once per chunk: paints the part of each road
 * ({@link RealmRoads}) that crosses the chunk (Kingsroad flagstones in the middle, a solid pale kerb at the edges, so
 * it stands out from every zone's ground), sets the milestones and lantern posts and places the outposts whose centre
 * lies in the chunk. An outpost template is centred on the road with
 * its y = 0 layer on the ground, its +Z side turned along the road towards the arena; the road is not painted under it
 * (the template paves its own stretch).
 */
public class RoadFeature extends Feature<DefaultFeatureConfig> {
	/** Deepest an outpost's footprint is filled down to the ground. */
	private static final int FOUNDATION = 16;
	private static final Set<Identifier> MISSING = ConcurrentHashMap.newKeySet();

	public RoadFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		ServerWorld server = world.toServerWorld();
		List<RoadLayout.Road> roads = RealmRoads.painted(server);
		if (roads.isEmpty()) {
			return false;
		}
		List<RealmArenas.Location> arenas = RealmRoads.arenas(server);
		ChunkPos chunk = new ChunkPos(context.getOrigin());
		int minX = chunk.getStartX();
		int minZ = chunk.getStartZ();
		boolean changed = false;
		List<Placed> outposts = new ArrayList<>();
		for (RoadLayout.Road road : roads) {
			for (RoadLayout.Outpost outpost : road.outposts()) {
				StructureTemplate template = template(server, outpostId(outpost, arenas.get(road.arena())));
				if (template != null) {
					outposts.add(new Placed(outpost, template));
				}
			}
		}

		for (RoadLayout.Road road : roads) {
			List<double[][]> near = segmentsNear(road, minX, minZ);
			if (near.isEmpty()) {
				continue;
			}
			BlockPos.Mutable pos = new BlockPos.Mutable();
			for (int x = minX; x < minX + 16; x++) {
				for (int z = minZ; z < minZ + 16; z++) {
					double d = Double.MAX_VALUE;
					for (double[][] segment : near) {
						d = Math.min(d, RoadLayout.segmentDistanceSquared(segment[0], segment[1], x + 0.5, z + 0.5));
					}
					if (d > RealmRoads.HALF_WIDTH * RealmRoads.HALF_WIDTH || underOutpost(outposts, x, z)
							|| road.arena() == RoadLayout.GATE && Math.hypot(x + 0.5, z + 0.5) < RealmRoads.PLAZA_RADIUS + 0.5) {
						// The gate road starts behind the Citadel Gate on the plaza rim (PLAN 21.2).
						continue;
					}
					int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z) - 1;
					if (y <= world.getBottomY()) {
						continue;
					}
					world.setBlockState(pos.set(x, y, z), paving(Math.sqrt(d)), 2);
					changed = true;
					for (int up = 1; up <= 2; up++) {
						if (!world.getBlockState(pos.set(x, y + up, z)).isAir()) {
							world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
						}
					}
				}
			}
		}

		for (RoadLayout.Road road : roads) {
			for (RoadLayout.Milestone milestone : road.milestones()) {
				if ((milestone.x() >> 4) == chunk.x && (milestone.z() >> 4) == chunk.z) {
					int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, milestone.x(), milestone.z());
					Direction facing = Direction.getFacing(milestone.faceX(), 0, milestone.faceZ());
					world.setBlockState(new BlockPos(milestone.x(), y, milestone.z()),
							ReachBlocks.MILESTONE.getDefaultState().with(HorizontalFacingBlock.FACING, facing), 2);
					changed = true;
				}
			}
		}

		for (RoadLayout.Road road : roads) {
			for (RoadLayout.Post post : road.posts()) {
				if ((post.x() >> 4) == chunk.x && (post.z() >> 4) == chunk.z) {
					placePost(world, post);
					changed = true;
				}
			}
		}

		for (Placed placed : outposts) {
			if ((placed.outpost.x() >> 4) == chunk.x && (placed.outpost.z() >> 4) == chunk.z) {
				placeOutpost(world, placed, context.getRandom());
				changed = true;
			}
		}
		return changed;
	}

	/** Template id of an outpost: {@code embervoid:ashen_reach/outpost/<kind>_<zone>} by the zone of its road's arena. */
	public static Identifier outpostId(RoadLayout.Outpost outpost, RealmArenas.Location arena) {
		String zone = ReachZones.zoneName(arena.spec().bossId());
		return Embervoid.id("ashen_reach/outpost/" + outpost.kind() + "_" + zone);
	}

	/** Rotation that turns a template's +Z side along {@code (dirX, dirZ)}, in 90° steps. */
	public static BlockRotation rotationAlong(double dirX, double dirZ) {
		if (Math.abs(dirX) > Math.abs(dirZ)) {
			return dirX > 0 ? BlockRotation.COUNTERCLOCKWISE_90 : BlockRotation.CLOCKWISE_90;
		}
		return dirZ > 0 ? BlockRotation.NONE : BlockRotation.CLOCKWISE_180;
	}

	private record Placed(RoadLayout.Outpost outpost, StructureTemplate template) {
		/** Whether x, z lies on the template's footprint (its paved road stretch included). */
		boolean covers(int x, int z) {
			Vec3i size = template.getSize();
			BlockRotation rotation = rotationAlong(outpost.dirX(), outpost.dirZ());
			boolean turned = rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90;
			int halfX = (turned ? size.getZ() : size.getX()) / 2;
			int halfZ = (turned ? size.getX() : size.getZ()) / 2;
			return Math.abs(x - outpost.x()) <= halfX && Math.abs(z - outpost.z()) <= halfZ;
		}
	}

	private static boolean underOutpost(List<Placed> outposts, int x, int z) {
		for (Placed placed : outposts) {
			if (placed.covers(x, z)) {
				return true;
			}
		}
		return false;
	}

	private static void placeOutpost(StructureWorldAccess world, Placed placed, Random random) {
		RoadLayout.Outpost outpost = placed.outpost;
		StructureTemplate template = placed.template;
		Vec3i size = template.getSize();
		BlockRotation rotation = rotationAlong(outpost.dirX(), outpost.dirZ());
		int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, outpost.x(), outpost.z()) - 1;
		BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
		BlockPos origin = new BlockPos(outpost.x(), y, outpost.z()).subtract(pivot);
		StructurePlacementData data = new StructurePlacementData()
				.setRotation(rotation)
				.setPosition(pivot)
				.addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS)
				.addProcessor(PlacedEnemyProcessor.INSTANCE);
		// Foundation under the whole footprint, so the outpost never hangs over a dip.
		BlockPos.Mutable pos = new BlockPos.Mutable();
		int half = Math.max(size.getX(), size.getZ()) / 2;
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				if (!placed.covers(outpost.x() + dx, outpost.z() + dz)) {
					continue;
				}
				for (int dy = 1; dy <= FOUNDATION; dy++) {
					pos.set(outpost.x() + dx, y - dy, outpost.z() + dz);
					if (!world.getBlockState(pos).isAir() && world.getFluidState(pos).isEmpty()) {
						break;
					}
					world.setBlockState(pos, ModBlocks.COBBLED_ASH_STONE.getDefaultState(), 2);
				}
			}
		}
		template.place(world, origin, origin, data, random, 2);
	}

	/** Road block at a distance from the centre line: flagstones in the middle, the kerb at the edges, no gaps. */
	static BlockState paving(double distance) {
		return (distance <= 1.5 ? ReachBlocks.KINGSROAD_FLAGSTONES : ReachBlocks.KINGSROAD_KERB).getDefaultState();
	}

	/** A lantern post like the plaza's: two ash-brick wall blocks on the ground, an ember lantern on top. */
	private static void placePost(StructureWorldAccess world, RoadLayout.Post post) {
		int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, post.x(), post.z());
		BlockPos base = new BlockPos(post.x(), y, post.z());
		world.setBlockState(base, ModBlocks.ASH_BRICK_WALL.getDefaultState(), 2);
		world.setBlockState(base.up(), ModBlocks.ASH_BRICK_WALL.getDefaultState(), 2);
		world.setBlockState(base.up(2), ModBlocks.EMBER_LANTERN.getDefaultState(), 2);
	}

	/** Road segments with an end within reach of the chunk. */
	private static List<double[][]> segmentsNear(RoadLayout.Road road, int minX, int minZ) {
		List<double[][]> near = new ArrayList<>();
		List<double[]> points = road.points();
		double margin = RealmRoads.HALF_WIDTH + RoadLayout.STEP;
		for (int k = 1; k < points.size(); k++) {
			double[] a = points.get(k - 1);
			double[] b = points.get(k);
			if (Math.max(a[0], b[0]) >= minX - margin && Math.min(a[0], b[0]) <= minX + 16 + margin
					&& Math.max(a[1], b[1]) >= minZ - margin && Math.min(a[1], b[1]) <= minZ + 16 + margin) {
				near.add(new double[][]{a, b});
			}
		}
		return near;
	}

	private static StructureTemplate template(ServerWorld world, Identifier id) {
		StructureTemplate template = world.getStructureTemplateManager().getTemplate(id).orElse(null);
		if (template == null && MISSING.add(id)) {
			Embervoid.LOGGER.warn("Road outpost template {} is missing, outposts of that kind are skipped", id);
		}
		return template;
	}
}
