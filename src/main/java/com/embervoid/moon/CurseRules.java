package com.embervoid.moon;

/** Curse of the Night rules (see {@link CurseManager}) on plain values. */
final class CurseRules {
	/** Who dealt a death blow, as far as the curse cares; for a projectile, its owner. */
	enum Attacker {
		/** No attacking entity: a fall, drowning, lava, the void. */
		NONE,
		PLAYER,
		/** A hostile mob or an arena boss. */
		HOSTILE,
		/** Any other entity, such as a wolf or an iron golem. */
		OTHER
	}

	/** Whether a death adds a stack: only a hostile attacker counts. */
	static boolean countsForCurse(Attacker attacker) {
		return attacker == Attacker.HOSTILE;
	}

	/** Stacks that count now: those gathered in an older curse epoch are void. */
	static int stacks(int savedStacks, long savedEpoch, long currentEpoch) {
		return savedEpoch == currentEpoch ? savedStacks : 0;
	}

	private CurseRules() {}
}
