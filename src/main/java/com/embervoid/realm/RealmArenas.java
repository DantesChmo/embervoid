package com.embervoid.realm;

import com.embervoid.boss.ArenaSpec;
import com.embervoid.realm.progress.RealmProgress;
import com.embervoid.realm.worldgen.RealmFeaturePlacement;
import com.embervoid.realm.worldgen.RealmFeatureStructure;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Where a world's boss arenas are, computed from their placements (no chunk has to be loaded or generated). */
public final class RealmArenas {
	/** An arena of a world. {@code pos} is its center column; y is 0 (unknown until the chunk generates). */
	public record Location(ArenaSpec spec, BlockPos pos) {}

	public static List<Location> all(ServerWorld world) {
		StructurePlacementCalculator calculator = world.getChunkManager().getStructurePlacementCalculator();
		Registry<Structure> structures = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
		List<Location> result = new ArrayList<>();
		for (RegistryEntry.Reference<Structure> entry : structures.streamEntries().toList()) {
			if (!(entry.value() instanceof RealmFeatureStructure feature) || feature.boss().isEmpty()) {
				continue;
			}
			for (StructurePlacement placement : calculator.getPlacements(entry)) {
				if (placement instanceof RealmFeaturePlacement realmPlacement) {
					ChunkPos chunk = realmPlacement.getStartChunk(calculator.getStructureSeed(), 0, 0);
					result.add(new Location(feature.boss().get(), new BlockPos(chunk.getStartX(), 0, chunk.getStartZ())));
				}
			}
		}
		return result;
	}

	/** The nearest arena in the player's world whose boss they have not beaten, or null. */
	@Nullable
	public static Location nearestUnbeaten(ServerPlayerEntity player) {
		RealmProgress progress = RealmProgress.get();
		return all(player.getServerWorld()).stream()
				.filter(arena -> !progress.hasBeatenBoss(player, arena.spec().bossId()))
				.min(Comparator.comparingDouble(arena -> arena.pos().getSquaredDistance(player.getX(), 0, player.getZ())))
				.orElse(null);
	}

	private RealmArenas() {}
}
