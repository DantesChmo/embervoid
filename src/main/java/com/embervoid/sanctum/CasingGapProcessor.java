package com.embervoid.sanctum;

import com.embervoid.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the gaps in the stairwell's ragged rock casing, marked in the template with structure void: the world keeps
 * its own block there, unless that block holds water or lava (an aquifer), which becomes rock instead.
 */
public final class CasingGapProcessor extends StructureProcessor {
	public static final CasingGapProcessor INSTANCE = new CasingGapProcessor();
	public static final Codec<CasingGapProcessor> CODEC = Codec.unit(INSTANCE);

	private static final BlockState[] ROCK = {
			Blocks.STONE.getDefaultState(), Blocks.TUFF.getDefaultState(),
			ModBlocks.ASH_STONE.getDefaultState(), ModBlocks.COBBLED_ASH_STONE.getDefaultState()};

	private CasingGapProcessor() {}

	@Nullable
	@Override
	public StructureTemplate.StructureBlockInfo process(WorldView world, BlockPos pos, BlockPos pivot,
			StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current,
			StructurePlacementData data) {
		if (!current.state().isOf(Blocks.STRUCTURE_VOID)) {
			return current;
		}
		if (world.getFluidState(current.pos()).isEmpty()) {
			return null;
		}
		Random random = data.getRandom(current.pos());
		return new StructureTemplate.StructureBlockInfo(current.pos(), ROCK[random.nextInt(ROCK.length)], null);
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return SanctumStructures.CASING_GAP;
	}
}
