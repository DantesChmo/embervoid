package com.embervoid.moon;

import com.embervoid.moon.api.MoonNight;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Operator-only debug commands under {@code /embervoid moon}. Output is not translated on purpose.
 * <ul>
 *   <li>{@code status}: the current night, the day, holds and the next scheduled event</li>
 *   <li>{@code start full | blood}: start that night now (moves the clock to dusk if it is day)</li>
 *   <li>{@code stop}: end the current night</li>
 *   <li>{@code setcycle <cycle>}: set the clock to the morning of that lunar cycle's full moon day</li>
 * </ul>
 */
final class MoonCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2))
						.then(literal("moon")
								.then(literal("status").executes(MoonCommands::status))
								.then(literal("start")
										.then(literal("full").executes(context -> start(context, MoonNight.FULL_MOON)))
										.then(literal("blood").executes(context -> start(context, MoonNight.BLOOD_NEW_MOON))))
								.then(literal("stop").executes(context -> {
									boolean stopped = MoonNightManager.forceStop(context.getSource().getServer());
									context.getSource().sendFeedback(() -> Text.literal(
											stopped ? "[server] Moon night stopped" : "[server] No moon night is on"), true);
									return stopped ? 1 : 0;
								}))
								.then(literal("setcycle")
										.then(argument("cycle", IntegerArgumentType.integer(0)).executes(context -> {
											int cycle = IntegerArgumentType.getInteger(context, "cycle");
											ServerWorld overworld = context.getSource().getServer().getOverworld();
											overworld.setTimeOfDay(
													(long) cycle * MoonSchedule.DAYS_PER_CYCLE * MoonSchedule.DAY_TICKS + 1_000);
											context.getSource().sendFeedback(() -> Text.literal(
													"[server] Clock set to the morning of cycle " + cycle + " (day "
															+ cycle * MoonSchedule.DAYS_PER_CYCLE + ")"), true);
											return 1;
										}))))));
	}

	private static int start(CommandContext<ServerCommandSource> context, MoonNight night) {
		MoonNightManager.forceStart(context.getSource().getServer(), night);
		context.getSource().sendFeedback(() -> Text.literal("[server] Moon night started: " + night.id()), true);
		return 1;
	}

	private static int status(CommandContext<ServerCommandSource> context) {
		MinecraftServer server = context.getSource().getServer();
		MoonNightState state = MoonNightState.get(server);
		MoonConfig config = MoonConfig.get();
		long time = server.getOverworld().getTimeOfDay();
		long day = MoonSchedule.day(time);
		StringBuilder out = new StringBuilder("[server] Moon: ").append(state.night().id());
		if (state.night().isEvent()) {
			out.append(" (day ").append(state.nightDay())
					.append(", ").append(state.elapsedTicks() / 20).append(" s of ")
					.append(config.fallbackTicks() / 20).append(" s, holds ").append(state.holds()).append(')');
		}
		out.append("\nDay ").append(day).append(" (phase ").append(Math.floorMod(day, MoonSchedule.DAYS_PER_CYCLE))
				.append(", cycle ").append(Math.floorDiv(day, MoonSchedule.DAYS_PER_CYCLE))
				.append("), time ").append(MoonSchedule.timeInDay(time))
				.append("; next event on day ").append(MoonSchedule.nextEventDay(day, config.cycleInterval()))
				.append(": ").append(MoonSchedule.nightOf(MoonSchedule.nextEventDay(day, config.cycleInterval()),
						config.cycleInterval()).id());
		if (!config.enabled()) {
			out.append(" (moon.enabled is false)");
		}
		String text = out.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	private MoonCommands() {}
}
