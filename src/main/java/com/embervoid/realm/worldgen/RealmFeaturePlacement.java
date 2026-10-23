package com.embervoid.realm.worldgen;

import com.embervoid.realm.RealmLayout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.SpreadType;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;

import java.util.Optional;

/**
 * Structure placement {@code embervoid:realm_feature}: exactly one structure per world, at a position derived from the
 * seed inside one angle sector of the realm (see {@link RealmLayout#featurePosition}). A distance of 0 puts it at the
 * origin (the landing).
 *
 * <p>Extends random spread only so that vanilla {@code /locate structure} and {@code locateStructure} handle it: every
 * region answers with the same start chunk.
 */
public class RealmFeaturePlacement extends RandomSpreadStructurePlacement {
	/**
	 * @param sectorCount number of sectors when they share the circle evenly (no {@code sector_width})
	 * @param sector      sector the feature belongs to
	 * @param startAngle  start of sector 0 in degrees, same as the biome source's {@code start_angle}
	 * @param sectorWidth width of each sector in degrees, as the biome source's open zones (default
	 *                    {@code 360 / sector_count})
	 * @param angleSpread fraction of the sector half-width the feature may stray from its center
	 */
	public record Params(int sectorCount, int sector, double startAngle, Optional<Double> sectorWidth, double angleSpread,
			int minDistance, int maxDistance) {
		public static final MapCodec<Params> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.intRange(1, 64).optionalFieldOf("sector_count", 1).forGetter(Params::sectorCount),
				Codec.intRange(0, 63).optionalFieldOf("sector", 0).forGetter(Params::sector),
				Codec.DOUBLE.optionalFieldOf("start_angle", 0.0).forGetter(Params::startAngle),
				Codec.doubleRange(0.001, 360).optionalFieldOf("sector_width").forGetter(Params::sectorWidth),
				Codec.doubleRange(0, 1).optionalFieldOf("angle_spread", 0.5).forGetter(Params::angleSpread),
				Codec.intRange(0, 100_000).optionalFieldOf("min_distance", 0).forGetter(Params::minDistance),
				Codec.intRange(0, 100_000).optionalFieldOf("max_distance", 0).forGetter(Params::maxDistance)
		).apply(instance, Params::new));

		/** Sector width in degrees. */
		public double width() {
			return sectorWidth.orElse(360.0 / sectorCount);
		}
	}

	public static final Codec<RealmFeaturePlacement> CODEC = RecordCodecBuilder.<RealmFeaturePlacement>mapCodec(instance ->
			buildCodec(instance)
					.and(Params.MAP_CODEC.forGetter(p -> p.params))
					.apply(instance, RealmFeaturePlacement::new)).codec();

	private final Params params;

	public RealmFeaturePlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency,
			int salt, Optional<ExclusionZone> exclusionZone, Params params) {
		// Spacing 1 / separation 0 are only read by the locate search, which then asks getStartChunk anyway.
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, 1, 0, SpreadType.LINEAR);
		this.params = params;
	}

	public Params params() {
		return params;
	}

	/** Block x, z of the feature for this world seed. */
	public int[] featurePosition(long seed) {
		double width = Math.toRadians(params.width());
		return RealmLayout.featurePosition(seed, getSalt(), Math.toRadians(params.startAngle()) + params.sector() * width,
				width, params.angleSpread(), params.minDistance(), params.maxDistance());
	}

	@Override
	public ChunkPos getStartChunk(long seed, int chunkX, int chunkZ) {
		int[] pos = featurePosition(seed);
		return new ChunkPos(Math.floorDiv(pos[0], 16), Math.floorDiv(pos[1], 16));
	}

	@Override
	public StructurePlacementType<?> getType() {
		return RealmWorldgen.FEATURE_PLACEMENT;
	}
}
