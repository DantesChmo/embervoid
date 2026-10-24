package com.embervoid.hub.datagen;

import com.embervoid.hub.HubLayout;
import com.embervoid.block.setpiece.LitSetPieceBlock;
import com.embervoid.block.setpiece.SetPieces;
import com.embervoid.hub.stele.CleftStoneBlock;
import com.embervoid.hub.stele.HubStele;
import com.embervoid.hub.stele.SteleBlocks;
import com.embervoid.hub.stele.SteleCrackBlock;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumEntities;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Map;
import java.util.Random;

import static com.embervoid.registry.ModBlocks.*;

/**
 * The hub's one template (PLAN 15.1): a floating island of ash rock with the old Sanctum core on top, open to the dusk
 * sky. Built from {@link HubLayout}; x and z below are world coordinates, y is relative to the floor.
 *
 * <ul>
 *   <li>The core, 21×21 as in the Sanctum: Sanctum Flagstone floor, broken walls with a 3-wide gap in the middle of each
 *   side, chiseled pillars where the old room had them, ember lanterns on the corners and beside the gaps.</li>
 *   <li>The Ember Shrine in the middle, four Void Obelisks in a ring, the Ashen Maiden south of the shrine.</li>
 *   <li>North of the core a paved path to the landing and a chiseled pad with the hub's own Cleft Stele, built lit,
 *   between two lantern posts (task 15.4).</li>
 *   <li>The underside tapers from {@value HubLayout#DEPTH} blocks of rock in the middle to one at the edge.</li>
 * </ul>
 */
final class HubTemplates {
	private static final BlockState BRICKS = ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState MOSSY = MOSSY_ASH_BRICKS.getDefaultState();
	private static final BlockState COBBLE_BRICKS = ASH_COBBLE_BRICKS.getDefaultState();
	private static final BlockState POLISHED = POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState CHISELED = CHISELED_ASH_STONE.getDefaultState();
	private static final BlockState CARVED = CARVED_ASH_STONE.getDefaultState();
	private static final BlockState PILLAR = ASH_BRICK_PILLAR.getDefaultState();
	private static final BlockState TILES = ASH_TILES.getDefaultState();
	private static final BlockState CRACKED_TILES = CRACKED_ASH_TILES.getDefaultState();
	private static final BlockState RUNE = SANCTUM_RUNE_STONE.getDefaultState();
	private static final BlockState BRICK_STAIRS = ASH_BRICK_STAIRS.getDefaultState();
	private static final BlockState COBBLED = COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState MOSSY_COBBLED = MOSSY_COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState PATH = ReachBlocks.ASH_COBBLE_PATH.getDefaultState();
	private static final BlockState STONE = ASH_STONE.getDefaultState();
	private static final BlockState SOIL = ASH_SOIL.getDefaultState();
	private static final BlockState WALL = ASH_BRICK_WALL.getDefaultState();
	private static final BlockState FLAGSTONE = SanctumBlocks.SANCTUM_FLAGSTONE.getDefaultState();
	private static final BlockState LANTERN = EMBER_LANTERN.getDefaultState();
	private static final BlockState KINDLED_STONE = SteleBlocks.CLEFT_STONE.getDefaultState().with(CleftStoneBlock.KINDLED, true);
	private static final BlockState CRACK = SteleBlocks.STELE_CRACK.getDefaultState()
			.with(SteleCrackBlock.AXIS, HubStele.FRAME.axis());

	static Map<Identifier, TemplateBuilder> all() {
		return Map.of(HubLayout.TEMPLATE, island());
	}

