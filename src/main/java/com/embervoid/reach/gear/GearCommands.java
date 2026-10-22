package com.embervoid.reach.gear;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid gear}. Output is not translated on purpose. */
final class GearCommands {
	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("gear")
						// /embervoid gear bleed <targets> [stacks]: adds Bleeding levels as that many hits would.
						.then(literal("bleed").then(argument("targets", EntityArgumentType.entities())
								.executes(context -> bleed(context.getSource(), EntityArgumentType.getEntities(context, "targets"), 1))
								.then(argument("stacks", IntegerArgumentType.integer(1, 10))
										.executes(context -> bleed(context.getSource(), EntityArgumentType.getEntities(context, "targets"),
												IntegerArgumentType.getInteger(context, "stacks")))))))));
	}

	private static int bleed(ServerCommandSource source, Collection<? extends Entity> targets, int stacks) {
		int count = 0;
		for (Entity entity : targets) {
			if (entity instanceof LivingEntity living && Bleeding.apply(living, stacks, source.getEntity() instanceof LivingEntity l ? l : null)) {
				count++;
			}
		}
		int bled = count;
		source.sendFeedback(() -> Text.literal(bled + " target(s) bleeding (immune ones skipped)"), false);
		return count;
	}

	private GearCommands() {}
}
