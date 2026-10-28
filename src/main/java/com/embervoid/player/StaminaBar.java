package com.embervoid.player;

/**
 * What the stamina bar shows: whether it is drawn, how much of it is filled and whether its frame flashes
 * after a failed spend. Pure logic, no rendering: the client's {@code StaminaHud} draws from it.
 */
public final class StaminaBar {
	/** Ticks the frame flashes after a spend failed for lack of stamina. */
	public static final int FLASH_TICKS = 10;

	/**
	 * @param filled   filled pixels of the inner width
	 * @param flashOn  the frame is red this tick (it blinks every 2 ticks while flashing)
	 */
	public record Look(boolean visible, int filled, boolean flashOn) {}

	/**
	 * @param current         stamina now (clamped to {@code [0, max]})
	 * @param ticksSinceFail  world ticks since the last failed spend (negative or large when none)
	 * @param hideWhenFull    client option {@code staminaBar.hideWhenFull}
	 * @param innerWidth      pixels between the frame's sides
	 */
	public static Look look(float current, float max, long ticksSinceFail, boolean hideWhenFull, int innerWidth) {
		float clamped = Math.max(0, Math.min(max, current));
		boolean flashing = ticksSinceFail >= 0 && ticksSinceFail < FLASH_TICKS;
		if (clamped >= max && !flashing && hideWhenFull) {
			return new Look(false, 0, false);
		}
		int filled = max <= 0 ? 0 : Math.round(innerWidth * clamped / max);
		return new Look(true, filled, flashing && (ticksSinceFail / 2) % 2 == 0);
	}

	private StaminaBar() {}
}
