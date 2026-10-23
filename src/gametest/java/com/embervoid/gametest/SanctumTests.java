package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.entity.mob.MobEntities;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;

/**
 * The Ember Sanctum as worldgen builds it, placed the way {@code /place structure} does, then its contents counted
 * inside the structure's bounding box.
 */
public final class SanctumTests implements FabricGameTest {
	/** Far from the test grid so the Sanctum never overlaps another test. */
	private static final BlockPos ORIGIN = new BlockPos(3000, 0, 3000);
	/** For the flooded Sanctum, away from the first one. */
	private static final BlockPos FLOODED_ORIGIN = new BlockPos(3000, 0, -3000);

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void sanctumGeneratesWithCoreAndGuards(TestContext context) {
		ServerWorld world = context.getWorld();
		Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(Embervoid.id("ember_sanctum"));
		context.assertTrue(structure != null, "embervoid:ember_sanctum is not registered");
		ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
		StructureStart start = structure.createStructureStart(world.getRegistryManager(), generator,
				generator.getBiomeSource(), world.getChunkManager().getNoiseConfig(), world.getStructureTemplateManager(),
				world.getSeed(), new ChunkPos(ORIGIN), 0, world, biome -> true);
		context.assertTrue(start.hasChildren(), "the Sanctum start produced no pieces");
		// The test world is superflat (ground at y -60), too shallow for the stairwell: lift the pieces clear of the
		// world bottom. Must happen before getBoundingBox, which caches the box.
		int minY = start.getChildren().stream().mapToInt(piece -> piece.getBoundingBox().getMinY()).min().orElseThrow();
		int lift = Math.max(0, world.getBottomY() + 8 - minY);
		start.getChildren().forEach(piece -> piece.translate(0, lift, 0));
		BlockBox box = start.getBoundingBox();
		String where = start.getChildren().size() + " pieces in " + box;

		ChunkPos min = new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4);
		ChunkPos max = new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4);
		ChunkPos.stream(min, max).forEach(chunk -> start.place(world, world.getStructureAccessor(), generator,
				world.getRandom(), new BlockBox(chunk.getStartX(), world.getBottomY(), chunk.getStartZ(), chunk.getEndX(),
						world.getTopY(), chunk.getEndZ()), chunk));

		int[] chests = {0};
		int[] obelisks = {0};
		ChunkPos.stream(min, max).forEach(pos -> {
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
		boolean shrine = BlockPos.stream(box).anyMatch(pos -> world.getBlockState(pos).isOf(SanctumBlocks.EMBER_SHRINE));
		Box area = Box.from(box).expand(2);
		int hollows = world.getEntitiesByType(MobEntities.HOLLOW, area, entity -> true).size();
		int maidens = world.getEntitiesByType(SanctumEntities.ASHEN_MAIDEN, area, entity -> true).size();
		String counts = where + ": chests " + chests[0] + ", obelisks " + obelisks[0] + ", shrine " + shrine
				+ ", hollows " + hollows + ", maidens " + maidens;
		Embervoid.LOGGER.info("Sanctum test: {}", counts);
		context.assertTrue(obelisks[0] == 4, "expected 4 Void Obelisks; " + counts);
		context.assertTrue(shrine, "no Ember Shrine in the core; " + counts);
		context.assertTrue(maidens == 1, "expected one Ashen Maiden; " + counts);
		context.assertTrue(hollows >= 1, "no Hollow guard (the gate hall always has one); " + counts);
		context.assertTrue(chests[0] >= 1, "no loot chests; " + counts);
		context.complete();
	}

	/**
	 * QA #7f, never checked in game: a stairwell sunk into water (an aquifer, a lake) ends with no water anywhere in its
	 * casing or inside it: the casing's gaps keep the world's block, but turn to rock where that block is a fluid.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void floodedStairwellKeepsWaterOut(TestContext context) {
		ServerWorld world = context.getWorld();
		Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(Embervoid.id("ember_sanctum"));
		ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
		StructureStart start = structure.createStructureStart(world.getRegistryManager(), generator,
				generator.getBiomeSource(), world.getChunkManager().getNoiseConfig(), world.getStructureTemplateManager(),
				world.getSeed(), new ChunkPos(FLOODED_ORIGIN), 0, world, biome -> true);
		context.assertTrue(start.hasChildren(), "the Sanctum start produced no pieces");
		int minY = start.getChildren().stream().mapToInt(piece -> piece.getBoundingBox().getMinY()).min().orElseThrow();
		int lift = Math.max(0, world.getBottomY() + 8 - minY);
		start.getChildren().forEach(piece -> piece.translate(0, lift, 0));
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
		ChunkPos min = new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4);
		ChunkPos max = new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4);
		ChunkPos.stream(min, max).forEach(chunk -> start.place(world, world.getStructureAccessor(), generator,
				world.getRandom(), new BlockBox(chunk.getStartX(), world.getBottomY(), chunk.getStartZ(), chunk.getEndX(),
						world.getTopY(), chunk.getEndZ()), chunk));

		// Checked again after the water had time to flow (it moves every 5 ticks).
		context.waitAndRun(20, () -> {
			long wet = BlockPos.stream(descent).filter(pos -> !world.getFluidState(pos).isEmpty()).count();
			Embervoid.LOGGER.info("Flooded Sanctum test: stairwell {} flooded with {} blocks, {} wet after placing", descent,
					flooded[0], wet);
			context.assertTrue(wet == 0, wet + " blocks of the stairwell " + descent + " still hold water");
			context.complete();
		});
	}
}
