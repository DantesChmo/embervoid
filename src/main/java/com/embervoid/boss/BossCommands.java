package com.embervoid.boss;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid boss}. Output is not translated on purpose. */
public final class BossCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("boss")
						// /embervoid boss fights: running fights.
						.then(literal("fights").executes(context -> {
							int count = 0;
							for (BossFight fight : ArenaManager.fights()) {
								if (!fight.isEnded()) {
									context.getSource().sendFeedback(() -> Text.literal(fight.world().getRegistryKey().getValue() + " " + fight), false);
									count++;
								}
							}
							int total = count;
							context.getSource().sendFeedback(() -> Text.literal(total + " fight(s)"), false);
							return total;
						})))));
	}

	private BossCommands() {}
}
