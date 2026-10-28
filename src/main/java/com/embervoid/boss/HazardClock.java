package com.embervoid.boss;

/**
 * The timing of a hazard that lasts {@code duration} ticks: what it does every few ticks happens on its first tick and
 * every {@code n} ticks after, and it is over after its last tick (a hazard always gets at least one).
 */
public final class HazardClock {
	private final int duration;
	private int age;

	public HazardClock(int duration) {
		this.duration = duration;
	}

	/** Ticks gone since the hazard started. */
	public int age() {
		return age;
	}

	/** Whether this tick is one of every {@code n} (the first included). */
	public boolean every(int n) {
		return age % n == 0;
	}

	/** Ends this tick; returns whether the hazard lasts into the next. */
	public boolean advance() {
		return ++age < duration;
	}
}
