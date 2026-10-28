package com.embervoid.moon.api;

import com.embervoid.moon.MoonNightManager;
import com.embervoid.moon.MoonNightState;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * The {@code moon} API: the current moon night of the overworld, events when one starts and
 * ends, and holds that keep a night going until its goal is met. Server side only.
 *
 * <p>A moon night runs from dusk to dawn. While any {@link #hold} is open it does not end at dawn (time stops just
 * before it); releasing the last hold ends it with {@link EndReason#GOAL_MET}. The {@code moon.fallbackMinutes}
 * real-time fallback ends it anyway ({@link EndReason#TIMEOUT}). Holds are saved with the world and cleared when
 * the night ends, so an owner that keeps its own state across restarts can check {@link #holds} on server start.
 */
public final class MoonNights {
	public enum EndReason {
		/** Dawn came with no hold open. */
		DAWN,
		/** The last hold was released. */
		GOAL_MET,
		/** The real-time fallback ran out. */
		TIMEOUT,
		/** An operator stopped or replaced it. */
		COMMAND
	}

	/** Fired on the server right after a moon night started; {@link #current} already returns it. */
	public static final Event<NightStarted> NIGHT_STARTED = EventFactory.createArrayBacked(NightStarted.class,
			listeners -> (server, night) -> {
				for (NightStarted listener : listeners) {
					listener.onNightStarted(server, night);
				}
			});

	/** Fired on the server right after a moon night ended; {@link #current} already returns NONE. */
	public static final Event<NightEnded> NIGHT_ENDED = EventFactory.createArrayBacked(NightEnded.class,
			listeners -> (server, night, reason) -> {
				for (NightEnded listener : listeners) {
					listener.onNightEnded(server, night, reason);
				}
			});

	@FunctionalInterface
	public interface NightStarted {
		void onNightStarted(MinecraftServer server, MoonNight night);
	}

	@FunctionalInterface
	public interface NightEnded {
		void onNightEnded(MinecraftServer server, MoonNight night, EndReason reason);
	}

	/** The moon night that is on now, or {@link MoonNight#NONE}. */
	public static MoonNight current(MinecraftServer server) {
		return MoonNightState.get(server).night();
	}

	/**
	 * Keeps the current moon night going until {@link #release} with the same id (e.g. {@code embervoid:wave/<uuid>}).
	 * Returns false (and holds nothing) when no moon night is on.
	 */
	public static boolean hold(MinecraftServer server, Identifier id) {
		return MoonNightManager.hold(server, id);
	}

	/**
	 * Releases a hold. When it was the last one, the night ends with {@link EndReason#GOAL_MET}. Releasing an id that
	 * holds nothing does nothing. Returns whether the hold was open.
	 */
	public static boolean release(MinecraftServer server, Identifier id) {
		return MoonNightManager.release(server, id);
	}

	public static Set<Identifier> holds(MinecraftServer server) {
		return MoonNightState.get(server).holds();
	}

	private MoonNights() {}
}
