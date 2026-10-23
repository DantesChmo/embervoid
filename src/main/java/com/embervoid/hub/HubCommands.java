package com.embervoid.hub;

import com.embervoid.sanctum.SanctumEntities;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid hub}. Output is not translated on purpose. */
final class HubCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("hub")
						// /embervoid hub: to the hub's landing, remembering the way back (the way in until task 15.4).
						.executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
							if (!HubTravel.enter(player)) {
								context.getSource().sendError(Text.literal("Hub dimension missing"));
								return 0;
							}
							return 1;
						})
						// /embervoid hub leave: the hub's way out with no lit stele, until task 15.4 builds the stele's.
						.then(literal("leave").executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
							HubTravel.Exit exit = HubTravel.lastOverworldExit(player);
							boolean recorded = exit != null && context.getSource().getServer().getWorld(exit.dimension()) != null;
							HubTravel.leave(player);
							context.getSource().sendFeedback(() -> Text.literal(!recorded ? "No recorded exit: to the bed or world spawn"
									: "To " + exit.dimension().getValue() + " " + BlockPos.ofFloored(exit.pos()).toShortString()), false);
							return 1;
						}))
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
