package com.embervoid.reach.citadel;

import com.embervoid.Embervoid;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * The fixed plan of the Burning City (PLAN 13.1–13.2, DESIGN §4.5 "Smoldering Citadel"): where the landing, the
 * Citadel's pieces and the Smoldering Lord's courtyard sit in {@code embervoid:ashen_reach_depths}, and the ground
 * height the terrain gives each column. Pure, so the terrain ({@link CitadelTerrain}), the Citadel structure and the
 * tests all read the same numbers.
 *
 * <p>The landing is at 0, 0 in the lower city (the outskirts); the route runs north (−Z) up to the hill: outskirts →
 * burning street → market → gatehouse in the city wall on the hill's rim → inner ring around the courtyard → keep.
 * The hill is a plateau at {@link #PLATEAU_Y} with steep sides, the city wall on its south rim.
 */
public final class CitadelLayout {
	/** Ground of the landing (top block). */
	public static final int LANDING_Y = 66;
	/** Ground of the hill's plateau (top block): the wall, the inner ring, the courtyard and the keep stand on it. */
	public static final int PLATEAU_Y = 88;

	/** The Smoldering Lord's courtyard (arena site, track reach-bosses builds on it): centre column and radius. */
	public static final int COURTYARD_X = 0;
	public static final int COURTYARD_Z = -192;
	public static final int COURTYARD_RADIUS = 36;

	/**
	 * Start column of the Citadel structure (its placement puts it here). A structure only generates in chunks within 8
	 * of its start chunk, so the start sits in the middle of the route.
	 */
	public static final int START_X = 0;
	public static final int START_Z = -160;
	public static final int START_REACH_CHUNKS = 8;

	/** The plateau: a rounded rectangle, its sides falling to the lower city over {@link #CLIFF_WIDTH} blocks. */
	public static final int PLATEAU_MIN_X = -76;
	public static final int PLATEAU_MAX_X = 76;
	public static final int PLATEAU_MIN_Z = -280;
	public static final int PLATEAU_MAX_Z = -128;
	private static final double PLATEAU_CORNER = 6;
	private static final double CLIFF_WIDTH = 7;

	/** Layers of foundation under every piece's floor (its template's y = 0 is this far below the floor). */
	public static final int FOUNDATION = 3;
	/** Flat ground around a terrace's rectangle before it blends into the natural ground. */
	private static final double TERRACE_MARGIN = 4;
	private static final double TERRACE_BLEND = 10;

	/**
	 * One Citadel piece: template {@code embervoid:ashen_reach/citadel/<name>}, its box (min corner and size) and its
	 * floor height. The template's y = 0 is {@link #FOUNDATION} below the floor, so it carries its own foundation and
	 * any steps up from the piece before it. Pieces never overlap each other or the courtyard square.
	 */
	public record Piece(String name, int minX, int floorY, int minZ, int sizeX, int sizeY, int sizeZ) {
		public Identifier template() {
			return Embervoid.id("ashen_reach/citadel/" + name);
		}

		public int maxX() {
			return minX + sizeX - 1;
		}

		public int maxZ() {
			return minZ + sizeZ - 1;
		}

		/** World y of the template's y = 0 layer. */
		public int originY() {
			return floorY - FOUNDATION;
		}
	}

	/** Flat ground at height {@code y} over a rectangle (inclusive), blended into the natural ground around it. */
	public record Terrace(int minX, int minZ, int maxX, int maxZ, int y) {
		/** Horizontal distance from the rectangle, 0 inside. */
		double distance(double x, double z) {
			double dx = Math.max(0, Math.max(minX - x, x - maxX));
			double dz = Math.max(0, Math.max(minZ - z, z - maxZ));
			return Math.sqrt(dx * dx + dz * dz);
		}
	}

	public static final Piece OUTSKIRTS = new Piece("outskirts", -16, 67, -62, 33, 16, 33);
	public static final Piece STREET = new Piece("street", -16, 69, -95, 33, 18, 33);
	public static final Piece MARKET = new Piece("market", -24, 71, -120, 49, 16, 25);
	/** Its floor is the gate approach; the tunnel climbs to the plateau and opens on a small plaza before the courtyard. */
	public static final Piece GATEHOUSE = new Piece("gatehouse", -12, 74, -155, 25, 36, 35);
	public static final Piece WALL_WEST = new Piece("wall_west", -64, PLATEAU_Y, -133, 52, 15, 6);
	public static final Piece WALL_EAST = new Piece("wall_east", 13, PLATEAU_Y, -133, 52, 15, 6);
	public static final Piece TOWER_WEST = new Piece("tower_west", -76, PLATEAU_Y, -139, 12, 28, 12);
	public static final Piece TOWER_EAST = new Piece("tower_east", 65, PLATEAU_Y, -139, 12, 28, 12);
	public static final Piece INNER_RING_WEST = new Piece("inner_ring_west", -76, PLATEAU_Y, -228, 39, 18, 89);
	public static final Piece INNER_RING_EAST = new Piece("inner_ring_east", 38, PLATEAU_Y, -228, 39, 18, 89);
	public static final Piece KEEP = new Piece("keep", -28, PLATEAU_Y, -272, 57, 36, 44);

	/** Every Citadel piece, in route order. */
	public static final List<Piece> PIECES = List.of(OUTSKIRTS, STREET, MARKET, GATEHOUSE, WALL_WEST, WALL_EAST,
			TOWER_WEST, TOWER_EAST, INNER_RING_WEST, INNER_RING_EAST, KEEP);

	/** The flat ground the terrain gives the landing, every piece and the courtyard. */
	public static final List<Terrace> TERRACES = terraces();

	private static List<Terrace> terraces() {
		List<Terrace> result = new java.util.ArrayList<>();
		result.add(new Terrace(-30, -29, 30, 30, LANDING_Y));
		for (Piece piece : PIECES) {
			if (piece == GATEHOUSE) {
				// Only the approach below the hill; the rest of the gatehouse is cut into the plateau.
				result.add(new Terrace(piece.minX(), -127, piece.maxX(), piece.maxZ(), piece.floorY()));
			} else {
				result.add(new Terrace(piece.minX(), piece.minZ(), piece.maxX(), piece.maxZ(), piece.floorY()));
			}
		}
		result.add(new Terrace(COURTYARD_X - COURTYARD_RADIUS, COURTYARD_Z - COURTYARD_RADIUS,
				COURTYARD_X + COURTYARD_RADIUS, COURTYARD_Z + COURTYARD_RADIUS, PLATEAU_Y));
		return List.copyOf(result);
	}

	/**
	 * Ground height (the top block's y, fractional) at a column.
	 *
	 * @param hills the realm hills noise at the column, roughly in [-1, 1]
	 */
	public static double groundHeight(double x, double z, double hills) {
		double natural = naturalHeight(x, z, hills);
		Terrace nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Terrace terrace : TERRACES) {
			double d = terrace.distance(x, z);
			if (d < nearestDistance) {
				nearest = terrace;
				nearestDistance = d;
			}
		}
		double weight = terraceWeight(nearestDistance);
		return natural + (nearest.y() - natural) * weight;
	}

	/**
	 * How flat a column is kept, 0–1: 1 on a terrace (no rough noise there, so pieces and the arena site sit on exactly
	 * their height), fading to 0 over the blend ring.
	 */
	public static double flatness(double x, double z) {
		double nearest = Double.MAX_VALUE;
		for (Terrace terrace : TERRACES) {
			nearest = Math.min(nearest, terrace.distance(x, z));
		}
		return terraceWeight(nearest);
	}

	/** How much of a column is the hill's side, 0–1 (rough noise breaks the cliffs up more than the flat city). */
	public static double cliff(double x, double z) {
		double d = plateauDistance(x, z);
		return d > -2 && d < CLIFF_WIDTH + 2 ? 1 : 0;
	}

	/** The ground without terraces: the lower city rising gently north, the plateau, the cliff between them. */
	static double naturalHeight(double x, double z, double hills) {
		double lower = LANDING_Y + 4 * clamp(-z / 120.0) + hills * 5;
		double plateau = PLATEAU_Y + hills * 1.5;
		double t = smoothstep(plateauDistance(x, z) / CLIFF_WIDTH);
		return plateau + (lower - plateau) * t;
	}

	/** Signed distance to the plateau's rounded rectangle: negative inside. */
	static double plateauDistance(double x, double z) {
		double cx = (PLATEAU_MIN_X + PLATEAU_MAX_X) / 2.0;
		double cz = (PLATEAU_MIN_Z + PLATEAU_MAX_Z) / 2.0;
		double qx = Math.abs(x - cx) - ((PLATEAU_MAX_X - PLATEAU_MIN_X) / 2.0 - PLATEAU_CORNER);
		double qz = Math.abs(z - cz) - ((PLATEAU_MAX_Z - PLATEAU_MIN_Z) / 2.0 - PLATEAU_CORNER);
		double outside = Math.sqrt(Math.max(qx, 0) * Math.max(qx, 0) + Math.max(qz, 0) * Math.max(qz, 0));
		return outside + Math.min(Math.max(qx, qz), 0) - PLATEAU_CORNER;
	}

	private static double terraceWeight(double distance) {
		return 1 - smoothstep((distance - TERRACE_MARGIN) / TERRACE_BLEND);
	}

	private static double smoothstep(double t) {
		t = clamp(t);
		return t * t * (3 - 2 * t);
	}

	private static double clamp(double t) {
		return Math.max(0, Math.min(1, t));
	}

	private CitadelLayout() {}
}
