package com.embervoid.moon;

import com.embervoid.moon.api.MoonNight;

/**
 * When moon nights happen (DESIGN §3.3), as pure functions of the overworld's time of day.
 *
 * <p>A day starts at sunrise (time of day 0 mod 24000), like vanilla's moon phase: day {@code d} has moon phase
 * {@code d % 8} and its night is that day's evening to the next sunrise. Lunar cycle {@code c = d / 8}. Events come in
 * cycles {@code c ≥ 1} with {@code c % interval == 0}: the full moon on the cycle's first day (phase 0), the blood
 * new moon four days later (phase 4). So with the default interval 3 the first full moon of a world (day 0) never
 * counts; the first events are the full moon of day 24 and the blood new moon of day 28.
 */
public final class MoonSchedule {
	public static final long DAY_TICKS = 24_000;
	/** Time of day (within the day) when a moon night starts: dusk, as monsters start to spawn. */
	public static final int NIGHT_START = 13_000;
	/** Time of day when a moon night ends at dawn; a held night stops just before it. */
	public static final int NIGHT_END = 23_000;
	public static final int DAYS_PER_CYCLE = 8;
	public static final int NEW_MOON_DAY = 4;

	/** Day number of a time of day (negative times count back from day 0). */
	public static long day(long timeOfDay) {
		return Math.floorDiv(timeOfDay, DAY_TICKS);
	}

	/** Ticks since the day's sunrise, 0–23999. */
	public static int timeInDay(long timeOfDay) {
		return (int) Math.floorMod(timeOfDay, DAY_TICKS);
	}

	/** Whether a time of day lies in the night part of its day ({@link #NIGHT_START} up to {@link #NIGHT_END}). */
	public static boolean isNight(long timeOfDay) {
		int t = timeInDay(timeOfDay);
		return t >= NIGHT_START && t < NIGHT_END;
	}

	/** The moon night that day {@code day}'s night is, with events every {@code interval} lunar cycles. */
	public static MoonNight nightOf(long day, int interval) {
		if (day < 0 || interval < 1) {
			return MoonNight.NONE;
		}
		long cycle = day / DAYS_PER_CYCLE;
		if (cycle < 1 || cycle % interval != 0) {
			return MoonNight.NONE;
		}
		return switch ((int) (day % DAYS_PER_CYCLE)) {
			case 0 -> MoonNight.FULL_MOON;
			case NEW_MOON_DAY -> MoonNight.BLOOD_NEW_MOON;
			default -> MoonNight.NONE;
		};
	}

	/** The first day at or after {@code fromDay} whose night is a moon night, for status output. */
	public static long nextEventDay(long fromDay, int interval) {
		long day = Math.max(0, fromDay);
		// Within one full period of the schedule there is always an event.
		for (long limit = day + (long) DAYS_PER_CYCLE * (interval + 1); day <= limit; day++) {
			if (nightOf(day, interval).isEvent()) {
				return day;
			}
		}
		return -1;
	}

	/** Time of day of the dawn that ends day {@code day}'s night. */
	public static long dawnOf(long day) {
		return day * DAY_TICKS + NIGHT_END;
	}

	private MoonSchedule() {}
}
