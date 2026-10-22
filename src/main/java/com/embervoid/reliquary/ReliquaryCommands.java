package com.embervoid.reliquary;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Operator-only debug commands under {@code /embervoid reliquary <player>}. Output is not translated on purpose.
 * <ul>
 *   <li>{@code /embervoid reliquary <player>}: show the slots</li>
 *   <li>{@code /embervoid reliquary <player> open}: open the screen for them</li>
 *   <li>{@code /embervoid reliquary <player> clear}: hand every accessory back to their inventory</li>
 * </ul>
 */
final class ReliquaryCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2))
						.then(literal("reliquary")
								.then(argument("player", EntityArgumentType.player())
										.executes(context -> show(context, target(context)))
										.then(literal("open").executes(context -> {
											ReliquaryManager.open(target(context));
											return 1;
										}))
										.then(literal("clear").executes(context -> {
											ServerPlayerEntity player = target(context);
											for (ItemStack stack : ReliquaryManager.takeAll(player)) {
												if (!stack.isEmpty()) {
													player.getInventory().offerOrDrop(stack);
												}
											}
											return show(context, player);
										}))))));
	}

	private static ServerPlayerEntity target(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		return EntityArgumentType.getPlayer(context, "player");
	}

	private static int show(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
		StringBuilder text = new StringBuilder(player.getGameProfile().getName()).append(" reliquary:");
		Reliquary reliquary = ReliquaryManager.get(player);
		for (ReliquaryManager.SlotRef ref : ReliquaryManager.slots()) {
			ItemStack stack = reliquary.get(ref.kind(), ref.slot());
			text.append("\n  ").append(ref.kind().name()).append(' ').append(ref.slot() + 1).append(": ")
					.append(stack.isEmpty() ? "-" : stack.getItem().toString());
		}
		context.getSource().sendFeedback(() -> Text.literal(text.toString()), false);
		return 1;
	}

	private ReliquaryCommands() {}
}
