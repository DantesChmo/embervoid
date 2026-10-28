package com.embervoid.reach.boss;

import org.jetbrains.annotations.Nullable;

/** The Ash Colossus's hit zones and move timing, free of game classes (JUnit-tested). See {@link AshColossusEntity}. */
public final class ColossusRules {
	public static final float FRONT_DAMAGE = 0.5f;
	public static final float BACK_DAMAGE = 1.5f;
	public static final float CORE_DAMAGE = 2.0f;

	public static final int SMASH_COOLDOWN = 30;
	public static final int SMASH_COOLDOWN_P2 = 20;
	public static final int MELEE_COOLDOWN = 32;
	public static final int MELEE_COOLDOWN_P2 = 22;
	public static final int RANGED_COOLDOWN = 50;
	public static final int RANGED_COOLDOWN_P2 = 35;
	public static final int FAR_COOLDOWN = 60;
	public static final int FAR_COOLDOWN_P2 = 40;

	/** Reach of the fist and the drag, and the band of the leap and the boulder beyond it. */
	public static final double MELEE_RANGE = 5.5;
	public static final double RANGED_MIN = 8;
	public static final double RANGED_MAX = 22;

	/** Where a hit lands on it, seen from above. */
	public enum Zone {
		/** Within ±60° of its facing. */
		FRONT,
		SIDE,
		/** Beyond ±120°: the core. */
		BACK
	}

	/** The kinds of move, each with its own cooldown. */
	public enum Move {
		/** Smashes the pillar its target hides behind. */
		SMASH,
		/** Fist or arm drag. */
		MELEE,
		/** Leap or boulder. */
		RANGED,
		/** A boulder at a target beyond the leap's reach. */
		FAR
	}

	/**
	 * The zone a hit from horizontal offset ({@code towardX}, {@code towardZ}) lands in when it faces along
	 * ({@code facingX}, {@code facingZ}), a unit vector. A hit from right on top of it counts as the side.
	 */
	public static Zone zone(double facingX, double facingZ, double towardX, double towardZ) {
		double lengthSquared = towardX * towardX + towardZ * towardZ;
		if (lengthSquared < 1e-4) {
			return Zone.SIDE;
		}
		double cos = (facingX * towardX + facingZ * towardZ) / Math.sqrt(lengthSquared);
		if (cos >= 0.5) {
			return Zone.FRONT;
		}
		if (cos <= -0.5) {
			return Zone.BACK;
		}
		return Zone.SIDE;
	}

	/** Damage factor of a hit into {@code zone}; the back takes more once the core is bare (P2). */
	public static float damageFactor(Zone zone, boolean coreExposed) {
		return switch (zone) {
			case FRONT -> FRONT_DAMAGE;
			case SIDE -> 1;
			case BACK -> coreExposed ? CORE_DAMAGE : BACK_DAMAGE;
		};
	}

	/**
	 * The kind of move for a target at {@code squaredDistance}, or null to walk on: a pillar it hides behind within
	 * reach is smashed first; a target behind cover out of reach is walked up to, unless it stands in melee or leap
	 * range.
	 */
	@Nullable
	public static Move pick(double squaredDistance, boolean hasCover, boolean coverInReach) {
		if (hasCover && coverInReach) {
			return Move.SMASH;
		}
		if (squaredDistance <= MELEE_RANGE * MELEE_RANGE) {
			return Move.MELEE;
		}
		if (squaredDistance >= RANGED_MIN * RANGED_MIN && squaredDistance <= RANGED_MAX * RANGED_MAX) {
			return Move.RANGED;
		}
		if (hasCover) {
			return null;
		}
		return squaredDistance > RANGED_MAX * RANGED_MAX ? Move.FAR : null;
	}

	/** Ticks until the next move after {@code move}; shorter in P2. */
	public static int cooldown(Move move, boolean p2) {
		return switch (move) {
			case SMASH -> p2 ? SMASH_COOLDOWN_P2 : SMASH_COOLDOWN;
			case MELEE -> p2 ? MELEE_COOLDOWN_P2 : MELEE_COOLDOWN;
			case RANGED -> p2 ? RANGED_COOLDOWN_P2 : RANGED_COOLDOWN;
			case FAR -> p2 ? FAR_COOLDOWN_P2 : FAR_COOLDOWN;
		};
	}

	private ColossusRules() {}
}
