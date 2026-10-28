package com.embervoid.reach.gear;

import com.embervoid.Embervoid;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GearLootTablesTest {
	private static final Path LOOT_TABLES = Path.of("src", "main", "generated", "data", "embervoid", "loot_tables");

	@Test
	void everyZoneHasThreeChestTablesAndTheCitadelTwo() {
		List<Identifier> all = GearLootTables.all();
		assertEquals(GearLootTables.ZONES.size() * 3 + 2, all.size());
		assertEquals(all.size(), new HashSet<>(all).size());
		for (String zone : GearLootTables.ZONES) {
			assertTrue(all.contains(GearLootTables.common(zone)), zone);
			assertTrue(all.contains(GearLootTables.treasure(zone)), zone);
			assertTrue(all.contains(GearLootTables.arena(zone)), zone);
		}
	}

	@Test
	void citadelTablesComeLast() {
		List<Identifier> all = GearLootTables.all();
		assertEquals(List.of(GearLootTables.CITADEL_COMMON, GearLootTables.CITADEL_TREASURE), all.subList(all.size() - 2, all.size()));
	}

	@Test
	void idsFollowTheFixedChestPattern() {
		assertEquals(Embervoid.id("chests/ashen_reach/woods_treasure"), GearLootTables.treasure("woods"));
		for (Identifier id : GearLootTables.all()) {
			assertEquals(Embervoid.MOD_ID, id.getNamespace());
			assertTrue(id.getPath().startsWith("chests/ashen_reach/"), id.toString());
		}
	}

	@Test
	void everyTableHasAGeneratedFile() {
		for (Identifier id : GearLootTables.all()) {
			assertTrue(Files.isRegularFile(LOOT_TABLES.resolve(id.getPath() + ".json")), id + " has no loot table file");
		}
	}
}
