package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Mob;
import com.embervoid.balance.BalanceData.Structure;
import com.embervoid.balance.BalanceData.Zone;
import com.embervoid.balance.BalanceModel.Playthrough;
import com.embervoid.reach.gear.GearConfig;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Expected realm materials from a normal playthrough's route (R40.1c "loot rates sanity"): the chests on the way
 * (chest loot tables read from the game's files) and the realm loot on the mobs killed ({@code gear.loot} chances,
 * {@code reach.gear.GearLoot} roles), against what the Smoldering Plate and the realm weapons cost.
 */
final class LootBudget {
	static final String CINDER = "embervoid:cinder_shard";
	static final String AMBER = "embervoid:royal_amber";
	static final String RAW_GRAVE_IRON = "embervoid:raw_grave_iron";
	static final String GRAVE_IRON_INGOT = "embervoid:grave_iron_ingot";
	static final String SMOLDERING_INGOT = "embervoid:smoldering_ingot";
	static final String TEMPLATE = "embervoid:smoldering_template";
	static final String SET_PIECE = "set piece";
	static final String[] ITEMS = {CINDER, AMBER, RAW_GRAVE_IRON, GRAVE_IRON_INGOT, SMOLDERING_INGOT, TEMPLATE, SET_PIECE};

	/** The zone's ore material (GearLoot): Citadel mobs drop none. */
	private static final Map<Zone, String> MATERIAL = Map.of(Zone.FIELDS, CINDER, Zone.WOODS, AMBER, Zone.NECROPOLIS, RAW_GRAVE_IRON);

	private static final GearConfig.Loot LOOT = BalanceModel.GEAR.loot();

	/** Mean drop of one ore block without Fortune: Cinder Ore 1–3, Amber Vein 1–2, Grave Iron Ore 1. */
	static final Map<String, Double> PER_ORE_BLOCK = Map.of(CINDER, 2.0, AMBER, 1.5, RAW_GRAVE_IRON, 1.0);

	private LootBudget() {}

	/** Materials one zone's route gives, by item. */
	static Map<String, Double> zone(Zone zone, Playthrough run) {
		Map<String, Double> got = new LinkedHashMap<>();
		String folder = zone.name().toLowerCase(java.util.Locale.ROOT);
		if (zone == Zone.CITADEL) {
			chests(got, "citadel_common", 8);
			chests(got, "citadel_treasure", 1);
			kills(got, zone, BalanceData.CITADEL);
			kills(got, zone, Map.of("cinder_hound", run.naturalKillsDepths()));
			return got;
		}
		// Two road outposts with a common chest each, the structures by weight, the arena chest.
		chests(got, folder + "_common", 2);
		double weights = BalanceData.STRUCTURES.stream().filter(s -> s.zone() == zone).mapToInt(Structure::weight).sum();
		for (Structure structure : BalanceData.STRUCTURES) {
			if (structure.zone() != zone) {
				continue;
			}
			double count = run.structuresPerZone() * structure.weight() / weights;
			if (!structure.chest().isEmpty()) {
				chests(got, folder + "_" + structure.chest(), count);
			}
			Map<String, Double> mobs = new HashMap<>();
			structure.mobs().forEach((id, n) -> mobs.put(id, n * count));
			killsWeighted(got, zone, mobs);
		}
		chests(got, folder + "_arena", 1);
		kills(got, zone, BalanceData.OUTPOSTS.get(zone));
		if (zone == Zone.NECROPOLIS) {
			// A crypt: burial hall and warden chamber (common), lower crypt (treasure).
			chests(got, "necropolis_common", 2.0 * run.cryptsPerZone());
			chests(got, "necropolis_treasure", run.cryptsPerZone());
			Map<String, Double> crypt = new HashMap<>();
			BalanceData.CRYPT.forEach((id, n) -> crypt.put(id, (double) n * run.cryptsPerZone()));
			killsWeighted(got, zone, crypt);
		}
		Map<String, Integer> natural = BalanceData.NATURAL.get(zone);
		double total = natural.values().stream().mapToInt(Integer::intValue).sum();
		Map<String, Double> naturalKills = new HashMap<>();
		natural.forEach((id, weight) -> naturalKills.put(id, run.naturalKillsPerZone() * weight / total));
		killsWeighted(got, zone, naturalKills);
		return got;
	}

