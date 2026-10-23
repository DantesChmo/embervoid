package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import com.google.gson.JsonElement;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * {@code /embervoid sanctum survey <radius> [<seed> [<spacing> <separation>]]} (operator, debug; output not translated,
 * PLAN 16.2): plans the Ruined Sanctums around the command's position as worldgen would, on this world's seed or
 * another, with the structure set's spacing or a trial one, without generating a chunk. Reports how many start within
 * the radius, of which variant, the biomes of the spots refused, and how far the land of that circle (where the
 * dungeon's biomes are) is from the nearest one. The server check runs it on two seeds.
 */
final class SanctumSurvey {
	/** The structure set's id ({@code worldgen/structure_set/ember_sanctum.json}). */
	private static final Identifier SET = Embervoid.id("ember_sanctum");
	/** Grid step of the points whose nearest dungeon is measured, in blocks. */
	static final int SAMPLE_STEP = 32;

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("embervoid").requires(source -> source.hasPermissionLevel(2)).then(literal("sanctum").then(literal("survey")
						.then(argument("radius", IntegerArgumentType.integer(64, 4096))
								.executes(context -> survey(context, null, 0, 0))
								.then(argument("seed", LongArgumentType.longArg())
										.executes(context -> survey(context, LongArgumentType.getLong(context, "seed"), 0, 0))
										.then(argument("spacing", IntegerArgumentType.integer(2, 4096))
												.then(argument("separation", IntegerArgumentType.integer(0, 4095))
														.executes(context -> survey(context, LongArgumentType.getLong(context, "seed"),
																IntegerArgumentType.getInteger(context, "spacing"),
																IntegerArgumentType.getInteger(context, "separation")))))))))));
	}

	private static int survey(CommandContext<ServerCommandSource> context, @Nullable Long seedArgument, int spacing, int separation) {
		ServerCommandSource source = context.getSource();
		ServerWorld world = source.getWorld();
		int radius = IntegerArgumentType.getInteger(context, "radius");
		DynamicRegistryManager registries = world.getRegistryManager();
		Structure structure = registries.get(RegistryKeys.STRUCTURE).get(SanctumArena.STRUCTURE);
		StructureSet set = registries.get(RegistryKeys.STRUCTURE_SET).get(SET);
		ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
		if (structure == null || set == null || !(set.placement() instanceof RandomSpreadStructurePlacement placement)) {
			source.sendError(Text.literal("No random-spread structure set " + SET));
			return 0;
		}
		if (spacing > 0) {
			if (separation >= spacing) {
				source.sendError(Text.literal("The separation must be smaller than the spacing"));
				return 0;
			}
			placement = withSpacing(placement, spacing, separation);
		}
		long seed = seedArgument != null ? seedArgument : world.getSeed();
		NoiseConfig noise = world.getChunkManager().getNoiseConfig();
		if (seed != world.getSeed()) {
			if (!(generator instanceof NoiseChunkGenerator noiseGenerator)) {
				source.sendError(Text.literal("Another seed needs a noise chunk generator"));
				return 0;
			}
			noise = NoiseConfig.create(noiseGenerator.getSettings().value(), registries.getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS), seed);
		}

		BlockPos center = BlockPos.ofFloored(source.getPosition());
		// Dungeons this far out can still be the nearest one of a point inside the circle.
		int cellBlocks = placement.getSpacing() * 16;
		int reach = radius + 2 * cellBlocks;
		int minCellX = Math.floorDiv((center.getX() - reach) >> 4, placement.getSpacing());
		int maxCellX = Math.floorDiv((center.getX() + reach) >> 4, placement.getSpacing());
		int minCellZ = Math.floorDiv((center.getZ() - reach) >> 4, placement.getSpacing());
		int maxCellZ = Math.floorDiv((center.getZ() + reach) >> 4, placement.getSpacing());
		List<BlockPos> dungeons = new ArrayList<>();
		Map<Identifier, Integer> variants = new LinkedHashMap<>();
		SanctumVariants.ALL.forEach(id -> variants.put(id, 0));
		Map<String, Integer> refused = new HashMap<>();
		int inside = 0;
		int cellsInside = 0;
		for (int cx = minCellX; cx <= maxCellX; cx++) {
			for (int cz = minCellZ; cz <= maxCellZ; cz++) {
				ChunkPos chunk = placement.getStartChunk(seed, cx * placement.getSpacing(), cz * placement.getSpacing());
				BlockPos pos = new BlockPos(chunk.getCenterX(), 0, chunk.getCenterZ());
				boolean near = horizontalDistance(pos, center) <= radius;
				cellsInside += near ? 1 : 0;
				StructureStart start = structure.createStructureStart(registries, generator, generator.getBiomeSource(), noise,
						world.getStructureTemplateManager(), seed, chunk, 0, world, structure.getValidBiomes()::contains);
				if (!start.hasChildren()) {
					if (near) {
						int y = generator.getHeightOnGround(pos.getX(), pos.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, noise);
						String biome = generator.getBiomeSource().getBiome(BiomeCoords.fromBlock(pos.getX()), BiomeCoords.fromBlock(y),
								BiomeCoords.fromBlock(pos.getZ()), noise.getMultiNoiseSampler()).getKey().map(key -> key.getValue().getPath()).orElse("?");
						refused.merge(biome, 1, Integer::sum);
					}
					continue;
				}
				dungeons.add(pos);
				if (near) {
					inside++;
					Identifier variant = SanctumVariants.of(start);
					if (variant != null) {
						variants.merge(variant, 1, Integer::sum);
					}
				}
			}
		}
		// Distances only from land a dungeon could stand on: the sea is no walk to the nearest one.
		NoiseConfig sampler = noise;
		Nearest nearest = nearest(dungeons, center, radius, SAMPLE_STEP, point -> structure.getValidBiomes().contains(generator.getBiomeSource()
				.getBiome(BiomeCoords.fromBlock(point.getX()), BiomeCoords.fromBlock(world.getSeaLevel()), BiomeCoords.fromBlock(point.getZ()),
						sampler.getMultiNoiseSampler())));
		StringBuilder kinds = new StringBuilder();
		variants.forEach((id, count) -> kinds.append(kinds.isEmpty() ? "" : ", ").append(id.getPath().substring("sanctum/".length()))
				.append(' ').append(count));
		StringBuilder biomes = new StringBuilder();
		refused.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.forEach(e -> biomes.append(biomes.isEmpty() ? "" : ", ").append(e.getKey()).append(' ').append(e.getValue()));
		String text = "Ruined Sanctums within " + radius + " of " + center.getX() + " " + center.getZ() + " on seed " + seed
				+ " (spacing " + placement.getSpacing() + ", separation " + placement.getSeparation() + "): " + inside + " ("
				+ kinds + "), " + (cellsInside - inside) + " of " + cellsInside + " spots refused (" + biomes + "); nearest from "
				+ nearest.points() + " land points (" + Math.round(nearest.share() * 100) + " % of the circle): mean " + Math.round(nearest.mean()) + ", max " + Math.round(nearest.max()) + ", "
				+ Math.round(nearest.within300() * 100) + " % within 300, " + Math.round(nearest.within400() * 100) + " % within 400";
		source.sendFeedback(() -> Text.literal(text), false);
		return inside;
	}

	/** The placement with another spacing and separation (its salt and spread type kept). */
	private static RandomSpreadStructurePlacement withSpacing(RandomSpreadStructurePlacement placement, int spacing, int separation) {
		JsonElement json = RandomSpreadStructurePlacement.CODEC.encodeStart(JsonOps.INSTANCE, placement).getOrThrow(false, Embervoid.LOGGER::error);
		json.getAsJsonObject().addProperty("spacing", spacing);
		json.getAsJsonObject().addProperty("separation", separation);
		return RandomSpreadStructurePlacement.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, Embervoid.LOGGER::error);
	}

	/**
	 * How far from the nearest dungeon the points of the circle are: a grid of {@code step} blocks over the circle of
	 * {@code radius} around {@code center}, for each point that {@code counts} its horizontal distance to the nearest of
	 * {@code dungeons}. {@link Nearest#share} is the part of the grid's points that count.
	 */
	static Nearest nearest(List<BlockPos> dungeons, BlockPos center, int radius, int step, Predicate<BlockPos> counts) {
		int all = 0;
		int points = 0;
		double sum = 0;
		double max = 0;
		int within300 = 0;
		int within400 = 0;
		for (int dx = -radius; dx <= radius; dx += step) {
			for (int dz = -radius; dz <= radius; dz += step) {
				if (dx * dx + dz * dz > radius * radius) {
					continue;
				}
				BlockPos point = center.add(dx, 0, dz);
				all++;
				if (!counts.test(point)) {
					continue;
				}
				double best = Double.POSITIVE_INFINITY;
				for (BlockPos dungeon : dungeons) {
					best = Math.min(best, horizontalDistance(point, dungeon));
				}
				points++;
				sum += best;
				max = Math.max(max, best);
				within300 += best <= 300 ? 1 : 0;
				within400 += best <= 400 ? 1 : 0;
			}
		}
		return new Nearest(points, all == 0 ? 0 : points / (double) all, points == 0 ? 0 : sum / points, max,
				points == 0 ? 0 : within300 / (double) points, points == 0 ? 0 : within400 / (double) points);
	}

	/**
	 * {@link #nearest}'s answer: the points measured and their share of the grid, the mean and largest distance, the
	 * shares within 300 and 400.
	 */
	record Nearest(int points, double share, double mean, double max, double within300, double within400) {}

	private static double horizontalDistance(BlockPos a, BlockPos b) {
		return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
	}

	private SanctumSurvey() {}
}
