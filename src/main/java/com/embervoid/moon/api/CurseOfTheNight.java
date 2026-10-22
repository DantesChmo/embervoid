package com.embervoid.moon.api;

import com.embervoid.moon.CurseManager;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Curse of the Night (DESIGN §3.3, PLAN 8.5): stacks a player gathers by dying to hostile mobs (players, falls,
 * drowning, suffocation, lava and the void do not count); they reset when the full moon ends. Server side.
 */
public final class CurseOfTheNight {
	/** The player's current number of curse stacks (0 if none, and always 0 on the client). */
	public static int stacks(PlayerEntity player) {
		return CurseManager.stacks(player);
	}

	private CurseOfTheNight() {}
}
