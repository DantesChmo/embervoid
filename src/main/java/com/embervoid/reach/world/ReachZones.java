package com.embervoid.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.realm.fog.FogStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** The Ashen Reach's biomes and what each one looks like beyond its biome JSON (DESIGN §4.5 "Zones and places"). */
public final class ReachZones {
	public static final RegistryKey<Biome> LANDING = biome("ashen_reach_landing");
	/** Ashen Fields, zone_a. */
	public static final RegistryKey<Biome> FIELDS = biome("ashen_reach_zone_a");
	/** Royal Woods, zone_b. */
	public static final RegistryKey<Biome> WOODS = biome("ashen_reach_zone_b");
	/** Sunken Necropolis, zone_c. */
	public static final RegistryKey<Biome> NECROPOLIS = biome("ashen_reach_zone_c");
	public static final RegistryKey<Biome> ABYSS = biome("ashen_reach_abyss");
	public static final RegistryKey<Biome> DEPTHS = biome("ashen_reach_depths");
	/** The final zone's sector of the realm dimension (PLAN 21.1): burnt outskirts until 21.3 builds the Citadel there. */
	public static final RegistryKey<Biome> CITADEL = biome("ashen_reach_citadel");

	/** Zone fog (PLAN 9.1): Fields grey-ochre, Woods dense grey-green, Necropolis blue-grey; dusk-dark colours. */
	private static final Map<RegistryKey<Biome>, FogStack.Layer> FOG = Map.of(
			LANDING, new FogStack.Layer(0.30f, 0x4E4636, true),
			FIELDS, new FogStack.Layer(0.35f, 0x4E4636, true),
			WOODS, new FogStack.Layer(0.60f, 0x38412F, true),
			NECROPOLIS, new FogStack.Layer(0.45f, 0x343C49, true),
			ABYSS, new FogStack.Layer(0.35f, 0x2A2622, true),
			DEPTHS, new FogStack.Layer(0.40f, 0x4A2318, true),
			CITADEL, new FogStack.Layer(0.40f, 0x4A2318, true));

	/** Fog of a realm biome, or null for biomes without zone fog. */
	@Nullable
	public static FogStack.Layer fog(RegistryKey<Biome> biome) {
		return FOG.get(biome);
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

	private static RegistryKey<Biome> biome(String path) {
		return RegistryKey.of(RegistryKeys.BIOME, Embervoid.id(path));
	}

	private ReachZones() {}
}
