package com.embervoid.vampire;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid vampire}. Output is not translated on purpose. */
final class VampireCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("vampire")
						// /embervoid vampire status: stage, timer, loaded Vampires, the fight.
						.then(literal("status").executes(context -> {
							VampireState state = VampireState.get(context.getSource().getServer());
							int loaded = context.getSource().getServer().getOverworld()
									.getEntitiesByType(VampireEntities.VAMPIRE, VampireNight::belongs).size();
							context.getSource().sendFeedback(() -> Text.literal("night #" + state.nightId + ", " + state.stage
									+ ", turns on its own in " + state.provokeTicks / 20 + " s, " + loaded + " Vampire(s) loaded"
									+ (state.stage == VampireState.Stage.FIGHT ? ", boss Lv " + state.bossLevel + " at "
									+ state.fightCenter.toShortString() + ", " + VampireFight.fighters() + " fighter(s), "
									+ state.fallen.size() + " fallen" : "")), false);
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
						}))
						// /embervoid vampire arena open|close|check: the arena without a fight (debug, server check).
						.then(literal("arena")
								.then(literal("open").executes(context -> {
									MinecraftServer server = context.getSource().getServer();
									VampireState state = VampireState.get(server);
									if (state.stage == VampireState.Stage.FIGHT || state.stage == VampireState.Stage.CLOSING) {
										context.getSource().sendError(Text.literal("A fight is running"));
										return 0;
									}
									List<ServerPlayerEntity> pulled = VampireArena.open(server, state);
									if (pulled == null) {
										context.getSource().sendError(Text.literal("Vampire arena dimension missing"));
										return 0;
									}
									context.getSource().sendFeedback(() -> Text.literal("Vampire arena open, " + pulled.size() + " player(s) pulled in"), true);
									return 1;
								}))
								.then(literal("close").executes(context -> {
									MinecraftServer server = context.getSource().getServer();
									VampireState state = VampireState.get(server);
									if (state.stage == VampireState.Stage.FIGHT || state.stage == VampireState.Stage.CLOSING) {
										context.getSource().sendError(Text.literal("A fight is running"));
										return 0;
									}
									VampireArena.close(server, state, Map.of());
									context.getSource().sendFeedback(() -> Text.literal("Vampire arena closed"), true);
									return 1;
								}))
								.then(literal("check").executes(context -> {
									ServerWorld arena = VampireArena.world(context.getSource().getServer());
									if (arena == null) {
										context.getSource().sendError(Text.literal("Vampire arena dimension missing"));
										return 0;
									}
									String wrong = VampireArena.verify(arena);
									boolean open = VampireArena.isForced(arena);
									context.getSource().sendFeedback(() -> Text.literal("Vampire arena " + (wrong == null ? "intact" : "broken: " + wrong)
											+ ", chunks " + (open ? "forced" : "not forced")), false);
									return wrong == null ? 1 : 0;
								}))))));
	}

	private VampireCommands() {}
}
