package com.embervoid.realm;

import com.embervoid.boss.ArenaSpec;
import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.worldgen.ArenaSite;
import com.embervoid.realm.worldgen.RealmFeaturePlacement;
import com.embervoid.realm.worldgen.RealmFeatureStructure;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Where a world's boss arenas are, computed from their placements (no chunk has to be loaded or generated). */
public final class RealmArenas {
	/**
	 * An arena of a world. {@code pos} is its center column; y is 0 (unknown until the chunk generates).
	 * {@code siteRadius} is the radius of its flat ground (the fog wall's radius for an arena without a site).
	 */
	public record Location(ArenaSpec spec, BlockPos pos, int siteRadius) {}

	public static List<Location> all(ServerWorld world) {
		return all(world.getChunkManager().getStructurePlacementCalculator());
	}

	/** The arenas of the world a placement calculator belongs to (usable during worldgen, without the world). */
	public static List<Location> all(StructurePlacementCalculator calculator) {
		List<Location> result = new ArrayList<>();
		for (RegistryEntry<StructureSet> set : calculator.getStructureSets()) {
			if (!(set.value().placement() instanceof RealmFeaturePlacement placement)) {
				continue;
			}
			for (StructureSet.WeightedEntry entry : set.value().structures()) {
				if (entry.structure().value() instanceof RealmFeatureStructure feature && feature.boss().isPresent()) {
					ChunkPos chunk = placement.getStartChunk(calculator.getStructureSeed(), 0, 0);
					int site = feature.site().map(ArenaSite::radius).orElse(feature.boss().get().radius());
					result.add(new Location(feature.boss().get(), new BlockPos(chunk.getStartX(), 0, chunk.getStartZ()), site));
				}
			}
		}
		return result;
	}

	/** The nearest arena in the player's world whose boss they have not beaten, or null. */
	@Nullable
	public static Location nearestUnbeaten(ServerPlayerEntity player) {
		return all(player.getServerWorld()).stream()
				.filter(arena -> !ProgressManager.hasBeatenBoss(player, arena.spec().bossId()))
				.min(Comparator.comparingDouble(arena -> arena.pos().getSquaredDistance(player.getX(), 0, player.getZ())))
				.orElse(null);
	}

	private RealmArenas() {}
}
