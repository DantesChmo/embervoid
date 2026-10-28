package com.embervoid.realm.placed;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.WorldView;

/**
 * Processor {@code embervoid:placed_enemies}: replaces each {@code embervoid:placed_enemy} marker with air
 * and spawns its entity. A template is processed once per chunk it crosses, so the entity is spawned only by the
 * pass whose box holds the marker. Add it to a template's processor list (JSON) or placement data (Java).
 */
public class PlacedEnemyProcessor extends StructureProcessor {
	public static final PlacedEnemyProcessor INSTANCE = new PlacedEnemyProcessor();
	public static final Codec<PlacedEnemyProcessor> CODEC = Codec.unit(() -> INSTANCE);

	@Override
	public StructureTemplate.StructureBlockInfo process(WorldView world, BlockPos pos, BlockPos pivot,
			StructureTemplate.StructureBlockInfo originalBlockInfo, StructureTemplate.StructureBlockInfo currentBlockInfo,
			StructurePlacementData data) {
		BlockState state = currentBlockInfo.state();
		if (!state.isOf(PlacedEnemies.MARKER)) {
			return currentBlockInfo;
		}
		BlockBox box = data.getBoundingBox();
		if ((box == null || box.contains(currentBlockInfo.pos())) && world instanceof ServerWorldAccess access
				&& currentBlockInfo.nbt() != null) {
			// Block states are rotated after the processors run, so turn the facing here.
			Direction facing = data.getRotation().rotate(data.getMirror().apply(state.get(PlacedEnemyMarkerBlock.FACING)));
			PlacedEnemies.spawn(access, currentBlockInfo.pos(), facing.asRotation(), currentBlockInfo.nbt());
		}
		return new StructureTemplate.StructureBlockInfo(currentBlockInfo.pos(), Blocks.AIR.getDefaultState(), null);
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return PlacedEnemies.PROCESSOR;
	}
}
