package com.embervoid.reach.world;

import com.embervoid.realm.fog.FogStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The Ashen Reach zones' fog and names keyed by plain ids, free of registries (JUnit-tested); {@link ReachZones}
 * holds the biome keys.
 */
public final class ZoneLooks {
	/** Zone fog (PLAN 9.1): Fields grey-ochre, Woods dense grey-green, Necropolis blue-grey; dusk-dark colours. */
	private static final Map<Identifier, FogStack.Layer> FOG = Map.of(
			biome("ashen_reach_landing"), new FogStack.Layer(0.30f, 0x4E4636, true),
			biome("ashen_reach_zone_a"), new FogStack.Layer(0.35f, 0x4E4636, true),
			biome("ashen_reach_zone_b"), new FogStack.Layer(0.60f, 0x38412F, true),
			biome("ashen_reach_zone_c"), new FogStack.Layer(0.45f, 0x343C49, true),
			biome("ashen_reach_abyss"), new FogStack.Layer(0.35f, 0x2A2622, true),
			biome("ashen_reach_citadel"), new FogStack.Layer(0.40f, 0x4A2318, true));

	/** Fog of the realm biome {@code biomeId}, or null for biomes without zone fog. */
	@Nullable
	public static FogStack.Layer fog(Identifier biomeId) {
		return FOG.get(biomeId);
	}

	/** Zone name used in template and loot table ids ({@code fields}, {@code woods}, {@code necropolis}) of a zone boss id. */
	public static String zoneName(Identifier bossId) {
		String path = bossId.getPath();
		if (path.endsWith("zone_a")) {
			return "fields";
		}
		if (path.endsWith("zone_b")) {
			return "woods";
		}
		if (path.endsWith("zone_c")) {
			return "necropolis";
		}
		throw new IllegalArgumentException("Not an Ashen Reach zone boss: " + bossId);
	}

	private static Identifier biome(String path) {
		return new Identifier("embervoid", path);
	}

	private ZoneLooks() {}
}
