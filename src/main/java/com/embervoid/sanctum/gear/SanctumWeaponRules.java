package com.embervoid.sanctum.gear;

/** Where the Sanctum weapons' specials reach, free of game classes (JUnit-tested). */
public final class SanctumWeaponRules {
	/**
	 * Whether a target at horizontal offset ({@code forward}, {@code side}) from the player, measured along and across
	 * the player's horizontal look, lies in an arc {@code arcDegrees} wide and {@code range} blocks long in front.
	 */
	public static boolean inArc(double forward, double side, double range, double arcDegrees) {
		double distanceSq = forward * forward + side * side;
		if (distanceSq > range * range) {
			return false;
		}
		if (distanceSq < 1.0E-6) {
			// Standing inside the player: in front by any measure.
			return true;
		}
		return forward / Math.sqrt(distanceSq) >= Math.cos(Math.toRadians(arcDegrees / 2));
	}

	/** Whether a target at horizontal offset ({@code dx}, {@code dz}) from the player lies in a ring of {@code radius}. */
	public static boolean inRing(double dx, double dz, double radius) {
		return dx * dx + dz * dz <= radius * radius;
	}

	private SanctumWeaponRules() {}
}
