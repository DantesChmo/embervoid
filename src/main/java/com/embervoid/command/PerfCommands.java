package com.embervoid.command;

import com.embervoid.realm.placed.PlacedEnemies;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ColumnPosArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColumnPos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Operator-only measurements under {@code /embervoid perf}, read by {@code tools/server_check.py --perf} (PLAN R40.4b).
 * Output is not translated on purpose and keeps a fixed shape the tool parses.
 *
 * <ul>
 *   <li>{@code perf ticks}: tick time over the last 100 ticks (mean, p95, max in ms).</li>
 *   <li>{@code perf chunks <from> <to>}: of the chunks in a column box of the command's dimension, how many are
 *   loaded and how many tick entities (run with {@code execute in}).</li>
 *   <li>{@code perf entities <from> <to>}: entities in that box, mobs, placed enemies, and the most common types.</li>
 * </ul>
 */
public final class PerfCommands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("perf")
						.then(literal("ticks").executes(context -> ticks(context.getSource())))
						.then(literal("chunks").then(argument("from", ColumnPosArgumentType.columnPos())
								.then(argument("to", ColumnPosArgumentType.columnPos()).executes(PerfCommands::chunks))))
						.then(literal("entities").then(argument("from", ColumnPosArgumentType.columnPos())
								.then(argument("to", ColumnPosArgumentType.columnPos()).executes(PerfCommands::entities)))))));
	}

	private static int ticks(ServerCommandSource source) {
		MinecraftServer server = source.getServer();
		long[] lengths = server.lastTickLengths.clone();
		Arrays.sort(lengths);
		double mean = Arrays.stream(lengths).average().orElse(0) / 1e6;
		double p95 = lengths[(int) Math.ceil(lengths.length * 0.95) - 1] / 1e6;
		double max = lengths[lengths.length - 1] / 1e6;
		int chunks = loadedChunks(server);
		source.sendFeedback(() -> Text.literal(String.format(Locale.ROOT, "ticks mean %.2f ms, p95 %.2f ms, max %.2f ms, chunks %d",
				mean, p95, max, chunks)), false);
		return (int) Math.round(mean * 100);
	}

	private static int loadedChunks(MinecraftServer server) {
		int count = 0;
		for (ServerWorld world : server.getWorlds()) {
			count += world.getChunkManager().getLoadedChunkCount();
		}
		return count;
	}

	private static int chunks(CommandContext<ServerCommandSource> context) {
		ServerWorld world = context.getSource().getWorld();
		ColumnPos from = ColumnPosArgumentType.getColumnPos(context, "from");
		ColumnPos to = ColumnPosArgumentType.getColumnPos(context, "to");
		int minX = Math.min(from.x(), to.x()) >> 4, maxX = Math.max(from.x(), to.x()) >> 4;
		int minZ = Math.min(from.z(), to.z()) >> 4, maxZ = Math.max(from.z(), to.z()) >> 4;
		int total = 0, loaded = 0, ticking = 0;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				total++;
				if (world.getChunkManager().isChunkLoaded(x, z)) {
					loaded++;
				}
				if (world.shouldTickEntity(new BlockPos(x << 4, 64, z << 4))) {
					ticking++;
				}
			}
		}
		int t = total, l = loaded, e = ticking;
		context.getSource().sendFeedback(() -> Text.literal("chunks " + l + "/" + t + " loaded, " + e + " ticking"), false);
		return ticking;
	}

	private static int entities(CommandContext<ServerCommandSource> context) {
		ServerWorld world = context.getSource().getWorld();
		ColumnPos from = ColumnPosArgumentType.getColumnPos(context, "from");
		ColumnPos to = ColumnPosArgumentType.getColumnPos(context, "to");
		Box box = new Box(Math.min(from.x(), to.x()), world.getBottomY(), Math.min(from.z(), to.z()),
				Math.max(from.x(), to.x()) + 1, world.getTopY(), Math.max(from.z(), to.z()) + 1);
		List<Entity> entities = world.getOtherEntities(null, box, entity -> true);
		long mobs = entities.stream().filter(entity -> entity instanceof MobEntity).count();
		long placed = entities.stream().filter(entity -> entity.getCommandTags().contains(PlacedEnemies.TAG)).count();
		Map<String, Long> byType = entities.stream().collect(Collectors.groupingBy(
				entity -> Registries.ENTITY_TYPE.getId(entity.getType()).toString(), TreeMap::new, Collectors.counting()));
		String top = byType.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
				.limit(8).map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining(", "));
		int total = entities.size();
		context.getSource().sendFeedback(() -> Text.literal("entities " + total + ", mobs " + mobs + ", placed " + placed
				+ (top.isEmpty() ? "" : ": " + top)), false);
		return total;
	}

	private PerfCommands() {}
}
