package com.embervoid.realm.worldgen;

import com.embervoid.realm.RealmLayout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Density function {@code embervoid:zone_blend} (PLAN 9.4): one terrain shape per zone, picked by the same angle
 * sectors and border warp as the biome source ({@code embervoid:realm_zones}), blended across the borders so a zone's
 * hills fade into the next zone's plain, and a landing shape inside the landing disc. The shapes themselves are
 * ordinary JSON density functions.
 *
 * @param warp         the biome source's warp noise (the router's {@code temperature}), roughly in [-1, 1]
 * @param warpDegrees  how far a warp of ±1 turns the borders, same as the biome source's {@code warp}
 * @param blend        half-width of a zone border blend, in blocks (narrower near the centre: at most a third of
 *                     the sector)
 * @param landingBlend width of the ring outside the landing disc where the landing shape fades out, in blocks
 */
public record ZoneBlend(DensityFunction warp, DensityFunction landing, List<DensityFunction> zones, double startAngle,
		double landingRadius, double landingBlend, double warpDegrees, double blend) implements DensityFunction {
	public static final MapCodec<ZoneBlend> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			DensityFunction.FUNCTION_CODEC.fieldOf("warp").forGetter(ZoneBlend::warp),
			DensityFunction.FUNCTION_CODEC.fieldOf("landing").forGetter(ZoneBlend::landing),
			DensityFunction.FUNCTION_CODEC.listOf().fieldOf("zones").forGetter(ZoneBlend::zones),
			Codec.DOUBLE.fieldOf("start_angle").forGetter(ZoneBlend::startAngle),
			Codec.DOUBLE.fieldOf("landing_radius").forGetter(ZoneBlend::landingRadius),
			Codec.DOUBLE.fieldOf("landing_blend").forGetter(ZoneBlend::landingBlend),
			Codec.DOUBLE.fieldOf("warp_degrees").forGetter(ZoneBlend::warpDegrees),
			Codec.DOUBLE.fieldOf("blend").forGetter(ZoneBlend::blend)
	).apply(instance, ZoneBlend::new));
	public static final CodecHolder<ZoneBlend> CODEC = CodecHolder.of(MAP_CODEC);

	public ZoneBlend {
		if (zones.isEmpty()) {
			throw new IllegalArgumentException("zone_blend needs at least one zone");
		}
		zones = List.copyOf(zones);
	}

	@Override
	public double sample(NoisePos pos) {
		double x = pos.blockX();
		double z = pos.blockZ();
		double distance = Math.sqrt(x * x + z * z);
		double landingWeight = RealmLayout.landingWeight(distance, landingRadius, landingBlend);
		double result = landingWeight > 0 ? landingWeight * landing.sample(pos) : 0;
		if (landingWeight >= 1) {
			return result;
		}
		double angle = RealmLayout.angle(x, z) + Math.max(-1, Math.min(1, warp.sample(pos))) * Math.toRadians(warpDegrees);
		double[] weights = RealmLayout.zoneWeights(angle, zones.size(), Math.toRadians(startAngle), distance, blend);
		for (int i = 0; i < weights.length; i++) {
			if (weights[i] > 0) {
				result += (1 - landingWeight) * weights[i] * zones.get(i).sample(pos);
			}
		}
		return result;
	}

	@Override
	public void fill(double[] densities, EachApplier applier) {
		applier.fill(densities, this);
	}

	@Override
	public DensityFunction apply(DensityFunctionVisitor visitor) {
		List<DensityFunction> visited = new ArrayList<>(zones.size());
		for (DensityFunction zone : zones) {
			visited.add(zone.apply(visitor));
		}
		return visitor.apply(new ZoneBlend(warp.apply(visitor), landing.apply(visitor), visited, startAngle,
				landingRadius, landingBlend, warpDegrees, blend));
	}

	@Override
	public double minValue() {
		double min = landing.minValue();
		for (DensityFunction zone : zones) {
			min = Math.min(min, zone.minValue());
		}
		return min;
	}

	@Override
	public double maxValue() {
		double max = landing.maxValue();
		for (DensityFunction zone : zones) {
			max = Math.max(max, zone.maxValue());
		}
		return max;
	}

	@Override
	public CodecHolder<? extends DensityFunction> getCodecHolder() {
		return CODEC;
	}
}
