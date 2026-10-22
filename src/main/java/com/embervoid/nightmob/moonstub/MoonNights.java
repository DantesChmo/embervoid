package com.embervoid.nightmob.moonstub;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Stub of {@code moon.api.MoonNights} (same signatures) until track-moon's API is on main; delete it then. No night
 * is ever on here, so only the debug command spawns a wave.
 */
public final class MoonNights {
	public enum EndReason {
		DAWN,
		GOAL_MET,
		TIMEOUT,
		COMMAND
	}

	public static final Event<NightStarted> NIGHT_STARTED = EventFactory.createArrayBacked(NightStarted.class,
			listeners -> (server, night) -> {
				for (NightStarted listener : listeners) {
					listener.onNightStarted(server, night);
				}
			});

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

	public static MoonNight current(MinecraftServer server) {
		return MoonNight.NONE;
	}

	public static boolean hold(MinecraftServer server, Identifier id) {
		return false;
	}

	public static boolean release(MinecraftServer server, Identifier id) {
		return false;
	}

	public static Set<Identifier> holds(MinecraftServer server) {
		return Set.of();
	}

	private MoonNights() {}
}
