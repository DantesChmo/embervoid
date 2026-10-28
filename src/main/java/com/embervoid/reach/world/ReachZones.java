package com.embervoid.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.realm.fog.FogStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

/** The Ashen Reach's biomes and what each one looks like beyond its biome JSON (DESIGN §4.5 "Zones and places"). */
public final class ReachZones {
	public static final RegistryKey<Biome> LANDING = biome("ashen_reach_landing");
	public static final RegistryKey<Biome> FIELDS = biome("ashen_reach_zone_a");
	public static final RegistryKey<Biome> WOODS = biome("ashen_reach_zone_b");
	public static final RegistryKey<Biome> NECROPOLIS = biome("ashen_reach_zone_c");
	public static final RegistryKey<Biome> ABYSS = biome("ashen_reach_abyss");
	/** The final zone's sector of the realm dimension: the Burning City and the Smoldering Citadel. */
	public static final RegistryKey<Biome> CITADEL = biome("ashen_reach_citadel");

	/** Fog of a realm biome, or null for biomes without zone fog. */
	@Nullable
	public static FogStack.Layer fog(RegistryKey<Biome> biome) {
		return ZoneLooks.fog(biome.getValue());
	}

	/** Zone name used in template and loot table ids ({@code fields}, {@code woods}, {@code necropolis}) of a zone boss id. */
	public static String zoneName(Identifier bossId) {
		return ZoneLooks.zoneName(bossId);
	}

	private static RegistryKey<Biome> biome(String path) {
		return RegistryKey.of(RegistryKeys.BIOME, Embervoid.id(path));
	}

	private ReachZones() {}
}
