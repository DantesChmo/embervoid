package com.embervoid.reach.mob;

/** Where realm mobs may spawn, as far as light and cover go, free of game classes (JUnit-tested). See {@link ReachMobs}. */
public final class SpawnRules {
	/** Torch light above this stops realm spawns. */
	public static final int MAX_BLOCK_LIGHT = 7;

	/** No torch light at the spot; sky light does not matter (eternal dusk). */
	public static boolean darkEnough(int blockLight) {
		return blockLight <= MAX_BLOCK_LIGHT;
	}

	/** Under a roof: a natural spawn needs the sky hidden; spawners, eggs and commands do not care. */
	public static boolean underCover(boolean naturalSpawn, boolean skyVisible) {
		return !naturalSpawn || !skyVisible;
	}

	private SpawnRules() {}
}
