package com.embervoid.command;

import com.embervoid.entity.AshRemnantEntity;
import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.StaminaManager;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.embervoid.player.XpUtil;
import com.embervoid.stats.DerivedStats;
import com.embervoid.stats.PlayerStats;
import com.embervoid.stats.Stat;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /embervoid}. Output is not translated on purpose. */
public final class ModCommands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2))
						.then(literal("data")
								.executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
									context.getSource().sendFeedback(
											() -> Text.literal("[server] " + PlayerDataManager.get(player)), false);
									return 1;
								})
								// /embervoid data flaskChargeUpgrades <n>: set a test value and sync it.
								.then(literal("flaskChargeUpgrades")
										.then(argument("value", IntegerArgumentType.integer(0))
												.executes(context -> {
													ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
													PlayerData data = PlayerDataManager.get(player);
													data.flaskChargeUpgrades = IntegerArgumentType.getInteger(context, "value");
													PlayerDataManager.sync(player);
													context.getSource().sendFeedback(
															() -> Text.literal("[server] " + data), false);
													return 1;
												}))))
						.then(literal("stamina")
								.executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
									context.getSource().sendFeedback(() -> Text.literal(
											"Stamina: " + StaminaManager.get(player) + "/" + StaminaManager.getMax(player)), false);
									return 1;
								})
								// /embervoid stamina spend <amount>: spend like an action would (not in creative).
								.then(literal("spend")
										.then(argument("amount", FloatArgumentType.floatArg(0))
												.executes(context -> {
													ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
													boolean spent = StaminaManager.tryConsume(player,
															FloatArgumentType.getFloat(context, "amount"));
													context.getSource().sendFeedback(() -> Text.literal(
															(spent ? "Spent, stamina: " : "Out of stamina: ")
																	+ StaminaManager.get(player) + "/" + StaminaManager.getMax(player)), false);
													return spent ? 1 : 0;
												})))
								.then(literal("set")
										.then(argument("value", FloatArgumentType.floatArg(0))
												.executes(context -> {
													ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
													StaminaManager.set(player, FloatArgumentType.getFloat(context, "value"));
													context.getSource().sendFeedback(() -> Text.literal(
															"Stamina: " + StaminaManager.get(player) + "/" + StaminaManager.getMax(player)), false);
													return 1;
												}))))
						.then(literal("stats")
								.executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
									context.getSource().sendFeedback(() -> Text.literal(describeStats(player)), false);
									return 1;
								}))
						// /embervoid levelup <stat>: level up like the Maiden would, paying XP.
						.then(literal("levelup")
								.then(argument("stat", StringArgumentType.word()).suggests(ModCommands::suggestStats)
										.executes(context -> {
											ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
											Stat stat = parseStat(context);
											int cost = PlayerStats.levelUpCost(player);
											PlayerStats.LevelUpResult result = PlayerStats.levelUp(player, stat);
											context.getSource().sendFeedback(() -> Text.literal(result + " (cost " + cost + " XP points): "
													+ stat.id() + " = " + PlayerStats.get(player, stat) + ", level " + PlayerStats.level(player)
													+ ", XP points left " + XpUtil.getPoints(player)), false);
											return result == PlayerStats.LevelUpResult.SUCCESS ? 1 : 0;
										})))
						// /embervoid setstat <stat> <value>: set a stat for free.
						.then(literal("setstat")
								.then(argument("stat", StringArgumentType.word()).suggests(ModCommands::suggestStats)
										.then(argument("value", IntegerArgumentType.integer(Stat.MIN, 999))
												.executes(context -> {
													ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
													Stat stat = parseStat(context);
													PlayerStats.set(player, stat, IntegerArgumentType.getInteger(context, "value"));
													context.getSource().sendFeedback(() -> Text.literal(describeStats(player)), false);
													return 1;
												}))))
						// /embervoid remnant: move your items and XP into an Ash Remnant at your feet (like a death).
						.then(literal("remnant")
								.executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
									AshRemnantEntity.retireCurrent(player);
									AshRemnantEntity remnant = AshRemnantEntity.create(player, player.getPos(), true, true);
									context.getSource().sendFeedback(() -> Text.literal("Ash Remnant " + remnant.getUuid()
											+ " at " + remnant.getBlockPos().toShortString()), false);
									return 1;
								}))));
	}

	private static final DynamicCommandExceptionType UNKNOWN_STAT =
			new DynamicCommandExceptionType(id -> Text.literal("Unknown stat: " + id));

	private static Stat parseStat(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String id = StringArgumentType.getString(context, "stat");
		Stat stat = Stat.byId(id);
		if (stat == null) {
			throw UNKNOWN_STAT.create(id);
		}
		return stat;
	}

	private static CompletableFuture<Suggestions> suggestStats(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(Arrays.stream(Stat.values()).map(Stat::id), builder);
	}

	private static String describeStats(ServerPlayerEntity player) {
		StringBuilder text = new StringBuilder("Level " + PlayerStats.level(player) + " (next: "
				+ PlayerStats.levelUpCost(player) + " XP points, you have " + XpUtil.getPoints(player) + ")\n");
		for (Stat stat : Stat.values()) {
			text.append(stat.shortId().toUpperCase(Locale.ROOT)).append(' ').append(PlayerStats.get(player, stat)).append("  ");
		}
		DerivedStats d = PlayerStats.derived(player);
		text.append(String.format(Locale.ROOT, "%nHP %.1f, stamina %.0f (+%.3f/t), knockback res %.3f, attack speed %+.1f%%,"
						+ " toughness %.1f, luck %.1f, fire -%.0f%%, flask x%.2f, STR dmg %+.0f%%, DEX dmg %+.0f%%",
				d.maxHealth(), d.maxStamina(), d.staminaRegenPerTick(), d.knockbackResistance(), d.attackSpeedBonus() * 100,
				d.armorToughness(), d.luck(), d.fireDamageReduction() * 100, d.flaskHealMultiplier(),
				d.strengthDamageBonus() * 100, d.dexterityDamageBonus() * 100));
		return text.toString();
	}

	private ModCommands() {}
}
