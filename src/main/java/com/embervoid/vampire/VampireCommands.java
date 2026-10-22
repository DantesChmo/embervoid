package com.embervoid.vampire;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid vampire}. Output is not translated on purpose. */
final class VampireCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("vampire")
						// /embervoid vampire status: stage, timer, loaded Vampires.
						.then(literal("status").executes(context -> {
							VampireState state = VampireState.get(context.getSource().getServer());
							int loaded = context.getSource().getServer().getOverworld()
									.getEntitiesByType(VampireEntities.VAMPIRE, VampireNight::belongs).size();
							context.getSource().sendFeedback(() -> Text.literal("night #" + state.nightId + ", " + state.stage
									+ ", turns on its own in " + state.provokeTicks / 20 + " s, " + loaded + " Vampire(s) loaded"), false);
							return 1;
						}))
						// /embervoid vampire provoke: the nearest Vampire turns now, as if the timer ran out.
						.then(literal("provoke").executes(context -> {
							VampireState state = VampireState.get(context.getSource().getServer());
							if (state.stage != VampireState.Stage.GROUPS) {
								context.getSource().sendError(Text.literal("No friendly Vampires tonight"));
								return 0;
							}
							state.provokeTicks = 1;
							return 1;
						})))));
	}

	private VampireCommands() {}
}
