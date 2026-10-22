package com.embervoid.reach.world;

import com.embervoid.Embervoid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;

/** Realm 1's world (PLAN milestone 9): registered from {@code RealmInit}. */
public final class ReachWorld {
	public static final RoadFeature ROADS = Registry.register(Registries.FEATURE, Embervoid.id("realm_roads"),
			new RoadFeature(DefaultFeatureConfig.CODEC));
	public static final SinkholePoolFeature SINKHOLE_POOL = Registry.register(Registries.FEATURE,
			Embervoid.id("sinkhole_pool"), new SinkholePoolFeature(DefaultFeatureConfig.CODEC));

	public static void register() {
		ReachBlocks.register();
		ZoneTitles.register();
	}

	private ReachWorld() {}
}
