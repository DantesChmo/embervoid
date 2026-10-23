package com.embervoid.reach.gear;

/** Weapon numbers, free of game classes (JUnit-tested). */
public final class WeaponRules {
	/** Vanilla treats a swing at more than 90 % charge as a full ("strong") attack. */
	public static final float FULL_CHARGE = 0.9f;

	/** Vanilla's damage factor for a swing at {@code charge} (0–1) of the attack cooldown. */
	public static float chargeFactor(float charge) {
		return 0.2f + charge * charge * 0.8f;
	}

	public static boolean fullyCharged(float charge) {
		return charge > FULL_CHARGE;
	}

	/**
	 * Damage multiplier from Kindling: {@code share} of the hit scales with KIN at {@code perPoint} per effective point
	 * above 10 (the ember part of a weapon; 1 for pure ember damage).
	 */
	public static float kindlingMultiplier(double kindlingPoints, float perPoint, float share) {
		return (float) Math.max(0, 1 + share * kindlingPoints * perPoint);
	}

	/**
	 * Whether a point at {@code forward} blocks ahead along the attacker's horizontal look and {@code side} blocks to
	 * the side lies in a wave {@code length} long and {@code width} wide that starts at the attacker.
	 */
	public static boolean inWave(double forward, double side, double length, double width) {
		return forward >= 0 && forward <= length && Math.abs(side) <= width / 2;
	}

	/**
	 * Start speed that slides a mob about {@code blocks} along the ground: vanilla ground friction keeps 0.546 of the
	 * speed each tick, so the distance covered is {@code speed / (1 − 0.546)}.
	 */
	public static double slideSpeed(double blocks) {
		return blocks * (1 - 0.546);
	}

	/**
	 * The Rapier's ramp: damage multiplier of a hit after {@code hitsBefore} full-charge hits in a row on the same
	 * target, {@code perHit} more for each, at most {@code max} more.
	 */
	public static float rampMultiplier(int hitsBefore, float perHit, float max) {
		return 1 + Math.min(Math.max(0, hitsBefore) * perHit, max);
	}

	/** Whether a hit {@code gapTicks} after the last one on the same target keeps the ramp ({@code window} ticks). */
	public static boolean keepsRamp(long gapTicks, int window) {
		return gapTicks >= 0 && gapTicks <= window;
	}

	/** A vanilla crossbow launches arrows at this speed (blocks per tick); a firework goes slower. */
	public static final float CROSSBOW_ARROW_SPEED = 3.15f;

	/**
	 * The base damage that makes a bolt deal {@code boltDamage} when it hits at {@code speed}: vanilla deals
	 * ceil(speed × base), so the base aims half a point low to stay on {@code boltDamage} through the launch's
	 * small random spread.
	 */
	public static double boltBaseDamage(float boltDamage, float speed) {
		return Math.max(0, boltDamage - 0.5) / speed;
	}

	private WeaponRules() {}
}
