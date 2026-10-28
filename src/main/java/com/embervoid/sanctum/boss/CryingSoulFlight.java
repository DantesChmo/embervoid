package com.embervoid.sanctum.boss;

import net.minecraft.util.math.Vec3d;

/**
 * What a crying soul is doing ({@link CryingSoulEntity}): circling the Sister, winding up a dive, diving, dazed where
 * it landed, flying back. Holds the state, how long it has lasted and the dive's line; the entity moves and draws.
 */
public final class CryingSoulFlight {
	public static final int DIVE_WINDUP = 20;
	public static final int DIVE_TICKS = 8;
	public static final int DAZED_TICKS = 40;
	/** How close to its place on the circle a returning soul counts as back. */
	public static final double ARRIVE_DISTANCE = 0.6;

	public enum State { CIRCLING, WINDUP, DIVING, DAZED, RETURNING }

	private State state = State.CIRCLING;
	private int ticks;
	private Vec3d diveFrom = Vec3d.ZERO;
	private Vec3d diveTo = Vec3d.ZERO;
	private float diveProgress;
	private boolean struck;

	public State state() {
		return state;
	}

	public int ticks() {
		return ticks;
	}

	/** A circling soul starts winding up a dive; false (and nothing changes) in any other state. */
	public boolean startWindup() {
		if (state != State.CIRCLING) {
			return false;
		}
		enter(State.WINDUP);
		return true;
	}

	/** The windup is over: the soul dives from {@code from} to {@code to}, not having struck anyone yet. */
	public void dive(Vec3d from, Vec3d to) {
		diveFrom = from;
		diveTo = to;
		diveProgress = 0;
		struck = false;
		enter(State.DIVING);
	}

	/**
	 * One tick: returns the state the soul acts in this tick, and moves on to the next state when this one is over.
	 * A dive lasts {@link #DIVE_TICKS}, the daze {@link #DAZED_TICKS}; a returning soul is back on the circle once
	 * {@code distanceToOrbit} is under {@link #ARRIVE_DISTANCE}. A windup ends only by {@link #dive}.
	 */
	public State tick(double distanceToOrbit) {
		ticks++;
		State acting = state;
		switch (state) {
			case DIVING -> {
				diveProgress = Math.min(1, ticks / (float) DIVE_TICKS);
				if (ticks >= DIVE_TICKS) {
					enter(State.DAZED);
				}
			}
			case DAZED -> {
				if (ticks >= DAZED_TICKS) {
					enter(State.RETURNING);
				}
			}
			case RETURNING -> {
				if (distanceToOrbit < ARRIVE_DISTANCE) {
					enter(State.CIRCLING);
				}
			}
			default -> {
			}
		}
		return acting;
	}

	/** Where the dive has got to: from its start ({@code 0}) to where it lands ({@code 1}). */
	public Vec3d divePosition() {
		return diveFrom.lerp(diveTo, diveProgress);
	}

	public Vec3d diveDirection() {
		return diveTo.subtract(diveFrom);
	}

	/** Where the dive lands, and so where the soul hangs dazed. */
	public Vec3d diveTarget() {
		return diveTo;
	}

	/** Whether this dive has hit a player already: a dive hits once. */
	public boolean hasStruck() {
		return struck;
	}

	public void markStruck() {
		struck = true;
	}

	private void enter(State next) {
		state = next;
		ticks = 0;
	}
}
