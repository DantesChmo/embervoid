package com.embervoid.moon;

import com.embervoid.Embervoid;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import com.embervoid.moon.api.MoonNights.EndReason;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Runs the moon-night state machine on the overworld's clock: starts a scheduled night at dusk, ends it at dawn
 * (while held, the clock stops just before dawn), on the last release (dawn comes at once) or after the real-time
 * fallback. Behind {@link MoonNights}.
 */
public final class MoonNightManager {
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(MoonNightManager::tick);
	}

	private static void tick(MinecraftServer server) {
		ServerWorld overworld = server.getOverworld();
		if (overworld == null) {
			return;
		}
		MoonNightState state = MoonNightState.get(server);
		MoonConfig config = MoonConfig.get();
		long time = overworld.getTimeOfDay();
		long day = MoonSchedule.day(time);

		if (state.night == MoonNight.NONE) {
			MoonNight scheduled = MoonSchedule.nightOf(day, config.cycleInterval());
			if (config.enabled() && scheduled.isEvent() && MoonSchedule.isNight(time) && day != state.lastEndedDay) {
				start(server, state, scheduled, day);
			}
			return;
		}

		state.elapsedTicks++;
		state.markDirty();
		if (state.elapsedTicks >= config.fallbackTicks()) {
			end(server, state, EndReason.TIMEOUT);
			return;
		}
		long lastHeldTime = MoonSchedule.dawnOf(state.nightDay) - 1;
		if (!state.holds.isEmpty()) {
			// Held: the clock stops just before dawn (also undoing a /time set past it).
			if (time > lastHeldTime) {
				overworld.setTimeOfDay(lastHeldTime);
			}
			return;
		}
		if (day != state.nightDay || !MoonSchedule.isNight(time)) {
			end(server, state, EndReason.DAWN);
		}
	}

	/**
	 * Whether beds refuse now: during a moon night, and on the evening before a scheduled one (vanilla lets players
	 * lie down a little before dusk, which would skip the night).
	 */
	public static boolean sleepBlocked(MinecraftServer server) {
		MoonNightState state = MoonNightState.get(server);
		if (state.night.isEvent()) {
			return true;
		}
		MoonConfig config = MoonConfig.get();
		long time = server.getOverworld().getTimeOfDay();
		long day = MoonSchedule.day(time);
		return config.enabled() && day != state.lastEndedDay
				&& MoonSchedule.nightOf(day, config.cycleInterval()).isEvent()
				&& MoonSchedule.timeInDay(time) >= MoonSchedule.EVENING;
	}

	/** Starts {@code night} now (debug), ending any current one; moves the clock to dusk if it is day. */
	public static void forceStart(MinecraftServer server, MoonNight night) {
		MoonNightState state = MoonNightState.get(server);
		if (state.night.isEvent()) {
			end(server, state, EndReason.COMMAND);
		}
		ServerWorld overworld = server.getOverworld();
		long time = overworld.getTimeOfDay();
		long day = MoonSchedule.day(time);
		if (!MoonSchedule.isNight(time)) {
			overworld.setTimeOfDay(day * MoonSchedule.DAY_TICKS + MoonSchedule.NIGHT_START);
		}
		start(server, state, night, day);
	}

	/** Ends the current night (debug). Returns false if none was on. */
	public static boolean forceStop(MinecraftServer server) {
		MoonNightState state = MoonNightState.get(server);
		if (!state.night.isEvent()) {
			return false;
		}
		end(server, state, EndReason.COMMAND);
		return true;
	}

	public static boolean hold(MinecraftServer server, Identifier id) {
		MoonNightState state = MoonNightState.get(server);
		if (!state.night.isEvent()) {
			return false;
		}
		if (state.holds.add(id)) {
			state.markDirty();
		}
		return true;
	}

	public static boolean release(MinecraftServer server, Identifier id) {
		MoonNightState state = MoonNightState.get(server);
		if (!state.holds.remove(id)) {
			return false;
		}
		state.markDirty();
		if (state.holds.isEmpty() && state.night.isEvent()) {
			end(server, state, EndReason.GOAL_MET);
		}
		return true;
	}

	private static void start(MinecraftServer server, MoonNightState state, MoonNight night, long day) {
		state.night = night;
		state.nightDay = day;
		state.elapsedTicks = 0;
		state.holds.clear();
		state.markDirty();
		Embervoid.LOGGER.info("Moon night started: {} (day {})", night.id(), day);
		for (ServerPlayerEntity player : server.getOverworld().getPlayers()) {
			if (player.isSleeping()) {
				player.wakeUp(true, true);
			}
		}
		MoonAnnouncements.started(server, night);
		MoonNights.NIGHT_STARTED.invoker().onNightStarted(server, night);
	}

	private static void end(MinecraftServer server, MoonNightState state, EndReason reason) {
		MoonNight night = state.night;
		state.night = MoonNight.NONE;
		state.lastEndedDay = state.nightDay;
		state.elapsedTicks = 0;
		state.holds.clear();
		state.markDirty();
		if (reason == EndReason.GOAL_MET) {
			// The goal is met: dawn comes now.
			ServerWorld overworld = server.getOverworld();
			long dawn = MoonSchedule.dawnOf(state.nightDay);
			if (overworld.getTimeOfDay() < dawn) {
				overworld.setTimeOfDay(dawn);
			}
		}
		Embervoid.LOGGER.info("Moon night ended: {} ({})", night.id(), reason.name().toLowerCase());
		MoonAnnouncements.ended(server, night, reason);
		MoonNights.NIGHT_ENDED.invoker().onNightEnded(server, night, reason);
	}

	private MoonNightManager() {}
}