	private static TemplateBuilder island() {
		Island t = new Island();
		Random random = new Random(15);
		int half = HubLayout.TEMPLATE_HALF;
		int core = HubLayout.CORE_HALF;
		for (int x = -half; x <= half; x++) {
			for (int z = -half; z <= half; z++) {
				double d = Math.sqrt(x * x + z * z);
				double edge = edgeRadius(Math.atan2(z, x)) + random.nextDouble() - 0.5;
				if (!HubLayout.inCore(x, z) && d > edge) {
					continue;
				}
				t.set(x, 0, z, top(x, z, random));
				// Rock under the floor: deepest in the middle, a single layer at the rim.
				double rim = Math.max(0, 1 - d / Math.max(edge, 1));
				int depth = Math.max(1, Math.min(HubLayout.DEPTH, (int) Math.round(HubLayout.DEPTH * Math.sqrt(rim)
						+ random.nextInt(3) - 1)));
				for (int y = -depth; y < 0; y++) {
					t.set(x, y, z, rock(random));
				}
			}
		}

		// Core walls: broken, low, open in the middle of each side; a cobble-brick base course, bricks cracking
		// upwards and mossy at the foot, a slab coping where a stretch stands level.
		for (int i = -core; i <= core; i++) {
			for (int[] p : new int[][]{{i, -core}, {i, core}, {-core, i}, {core, i}}) {
				if (Math.abs(i) <= 1) {
					continue;
				}
				int height = 1 + random.nextInt(3);
				for (int y = 1; y <= height; y++) {
					t.set(p[0], y, p[1], TemplateBuilder.weathered(random, (y - 1) / 3.0, 0, COBBLE_BRICKS, 2, y, BRICKS, CRACKED, MOSSY));
				}
				if (height == 3 && random.nextBoolean()) {
					t.set(p[0], 4, p[1], ASH_BRICK_SLAB.getDefaultState());
				}
			}
		}
		// Pillars where the old core had them (±2 beside the gaps, ±6) and on the corners.
		for (int i : new int[]{-6, -2, 2, 6}) {
			for (int[] p : new int[][]{{i, -core}, {i, core}, {-core, i}, {core, i}}) {
				int height = Math.abs(i) == 2 ? 5 : 3 + random.nextInt(2);
				pillar(t, p[0], p[1], height, Math.abs(i) == 2);
			}
		}
		for (int[] p : new int[][]{{-core, -core}, {core, -core}, {-core, core}, {core, core}}) {
			pillar(t, p[0], p[1], 6, true);
		}
		// Lit censers over the four gaps into the core, on beams between the gap pillars (PLAN 20.2).
		NbtCompound lit = new NbtCompound();
		lit.putString("State", "lit");
		for (int[] gap : new int[][]{{0, -core}, {0, core}, {-core, 0}, {core, 0}}) {
			boolean alongX = gap[1] != 0;
			for (int i = -1; i <= 1; i++) {
				t.set(alongX ? i : gap[0], 6, alongX ? gap[1] : i, DEAD_LOG.getDefaultState().with(PillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z));
			}
			t.builder.blockEntity(gap[0] + half, 5, gap[1] + half, SetPieces.CENSER.getDefaultState().with(LitSetPieceBlock.LIT, true), "embervoid:set_piece", lit);
		}

		t.set(HubLayout.SHRINE, SanctumBlocks.EMBER_SHRINE.getDefaultState());
		for (HubLayout.Obelisk obelisk : HubLayout.OBELISKS) {
			NbtCompound data = new NbtCompound();
			data.putString("Realm", obelisk.realm().toString());
			BlockPos pos = obelisk.pos();
			t.builder.blockEntity(pos.getX() + half, pos.getY() - HubLayout.FLOOR_Y, pos.getZ() + half,
					SanctumBlocks.VOID_OBELISK.getDefaultState(), "embervoid:void_obelisk", data);
		}
		NbtCompound maiden = new NbtCompound();
		maiden.putString("id", Registries.ENTITY_TYPE.getId(SanctumEntities.ASHEN_MAIDEN).toString());
		maiden.putBoolean("PersistenceRequired", true);
		NbtList rotation = new NbtList();
		rotation.add(NbtFloat.of(HubLayout.MAIDEN_YAW));
		rotation.add(NbtFloat.of(0));
		maiden.put("Rotation", rotation);
		t.builder.entity(HubLayout.MAIDEN.getX() + half, HubLayout.MAIDEN.getY() - HubLayout.FLOOR_Y,
				HubLayout.MAIDEN.getZ() + half, maiden);

		// North of the core: the tiled path out of the gap, past the landing, to the stele pad, a rune every third
		// block down its middle.
		BlockPos stele = HubLayout.STELE_CRACK_BOTTOM;
		int steleHalf = HubLayout.STELE_WIDTH / 2;
		for (int z = stele.getZ() + 2; z < -core; z++) {
			for (int x = -1; x <= 1; x++) {
				t.set(x, 0, z, x == 0 && z % 3 == 0 ? RUNE : random.nextInt(6) == 0 ? CRACKED_TILES : TILES);
			}
		}
		for (int x = stele.getX() - steleHalf - 1; x <= stele.getX() + steleHalf + 1; x++) {
			for (int z = stele.getZ() - 1; z <= stele.getZ() + 1; z++) {
				boolean corner = Math.abs(x - stele.getX()) == steleHalf + 1 && z != stele.getZ();
				t.set(x, 0, z, corner ? RUNE : CHISELED);
			}
		}
		// The gateposts of the core's four gaps: pillars with carved capitals.
		for (int[] g : new int[][]{{-2, -core}, {2, -core}, {-2, core}, {2, core}, {-core, -2}, {-core, 2}, {core, -2}, {core, 2}}) {
			t.set(g[0], 1, g[1], CHISELED);
		}
		// The hub's own stele, always lit: kindled stones either side of the crack.
		for (BlockPos pos : HubStele.FRAME.stones()) {
			t.set(pos, KINDLED_STONE);
		}
		for (BlockPos pos : HubStele.FRAME.crack()) {
			t.set(pos, CRACK);
		}
		for (int side : new int[]{-1, 1}) {
			int x = stele.getX() + side * (steleHalf + 2);
			t.set(x, 0, stele.getZ(), CHISELED);
			t.set(x, 1, stele.getZ(), WALL).set(x, 2, stele.getZ(), WALL).set(x, 3, stele.getZ(), LANTERN);
		}
		return t.builder;
	}

