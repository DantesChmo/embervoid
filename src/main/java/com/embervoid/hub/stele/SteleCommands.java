package com.embervoid.hub.stele;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid stele}. Output is not translated on purpose. */
final class SteleCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("stele")
						// /embervoid stele: the world's lit stele (server check).
						.executes(context -> {
							SteleState.Lit lit = SteleTravel.lit(context.getSource().getServer());
							context.getSource().sendFeedback(() -> Text.literal(lit == null ? "No lit stele"
									: "Lit stele: " + lit.dimension().getValue() + " "
									+ lit.frame().at(SteleFrame.CRACK, 0).toShortString() + " along " + lit.frame().axis()), false);
							return lit == null ? 0 : 1;
						})
						// /embervoid stele kindle <pos>: kindles the cold stele there as a Crying Soul would, with no player.
						.then(literal("kindle").then(argument("pos", BlockPosArgumentType.blockPos()).executes(context -> {
							ServerWorld world = context.getSource().getWorld();
							BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
							if (!CleftStele.canKindleIn(world)) {
								context.getSource().sendError(Text.literal("Steles do not burn in " + world.getRegistryKey().getValue()));
								return 0;
							}
							if (!CleftStele.tryKindle(world, pos, null)) {
								context.getSource().sendError(Text.literal("No cold stele at " + pos.toShortString()));
								return 0;
							}
							context.getSource().sendFeedback(() -> Text.literal("Kindled the stele at " + pos.toShortString()), false);
							return 1;
						}))))));
	}

	private SteleCommands() {}
}
