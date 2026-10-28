package com.embervoid.vampire;

/**
 * The shape of the Vampire arena ({@link VampireArena}): a disc with a crying heart, a blood-red ring and inlay, a low
 * rim and eight lit pillars, by the part each block plays; {@link VampireArena} picks the blocks.
 */
public final class VampireArenaLayout {
	public static final int RADIUS = 22;
	public static final int PILLARS = 8;
	public static final int PILLAR_RADIUS = 18;
	/** Height of a pillar above the floor; its top block is the light. */
	public static final int PILLAR_HEIGHT = 6;

	public enum Role {
		AIR,
		/** Under the floor, short of the edge so the disc looks thin from the side. */
		UNDERSIDE,
		/** The layer right under the floor. */
		SUBFLOOR,
		FLOOR,
		/** The floor's outer band. */
		FLOOR_EDGE,
		/** The crying centre of the floor. */
		HEART,
		/** The red rings and the cross between them. */
		INLAY,
		/** The low wall on the floor's edge. */
		RIM,
		PILLAR,
		PILLAR_LIGHT
	}

	/** The part of the block at ({@code x}, floor + {@code dy}, {@code z}), the arena centred on x = z = 0. */
	public static Role roleAt(int x, int dy, int z) {
		double d = Math.sqrt(x * x + z * z);
		if (d > RADIUS + 0.5) {
			return Role.AIR;
		}
		if (dy == -2) {
			return d > RADIUS - 1.5 ? Role.AIR : Role.UNDERSIDE;
		}
		if (dy == -1) {
			return Role.SUBFLOOR;
		}
		if (dy == 0) {
			if (d > RADIUS - 1) {
				return Role.FLOOR_EDGE;
			}
			// The heart first, then the rings and the cross between them.
			if (d < 1.5) {
				return Role.HEART;
			}
			if (Math.abs(d - (RADIUS - 3)) < 0.6 || Math.abs(d - 6) < 0.6 || (d < 6 && (x == 0 || z == 0))) {
				return Role.INLAY;
			}
			return Role.FLOOR;
		}
		if (dy == 1 && d > RADIUS - 1) {
			return Role.RIM;
		}
		if (dy >= 1 && dy <= PILLAR_HEIGHT && isPillar(x, z)) {
			return dy == PILLAR_HEIGHT ? Role.PILLAR_LIGHT : Role.PILLAR;
		}
		return Role.AIR;
	}

	/** Whether a pillar stands on column ({@code x}, {@code z}): {@link #PILLARS} spread evenly on a ring of {@link #PILLAR_RADIUS}. */
	public static boolean isPillar(int x, int z) {
		for (int i = 0; i < PILLARS; i++) {
			double angle = Math.PI * 2 * i / PILLARS;
			if (x == (int) Math.round(Math.cos(angle) * PILLAR_RADIUS) && z == (int) Math.round(Math.sin(angle) * PILLAR_RADIUS)) {
				return true;
			}
		}
		return false;
	}

	private VampireArenaLayout() {}
}
