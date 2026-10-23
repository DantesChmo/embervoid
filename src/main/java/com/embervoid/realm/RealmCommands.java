package com.embervoid.realm;

import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.realm.worldgen.RealmFeatures;
import com.embervoid.realm.worldgen.RealmRoads;
import com.embervoid.registry.ModBlocks;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid realm}. Output is not translated on purpose. */
public final class RealmCommands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("realm")
						// /embervoid realm enter <realm> [toShrine]: what a lit Void Obelisk does.
						.then(literal("enter").then(argument("realm", IdentifierArgumentType.identifier())
								.executes(context -> enter(context, false))
								.then(argument("toShrine", BoolArgumentType.bool())
										.executes(context -> enter(context, BoolArgumentType.getBool(context, "toShrine"))))))
						// /embervoid realm leave: what the return obelisk does.
						.then(literal("leave").executes(context -> {
							RealmTeleports.leaveRealm(context.getSource().getPlayerOrThrow());
							return 1;
						}))
						// /embervoid realm where: realm, zone, level band and feature at the caller's position.
						.then(literal("where").executes(RealmCommands::where))
						// /embervoid realm arenas: every arena of the current world and whether the caller beat it.
						.then(literal("arenas").executes(RealmCommands::arenas))
						// /embervoid realm roads: the roads of the current world, their milestones, lantern posts and outposts.
						.then(literal("roads").executes(RealmCommands::roads)
								// /embervoid realm roads posts: where each road's lantern posts stand.
								.then(literal("posts").executes(RealmCommands::posts)))
						// /embervoid realm data: the caller's realm travel data.
						.then(literal("data").executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
							context.getSource().sendFeedback(() -> Text.literal("[server] " + RealmPlayerData.get(player)), false);
							return 1;
						}))
				)));
	}

	private static int enter(CommandContext<ServerCommandSource> context, boolean toShrine) throws CommandSyntaxException {
		Identifier realm = IdentifierArgumentType.getIdentifier(context, "realm");
		return RealmTeleports.enterRealm(context.getSource().getPlayerOrThrow(), realm, toShrine) ? 1 : 0;
	}

	private static int where(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		RealmDefinition realm = Realms.byWorld(player.getWorld().getRegistryKey());
		if (realm == null) {
			context.getSource().sendFeedback(() -> Text.literal("Not in a realm"), false);
			return 0;
		}
		int zone = RealmMobLevels.zoneAt(player.getServerWorld(), player.getBlockPos());
		RealmDefinition.LevelBand band = RealmMobLevels.bandAt(player.getServerWorld(), player.getBlockPos());
		RealmFeatures.Placed feature = RealmFeatures.at(player.getServerWorld(), player.getBlockPos());
		double distance = Math.sqrt(player.getX() * player.getX() + player.getZ() * player.getZ());
		String zoneName = zone == RealmLayout.LANDING ? "landing" : zone == RealmLayout.ABYSS ? "abyss" : "zone " + zone;
		String text = String.format("%s %s, %s, distance %.0f, levels %s, feature %s", realm.id(), player.getWorld().getRegistryKey().getValue(),
				zoneName, distance, band == null ? "-" : band.min() + "-" + band.max(),
				feature == null ? "-" : feature.structure().kind().asString() + " " + feature.piece().center().toShortString());
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}

	private static int arenas(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		for (RealmArenas.Location arena : RealmArenas.all(player.getServerWorld())) {
			boolean beaten = ProgressManager.hasBeatenBoss(player, arena.spec().bossId());
			context.getSource().sendFeedback(() -> Text.literal(arena.spec().bossId() + " at " + arena.pos().getX() + ", "
					+ arena.pos().getZ() + " (level " + arena.spec().level() + (beaten ? ", beaten)" : ")")), false);
		}
		return 1;
	}

	/** Works from the console too ({@code execute in <realm> run …}): roads, milestones, outposts and their chests. */
	private static int roads(CommandContext<ServerCommandSource> context) {
		ServerWorld world = context.getSource().getWorld();
		List<RoadLayout.Road> roads = RealmRoads.of(world);
		StringBuilder text = new StringBuilder(roads.size() + " roads");
		for (RoadLayout.Road road : roads) {
			text.append(String.format("%nroad %d: %.0f blocks, %d milestones, %d posts (%s)", road.arena(), road.length(),
					road.milestones().size(), road.posts().size(), postStates(world, road)));
			for (RoadLayout.Outpost outpost : road.outposts()) {
				text.append(String.format(", %s at %d %d (%s)", outpost.kind(), outpost.x(), outpost.z(), outpostState(world, outpost)));
			}
		}
		context.getSource().sendFeedback(() -> Text.literal(text.toString()), false);
		return roads.size();
	}

	/** Lantern post positions, one line per road. */
	private static int posts(CommandContext<ServerCommandSource> context) {
		List<RoadLayout.Road> roads = RealmRoads.of(context.getSource().getWorld());
		StringBuilder text = new StringBuilder();
		for (RoadLayout.Road road : roads) {
			text.append(text.isEmpty() ? "" : "\n").append("road ").append(road.arena()).append(":");
			for (RoadLayout.Post post : road.posts()) {
				text.append(" ").append(post.x()).append(" ").append(post.z()).append(";");
			}
		}
		context.getSource().sendFeedback(() -> Text.literal(text.toString()), false);
		return roads.size();
	}

	/** How many of a road's posts carry their lantern ("lit"), stand without it ("dark") or are not generated yet. */
	private static String postStates(ServerWorld world, RoadLayout.Road road) {
		int lit = 0;
		int dark = 0;
		int unloaded = 0;
		for (RoadLayout.Post post : road.posts()) {
			if (!world.isChunkLoaded(post.x() >> 4, post.z() >> 4)) {
				unloaded++;
				continue;
			}
			int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, post.x(), post.z());
			boolean lantern = false;
			for (int y = top; y > top - 6; y--) {
				lantern |= world.getBlockState(new BlockPos(post.x(), y, post.z())).isOf(ModBlocks.EMBER_LANTERN);
			}
			if (lantern) {
				lit++;
			} else {
				dark++;
			}
		}
		String text = lit + " lit";
		if (dark > 0) {
			text += ", " + dark + " dark";
		}
		if (unloaded > 0) {
			text += ", " + unloaded + " unloaded";
		}
		return text;
	}

	/** "chest" when the outpost's chest is there, "missing" when its chunks are loaded without it, else "not loaded". */
	private static String outpostState(ServerWorld world, RoadLayout.Outpost outpost) {
		ChunkPos centre = new ChunkPos(new BlockPos(outpost.x(), 0, outpost.z()));
		for (int cx = -1; cx <= 1; cx++) {
			for (int cz = -1; cz <= 1; cz++) {
				if (!world.isChunkLoaded(centre.x + cx, centre.z + cz)) {
					return "not loaded";
				}
			}
		}
		for (int cx = -1; cx <= 1; cx++) {
			for (int cz = -1; cz <= 1; cz++) {
				for (BlockEntity entity : world.getChunk(centre.x + cx, centre.z + cz).getBlockEntities().values()) {
					if (entity instanceof LootableContainerBlockEntity) {
						return "chest";
					}
				}
			}
		}
		return "missing";
	}

	private RealmCommands() {}
}
