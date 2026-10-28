package com.embervoid.reach.mob;

/**
 * A Hollow Sentinel's shield (free of game classes, JUnit-tested): raised while it has a target, lowered without one,
 * hanging open for {@value #OPEN_TICKS} ticks after a bash. Raised, it turns melee and projectiles from its front
 * half. See {@link HollowSentinelEntity}.
 */
public final class SentinelShield {
	public static final int OPEN_TICKS = 30;

	public enum Stance {
		LOWERED, GUARD, OPEN
	}

	/** The stance after one tick in {@code stance}; {@code openTicksLeft} is what is left of an open shield. */
	public static Stance next(Stance stance, int openTicksLeft, boolean hasTarget) {
		if (stance == Stance.OPEN) {
			return openTicksLeft <= 0 ? Stance.LOWERED : Stance.OPEN;
		}
		return hasTarget ? Stance.GUARD : Stance.LOWERED;
	}

	/**
	 * Whether the shield turns a hit: not while open, not damage that bypasses shields; only melee or a projectile, and
	 * only from the front half, the hit's horizontal offset ({@code towardX}, {@code towardZ}) against its facing
	 * ({@code facingX}, {@code facingZ}). A hit from right on top of it counts as the front.
	 */
	public static boolean blocks(boolean open, boolean bypassesShield, boolean meleeOrProjectile,
			double towardX, double towardZ, double facingX, double facingZ) {
		if (open || bypassesShield || !meleeOrProjectile) {
			return false;
		}
		double lengthSquared = towardX * towardX + towardZ * towardZ;
		return lengthSquared < 1.0E-4 || towardX * facingX + towardZ * facingZ >= 0;
	}

	private SentinelShield() {}
}
