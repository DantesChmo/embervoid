package com.embervoid.realm;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;
import java.util.stream.Stream;

/**
 * One realm: its dimension, its angle sectors (the open zones and the final zone behind the Citadel Gate, DESIGN
 * §4.1), its bosses and mob levels. Worldgen (zones, arenas, radius) is datapack JSON; this is what the game logic
 * needs to know about it, and a GameTest holds the two together.
 *
 * @param id            progress id of the realm, e.g. {@code embervoid:realm_1}
 * @param zoneBosses    progress ids of the zone bosses, in zone order; all of them open the Citadel Gate
 * @param finalBoss     progress id of the realm boss; beating it completes the realm
 * @param sectors       the realm's angle sectors, as the biome source's {@code start_angle} and {@code final_width}
 * @param zoneLevels    mob level band of each open zone, in zone order (same order as the biome source's zones)
 * @param landingLevels mob level band on the landing disc
 * @param finalLevels   mob level band of the final zone's sector
 * @param radius        realm radius in blocks, as in the dimension JSON; past it the void pushes players back
 */
public record RealmDefinition(Identifier id, RegistryKey<World> world, List<Identifier> zoneBosses, Identifier finalBoss,
		RealmLayout.Sectors sectors, List<LevelBand> zoneLevels, LevelBand landingLevels, LevelBand finalLevels, int radius) {

	/** Inclusive mob level range. */
	public record LevelBand(int min, int max) {}

	public boolean contains(RegistryKey<World> key) {
		return world.equals(key);
	}

	/** Every boss of the realm: the zone bosses, then the realm boss. */
	public List<Identifier> allBosses() {
		return Stream.concat(zoneBosses.stream(), Stream.of(finalBoss)).toList();
	}

	/** Level band of a {@link RealmLayout#zoneAt} result in the realm world: its open zone, the final zone or the landing. */
	public LevelBand levelsOf(int zone) {
		if (zone == RealmLayout.FINAL) {
			return finalLevels;
		}
		return zone >= 0 && zone < zoneLevels.size() ? zoneLevels.get(zone) : landingLevels;
	}
}
