package com.embervoid.command;

import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.StaminaManager;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid}. Output is not translated on purpose. */
public final class ModCommands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2))
						.then(literal("data")
								// /embervoid data: show the caller's player data as stored on the server.
								.executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
									context.getSource().sendFeedback(
											() -> Text.literal("[server] " + PlayerDataManager.get(player)), false);
									return 1;
								})
								// /embervoid data flaskChargeUpgrades <n>: set a test value and sync it.
								.then(literal("flaskChargeUpgrades")
										.then(argument("value", IntegerArgumentType.integer(0))
												.executes(context -> {
													ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
													PlayerData data = PlayerDataManager.get(player);
													data.flaskChargeUpgrades = IntegerArgumentType.getInteger(context, "value");
													PlayerDataManager.sync(player);
													context.getSource().sendFeedback(
															() -> Text.literal("[server] " + data), false);
													return 1;
												}))))
						.then(literal("stamina")
								// /embervoid stamina: show current/max.
								.executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
									context.getSource().sendFeedback(() -> Text.literal(
											"Stamina: " + StaminaManager.get(player) + "/" + StaminaManager.getMax()), false);
									return 1;
								})
								// /embervoid stamina spend <amount>: spend like an action would (not in creative).
								.then(literal("spend")
										.then(argument("amount", FloatArgumentType.floatArg(0))
												.executes(context -> {
													ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
													boolean spent = StaminaManager.tryConsume(player,
															FloatArgumentType.getFloat(context, "amount"));
													context.getSource().sendFeedback(() -> Text.literal(
															(spent ? "Spent, stamina: " : "Out of stamina: ")
																	+ StaminaManager.get(player) + "/" + StaminaManager.getMax()), false);
													return spent ? 1 : 0;
												})))
								// /embervoid stamina set <value>
								.then(literal("set")
										.then(argument("value", FloatArgumentType.floatArg(0))
												.executes(context -> {
													ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
													StaminaManager.set(player, FloatArgumentType.getFloat(context, "value"));
													context.getSource().sendFeedback(() -> Text.literal(
															"Stamina: " + StaminaManager.get(player) + "/" + StaminaManager.getMax()), false);
													return 1;
												}))))));
	}

	private ModCommands() {}
}
