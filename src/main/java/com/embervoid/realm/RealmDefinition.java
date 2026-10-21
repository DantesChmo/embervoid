package com.embervoid.realm;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;
import java.util.stream.Stream;

/**
 * One realm: its two dimensions (open zones and the final zone behind the Sealed Gate), its bosses and mob levels.
 * Worldgen (zones, arenas, radius) is datapack JSON; this is what the game logic needs to know about it.
 *
 * @param id            progress id of the realm, e.g. {@code embervoid:ashen_reach}
 * @param zoneBosses    progress ids of the zone bosses, in zone order; all of them open the Sealed Gate
 * @param finalBoss     progress id of the realm boss; beating it completes the realm
 * @param zoneLevels    mob level band of each zone, in zone order (same order as the biome source's zones)
 * @param landingLevels mob level band on the landing disc
 * @param depthsLevels  mob level band of the final zone
 * @param radius        open-zone radius in blocks, as in the dimension JSON; past it the void pushes players back
 * @param depthsRadius  final-zone radius in blocks
 */
public record RealmDefinition(Identifier id, RegistryKey<World> world, RegistryKey<World> depths,
		List<Identifier> zoneBosses, Identifier finalBoss, List<LevelBand> zoneLevels, LevelBand landingLevels,
		LevelBand depthsLevels, int radius, int depthsRadius) {

	/** Inclusive mob level range. */
	public record LevelBand(int min, int max) {}

	public boolean contains(RegistryKey<World> key) {
		return world.equals(key) || depths.equals(key);
	}

	/** Every boss of the realm: the zone bosses, then the realm boss. */
	public List<Identifier> allBosses() {
		return Stream.concat(zoneBosses.stream(), Stream.of(finalBoss)).toList();
	}

	public int radiusOf(RegistryKey<World> key) {
		return depths.equals(key) ? depthsRadius : radius;
	}
}
