package com.embervoid.stats;

/**
 * Implemented on players by {@code PlayerEntityMixin}: the health value read from the save, before vanilla clamped
 * it to the max health of a player that has no stat modifiers yet.
 */
public interface LoadedHealth {
	/** The saved health, or a negative value if none was read (or it was already taken). Resets it. */
	float embervoid$takeLoadedHealth();
}
