package com.embervoid.reach.citadel;

import com.embervoid.Embervoid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;

/**
 * Realm 1's final zone, the Burning City with the Smoldering Citadel (PLAN 13.1–13.2): the depths terrain, its
 * debris, the Citadel structure. The dimension itself is datapack JSON ({@code ashen_reach_depths}).
 */
public final class Citadel {
	public static final SmolderingDebrisFeature DEBRIS = Registry.register(Registries.FEATURE,
			Embervoid.id("smoldering_debris"), new SmolderingDebrisFeature(DefaultFeatureConfig.CODEC));

	public static void register() {
		Registry.register(Registries.DENSITY_FUNCTION_TYPE, Embervoid.id("citadel_terrain"), CitadelTerrain.CODEC.codec());
	}

	private Citadel() {}
}