	/** The island's rim at an angle: {@link HubLayout#ISLAND_RADIUS}, give or take two blocks. */
	private static double edgeRadius(double angle) {
		return HubLayout.ISLAND_RADIUS + 1.2 * Math.sin(3 * angle + 1) + 0.8 * Math.sin(7 * angle + 2);
	}

	private static BlockState top(int x, int z, Random random) {
		if (HubLayout.inCore(x, z)) {
			return FLAGSTONE;
		}
		double r = random.nextDouble();
		return r < 0.4 ? SOIL : r < 0.55 ? PATH : r < 0.7 ? COBBLED : r < 0.8 ? MOSSY_COBBLED : STONE;
	}

	private static BlockState rock(Random random) {
		double r = random.nextDouble();
		return r < 0.5 ? STONE : r < 0.75 ? COBBLED : r < 0.9 ? Blocks.TUFF.getDefaultState() : Blocks.BLACKSTONE.getDefaultState();
	}

	/** A pillar: a chiseled base, a brick-pillar shaft, a carved capital, a lantern on top where asked. */
	private static void pillar(Island t, int x, int z, int height, boolean lantern) {
		t.set(x, 1, z, CHISELED);
		for (int y = 2; y < height; y++) {
			t.set(x, y, z, PILLAR);
		}
		t.set(x, height, z, height > 1 ? CARVED : CHISELED);
		if (lantern) {
			t.set(x, height + 1, z, LANTERN);
		}
	}

	/** {@link TemplateBuilder} in world x and z, y from the floor. */
	private static final class Island {
		final TemplateBuilder builder = new TemplateBuilder(HubLayout.TEMPLATE_SIZE_XZ, HubLayout.TEMPLATE_SIZE_Y,
				HubLayout.TEMPLATE_SIZE_XZ).baseY(HubLayout.DEPTH);

		Island set(int x, int y, int z, BlockState state) {
			builder.set(x + HubLayout.TEMPLATE_HALF, y, z + HubLayout.TEMPLATE_HALF, state);
			return this;
		}

		void set(BlockPos pos, BlockState state) {
			set(pos.getX(), pos.getY() - HubLayout.FLOOR_Y, pos.getZ(), state);
		}
	}

	private HubTemplates() {}
}
