package com.embervoid.hub;

import com.embervoid.sanctum.SanctumEntities;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid hub}. Output is not translated on purpose. */
final class HubCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("hub")
						// /embervoid hub: to the hub's landing (the only way in until tasks 15.2 and 15.4).
						.executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
							if (!Hub.sendToLanding(player)) {
								context.getSource().sendError(Text.literal("Hub dimension missing"));
								return 0;
							}
							return 1;
						})
						// /embervoid hub check: is the placed hub whole (server check)?
						.then(literal("check").executes(context -> {
							ServerWorld hub = Hub.world(context.getSource().getServer());
							if (hub == null) {
								context.getSource().sendError(Text.literal("Hub dimension missing"));
								return 0;
							}
							String wrong = Hub.verify(hub);
							HubState state = HubState.get(hub);
							int maidens = hub.getEntitiesByType(SanctumEntities.ASHEN_MAIDEN, Hub.area(), maiden -> true).size();
							String loaded = ", " + maidens + " Maiden(s) loaded";
							if (wrong != null) {
								context.getSource().sendError(Text.literal("Hub broken: " + wrong + loaded));
								return 0;
							}
							context.getSource().sendFeedback(() -> Text.literal("Hub intact, placed at tick " + state.placedAt
									+ loaded), false);
							return 1;
						})))));
	}

	private HubCommands() {}
}
