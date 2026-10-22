package com.embervoid.moon.api;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Curse of the Night (DESIGN §3.3, PLAN 8.5): stacks a player gathers by dying to hostile mobs; they reset when the
 * full moon ends. Server side.
 */
public final class CurseOfTheNight {
	/** The player's current number of curse stacks (0 if none). */
	public static int stacks(PlayerEntity player) {
		// Stub until PLAN 8.5.
		return 0;
	}

	private CurseOfTheNight() {}
}
