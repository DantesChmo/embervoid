package com.embervoid.reach.boss;

/**
 * The Grieving Tree's lanterns as a set (free of game classes, JUnit-tested): breaking the last hanging lantern stuns
 * the tree for {@value #STUN_TICKS} ticks and lifts the fog as long; once the stun is over and no lantern hangs, the
 * whole set hangs again after {@value #REHANG_DELAY} ticks. The tree keeps the lantern entities and tells the set how
 * many still hang.
 */
public final class LanternSet {
	public static final int LANTERNS = 7;
	public static final int P2_LANTERNS = 6;
	/** The stun the last broken lantern buys: the window to hit the trunk. */
	public static final int STUN_TICKS = 200;
	public static final int FOG_LIFT_TICKS = STUN_TICKS;
	/** Ticks after the stun until the whole set of lanterns hangs again at once. */
	public static final int REHANG_DELAY = 40;

	/** What a broken lantern does to the tree. */
	public enum Break {
		/** Others still hang: the tree only flinches. */
		FLINCH,
		/** It was the last of the set: the tree is stunned. */
		STUN
	}

	/** What changed in one tick. */
	public record Tick(boolean stunEnded, boolean fogReturns, boolean rehang) {}

	private int stunTicks;
	private int fogLiftTicks;
	private int rehangTimer = REHANG_DELAY;

	/** Lanterns in a full set: {@value #LANTERNS} while rooted, {@value #P2_LANTERNS} once it walks. */
	public static int size(boolean rooted) {
		return rooted ? LANTERNS : P2_LANTERNS;
	}

	/**
	 * A lantern broke with {@code stillHanging} others left. The last one stuns the tree and, when {@code liftsFog}
	 * (there is a fight with fog), lifts the fog for {@value #FOG_LIFT_TICKS} ticks.
	 */
	public Break onBroken(int stillHanging, boolean liftsFog) {
		if (stillHanging > 0) {
			return Break.FLINCH;
		}
		stunTicks = STUN_TICKS;
		if (liftsFog) {
			fogLiftTicks = FOG_LIFT_TICKS;
		}
		return Break.STUN;
	}

	/** One tick with {@code hanging} lanterns on the branches. */
	public Tick tick(int hanging) {
		boolean stunEnded = stunTicks > 0 && --stunTicks == 0;
		boolean fogReturns = fogLiftTicks > 0 && --fogLiftTicks == 0;
		boolean rehang = false;
		if (hanging > 0 || isStunned()) {
			rehangTimer = REHANG_DELAY;
		} else if (--rehangTimer <= 0) {
			rehangTimer = REHANG_DELAY;
			rehang = true;
		}
		return new Tick(stunEnded, fogReturns, rehang);
	}

	/** The stun ends at once (the tree uproots). The fog lift runs on. */
	public void endStun() {
		stunTicks = 0;
	}

	public boolean isStunned() {
		return stunTicks > 0;
	}

	public int stunTicks() {
		return stunTicks;
	}
}
