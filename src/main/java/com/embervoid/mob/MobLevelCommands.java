package com.embervoid.mob;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Operator-only debug commands: {@code /embervoid moblevel <targets>} shows levels (and the level a new spawn would
 * get at the current position, providers included), {@code /embervoid moblevel <targets> <level>} overrides them.
 * Not translated.
 */
final class MobLevelCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2))
						.then(literal("moblevel")
								.then(argument("targets", EntityArgumentType.entities())
										.executes(MobLevelCommands::show)
										.then(argument("level", IntegerArgumentType.integer(MobLevelRules.MIN_LEVEL, MobLevelRules.MAX_LEVEL))
												.executes(MobLevelCommands::set))))));
	}

	private static int show(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		int count = 0;
		for (Entity entity : EntityArgumentType.getEntities(context, "targets")) {
			String rules = entity instanceof MobEntity mob && MobLevels.canHaveLevel(mob)
					? ", at spawn here " + MobLevels.spawnLevel(mob, (ServerWorld) mob.getWorld()) : "";
			String health = entity instanceof LivingEntity living
					? String.format(", HP %.1f/%.1f", living.getHealth(), living.getMaxHealth()) : "";
			String line = entity.getName().getString() + ": level " + MobLevels.getLevel(entity) + rules + health;
			context.getSource().sendFeedback(() -> Text.literal(line), false);
			count++;
		}
		return count;
	}

	private static int set(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		int level = IntegerArgumentType.getInteger(context, "level");
		int count = 0;
		for (Entity entity : EntityArgumentType.getEntities(context, "targets")) {
			if (entity instanceof LivingEntity living) {
				MobLevels.setLevel(living, level);
				count++;
			}
		}
		int changed = count;
		context.getSource().sendFeedback(() -> Text.literal("Set level " + level + " on " + changed + " entities"), true);
		return count;
	}

	private MobLevelCommands() {}
}