	private static void chests(Map<String, Double> got, String table, double opened) {
		for (String item : ITEMS) {
			if (!item.equals(SET_PIECE)) {
				got.merge(item, opened * GameFiles.expectedCount(table, item), Double::sum);
			}
		}
		// Set pieces in treasure chests: one pool of the zone's four (the Citadel's: any zone's).
		if (table.endsWith("_treasure")) {
			got.merge(SET_PIECE, opened * (table.startsWith("citadel") ? 0.35 : 0.3), Double::sum);
		}
	}

	private static void kills(Map<String, Double> got, Zone zone, Map<String, Integer> mobs) {
		Map<String, Double> weighted = new HashMap<>();
		mobs.forEach((id, n) -> weighted.put(id, (double) n));
		killsWeighted(got, zone, weighted);
	}

	/** Realm loot of the killed mobs: placed-only mobs and the Ashen Carrion at the elite rates (GearLoot). */
	private static void killsWeighted(Map<String, Double> got, Zone zone, Map<String, Double> mobs) {
		String material = MATERIAL.get(zone);
		mobs.forEach((id, n) -> {
			Mob mob = BalanceData.mob(id);
			boolean elite = mob.placedOnly() || id.equals("ashen_carrion");
			if (material != null) {
				got.merge(material, n * (elite ? LOOT.eliteMaterial() * 1.5 : LOOT.fodderMaterial()), Double::sum);
			}
			// The Ash Stalker and Crypt Moth carry no set; the Citadel has none.
			if (zone != Zone.CITADEL && !id.equals("ash_stalker") && !id.equals("crypt_moth")) {
				got.merge(SET_PIECE, n * (elite ? LOOT.eliteSet() : LOOT.fodderSet()), Double::sum);
			}
		});
	}

	/** The whole realm before the Smoldering Lord (three zones), by item. */
	static Map<String, Double> beforeLord(Playthrough run) {
		Map<String, Double> total = new LinkedHashMap<>();
		for (Zone zone : Zone.OPEN) {
			zone(zone, run).forEach((item, n) -> total.merge(item, n, Double::sum));
		}
		return total;
	}

	/**
	 * What the Smoldering Plate (plus the three zone uniques when {@code zoneWeapons}: each 4 of its zone's material
	 * since 0.5.0) costs in raw materials, after the Smoldering Ingots and templates found: grave iron (ingots and raw
	 * count the same), cinder, amber, by item.
	 */
	static Map<String, Double> need(Map<String, Double> found, boolean zoneWeapons) {
		double ingots = Math.max(0, 4 - found.getOrDefault(SMOLDERING_INGOT, 0.0));
		double templates = Math.max(0, 4 - found.getOrDefault(TEMPLATE, 0.0));
		int weapon = zoneWeapons ? 4 : 0;
		Map<String, Double> need = new LinkedHashMap<>();
		need.put(RAW_GRAVE_IRON, 4 * ingots + weapon);
		need.put(CINDER, 2 * ingots + 6 * templates + weapon);
		need.put(AMBER, 2 * ingots + weapon);
		return need;
	}

	/** Ore blocks still to mine for {@code need}, by material, after the route's drops. */
	static Map<String, Double> oreToMine(Map<String, Double> found, Map<String, Double> need) {
		Map<String, Double> mine = new LinkedHashMap<>();
		need.forEach((item, n) -> {
			double have = found.getOrDefault(item, 0.0) + (item.equals(RAW_GRAVE_IRON) ? found.getOrDefault(GRAVE_IRON_INGOT, 0.0) : 0);
			mine.put(item, Math.max(0, n - have) / PER_ORE_BLOCK.get(item));
		});
		return mine;
	}
}
