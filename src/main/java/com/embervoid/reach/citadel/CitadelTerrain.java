package com.embervoid.reach.citadel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Density function {@code embervoid:citadel_terrain} (PLAN 13.1, 21.3): the Burning City's ground from
 * {@link CitadelLayout#groundHeight}: the lower city around the entry plaza, the hill's plateau with the Citadel, flat
 * terraces under its pieces and the courtyard. Positive below the ground, {@code 1 / scale} per block. Sampled at
 * realm columns, turned into the plan's frame ({@link CitadelLayout#planX}); the realm's final zone shape.
 *
 * @param hills     2D hills noise (the realm's {@code realm_hills}), roughly in [-1, 1]
 * @param roughness 3D noise that breaks up the ground away from the terraces, most on the hill's sides
 * @param scale     blocks per unit of density
 */
public record CitadelTerrain(DensityFunction hills, DensityFunction roughness, double scale) implements DensityFunction {
	public static final MapCodec<CitadelTerrain> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			DensityFunction.FUNCTION_CODEC.fieldOf("hills").forGetter(CitadelTerrain::hills),
			DensityFunction.FUNCTION_CODEC.fieldOf("roughness").forGetter(CitadelTerrain::roughness),
			Codec.doubleRange(1, 64).optionalFieldOf("scale", 8.0).forGetter(CitadelTerrain::scale)
	).apply(instance, CitadelTerrain::new));
	public static final CodecHolder<CitadelTerrain> CODEC = CodecHolder.of(MAP_CODEC);

	/** Most a roughness of ±1 moves the ground in the flat city and on the hill's sides, in density units. */
	private static final double ROUGH_CITY = 0.15;
	private static final double ROUGH_CLIFF = 0.6;

	@Override
	public double sample(NoisePos pos) {
		double x = CitadelLayout.planX(pos.blockX());
		double z = CitadelLayout.planZ(pos.blockZ());
		double ground = CitadelLayout.groundHeight(x, z, hills.sample(pos));
		double density = (ground + 0.5 - pos.blockY()) / scale;
		double rough = 1 - CitadelLayout.flatness(x, z);
		if (rough > 0) {
			double amount = ROUGH_CITY + (ROUGH_CLIFF - ROUGH_CITY) * CitadelLayout.cliff(x, z);
			density += rough * amount * roughness.sample(pos);
		}
		return density;
	}

	@Override
	public void fill(double[] densities, EachApplier applier) {
		applier.fill(densities, this);
	}

	@Override
	public DensityFunction apply(DensityFunctionVisitor visitor) {
		return visitor.apply(new CitadelTerrain(hills.apply(visitor), roughness.apply(visitor), scale));
	}

	@Override
	public double minValue() {
		return -256 / scale - ROUGH_CLIFF;
	}

	@Override
	public double maxValue() {
		return 256 / scale + ROUGH_CLIFF;
	}

	@Override
	public CodecHolder<? extends DensityFunction> getCodecHolder() {
		return CODEC;
	}
}
