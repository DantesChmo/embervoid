package com.embervoid.moon;

import com.embervoid.Embervoid;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import com.embervoid.moon.api.MoonNights.EndReason;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Runs the moon-night state machine on the overworld's clock: starts a scheduled night at dusk, ends it at dawn
 * (unless held), on the last release or after the real-time fallback. Behind {@link MoonNights}.
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
		boolean nightOver = day != state.nightDay || !MoonSchedule.isNight(time);
		if (nightOver && state.holds.isEmpty()) {
			end(server, state, EndReason.DAWN);
		}
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
		MoonNights.NIGHT_STARTED.invoker().onNightStarted(server, night);
	}

	private static void end(MinecraftServer server, MoonNightState state, EndReason reason) {
		MoonNight night = state.night;
		state.night = MoonNight.NONE;
		state.lastEndedDay = state.nightDay;
		state.elapsedTicks = 0;
		state.holds.clear();
		state.markDirty();
		Embervoid.LOGGER.info("Moon night ended: {} ({})", night.id(), reason.name().toLowerCase());
		MoonNights.NIGHT_ENDED.invoker().onNightEnded(server, night, reason);
	}

	private MoonNightManager() {}
}
