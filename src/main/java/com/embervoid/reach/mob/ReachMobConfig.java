package com.embervoid.reach.mob;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.config.SectionReader;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The {@code reachMobs} config section: Realm 1 mobs (DESIGN §4.5, PLAN 10). Base levels outside the realm live in
 * {@code mobs.levels.baseLevels} like every other mob; inside it the zone bands decide.
 *
 * @param spawns natural spawn entries by mob id path (e.g. {@code cinder_husk}); applied at startup, weight 0 = none
 */
public record ReachMobConfig(Map<String, Spawn> spawns) {
	public static final ReachMobConfig DEFAULT = new ReachMobConfig(Map.of(
			"cinder_husk", new Spawn(100, 2, 4),
			"ash_stalker", new Spawn(35, 1, 1),
			"ashen_carrion", new Spawn(8, 1, 1),
			"withered_thrall", new Spawn(100, 1, 2),
			"lantern_hag", new Spawn(40, 1, 1),
			"restless_bones", new Spawn(100, 2, 3),
			"crypt_moth", new Spawn(50, 1, 3)));

	/** A biome spawn entry: weight among the zone's monsters, group size. */
	public record Spawn(int weight, int minGroup, int maxGroup) {}

	public static final ConfigSection<ReachMobConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"reachMobs", DEFAULT, ReachMobConfig::read, ReachMobConfig::write));

	public static ReachMobConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}

	private static ReachMobConfig read(SectionReader in) {
		SectionReader spawns = in.section("spawns");
		Map<String, Spawn> read = new LinkedHashMap<>();
		DEFAULT.spawns.keySet().stream().sorted().forEach(key -> {
			Spawn def = DEFAULT.spawns.get(key);
			SectionReader entry = spawns.section(key);
			int min = entry.readInt("minGroup", def.minGroup, 1, 16);
			read.put(key, new Spawn(entry.readInt("weight", def.weight, 0, 1000), min,
					entry.readInt("maxGroup", Math.max(def.maxGroup, min), min, 16)));
		});
		return new ReachMobConfig(Map.copyOf(read));
	}

	private static void write(ReachMobConfig config, JsonObject out) {
		JsonObject spawns = new JsonObject();
		config.spawns.keySet().stream().sorted().forEach(key -> {
			Spawn spawn = config.spawns.get(key);
			JsonObject entry = new JsonObject();
			entry.addProperty("weight", spawn.weight);
			entry.addProperty("minGroup", spawn.minGroup);
			entry.addProperty("maxGroup", spawn.maxGroup);
			spawns.add(key, entry);
		});
		out.add("spawns", spawns);
	}
}
