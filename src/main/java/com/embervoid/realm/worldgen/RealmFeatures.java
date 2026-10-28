package com.embervoid.realm.worldgen;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.Nullable;

/** Looks up the realm feature (landing or arena) at a position from the generated structure starts. */
public final class RealmFeatures {
	public record Placed(RealmFeatureStructure structure, RealmFeaturePiece piece) {}

	/** The landing or arena whose bounds contain {@code pos}, or null. Only looks at loaded chunks' references. */
	@Nullable
	public static Placed at(ServerWorld world, BlockPos pos) {
		if (!world.isChunkLoaded(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4))) {
			return null;
		}
		for (StructureStart start : world.getStructureAccessor().getStructureStarts(new ChunkPos(pos), s -> s instanceof RealmFeatureStructure)) {
			for (StructurePiece child : start.getChildren()) {
				if (child instanceof RealmFeaturePiece piece && piece.getBoundingBox().contains(pos)) {
					return new Placed((RealmFeatureStructure) start.getStructure(), piece);
				}
			}
		}
		return null;
	}

	private RealmFeatures() {}
}
