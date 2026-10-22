package com.embervoid.nightmob;

import com.embervoid.moon.api.MoonNights;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Operator-only debug commands for the full moon waves: {@code /embervoid wave status}, {@code /embervoid wave spawn
 * <players>} (a wave now, full moon or not; it holds the night only when one is on) and {@code /embervoid wave clear
 * <players>}. Not translated.
 */
final class WaveCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2))
						.then(literal("wave")
								.then(literal("status").executes(WaveCommands::status))
								.then(literal("spawn")
										.then(argument("players", EntityArgumentType.players()).executes(WaveCommands::spawn)))
								.then(literal("clear")
										.then(argument("players", EntityArgumentType.players()).executes(WaveCommands::clear))))));
	}

	private static int status(CommandContext<ServerCommandSource> context) {
		MinecraftServer server = context.getSource().getServer();
		WaveState state = WaveState.get(server);
		StringBuilder line = new StringBuilder("Full moon night #" + state.night + ", moon night now: "
				+ MoonNights.current(server) + ", served: " + state.served.size() + ", waves: " + state.waves.size());
		state.waves.forEach((owner, wave) -> {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(owner);
			String name = player != null ? player.getName().getString() : owner.toString();
			line.append("\n  ").append(name).append(": ").append(wave.mobs.size()).append(' ')
					.append(wave.kind.name().toLowerCase()).append(" left, hold ")
					.append(MoonNights.holds(server).contains(NightWaves.holdId(owner)) ? "open" : "none");
		});
		String text = line.toString();
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return state.waves.size();
	}

	private static int spawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		int total = 0;
		for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "players")) {
			int count = NightWaves.spawnWave(player);
			String line = player.getName().getString() + ": " + count + " wave mobs"
					+ (player.getWorld() == context.getSource().getServer().getOverworld() ? "" : " (not in the overworld)");
			context.getSource().sendFeedback(() -> Text.literal(line), true);
			total += count;
		}
		return total;
	}

	private static int clear(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		int total = 0;
		for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "players")) {
			UUID owner = player.getUuid();
			total += NightWaves.clearWave(context.getSource().getServer(), owner);
		}
		int removed = total;
		context.getSource().sendFeedback(() -> Text.literal("Removed " + removed + " wave mobs"), true);
		return total;
	}

	private WaveCommands() {}
}
