package com.embervoid.vampire.moonstub;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Local stand-in for track-moon's {@code moon.api.MoonNights} (PLAN 8.1) with the same signatures, so the vampire
 * track compiles and can be tried before the real API is merged. Nights only start and stop by command
 * ({@code /embervoid vampire stubnight start full|blood | stop}); the last released hold ends the night at once.
 * Delete this package after syncing with {@code main}.
 */
public final class MoonNights {
	public enum EndReason {DAWN, GOAL_MET, TIMEOUT, COMMAND}

	@FunctionalInterface
	public interface NightStarted {
		void onNightStarted(MinecraftServer server, MoonNight night);
	}

	@FunctionalInterface
	public interface NightEnded {
		void onNightEnded(MinecraftServer server, MoonNight night, EndReason reason);
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

	private static MoonNight current = MoonNight.NONE;
	private static final Set<Identifier> HOLDS = new HashSet<>();

	public static MoonNight current(MinecraftServer server) {
		return current;
	}

	public static void hold(MinecraftServer server, Identifier id) {
		if (current != MoonNight.NONE) {
			HOLDS.add(id);
		}
	}

	public static void release(MinecraftServer server, Identifier id) {
		if (HOLDS.remove(id) && HOLDS.isEmpty()) {
			end(server, EndReason.GOAL_MET);
		}
	}

	private static void start(MinecraftServer server, MoonNight night) {
		end(server, EndReason.COMMAND);
		current = night;
		NIGHT_STARTED.invoker().onNightStarted(server, night);
	}

	private static void end(MinecraftServer server, EndReason reason) {
		if (current == MoonNight.NONE) {
			return;
		}
		MoonNight night = current;
		current = MoonNight.NONE;
		HOLDS.clear();
		NIGHT_ENDED.invoker().onNightEnded(server, night, reason);
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			current = MoonNight.NONE;
			HOLDS.clear();
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("vampire").then(literal("stubnight")
						.then(literal("start").then(argument("night", StringArgumentType.word()).executes(context -> {
							MoonNight night = StringArgumentType.getString(context, "night").equals("full")
									? MoonNight.FULL_MOON : MoonNight.BLOOD_NEW_MOON;
							start(context.getSource().getServer(), night);
							context.getSource().sendFeedback(() -> Text.literal("[stub] night started: " + night), true);
							return 1;
						})))
						.then(literal("stop").executes(context -> {
							end(context.getSource().getServer(), EndReason.COMMAND);
							context.getSource().sendFeedback(() -> Text.literal("[stub] night stopped"), true);
							return 1;
						}))
						.then(literal("status").executes(context -> {
							context.getSource().sendFeedback(() -> Text.literal("[stub] " + current + ", holds " + HOLDS), false);
							return 1;
						}))))));
	}

	private MoonNights() {}
}
