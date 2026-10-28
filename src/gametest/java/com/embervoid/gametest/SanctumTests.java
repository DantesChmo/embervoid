package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.BossFight;
import com.embervoid.entity.mob.MobEntities;
import com.embervoid.hub.stele.CleftStele;
import com.embervoid.hub.stele.SteleBlocks;
import com.embervoid.sanctum.SanctumArena;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumEntities;
import com.embervoid.sanctum.SanctumVariants;
import com.embervoid.sanctum.boss.FallenSisterEntity;
import com.embervoid.sanctum.boss.SanctumBossArena;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesList;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashMap;
import java.util.Map;

/**
 * The Ruined Sanctum as worldgen builds it, placed the way {@code /place structure} does, then its contents counted
 * inside the structure's bounding box.
 */
public final class SanctumTests implements FabricGameTest {
	/** Far from the test grid so the Sanctum never overlaps another test. */
	private static final BlockPos ORIGIN = new BlockPos(3000, 0, 3000);
	private static final BlockPos FLOODED_ORIGIN = new BlockPos(3000, 0, -3000);
	private static final BlockPos MINED_ORIGIN = new BlockPos(-3000, 0, 3000);

	/**
	 * A fresh dungeon: guards and loot in the outer halls; no Maiden, obelisks or Sanctum Flagstone; the
	 * boss wing with one Ember Shrine, the fog wall in its doorway and a cold Cleft Stele, and no marker entity.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void dungeonHasArenaInsteadOfCore(TestContext context) {
		assertDungeon(context, place(context, ORIGIN));
		context.complete();
	}

	/**
	 * Each variant, found among fresh plans and moved to its own spot: its own surface ruin and guarded hall,
	 * the same boss wing as {@link #dungeonHasArenaInsteadOfCore} checks, and a player stepping into its arena summons
	 * the Fallen Sister.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sanctum_variant_ruin", tickLimit = PlacedFight.LOAD_LIMIT + 200)
	public void ruinVariantIsWhole(TestContext context) {
		variantIsWhole(context, SanctumVariants.RUIN, "sanctum/gate_hall", new BlockPos(-3000, 0, -3000));
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sanctum_variant_chapel", tickLimit = PlacedFight.LOAD_LIMIT + 200)
	public void chapelVariantIsWhole(TestContext context) {
		variantIsWhole(context, SanctumVariants.CHAPEL, "sanctum/nave", new BlockPos(0, 0, -6000));
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sanctum_variant_cloister", tickLimit = PlacedFight.LOAD_LIMIT + 200)
	public void cloisterVariantIsWhole(TestContext context) {
		variantIsWhole(context, SanctumVariants.CLOISTER, "sanctum/cloister_walk", new BlockPos(6000, 0, -6000));
	}

	private static void variantIsWhole(TestContext context, Identifier variant, String hall, BlockPos origin) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			ServerWorld world = context.getWorld();
			StructureStart start = placeVariant(context, variant, origin);
			register(world, start);
			long halls = start.getChildren().stream().filter(piece -> SanctumArena.isPiece(piece, Embervoid.id(hall))).count();
			context.assertTrue(halls == 1, "the " + variant + " Sanctum has " + halls + " " + hall + " pieces in " + start.getChildren().size());
			assertDungeon(context, start);
			StructurePiece piece = start.getChildren().stream().filter(p -> SanctumArena.isPiece(p, SanctumArena.TEMPLATE)).findFirst().orElseThrow();
			BlockPos center = SanctumBossArena.center(piece);
			arena.forceChunks(center, 16);
			arena.await(() -> world.shouldTickEntity(center.up()), PlacedFight.LOAD_LIMIT, "the arena chunks to tick", () -> {
				arena.joinAt(Vec3d.ofBottomCenter(center.up()).add(0, 0, 3));
				arena.await(() -> ArenaManager.fightAt(world, center) != null, 40, "the summon rule to start her fight", () -> {
					BossFight fight = ArenaManager.fightAt(world, center);
					arena.track(fight);
					context.assertTrue(fight.boss() instanceof FallenSisterEntity, "the " + variant + " arena summoned " + fight.boss());
					arena.finish();
				});
			});
		});
	}

	/**
	 * A placed dungeon: guards and loot in the outer halls; no Maiden, obelisks or Sanctum Flagstone; the
	 * boss wing with one Ember Shrine, the fog wall in its doorway and a cold Cleft Stele, and no marker entity.
	 */
	private static void assertDungeon(TestContext context, StructureStart start) {
		ServerWorld world = context.getWorld();
		BlockBox box = start.getBoundingBox();
		String where = SanctumVariants.of(start) + ", " + start.getChildren().size() + " pieces in " + box;

		int[] chests = {0};
		int[] obelisks = {0};
		ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4)).forEach(pos -> {
			WorldChunk chunk = world.getChunk(pos.x, pos.z);
			for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
				if (!box.contains(blockEntity.getPos())) {
					continue;
				}
				if (blockEntity instanceof LootableContainerBlockEntity) {
					chests[0]++;
				} else if (blockEntity.getCachedState().isOf(SanctumBlocks.VOID_OBELISK)) {
					obelisks[0]++;
				}
			}
		});
		Map<Block, Integer> blocks = new HashMap<>();
		BlockPos[] stone = {null};
		BlockPos.stream(box).forEach(pos -> {
			BlockState state = world.getBlockState(pos);
			if (state.isOf(SanctumBlocks.EMBER_SHRINE) || state.isOf(SanctumBlocks.FOG_WALL)
					|| state.isOf(SanctumBlocks.SANCTUM_FLAGSTONE) || state.isOf(SteleBlocks.CLEFT_STONE)) {
				blocks.merge(state.getBlock(), 1, Integer::sum);
			}
			if (state.isOf(SteleBlocks.CLEFT_STONE) && stone[0] == null) {
				stone[0] = pos.toImmutable();
			}
		});
		Box area = Box.from(box).expand(2);
		int hollows = world.getEntitiesByType(MobEntities.HOLLOW, area, entity -> true).size();
		int maidens = world.getEntitiesByType(SanctumEntities.ASHEN_MAIDEN, area, entity -> true).size();
		// No spawn marker: she is summoned at the arena centre (FallenSisterTests).
		int markers = world.getEntitiesByType(EntityType.MARKER, area, entity -> true).size();
		long arenas = start.getChildren().stream().filter(piece -> SanctumArena.isPiece(piece, SanctumArena.TEMPLATE)).count();
		long cores = start.getChildren().stream().filter(piece -> SanctumArena.isPiece(piece, SanctumArena.OLD_CORE)).count();
		int shrines = blocks.getOrDefault(SanctumBlocks.EMBER_SHRINE, 0);
		int fog = blocks.getOrDefault(SanctumBlocks.FOG_WALL, 0);
		int flagstone = blocks.getOrDefault(SanctumBlocks.SANCTUM_FLAGSTONE, 0);
		int cleft = blocks.getOrDefault(SteleBlocks.CLEFT_STONE, 0);
		boolean coldStele = stone[0] != null && CleftStele.coldFrame(world, stone[0]).isPresent();
		String counts = where + ": arenas " + arenas + ", cores " + cores + ", chests " + chests[0] + ", obelisks "
				+ obelisks[0] + ", shrines " + shrines + ", fog " + fog + ", flagstone " + flagstone + ", cleft stone "
				+ cleft + ", cold stele " + coldStele + ", markers " + markers + ", hollows " + hollows
				+ ", maidens " + maidens;
		Embervoid.LOGGER.info("Sanctum test: {}", counts);
		context.assertTrue(arenas == 1 && cores == 0, "expected the arena piece and no core; " + counts);
		context.assertTrue(obelisks[0] == 0, "Void Obelisks in a new dungeon; " + counts);
		context.assertTrue(maidens == 0, "an Ashen Maiden in a new dungeon; " + counts);
		context.assertTrue(flagstone == 0, "Sanctum Flagstone (it attunes) in a new dungeon; " + counts);
		context.assertTrue(shrines == 1, "expected one Ember Shrine, at the boss door; " + counts);
		context.assertTrue(fog == 9, "expected a 3x3 fog wall in the arena doorway; " + counts);
		context.assertTrue(cleft == 13 && coldStele, "expected a whole cold Cleft Stele circle; " + counts);
		context.assertTrue(markers == 0, "a marker entity in the dungeon; " + counts);
		context.assertTrue(hollows >= 1, "no Hollow guard (the gate hall always has one); " + counts);
		context.assertTrue(chests[0] >= 1, "no loot chests; " + counts);
	}

	/**
	 * The arena's stele cannot be mined in survival while it stands in the dungeon (the dungeon registered in its
	 * chunks as worldgen does); the same stone outside the dungeon breaks, and a creative player can break the stele.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void dungeonSteleCannotBeMined(TestContext context) {
		ServerWorld world = context.getWorld();
		StructureStart start = place(context, MINED_ORIGIN);
		register(world, start);
		StructurePiece arena = start.getChildren().stream().filter(piece -> SanctumArena.isPiece(piece, SanctumArena.TEMPLATE))
				.findFirst().orElse(null);
		context.assertTrue(arena != null, "no arena piece in " + start.getChildren().size() + " pieces");
		BlockPos stone = BlockPos.stream(arena.getBoundingBox()).filter(pos -> world.getBlockState(pos).isOf(SteleBlocks.CLEFT_STONE))
				.findFirst().map(BlockPos::toImmutable).orElse(null);
		context.assertTrue(stone != null, "no Cleft Stone in the arena " + arena.getBoundingBox());
		context.assertTrue(SanctumArena.arenaAt(world, stone) != null, "the stele at " + stone + " is not found in the arena");

		ServerPlayerEntity player = TestPlayers.create(context, 1, 1);
		context.assertTrue(!player.interactionManager.tryBreakBlock(stone), "the dungeon's stele was mined");
		context.assertTrue(world.getBlockState(stone).isOf(SteleBlocks.CLEFT_STONE) && CleftStele.coldFrame(world, stone).isPresent(),
				"the stele is not whole after mining was refused: " + world.getBlockState(stone));

		BlockPos outside = context.getAbsolutePos(new BlockPos(3, 1, 3));
		world.setBlockState(outside, SteleBlocks.CLEFT_STONE.getDefaultState());
		context.assertTrue(player.interactionManager.tryBreakBlock(outside) && world.getBlockState(outside).isAir(),
				"Cleft Stone outside the dungeon could not be mined");

		player.changeGameMode(GameMode.CREATIVE);
		context.assertTrue(player.interactionManager.tryBreakBlock(stone) && !world.getBlockState(stone).isOf(SteleBlocks.CLEFT_STONE),
				"a creative player could not break the stele");
		TestPlayers.complete(context, player);
	}

	/**
	 * Starts planned now never lead to the old core, and always reach the arena; the old core's template and pool stay
	 * loadable for Sanctums planned before 0.5.0.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void newStartsNeverUseTheOldCore(TestContext context) {
		ServerWorld world = context.getWorld();
		for (int i = 0; i < 12; i++) {
			ChunkPos chunk = new ChunkPos(400 + i * 37, -400 - i * 53);
			StructureStart start = plan(world, chunk);
			context.assertTrue(start.hasChildren(), "no Sanctum planned at " + chunk);
			long arenas = start.getChildren().stream().filter(piece -> SanctumArena.isPiece(piece, SanctumArena.TEMPLATE)).count();
			long cores = start.getChildren().stream().filter(piece -> SanctumArena.isPiece(piece, SanctumArena.OLD_CORE)).count();
			context.assertTrue(arenas == 1 && cores == 0, "the Sanctum at " + chunk + " has " + arenas + " arenas and "
					+ cores + " cores in " + start.getChildren().size() + " pieces");
		}
		StructureTemplate core = world.getStructureTemplateManager().getTemplate(SanctumArena.OLD_CORE).orElse(null);
		context.assertTrue(core != null, "the old core template " + SanctumArena.OLD_CORE + " does not load");
		int obelisks = core.getInfosForBlock(BlockPos.ORIGIN, new StructurePlacementData(), SanctumBlocks.VOID_OBELISK).size();
		context.assertTrue(obelisks == 4, "the old core template holds " + obelisks + " obelisks");
		context.assertTrue(world.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL).containsId(SanctumArena.OLD_CORE),
				"the old core's pool is gone");
		context.complete();
	}

	/**
	 * A stairwell sunk into water (an aquifer, a lake) ends with no water anywhere in its
	 * casing or inside it: the casing's gaps keep the world's block, but turn to rock where that block is a fluid.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void floodedStairwellKeepsWaterOut(TestContext context) {
		ServerWorld world = context.getWorld();
		StructureStart start = lifted(context, plan(world, new ChunkPos(FLOODED_ORIGIN)));
		BlockBox box = start.getBoundingBox();
		BlockBox descent = start.getChildren().stream()
				.filter(piece -> piece instanceof PoolStructurePiece pool && pool.getPoolElement().toString().contains("sanctum/descent"))
				.map(StructurePiece::getBoundingBox).findFirst().orElse(null);
		context.assertTrue(descent != null, "no stairwell piece in " + start.getChildren().size() + " pieces");

		// The world around and inside the stairwell is water before the Sanctum is placed.
		int[] flooded = {0};
		BlockPos.stream(descent).forEach(pos -> {
			world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_LISTENERS);
			flooded[0]++;
		});
		placeAll(world, start, box);

		// Checked again after the water had time to flow (it moves every 5 ticks).
		context.waitAndRun(20, () -> {
			long wet = BlockPos.stream(descent).filter(pos -> !world.getFluidState(pos).isEmpty()).count();
			Embervoid.LOGGER.info("Flooded Sanctum test: stairwell {} flooded with {} blocks, {} wet after placing", descent,
					flooded[0], wet);
			context.assertTrue(wet == 0, wet + " blocks of the stairwell " + descent + " still hold water");
			context.complete();
		});
	}

	/** Registers a placed Sanctum in its chunks as worldgen does, so the world's structure lookups find it. */
	static void register(ServerWorld world, StructureStart start) {
		Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(SanctumArena.STRUCTURE);
		BlockBox box = start.getBoundingBox();
		ChunkPos startChunk = start.getPos();
		world.getChunk(startChunk.x, startChunk.z).setStructureStart(structure, start);
		ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4))
				.forEach(chunk -> world.getChunk(chunk.x, chunk.z).addStructureReference(structure, startChunk.toLong()));
	}

	/** Plans a Sanctum start at a chunk as worldgen would, in any biome. */
	private static StructureStart plan(ServerWorld world, ChunkPos chunk) {
		Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(SanctumArena.STRUCTURE);
		if (structure == null) {
			throw new IllegalStateException(SanctumArena.STRUCTURE.getValue() + " is not registered");
		}
		ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
		return structure.createStructureStart(world.getRegistryManager(), generator, generator.getBiomeSource(),
				world.getChunkManager().getNoiseConfig(), world.getStructureTemplateManager(), world.getSeed(), chunk, 0,
				world, biome -> true);
	}

	/**
	 * The test world is superflat (ground at y -60), too shallow for the stairwell: lifts the pieces clear of the world
	 * bottom. Must happen before getBoundingBox, which caches the box.
	 */
	private static StructureStart lifted(TestContext context, StructureStart start) {
		context.assertTrue(start.hasChildren(), "the Sanctum start produced no pieces");
		int minY = start.getChildren().stream().mapToInt(piece -> piece.getBoundingBox().getMinY()).min().orElseThrow();
		int lift = Math.max(0, context.getWorld().getBottomY() + 8 - minY);
		start.getChildren().forEach(piece -> piece.translate(0, lift, 0));
		return start;
	}

	/** Plans, lifts and places a whole Sanctum at {@code origin}, chunk by chunk as worldgen does. */
	static StructureStart place(TestContext context, BlockPos origin) {
		ServerWorld world = context.getWorld();
		StructureStart start = lifted(context, plan(world, new ChunkPos(origin)));
		placeAll(world, start, start.getBoundingBox());
		return start;
	}

	/**
	 * Plans fresh Sanctums from {@code origin} outwards until one is the given variant (each is about one in three),
	 * moves it to {@code origin}'s chunk, lifts and places it.
	 */
	static StructureStart placeVariant(TestContext context, Identifier variant, BlockPos origin) {
		ServerWorld world = context.getWorld();
		ChunkPos home = new ChunkPos(origin);
		for (int i = 0; i < 64; i++) {
			ChunkPos chunk = new ChunkPos(home.x + i * 11, home.z - i * 7);
			StructureStart planned = plan(world, chunk);
			if (!variant.equals(SanctumVariants.of(planned))) {
				continue;
			}
			planned.getChildren().forEach(piece -> piece.translate(home.getStartX() - chunk.getStartX(), 0, home.getStartZ() - chunk.getStartZ()));
			StructureStart start = lifted(context, new StructureStart(planned.getStructure(), home, 0, new StructurePiecesList(planned.getChildren())));
			placeAll(world, start, start.getBoundingBox());
			return start;
		}
		throw new GameTestException("no " + variant + " Sanctum among 64 fresh plans from " + home);
	}

	private static void placeAll(ServerWorld world, StructureStart start, BlockBox box) {
		ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
		ChunkPos min = new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4);
		ChunkPos max = new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4);
		ChunkPos.stream(min, max).forEach(chunk -> start.place(world, world.getStructureAccessor(), generator,
				world.getRandom(), new BlockBox(chunk.getStartX(), world.getBottomY(), chunk.getStartZ(), chunk.getEndX(),
						world.getTopY(), chunk.getEndZ()), chunk));
	}
}
