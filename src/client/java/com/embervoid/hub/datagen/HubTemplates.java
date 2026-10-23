package com.embervoid.hub.datagen;

import com.embervoid.hub.HubLayout;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumEntities;
import com.embervoid.sanctum.datagen.TemplateBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

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
 *   <li>North of the core a paved path to the landing and a chiseled pad with the Cleft Stele's spot left empty
 *   between two lantern posts (task 15.3 builds the stele).</li>
 *   <li>The underside tapers from {@value HubLayout#DEPTH} blocks of rock in the middle to one at the edge.</li>
 * </ul>
 */
final class HubTemplates {
	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final BlockState BRICKS = ASH_BRICKS.getDefaultState();
	private static final BlockState CRACKED = CRACKED_ASH_BRICKS.getDefaultState();
	private static final BlockState POLISHED = POLISHED_ASH_STONE.getDefaultState();
	private static final BlockState CHISELED = CHISELED_ASH_STONE.getDefaultState();
	private static final BlockState COBBLED = COBBLED_ASH_STONE.getDefaultState();
	private static final BlockState STONE = ASH_STONE.getDefaultState();
	private static final BlockState SOIL = ASH_SOIL.getDefaultState();
	private static final BlockState WALL = ASH_BRICK_WALL.getDefaultState();
	private static final BlockState FLAGSTONE = SanctumBlocks.SANCTUM_FLAGSTONE.getDefaultState();
	private static final BlockState LANTERN = EMBER_LANTERN.getDefaultState();

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

		// Core walls: broken, low, open in the middle of each side.
		for (int i = -core; i <= core; i++) {
			for (int[] p : new int[][]{{i, -core}, {i, core}, {-core, i}, {core, i}}) {
				if (Math.abs(i) <= 1) {
					continue;
				}
				int height = 1 + random.nextInt(3);
				for (int y = 1; y <= height; y++) {
					t.set(p[0], y, p[1], random.nextInt(4) == 0 ? CRACKED : y == 1 ? POLISHED : BRICKS);
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

		// North of the core: the path out of the gap, past the landing, to the stele pad.
		BlockPos stele = HubLayout.STELE_CRACK_BOTTOM;
		int steleHalf = HubLayout.STELE_WIDTH / 2;
		for (int z = stele.getZ() + 2; z < -core; z++) {
			for (int x = -1; x <= 1; x++) {
				t.set(x, 0, z, POLISHED);
			}
		}
		for (int x = stele.getX() - steleHalf - 1; x <= stele.getX() + steleHalf + 1; x++) {
			for (int z = stele.getZ() - 1; z <= stele.getZ() + 1; z++) {
				t.set(x, 0, z, CHISELED);
			}
		}
		// The stele's own space stays empty; the template clears it so nothing else can end up there.
		for (int x = stele.getX() - steleHalf; x <= stele.getX() + steleHalf; x++) {
			for (int y = 1; y <= HubLayout.STELE_HEIGHT; y++) {
				t.set(x, y, stele.getZ(), AIR);
			}
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
		return r < 0.45 ? SOIL : r < 0.75 ? COBBLED : STONE;
	}

	private static BlockState rock(Random random) {
		double r = random.nextDouble();
		return r < 0.5 ? STONE : r < 0.75 ? COBBLED : r < 0.9 ? Blocks.TUFF.getDefaultState() : Blocks.BLACKSTONE.getDefaultState();
	}

	private static void pillar(Island t, int x, int z, int height, boolean lantern) {
		for (int y = 1; y <= height; y++) {
			t.set(x, y, z, CHISELED);
		}
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
