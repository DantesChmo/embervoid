package com.embervoid.reach.boss;

/** Rules every realm boss shares, free of game classes (JUnit-tested). See {@link ReachBoss}. */
public final class BossRules {
	/** Ticks between two flinches. */
	public static final int HURT_COOLDOWN = 10;

	/**
	 * Whether a hit that did {@code amount} damage at {@code age} plays a flinch: only a real hit on a living boss,
	 * never over a windup (the tell must stay readable), and not within {@value #HURT_COOLDOWN} ticks of the last one
	 * at {@code lastFlinch}.
	 */
	public static boolean flinches(boolean hurt, float amount, boolean alive, boolean windingUp, int age, int lastFlinch) {
		return hurt && amount > 0 && alive && !windingUp && age - lastFlinch > HURT_COOLDOWN;
	}

	/** Whether a hit of {@code amount} is heavy enough to stagger a boss of {@code maxHealth}: {@code share} of it or more. */
	public static boolean staggers(float amount, float maxHealth, float share) {
		return amount >= maxHealth * share;
	}

	/**
	 * Whether a boss stands in light: block light at its feet or two blocks up ({@code feetLight}, {@code headLight})
	 * reaches {@code litLevel}. Sky light does not count, the arenas' darkness is the point.
	 */
	public static boolean lit(int feetLight, int headLight, int litLevel) {
		return Math.max(feetLight, headLight) >= litLevel;
	}

	private BossRules() {}
}
