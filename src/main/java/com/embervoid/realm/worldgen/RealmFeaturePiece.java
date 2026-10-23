package com.embervoid.realm.worldgen;

import com.embervoid.realm.RealmBlocks;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.Realms;
import com.embervoid.realm.RoadLayout;
import com.embervoid.realm.placed.PlacedEnemyProcessor;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.SanctumBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Placeholder landing platform / boss arena, built from code. Replaced by templates once they are built in game;
 * the positions it exposes ({@link #center}, {@link #shrinePos}) stay the contract for the rest of the realm code.
 */
public class RealmFeaturePiece extends StructurePiece {
	/** The landing plaza's paved radius (PLAN 9.6); its broken wall stands on the rim. */
	public static final int LANDING_RADIUS = 21;
	/** The old inner landing disc, polished stone around the shrine. */
	private static final int LANDING_INNER = 9;
	/** Air cleared above the floor. */
	private static final int HEADROOM = 14;
	/** Deepest a floor column is extended down to reach the ground. */
	private static final int MAX_FOUNDATION = 24;
	/** How far from the landing centre the return obelisk stands (opposite the Citadel Gate). */
	public static final int OBELISK_DISTANCE = 6;
	/** The Citadel Gate's fog wall: this many blocks either side of its middle, this high (PLAN 21.2). */
	public static final int GATE_HALF_WIDTH = 5;
	public static final int GATE_HEIGHT = 4;
	/** How far outside the fog wall an arena's shrine stands. */
	private static final int SHRINE_DISTANCE = 4;

	/** Deepest the ground under an arena site is filled down to. */
	private static final int SITE_FOUNDATION = 64;
	/** Highest the air above an arena site is cleared up to (a sunken site digs out its hillside). */
	private static final int SITE_CLEARANCE = 64;

	private final RealmFeatureStructure.Kind kind;
	private final BlockPos center;
	private final int radius;
	/** Arena ground and template; null for the landing and arenas without a site. */
	@Nullable
	private final ArenaSite site;

	public RealmFeaturePiece(RealmFeatureStructure.Kind kind, BlockPos center, int radius) {
		this(kind, center, radius, null);
	}

	public RealmFeaturePiece(RealmFeatureStructure.Kind kind, BlockPos center, int radius, @Nullable ArenaSite site) {
		super(RealmWorldgen.FEATURE_PIECE, 0, box(center, radius, site));
		this.kind = kind;
		this.center = center;
		this.radius = radius;
		this.site = site;
	}

	public RealmFeaturePiece(NbtCompound nbt) {
		super(RealmWorldgen.FEATURE_PIECE, nbt);
		this.kind = RealmFeatureStructure.Kind.valueOf(nbt.getString("Kind"));
		this.center = BlockPos.fromLong(nbt.getLong("Center"));
		this.radius = nbt.getInt("Radius");
		this.site = nbt.contains("Site") ? ArenaSite.CODEC.parse(NbtOps.INSTANCE, nbt.get("Site")).result().orElse(null) : null;
	}

	private static BlockBox box(BlockPos center, int radius, @Nullable ArenaSite site) {
		int r = Math.max(radius + SHRINE_DISTANCE + 3, site == null ? 0 : site.radius());
		return new BlockBox(center.getX() - r, center.getY() - 1, center.getZ() - r,
				center.getX() + r, center.getY() + HEADROOM, center.getZ() + r);
	}

	@Override
	protected void writeNbt(StructureContext context, NbtCompound nbt) {
		nbt.putString("Kind", kind.name());
		nbt.putLong("Center", center.asLong());
		nbt.putInt("Radius", radius);
		if (site != null) {
			ArenaSite.CODEC.encodeStart(NbtOps.INSTANCE, site).result().ifPresent(site -> nbt.put("Site", site));
		}
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

	@Nullable
	public ArenaSite site() {
		return site;
	}

	/**
	 * Where the feature's Ember Shrine stands: the landing's center, or outside an arena on the arena's north axis
	 * (the side turned towards the landing, see {@link #rotationTowardsLanding}), {@value #SHRINE_DISTANCE} blocks past
	 * the fog wall. Arena templates build the shrine platform there.
	 */
	public BlockPos shrinePos() {
		if (kind == RealmFeatureStructure.Kind.LANDING) {
			return center.up();
		}
		return center.add(new BlockPos(0, 1, -(radius + SHRINE_DISTANCE)).rotate(rotationTowardsLanding()));
	}

	@Override
	public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
			Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
		if (kind == RealmFeatureStructure.Kind.LANDING) {
			generateLanding(world, chunkBox);
		} else {
			generateArena(world, chunkBox, random);
		}
	}

	private void generateLanding(StructureWorldAccess world, BlockBox chunkBox) {
		// The kingdom's crossroads (PLAN 9.6): a brick plaza, the old polished disc around the shrine, a broken low wall
		// on the rim with a gap and two lantern posts where each road leaves.
		disc(world, chunkBox, center, radius, ModBlocks.ASH_BRICKS.getDefaultState(), ModBlocks.ASH_BRICKS.getDefaultState());
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz <= radius * radius && MathHelper.hashCode(dx, 7, dz) % 5 == 0) {
					put(world, chunkBox, pos.set(center.getX() + dx, center.getY(), center.getZ() + dz),
							ModBlocks.CRACKED_ASH_BRICKS.getDefaultState());
				}
			}
		}
		disc(world, chunkBox, center, LANDING_INNER, ModBlocks.POLISHED_ASH_STONE.getDefaultState(), ModBlocks.ASH_BRICKS.getDefaultState());
		List<Double> exits = new ArrayList<>();
		for (RoadLayout.Road road : RealmRoads.painted(world.toServerWorld())) {
			double[] start = road.points().get(0);
			exits.add(Math.atan2(start[1], start[0]));
		}
		double gap = 4.5 / radius;
		for (int step = 0; step < 360; step += 2) {
			double angle = Math.toRadians(step);
			BlockPos rim = center.add((int) Math.round(Math.cos(angle) * radius), 1, (int) Math.round(Math.sin(angle) * radius));
			double nearest = exits.stream().mapToDouble(exit -> Math.abs(MathHelper.wrapDegrees(Math.toDegrees(angle - exit))))
					.min().orElse(180);
			if (Math.toRadians(nearest) < gap) {
				continue;
			}
			if (MathHelper.hashCode(rim.getX(), 1, rim.getZ()) % 4 != 0) {
				put(world, chunkBox, rim, ModBlocks.ASH_BRICK_WALL.getDefaultState());
			}
		}
		for (double exit : exits) {
			for (int side : new int[]{-1, 1}) {
				double angle = exit + side * (gap + 1.0 / radius);
				BlockPos post = center.add((int) Math.round(Math.cos(angle) * radius), 1, (int) Math.round(Math.sin(angle) * radius));
				put(world, chunkBox, post, ModBlocks.ASH_BRICK_WALL.getDefaultState());
				put(world, chunkBox, post.up(), ModBlocks.ASH_BRICK_WALL.getDefaultState());
				put(world, chunkBox, post.up(2), ModBlocks.EMBER_LANTERN.getDefaultState());
			}
		}
		put(world, chunkBox, shrinePos(), SanctumBlocks.EMBER_SHRINE.getDefaultState());

		// The Citadel Gate on the rim facing the final zone's sector, across the gate road's exit (PLAN 21.2): its fog
		// wall between two pillars under a lintel. The return obelisk opposite, lanterns on the diagonals.
		Direction gateSide = gateSide(world.toServerWorld());
		Direction across = gateSide.rotateYClockwise();
		BlockPos obelisk = center.up().offset(gateSide.getOpposite(), OBELISK_DISTANCE);
		put(world, chunkBox, obelisk, RealmBlocks.RETURN_OBELISK.getDefaultState());
		put(world, chunkBox, obelisk.up(), RealmBlocks.RETURN_OBELISK.getDefaultState());
		BlockPos gate = gatePos(world.toServerWorld(), center);
		for (int d = -GATE_HALF_WIDTH - 1; d <= GATE_HALF_WIDTH + 1; d++) {
			BlockPos column = gate.offset(across, d);
			boolean pillar = Math.abs(d) > GATE_HALF_WIDTH;
			put(world, chunkBox, column, ModBlocks.ASH_BRICKS.getDefaultState());
			for (int dy = 1; dy <= GATE_HEIGHT; dy++) {
				put(world, chunkBox, column.up(dy), pillar ? masonry(column.up(dy)) : RealmBlocks.CITADEL_GATE.getDefaultState());
			}
			put(world, chunkBox, column.up(GATE_HEIGHT + 1), pillar ? ModBlocks.CHISELED_ASH_STONE.getDefaultState()
					: masonry(column.up(GATE_HEIGHT + 1)));
			if (!pillar && Math.floorMod(d, 2) == 0) {
				put(world, chunkBox, column.up(GATE_HEIGHT + 2), ModBlocks.ASH_BRICK_SLAB.getDefaultState());
			}
		}
		for (int[] d : new int[][]{{5, 5}, {-5, 5}, {5, -5}, {-5, -5}}) {
			put(world, chunkBox, center.add(d[0], 1, d[1]), ModBlocks.ASH_BRICK_WALL.getDefaultState());
			put(world, chunkBox, center.add(d[0], 2, d[1]), ModBlocks.EMBER_LANTERN.getDefaultState());
		}
	}

	/** The floor block under the middle of the Citadel Gate: on the plaza rim on the {@link #gateSide}. */
	public static BlockPos gatePos(ServerWorld world, BlockPos center) {
		return center.offset(gateSide(world), LANDING_RADIUS);
	}

	private static BlockState masonry(BlockPos pos) {
		return Math.floorMod(MathHelper.hashCode(pos), 4) == 0 ? ModBlocks.CRACKED_ASH_BRICKS.getDefaultState()
				: ModBlocks.ASH_BRICKS.getDefaultState();
	}

	/**
	 * The side of the landing the Citadel Gate stands on: the horizontal direction nearest the final zone's sector's
	 * middle line (south for a realm without one, as the Sealed Gate stood).
	 */
	public static Direction gateSide(ServerWorld world) {
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		if (realm == null || !realm.sectors().hasFinal()) {
			return Direction.SOUTH;
		}
		double angle = realm.sectors().finalCenter();
		return Direction.getFacing(Math.cos(angle), 0, Math.sin(angle));
	}

	private void generateArena(StructureWorldAccess world, BlockBox chunkBox, Random random) {
		if (site != null) {
			siteGround(world, chunkBox, site);
			StructureTemplate template = site.template()
					.flatMap(id -> world.toServerWorld().getStructureTemplateManager().getTemplate(id)).orElse(null);
			if (template != null) {
				placeTemplate(world, chunkBox, template, random);
				put(world, chunkBox, shrinePos(), SanctumBlocks.EMBER_SHRINE.getDefaultState());
				return;
			}
		}
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

	/**
	 * The flat ground of an arena site: solid down to the terrain, open sky above (a sunken site cuts its crater or
	 * sinkhole out of the ground). Terrain adaptation in the structure JSON blends the ground around it.
	 */
	private void siteGround(StructureWorldAccess world, BlockBox chunkBox, ArenaSite site) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		int r = site.radius();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > (r + 0.5) * (r + 0.5)) {
					continue;
				}
				int x = center.getX() + dx;
				int z = center.getZ() + dz;
				if (!chunkBox.contains(x, center.getY(), z)) {
					continue;
				}
				world.setBlockState(pos.set(x, center.getY(), z), site.ground(), 2);
				int top = Math.min(world.getTopY() - 1, center.getY() + SITE_CLEARANCE);
				for (int y = center.getY() + 1; y <= top; y++) {
					world.setBlockState(pos.set(x, y, z), Blocks.AIR.getDefaultState(), 2);
				}
				for (int y = center.getY() - 1; y >= center.getY() - SITE_FOUNDATION && y > world.getBottomY(); y--) {
					pos.set(x, y, z);
					BlockState state = world.getBlockState(pos);
					if (!state.isAir() && world.getFluidState(pos).isEmpty()) {
						break;
					}
					world.setBlockState(pos, ModBlocks.ASH_STONE.getDefaultState(), 2);
				}
			}
		}
	}

	/** The arena template, centred on the site, its y = 0 layer the floor, its north side facing the landing. */
	private void placeTemplate(StructureWorldAccess world, BlockBox chunkBox, StructureTemplate template, Random random) {
		Vec3i size = template.getSize();
		BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
		StructurePlacementData data = new StructurePlacementData()
				.setRotation(rotationTowardsLanding())
				.setPosition(pivot)
				.setBoundingBox(chunkBox)
				.addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS)
				.addProcessor(PlacedEnemyProcessor.INSTANCE);
		BlockPos origin = center.subtract(pivot);
		template.place(world, origin, origin, data, random, 2);
	}

	/** Rotation that turns a template's north (−Z) side towards the landing, in 90° steps. */
	public BlockRotation rotationTowardsLanding() {
		int dx = -center.getX();
		int dz = -center.getZ();
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? BlockRotation.CLOCKWISE_90 : BlockRotation.COUNTERCLOCKWISE_90;
		}
		return dz < 0 ? BlockRotation.NONE : BlockRotation.CLOCKWISE_180;
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
