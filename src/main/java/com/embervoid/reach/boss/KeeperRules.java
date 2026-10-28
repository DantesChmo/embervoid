package com.embervoid.reach.boss;

import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/** The Crypt Keeper's phases, damage taken and move choice, free of game classes (JUnit-tested). See {@link CryptKeeperEntity}. */
public final class KeeperRules {
	public static final int LIT_LEVEL = 8;
	public static final float DARK_DAMAGE = 0.5f;

	public static final int CLOSE_COOLDOWN = 28;
	public static final int CLOSE_COOLDOWN_LATE = 22;
	public static final int RANGED_COOLDOWN = 40;
	public static final int RANGED_COOLDOWN_LATE = 30;
	/** Reach of the halberd, and of the lantern throw or the thrust beyond it. */
	public static final double SWING_RANGE = 4.8;
	public static final double RANGED_RANGE = 16;
	/** Fighters this many or more within the sweep's ring always get the sweep. */
	public static final int SWEEP_CROWD = 2;

	public enum Move {
		/** The halberd combo. */
		COMBO,
		/** A ring around it. */
		SWEEP,
		/** P1: a lantern thrown at the target. */
		LANTERN_THROW,
		/** From P2, with the lantern gone: a lunging thrust. */
		THRUST
	}

	/** Phase 0 (P1): it still carries its lantern. */
	public static boolean hasLantern(int phase) {
		return phase == 0;
	}

	/** From P2 on, the dark phases: faster, and half damage outside light. */
	public static boolean late(int phase) {
		return phase > 0;
	}

	/** Damage factor of a hit: half in the dark phases unless it stands in light. */
	public static float damageFactor(int phase, boolean lit) {
		return late(phase) && !lit ? DARK_DAMAGE : 1;
	}

	/**
	 * The move for a target at {@code squaredDistance}, or null to walk on. Close, a {@code crowd} of
	 * {@value #SWEEP_CROWD} fighters in the sweep's ring or the one-in-three {@code sweepRoll} makes it sweep; the crowd
	 * is counted only close, the roll asked only when the crowd does not decide.
	 */
	@Nullable
	public static Move pick(double squaredDistance, int phase, IntSupplier crowd, BooleanSupplier sweepRoll) {
		if (squaredDistance <= SWING_RANGE * SWING_RANGE) {
			return crowd.getAsInt() >= SWEEP_CROWD || sweepRoll.getAsBoolean() ? Move.SWEEP : Move.COMBO;
		}
		if (squaredDistance <= RANGED_RANGE * RANGED_RANGE) {
			return late(phase) ? Move.THRUST : Move.LANTERN_THROW;
		}
		return null;
	}

	/** Ticks until the next move after {@code move}; shorter from P2 on. */
	public static int cooldown(Move move, int phase) {
		boolean late = late(phase);
		return switch (move) {
			case COMBO, SWEEP -> late ? CLOSE_COOLDOWN_LATE : CLOSE_COOLDOWN;
			case LANTERN_THROW, THRUST -> late ? RANGED_COOLDOWN_LATE : RANGED_COOLDOWN;
		};
	}

	private KeeperRules() {}
}
