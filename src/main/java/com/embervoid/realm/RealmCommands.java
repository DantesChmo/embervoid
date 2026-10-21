package com.embervoid.realm;

import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.realm.progress.RealmProgress;
import com.embervoid.realm.worldgen.RealmFeatures;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
						// /embervoid realm data: the caller's realm travel data.
						.then(literal("data").executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
							context.getSource().sendFeedback(() -> Text.literal("[server] " + RealmPlayerData.get(player)), false);
							return 1;
						}))
						// /embervoid realm beat <bossId> and /embervoid realm complete <realm>: set progress for tests.
						.then(literal("beat").then(argument("boss", IdentifierArgumentType.identifier()).executes(context -> {
							Identifier boss = IdentifierArgumentType.getIdentifier(context, "boss");
							RealmProgress.get().markBossBeaten(context.getSource().getPlayerOrThrow(), boss);
							context.getSource().sendFeedback(() -> Text.literal("Beaten: " + boss), false);
							return 1;
						})))
						.then(literal("complete").then(argument("realm", IdentifierArgumentType.identifier()).executes(context -> {
							Identifier realm = IdentifierArgumentType.getIdentifier(context, "realm");
							RealmProgress.get().markRealmCompleted(context.getSource().getPlayerOrThrow(), realm);
							context.getSource().sendFeedback(() -> Text.literal("Completed: " + realm), false);
							return 1;
						})))
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
			boolean beaten = RealmProgress.get().hasBeatenBoss(player, arena.spec().bossId());
			context.getSource().sendFeedback(() -> Text.literal(arena.spec().bossId() + " at " + arena.pos().getX() + ", "
					+ arena.pos().getZ() + " (level " + arena.spec().level() + (beaten ? ", beaten)" : ")")), false);
		}
		return 1;
	}

	private RealmCommands() {}
}
