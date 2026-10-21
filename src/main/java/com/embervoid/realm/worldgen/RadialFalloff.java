package com.embervoid.realm.worldgen;

import com.embervoid.realm.RealmLayout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Density function {@code embervoid:radial_falloff}: {@code +strength} within {@code inner} blocks of the origin,
 * {@code -strength} from {@code outer} on, smooth in between. A realm's {@code final_density} is
 * {@code min(terrain, falloff)}, so the land ends in cliffs over the void.
 */
public record RadialFalloff(double inner, double outer, double strength) implements DensityFunction.Base {
	public static final MapCodec<RadialFalloff> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.DOUBLE.fieldOf("inner").forGetter(RadialFalloff::inner),
			Codec.DOUBLE.fieldOf("outer").forGetter(RadialFalloff::outer),
			Codec.DOUBLE.fieldOf("strength").forGetter(RadialFalloff::strength)
	).apply(instance, RadialFalloff::new));
	public static final CodecHolder<RadialFalloff> CODEC = CodecHolder.of(MAP_CODEC);

	@Override
	public double sample(NoisePos pos) {
		double x = pos.blockX();
		double z = pos.blockZ();
		return strength * RealmLayout.falloff(Math.sqrt(x * x + z * z), inner, outer);
	}

	@Override
	public double minValue() {
		return -strength;
	}

	@Override
	public double maxValue() {
		return strength;
	}

	@Override
	public CodecHolder<? extends DensityFunction> getCodecHolder() {
		return CODEC;
	}
}
