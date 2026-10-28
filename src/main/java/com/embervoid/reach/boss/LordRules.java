package com.embervoid.reach.boss;

import org.jetbrains.annotations.Nullable;

/** The Smoldering Lord's phases, damage taken and move timing, free of game classes (JUnit-tested). See {@link SmolderingLordEntity}. */
public final class LordRules {
	public static final float SHATTERED_DAMAGE = 1.25f;
	public static final float DARK_DAMAGE = 0.5f;
	public static final int LIT_LEVEL = 8;

	public static final int COMBO_COOLDOWN = 30;
	public static final int COMBO_COOLDOWN_P2 = 22;
	public static final int LUNGE_COOLDOWN = 40;
	public static final int LUNGE_COOLDOWN_P2 = 30;
	/** Reach of the greatsword combo, and of the lunge beyond it. */
	public static final double COMBO_RANGE = 5;
	public static final double LUNGE_RANGE = 14;

	/** The three arena hazards he replays in P2, in turn. */
	public enum Hazard {
		ASH_STORM, FOG, DARKNESS;

		/** The hazard numbered {@code which}: 0 ash storm, 1 fog, anything else darkness. */
		public static Hazard of(int which) {
			return which == 0 ? ASH_STORM : which == 1 ? FOG : DARKNESS;
		}

		public Hazard next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	public enum Move {
		COMBO, LUNGE
	}

	/** Phase 0: up on the keep wall, untouchable. */
	public static boolean onWall(int phase) {
		return phase == 0;
	}

	/** From phase 2 (below half health) his armor is gone. */
	public static boolean armorShattered(int phase) {
		return phase >= 2;
	}

	/** Whether a hit touches him at all: not on the wall, not while he comes down from it. */
	public static boolean touchable(int phase, boolean descending) {
		return !onWall(phase) && !descending;
	}

	/**
	 * The damage {@code amount} becomes: more with the armor shattered, half while the darkness hazard lasts and he
	 * stands outside block light.
	 */
	public static float damageTaken(float amount, boolean armorShattered, boolean darkAndUnlit) {
		if (armorShattered) {
			amount *= SHATTERED_DAMAGE;
		}
		if (darkAndUnlit) {
			amount *= DARK_DAMAGE;
		}
		return amount;
	}

	/** The move for a target at {@code squaredDistance}, or null to walk on. */
	@Nullable
	public static Move pick(double squaredDistance) {
		if (squaredDistance <= COMBO_RANGE * COMBO_RANGE) {
			return Move.COMBO;
		}
		if (squaredDistance <= LUNGE_RANGE * LUNGE_RANGE) {
			return Move.LUNGE;
		}
		return null;
	}

	/** Ticks until the next move after {@code move}; shorter with the armor shattered. */
	public static int cooldown(Move move, boolean armorShattered) {
		return switch (move) {
			case COMBO -> armorShattered ? COMBO_COOLDOWN_P2 : COMBO_COOLDOWN;
			case LUNGE -> armorShattered ? LUNGE_COOLDOWN_P2 : LUNGE_COOLDOWN;
		};
	}

	private LordRules() {}
}
