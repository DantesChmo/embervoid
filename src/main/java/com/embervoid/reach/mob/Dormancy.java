package com.embervoid.reach.mob;

import java.util.function.BooleanSupplier;

/**
 * A Bark Hollow's sleep and wake (free of game classes, JUnit-tested): dormant until woken, then
 * {@value #AWAKEN_TICKS} ticks creaking awake, then awake; awake with nothing to fight for {@value #SETTLE_TICKS}
 * ticks, back at its home, it goes dormant again. The Hollow keeps its stage in its synced state and tells this the
 * stage each tick. See {@link BarkHollowEntity}.
 */
public final class Dormancy {
	public static final int AWAKEN_TICKS = 20;
	public static final int SETTLE_TICKS = 300;

	public enum Stage {
		DORMANT, AWAKENING, AWAKE
	}

	/** What one tick asks of the Hollow. */
	public enum Change {
		NONE,
		/** A player came close: it wakes, turning on them. */
		WAKE,
		/** The creaking is over: it fights. */
		AWAKE,
		/** Nothing to fight for long enough, at home: it goes dormant. */
		SETTLE
	}

	private int awakenTicks;
	private int idleTicks;

	/** It went dormant: the idle count starts over. */
	public void slept() {
		idleTicks = 0;
	}

	/** It started waking: the creaking takes {@value #AWAKEN_TICKS} ticks. */
	public void woke() {
		awakenTicks = AWAKEN_TICKS;
	}

	/**
	 * One tick in {@code stage}. {@code playerNear}: a player within wake range (asked only while dormant).
	 * {@code fighting}: it has a living target. {@code atHome}: it stands at its home or has none (asked only once it
	 * has idled long enough).
	 */
	public Change tick(Stage stage, BooleanSupplier playerNear, boolean fighting, BooleanSupplier atHome) {
		switch (stage) {
			case DORMANT -> {
				return playerNear.getAsBoolean() ? Change.WAKE : Change.NONE;
			}
			case AWAKENING -> {
				return --awakenTicks <= 0 ? Change.AWAKE : Change.NONE;
			}
			default -> {
				if (fighting) {
					idleTicks = 0;
					return Change.NONE;
				}
				return ++idleTicks >= SETTLE_TICKS && atHome.getAsBoolean() ? Change.SETTLE : Change.NONE;
			}
		}
	}
}
