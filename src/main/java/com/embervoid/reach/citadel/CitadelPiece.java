package com.embervoid.reach.citadel;

import com.embervoid.realm.placed.PlacedEnemyProcessor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.SimpleStructurePiece;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;

/**
 * One piece of the Citadel ({@link CitadelLayout.Piece}): its template placed unrotated at the layout's position, with
 * placed enemies spawned from their markers (reach-world's {@link PlacedEnemyProcessor}).
 */
public class CitadelPiece extends SimpleStructurePiece {
	public CitadelPiece(StructureTemplateManager manager, CitadelLayout.Piece piece) {
		super(Citadel.PIECE, 0, manager, piece.template(), piece.template().toString(), placement(piece.template()),
				new BlockPos(piece.minX(), piece.originY(), piece.minZ()));
	}

	public CitadelPiece(StructureTemplateManager manager, NbtCompound nbt) {
		super(Citadel.PIECE, nbt, manager, CitadelPiece::placement);
	}

	private static StructurePlacementData placement(Identifier template) {
		return new StructurePlacementData()
				.addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS)
				.addProcessor(PlacedEnemyProcessor.INSTANCE);
	}

	/** Template id of the piece. */
	public Identifier templateId() {
		return getId();
	}

	@Override
	protected void handleMetadata(String metadata, BlockPos pos, ServerWorldAccess world, Random random, BlockBox boundingBox) {
	}
}
