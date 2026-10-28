package com.embervoid.reliquary;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Accessories drop from each zone's treasure chest and its elite (DESIGN §4.5): those loot tables must exist. */
class ReliquaryLootZoneTest {
	private static boolean tableExists(Identifier id) {
		String file = "data/" + id.getNamespace() + "/loot_tables/" + id.getPath() + ".json";
		return Files.exists(Path.of("src/main/resources", file)) || Files.exists(Path.of("src/main/generated", file));
	}

	@Test
	void everyZoneHasATreasureChestTable() {
		for (String zone : List.of("fields", "woods", "necropolis", "citadel")) {
			Identifier chest = new ReliquaryLoot.Zone(zone, List.of(), List.of(), null).treasureChest();
			assertTrue(tableExists(chest), "no table " + chest);
		}
	}

	@Test
	void everyEliteHasAnEntityLootTable() {
		for (String elite : List.of("ashen_carrion", "grave_warden", "smoldering_knight")) {
			Identifier table = new ReliquaryLoot.Zone("any", List.of(), List.of(), new Identifier("embervoid", elite)).eliteTable();
			assertTrue(tableExists(table), "no table " + table);
		}
	}

	@Test
	void aZoneWithoutAnEliteHasNoEliteTable() {
		assertNull(new ReliquaryLoot.Zone("woods", List.of(), List.of(), null).eliteTable());
	}
}
