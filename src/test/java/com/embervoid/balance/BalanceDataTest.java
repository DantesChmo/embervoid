package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Boss;
import com.embervoid.balance.BalanceData.Structure;
import com.embervoid.balance.BalanceData.Zone;
import com.embervoid.reach.mob.ReachMobConfig;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link BalanceData}'s world numbers against the game's own files: placed enemies in every template, structure-set
 * weights, natural spawn weights, arena boss levels. (Entity attributes are checked in the engine by the
 * {@code BalanceFightTests} GameTest.)
 */
class BalanceDataTest {
	private static String folder(Zone zone) {
		return zone.name().toLowerCase(Locale.ROOT);
	}

	@Test
	void minorStructuresHoldTheModelsEnemies() {
		for (Structure structure : BalanceData.STRUCTURES) {
			assertEquals(structure.mobs(), GameFiles.placedEnemies(folder(structure.zone()) + "/" + structure.name()), structure.name());
		}
	}

	@Test
	void structureWeightsMatchTheSets() {
		for (Zone zone : Zone.OPEN) {
			Map<String, Integer> weights = new HashMap<>();
			BalanceData.STRUCTURES.stream().filter(s -> s.zone() == zone).forEach(s -> weights.put(s.name(), s.weight()));
			assertEquals(GameFiles.structureWeights("ashen_reach_" + folder(zone)), weights, zone.title);
		}
	}

	@Test
	void outpostsCryptAndCitadelMatchTheirTemplates() {
		for (Zone zone : Zone.OPEN) {
			Map<String, Integer> both = new HashMap<>();
			GameFiles.placedEnemies("outpost/toll_gate_" + folder(zone)).forEach((id, n) -> both.merge(id, n, Integer::sum));
			GameFiles.placedEnemies("outpost/watchtower_" + folder(zone)).forEach((id, n) -> both.merge(id, n, Integer::sum));
			assertEquals(BalanceData.OUTPOSTS.get(zone), both, zone.title + " outposts");
		}
		Map<String, Integer> crypt = new HashMap<>();
		for (String room : List.of("entrance", "descent", "junction", "corridor", "flooded_hall", "burial_hall", "warden_chamber",
				"lower_crypt", "dead_end")) {
			GameFiles.placedEnemies("necropolis/crypt/" + room).forEach((id, n) -> crypt.merge(id, n, Integer::sum));
		}
		assertEquals(BalanceData.CRYPT, crypt, "one of each crypt room");
		Map<String, Integer> citadel = new HashMap<>();
		for (String piece : List.of("outskirts", "street", "market", "gatehouse", "wall_west", "wall_east", "tower_west",
				"tower_east", "inner_ring_west", "inner_ring_east", "keep")) {
			GameFiles.placedEnemies("citadel/" + piece).forEach((id, n) -> citadel.merge(id, n, Integer::sum));
		}
		assertEquals(BalanceData.CITADEL, citadel, "every Citadel piece once");
	}

	@Test
	void naturalSpawnWeightsMatchTheConfig() {
		Map<String, ReachMobConfig.Spawn> spawns = ReachMobConfig.DEFAULT.spawns();
		BalanceData.NATURAL.forEach((zone, weights) -> weights.forEach((id, weight) ->
				assertEquals(spawns.get(id).weight(), weight, id)));
		int listed = BalanceData.NATURAL.values().stream().mapToInt(Map::size).sum();
		assertEquals(spawns.size(), listed, "every naturally spawning mob is in the model");
	}

	@Test
	void bossLevelsMatchTheArenas() {
		Map<Boss, String> arenas = Map.of(BalanceData.ASH_COLOSSUS, "ashen_reach_arena_a", BalanceData.GRIEVING_TREE, "ashen_reach_arena_b",
				BalanceData.CRYPT_KEEPER, "ashen_reach_arena_c", BalanceData.SMOLDERING_LORD, "ashen_reach_arena_final");
		arenas.forEach((boss, arena) -> {
			JsonObject json = GameFiles.arenaBoss(arena);
			assertEquals("embervoid:" + boss.id(), json.get("entity").getAsString());
			assertEquals(boss.level(), json.get("level").getAsInt(), boss.name());
		});
	}
}
