package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.entity.mob.MobEntities;
import com.embervoid.hub.stele.CleftStele;
import com.embervoid.hub.stele.SteleBlocks;
import com.embervoid.sanctum.SanctumArena;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumEntities;
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
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashMap;
import java.util.Map;

/**
 * The Ruined Sanctum as worldgen builds it, placed the way {@code /place structure} does, then its contents counted
 * inside the structure's bounding box (PLAN 16.1).
 */
public final class SanctumTests implements FabricGameTest {
	/** Far from the test grid so the Sanctum never overlaps another test. */
	private static final BlockPos ORIGIN = new BlockPos(3000, 0, 3000);
	/** For the flooded Sanctum, away from the first one. */
	private static final BlockPos FLOODED_ORIGIN = new BlockPos(3000, 0, -3000);
	/** For the Sanctum whose stele is mined, away from both. */
	private static final BlockPos MINED_ORIGIN = new BlockPos(-3000, 0, 3000);

	/**
	 * A fresh dungeon: guards and loot in the outer halls as before; no Maiden, obelisks or Sanctum Flagstone; the
	 * boss wing with one Ember Shrine, the fog wall in its doorway, a cold Cleft Stele and the boss spawn marker.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void dungeonHasArenaInsteadOfCore(TestContext context) {
		ServerWorld world = context.getWorld();
		StructureStart start = place(context, ORIGIN);
		BlockBox box = start.getBoundingBox();
		String where = start.getChildren().size() + " pieces in " + box;

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
		int markers = world.getEntitiesByType(EntityType.MARKER, area,
				entity -> entity.getCommandTags().contains(SanctumArena.BOSS_SPAWN)).size();
		long arenas = start.getChildren().stream().filter(piece -> SanctumArena.isPiece(piece, SanctumArena.TEMPLATE)).count();
		long cores = start.getChildren().stream().filter(piece -> SanctumArena.isPiece(piece, SanctumArena.OLD_CORE)).count();
		int shrines = blocks.getOrDefault(SanctumBlocks.EMBER_SHRINE, 0);
		int fog = blocks.getOrDefault(SanctumBlocks.FOG_WALL, 0);
		int flagstone = blocks.getOrDefault(SanctumBlocks.SANCTUM_FLAGSTONE, 0);
		int cleft = blocks.getOrDefault(SteleBlocks.CLEFT_STONE, 0);
		boolean coldStele = stone[0] != null && CleftStele.coldFrame(world, stone[0]).isPresent();
		String counts = where + ": arenas " + arenas + ", cores " + cores + ", chests " + chests[0] + ", obelisks "
				+ obelisks[0] + ", shrines " + shrines + ", fog " + fog + ", flagstone " + flagstone + ", cleft stone "
				+ cleft + ", cold stele " + coldStele + ", boss markers " + markers + ", hollows " + hollows
				+ ", maidens " + maidens;
		Embervoid.LOGGER.info("Sanctum test: {}", counts);
		context.assertTrue(arenas == 1 && cores == 0, "expected the arena piece and no core; " + counts);
		context.assertTrue(obelisks[0] == 0, "Void Obelisks in a new dungeon; " + counts);
		context.assertTrue(maidens == 0, "an Ashen Maiden in a new dungeon; " + counts);
		context.assertTrue(flagstone == 0, "Sanctum Flagstone (it attunes) in a new dungeon; " + counts);
		context.assertTrue(shrines == 1, "expected one Ember Shrine, at the boss door; " + counts);
		context.assertTrue(fog == 9, "expected a 3x3 fog wall in the arena doorway; " + counts);
		context.assertTrue(cleft == 24 && coldStele, "expected a whole cold Cleft Stele; " + counts);
		context.assertTrue(markers == 1, "expected one boss spawn marker; " + counts);
		context.assertTrue(hollows >= 1, "no Hollow guard (the gate hall always has one); " + counts);
		context.assertTrue(chests[0] >= 1, "no loot chests; " + counts);
		context.complete();
	}

	/**
	 * The arena's stele cannot be mined in survival while it stands in the dungeon (the dungeon registered in its
	 * chunks as worldgen does); the same stone outside the dungeon breaks, and a creative player can break the stele.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void dungeonSteleCannotBeMined(TestContext context) {
		ServerWorld world = context.getWorld();
		StructureStart start = place(context, MINED_ORIGIN);
		Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(SanctumArena.STRUCTURE);
		BlockBox box = start.getBoundingBox();
		ChunkPos startChunk = start.getPos();
		world.getChunk(startChunk.x, startChunk.z).setStructureStart(structure, start);
		ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4))
				.forEach(chunk -> world.getChunk(chunk.x, chunk.z).addStructureReference(structure, startChunk.toLong()));
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
	 * loadable for Sanctums planned before 0.5.0 (PLAN 15.5 places one).
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
	 * QA #7f, never checked in game: a stairwell sunk into water (an aquifer, a lake) ends with no water anywhere in its
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
	private static StructureStart place(TestContext context, BlockPos origin) {
		ServerWorld world = context.getWorld();
		StructureStart start = lifted(context, plan(world, new ChunkPos(origin)));
		placeAll(world, start, start.getBoundingBox());
		return start;
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
