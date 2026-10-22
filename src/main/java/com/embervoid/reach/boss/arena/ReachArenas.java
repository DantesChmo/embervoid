package com.embervoid.reach.boss.arena;

import com.embervoid.Embervoid;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Layouts of the three Ashen Reach zone arenas (PLAN 12.2, DESIGN §4.5): the numbers both the arena templates
 * (datagen, {@code client.reach.boss.datagen.ArenaTemplates}) and the bosses at runtime use. Offsets are from the
 * arena centre, the floor block under the fog wall's centre, before rotation (north = towards the landing); turn
 * them into world positions with {@link ArenaFrame}.
 *
 * <p>Each template is a square of side 2·site + 1, centred, y = 0 its floor; the shrine stands on the north axis
 * {@link #SHRINE_GAP} blocks outside the fog wall (placed by the arena structure), the arena-side chest beside it.
 */
public final class ReachArenas {
	/** Shrine distance outside the fog wall (the arena structure's rule). */
	public static final int SHRINE_GAP = 4;

	/** One zone arena: template id, fog wall radius, site radius (template half-size), template height. */
	public record Arena(String zone, Identifier template, int radius, int site, int height) {
		/** Shrine offset: on the north axis, outside the fog wall. */
		public BlockPos shrine() {
			return new BlockPos(0, 1, -(radius + SHRINE_GAP));
		}

		/** Arena-side chest offset: two blocks east of the shrine. */
		public BlockPos chest() {
			return new BlockPos(2, 1, -(radius + SHRINE_GAP));
		}

		public Identifier chestLoot() {
			return Embervoid.id("chests/ashen_reach/" + zone + "_arena");
		}

		public int size() {
			return 2 * site + 1;
		}
	}

	/** Ashen Fields: market square sunk into a crater, burnt-granary pillars. */
	public static final Arena FIELDS = new Arena("fields", Embervoid.id("ashen_reach/arena_fields"), 40, 48, 12);
	/** Royal Woods: the walled Royal Grove behind the hunting lodge. */
	public static final Arena WOODS = new Arena("woods", Embervoid.id("ashen_reach/arena_woods"), 48, 56, 8);
	/** Sunken Necropolis: a sinkhole with a switchback stair down into a flooded ossuary. */
	public static final Arena NECROPOLIS = new Arena("necropolis", Embervoid.id("ashen_reach/arena_necropolis"), 32, 40, 20);

	public static final List<Arena> ALL = List.of(FIELDS, WOODS, NECROPOLIS);

	// --- Fields ---

	/** Granary pillars: 3×3, this tall, smashable by the Ash Colossus. */
	public static final int PILLAR_HEIGHT = 8;
	public static final int PILLAR_COUNT = 10;
	public static final int PILLAR_RING = 24;

	/** Centres (x, z) of the granary pillars, on a ring, the first one off the north axis. */
	public static List<BlockPos> pillars() {
		List<BlockPos> pillars = new ArrayList<>();
		for (int i = 0; i < PILLAR_COUNT; i++) {
			double angle = Math.PI * 2 * (i + 0.5) / PILLAR_COUNT;
			pillars.add(new BlockPos((int) Math.round(Math.sin(angle) * PILLAR_RING), 0, (int) Math.round(-Math.cos(angle) * PILLAR_RING)));
		}
		return pillars;
	}

	// --- Woods ---

	/** Garden wall ring (inner radius), just outside the fog wall; gate on the north axis. */
	public static final int GROVE_WALL = 51;
	public static final int GROVE_WALL_HEIGHT = 5;
	public static final int GROVE_GATE_HALF_WIDTH = 3;

	// --- Necropolis ---

	/** Pit depth: the rim (ground level around the sinkhole) is this many blocks above the floor. */
	public static final int PIT_DEPTH = 18;
	/** Walkways, pedestals and the centre dais are solid from y = 1 to this, so they stay dry in two blocks of water. */
	public static final int WALKWAY_TOP = 2;
	/** Ossuary roof layer: the ceiling is open from y = 1 to {@code ROOF_Y − 1} (9 blocks). */
	public static final int ROOF_Y = 10;
	/** The roof covers the ring between these radii (the middle has fallen in, open to the sky). */
	public static final int ROOF_INNER = 19;
	public static final int ROOF_OUTER = 31;
	/** Brazier pedestals (3×3) on the diagonals at this offset; the brazier stands on top ({@code WALKWAY_TOP + 1}). */
	public static final int BRAZIER_OFFSET = 10;
	public static final int DAIS_RADIUS = 4;
	public static final double RING_WALK_INNER = 11.5;
	public static final double RING_WALK_OUTER = 13.5;
	public static final int CROSS_HALF_WIDTH = 1;

	/** Brazier offsets (on the pedestals). */
	public static List<BlockPos> braziers() {
		int d = BRAZIER_OFFSET;
		return List.of(new BlockPos(d, WALKWAY_TOP + 1, d), new BlockPos(-d, WALKWAY_TOP + 1, d),
				new BlockPos(-d, WALKWAY_TOP + 1, -d), new BlockPos(d, WALKWAY_TOP + 1, -d));
	}

	/** Whether a floor column (offset x, z) is raised dry ground: dais, cross walkways, ring walkway or a pedestal. */
	public static boolean isWalkway(int x, int z) {
		double r = Math.sqrt(x * x + z * z);
		if (r > NECROPOLIS.radius() - 2) {
			return false;
		}
		if (r <= DAIS_RADIUS + 0.5 || (r >= RING_WALK_INNER && r <= RING_WALK_OUTER)) {
			return true;
		}
		if (Math.abs(x) <= CROSS_HALF_WIDTH || Math.abs(z) <= CROSS_HALF_WIDTH) {
			return true;
		}
		int d = BRAZIER_OFFSET;
		return Math.abs(Math.abs(x) - d) <= 1 && Math.abs(Math.abs(z) - d) <= 1;
	}

	private ReachArenas() {}
}
