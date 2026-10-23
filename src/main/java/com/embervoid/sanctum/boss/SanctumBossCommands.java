package com.embervoid.sanctum.boss;

import com.embervoid.boss.ArenaManager;
import com.embervoid.sanctum.SanctumArena;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.structure.Structure;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid sanctum}. Output is not translated on purpose. */
final class SanctumBossCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("sanctum").then(literal("arena")
						// /embervoid sanctum arena <x> <z>: the boss arena of the Ruined Sanctum that starts in that chunk
						// (where /locate points), its centre and whether it is cleared (server check).
						.then(argument("x", IntegerArgumentType.integer()).then(argument("z", IntegerArgumentType.integer()).executes(context -> {
							ServerWorld world = context.getSource().getWorld();
							int x = IntegerArgumentType.getInteger(context, "x");
							int z = IntegerArgumentType.getInteger(context, "z");
							Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(SanctumArena.STRUCTURE);
							StructureStart start = world.getChunk(x >> 4, z >> 4, ChunkStatus.STRUCTURE_STARTS).getStructureStart(structure);
							StructurePiece arena = start == null ? null : start.getChildren().stream()
									.filter(piece -> SanctumArena.isPiece(piece, SanctumArena.TEMPLATE)).findFirst().orElse(null);
							if (arena == null) {
								context.getSource().sendError(Text.literal("No Ruined Sanctum arena starts at " + x + " " + z));
								return 0;
							}
							BlockPos center = SanctumBossArena.center(arena);
							boolean cleared = SanctumBossArena.isCleared(world, center);
							boolean fighting = ArenaManager.fightAt(world, center) != null;
							context.getSource().sendFeedback(() -> Text.literal("Sanctum arena at " + center.toShortString().replace(",", "")
									+ ", " + (cleared ? "cleared" : "not cleared") + (fighting ? ", fight running" : "")), false);
							return 1;
						})))))));
	}

	private SanctumBossCommands() {}
}
