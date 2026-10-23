package com.embervoid.progress;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Operator-only debug commands under {@code /embervoid progress <player>}. Output is not translated on purpose.
 * <ul>
 *   <li>{@code /embervoid progress <player>}: show</li>
 *   <li>{@code /embervoid progress <player> reset}</li>
 *   <li>{@code /embervoid progress <player> unlock | complete | boss <id>}</li>
 * </ul>
 */
final class ProgressCommands {
	@FunctionalInterface
	private interface IdAction {
		boolean apply(ServerPlayerEntity player, Identifier id);
	}

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2))
						.then(literal("progress")
								.then(argument("player", EntityArgumentType.player())
										.executes(context -> show(context, target(context)))
										.then(literal("reset").executes(context -> {
											ServerPlayerEntity player = target(context);
											ProgressManager.reset(player);
											return show(context, player);
										}))
										.then(idCommand("unlock", ProgressManager::unlockRealm))
										.then(idCommand("complete", ProgressManager::completeRealm))
										.then(idCommand("boss", ProgressManager::markBossBeaten))))));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> idCommand(String name, IdAction action) {
		return literal(name).then(argument("id", IdentifierArgumentType.identifier()).executes(context -> {
			ServerPlayerEntity player = target(context);
			action.apply(player, IdentifierArgumentType.getIdentifier(context, "id"));
			return show(context, player);
		}));
	}

	private static ServerPlayerEntity target(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		return EntityArgumentType.getPlayer(context, "player");
	}

	private static int show(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
		context.getSource().sendFeedback(() -> Text.literal(
				"[server] " + player.getName().getString() + ": " + ProgressManager.get(player)), false);
		return 1;
	}

	private ProgressCommands() {}
}
