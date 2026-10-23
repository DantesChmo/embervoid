package com.embervoid.hub;

import com.embervoid.Embervoid;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * Where everything stands in the hub (PLAN 15.1, DESIGN §2): one island around the origin, its floor at
 * {@link #FLOOR_Y}. The hub template ({@code embervoid:hub/ember_sanctum}, built by the client datagen from these
 * numbers) is placed with its corner at {@link #TEMPLATE_ORIGIN}.
 *
 * <pre>
 *            z = -16   Cleft Stele spot (5 wide along x, crack at x = 0), task 15.3
 *            z = -14   landing, facing south
 *   z = -10 ... 10     the open core: flagstone floor, broken walls, a gap in the middle of each side
 *            z = ±5    obelisk ring at x = ±5, the Ember Shrine at the centre, the Maiden at z = 5
 * </pre>
 */
public final class HubLayout {
	/** The island's top layer; players stand at {@code FLOOR_Y + 1}. */
	public static final int FLOOR_Y = 64;
	/** Rough edge of the island: the top layer reaches this far from the origin, give or take two blocks. */
	public static final int ISLAND_RADIUS = 21;
	/** The core square spans {@code -CORE_HALF..CORE_HALF} on both axes, walls included (the old 21×21 core). */
	public static final int CORE_HALF = 10;

	/** Half the template's width: it spans {@code -TEMPLATE_HALF..TEMPLATE_HALF} on x and z. */
	public static final int TEMPLATE_HALF = 24;
	/** Rock under the floor at the island's middle; the underside tapers off towards the edge. */
	public static final int DEPTH = 10;
	/** Template rows above the floor (walls, pillars, lanterns). */
	public static final int HEIGHT = 10;
	public static final int TEMPLATE_SIZE_XZ = TEMPLATE_HALF * 2 + 1;
	public static final int TEMPLATE_SIZE_Y = DEPTH + 1 + HEIGHT;
	public static final BlockPos TEMPLATE_ORIGIN = new BlockPos(-TEMPLATE_HALF, FLOOR_Y - DEPTH, -TEMPLATE_HALF);
	public static final Identifier TEMPLATE = Embervoid.id("hub/ember_sanctum");

	public static final BlockPos SHRINE = new BlockPos(0, FLOOR_Y + 1, 0);

	/** A Void Obelisk of the ring and the realm it leads to. */
	public record Obelisk(BlockPos pos, Identifier realm) {}

	/** Same realms and order as the old Sanctum core ({@code ProgressManager.REALM_ORDER}). */
	public static final List<Obelisk> OBELISKS = List.of(
			new Obelisk(new BlockPos(-5, FLOOR_Y + 1, -5), Embervoid.id("realm_1")),
			new Obelisk(new BlockPos(5, FLOOR_Y + 1, -5), Embervoid.id("realm_2")),
			new Obelisk(new BlockPos(5, FLOOR_Y + 1, 5), Embervoid.id("realm_3")),
			new Obelisk(new BlockPos(-5, FLOOR_Y + 1, 5), Embervoid.id("realm_4")));

	/** The Ashen Maiden stands south of the shrine, facing north towards the landing. */
	public static final BlockPos MAIDEN = new BlockPos(0, FLOOR_Y + 1, 5);
	public static final float MAIDEN_YAW = 180;

	/**
	 * Placeholder for the hub's own Cleft Stele (task 15.3 builds the blocks): the bottom block of the crack. The
	 * stele is {@link #STELE_WIDTH} wide along {@link #STELE_AXIS}, centred on the crack, and {@link #STELE_HEIGHT}
	 * tall from here up. The template leaves that space empty on a chiseled pad.
	 */
	public static final BlockPos STELE_CRACK_BOTTOM = new BlockPos(0, FLOOR_Y + 1, -16);
	public static final Direction.Axis STELE_AXIS = Direction.Axis.X;
	public static final int STELE_WIDTH = 5;
	public static final int STELE_HEIGHT = 6;

	/** Where {@code /embervoid hub} (and later the stele) puts a player: between the stele and the core. */
	public static final BlockPos LANDING = new BlockPos(0, FLOOR_Y + 1, -13);
	/** Facing south, towards the core. */
	public static final float LANDING_YAW = 0;

	/** Whether a position is one of the stele's blocks (crack included). */
	public static boolean isSteleSpot(BlockPos pos) {
		int along = STELE_AXIS == Direction.Axis.X ? pos.getX() - STELE_CRACK_BOTTOM.getX() : pos.getZ() - STELE_CRACK_BOTTOM.getZ();
		int across = STELE_AXIS == Direction.Axis.X ? pos.getZ() - STELE_CRACK_BOTTOM.getZ() : pos.getX() - STELE_CRACK_BOTTOM.getX();
		int up = pos.getY() - STELE_CRACK_BOTTOM.getY();
		return across == 0 && Math.abs(along) <= STELE_WIDTH / 2 && up >= 0 && up < STELE_HEIGHT;
	}

	/** Whether a column is inside the core square (walls included). */
	public static boolean inCore(int x, int z) {
		return Math.abs(x) <= CORE_HALF && Math.abs(z) <= CORE_HALF;
	}

	private HubLayout() {}
}
