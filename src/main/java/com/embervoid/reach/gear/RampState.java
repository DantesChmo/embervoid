package com.embervoid.reach.gear;

import java.util.UUID;

/**
 * The Royal Ash Rapier's ramp on one player (free of game classes, JUnit-tested): {@code hits} full-charge hits in a
 * row on {@code target}, the last at tick {@code lastTick}. See {@link RapierRamp}.
 */
public record RampState(UUID target, int hits, long lastTick) {
	/** Full-charge hits in a row on {@code target} that still count at tick {@code now} ({@code window} ticks apart at most). */
	public int hitsOn(UUID target, long now, int window) {
		if (!this.target.equals(target) || !WeaponRules.keepsRamp(now - lastTick, window)) {
			return 0;
		}
		return hits;
	}

	/**
	 * The ramp after a hit on {@code target} at tick {@code now} and {@code charge}: a full-charge hit builds it (and
	 * starts it over on another target or after the window), a lesser one leaves it as it was. {@code state} may be null
	 * for no ramp yet.
	 */
	public static RampState onHit(RampState state, long now, float charge, UUID target, int window) {
		if (!WeaponRules.fullyCharged(charge)) {
			return state;
		}
		int before = state == null ? 0 : state.hitsOn(target, now, window);
		return new RampState(target, before + 1, now);
	}
}
