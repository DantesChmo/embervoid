package com.embervoid.realm.worldgen;

import com.embervoid.realm.RealmBlocks;
import com.embervoid.realm.placed.PlacedEnemyProcessor;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.SanctumBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

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
			generateArena(world, chunkBox, random);
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

	/** Rotation that turns a template's north (−Z) side towards the landing. */
	BlockRotation rotationTowardsLanding() {
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
