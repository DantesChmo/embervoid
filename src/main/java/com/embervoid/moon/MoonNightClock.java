package com.embervoid.moon;

import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights.EndReason;

/**
 * The moon-night state machine as pure functions of the saved state, the overworld's time of day and the config;
 * {@link MoonNightManager} carries out what they decide.
 */
final class MoonNightClock {
	/** What one server tick does to the moon night. */
	sealed interface Step {
		record Idle() implements Step {}

		record Start(MoonNight night, long day) implements Step {}

		/** The night is held: the clock goes back to {@code timeOfDay}. */
		record HoldClock(long timeOfDay) implements Step {}

		record End(EndReason reason) implements Step {}
	}

	static final Step IDLE = new Step.Idle();

	/**
	 * The step for the tick at {@code timeOfDay}. {@code state.elapsedTicks} must already count this tick of a night
	 * that is on.
	 */
	static Step tick(MoonNightState state, long timeOfDay, MoonConfig config) {
		long day = MoonSchedule.day(timeOfDay);
		if (state.night == MoonNight.NONE) {
			MoonNight scheduled = MoonSchedule.nightOf(day, config.cycleInterval());
			if (config.enabled() && scheduled.isEvent() && MoonSchedule.isNight(timeOfDay) && day != state.lastEndedDay) {
				return new Step.Start(scheduled, day);
			}
			return IDLE;
		}
		if (state.elapsedTicks >= config.fallbackTicks()) {
			return new Step.End(EndReason.TIMEOUT);
		}
		if (!state.holds.isEmpty()) {
			// The manager runs after the worlds ticked, so the clock is set two ticks short: the next tick's advance
			// then reaches dawn - 1, never dawn itself. Also undoes a /time set past it.
			long heldTime = MoonSchedule.dawnOf(state.nightDay) - 2;
			return timeOfDay > heldTime ? new Step.HoldClock(heldTime) : IDLE;
		}
		if (day != state.nightDay || !MoonSchedule.isNight(timeOfDay)) {
			return new Step.End(EndReason.DAWN);
		}
		return IDLE;
	}

	/**
	 * Whether beds refuse at {@code timeOfDay}: during a moon night, and on the evening before a scheduled one (vanilla
	 * lets players lie down a little before dusk, which would skip the night).
	 */
	static boolean sleepBlocked(MoonNightState state, long timeOfDay, MoonConfig config) {
		if (state.night.isEvent()) {
			return true;
		}
		long day = MoonSchedule.day(timeOfDay);
		return config.enabled() && day != state.lastEndedDay
				&& MoonSchedule.nightOf(day, config.cycleInterval()).isEvent()
				&& MoonSchedule.timeInDay(timeOfDay) >= MoonSchedule.EVENING;
	}

	private MoonNightClock() {}
}
