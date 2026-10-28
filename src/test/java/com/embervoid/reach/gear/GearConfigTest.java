package com.embervoid.reach.gear;

import com.embervoid.config.EmbervoidConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GearConfigTest {
	private static final GearConfig DEFAULTS = GearConfig.SECTION.defaults();

	private static GearConfig parse(String gearJson) {
		return EmbervoidConfig.fromNetworkString("{\"gear\": " + gearJson + "}").section(GearConfig.SECTION);
	}

	private static GearConfig parse(JsonObject gear) {
		JsonObject root = new JsonObject();
		root.add("gear", gear);
		return EmbervoidConfig.fromNetworkString(root.toString()).section(GearConfig.SECTION);
	}

	private static JsonObject writtenDefaults() {
		JsonObject all = JsonParser.parseString(EmbervoidConfig.toNetworkString(EmbervoidConfig.DEFAULT)).getAsJsonObject();
		return all.getAsJsonObject(GearConfig.SECTION.name());
	}

	@Test
	void missingSectionReadsAsDefaults() {
		assertEquals(DEFAULTS, parse("{}"));
		assertEquals(DEFAULTS, EmbervoidConfig.fromNetworkString("{}").section(GearConfig.SECTION));
	}

	@Test
	void writtenSectionHasEveryGroup() {
		JsonObject written = writtenDefaults();
		for (String group : new String[]{"bleeding", "smoldering", "sets", "uniques", "weapons", "crossbow", "consumables", "loot"}) {
			assertTrue(written.has(group) && written.get(group).isJsonObject(), group);
		}
	}

	/** Each written key is read back from the same name: changing it in the file changes the parsed config. */
	@Test
	void everyWrittenKeyIsRead() {
		JsonObject written = writtenDefaults();
		for (Map.Entry<String, JsonElement> group : written.entrySet()) {
			for (Map.Entry<String, JsonElement> key : group.getValue().getAsJsonObject().entrySet()) {
				double value = key.getValue().getAsDouble();
				boolean changed = false;
				for (double candidate : new double[]{value + 1, value - 1, Math.round(value / 2), 0, 1, value / 2}) {
					if (candidate == value) {
						continue;
					}
					JsonObject edited = written.deepCopy();
					edited.getAsJsonObject(group.getKey()).add(key.getKey(), new JsonPrimitive(candidate));
					if (!parse(edited).equals(DEFAULTS)) {
						changed = true;
						break;
					}
				}
				assertTrue(changed, group.getKey() + "." + key.getKey() + " is written but never read");
			}
		}
	}

	@Test
	void validValuesAreKeptAndTheRestStayDefault() {
		GearConfig config = parse("{\"bleeding\": {\"maxLevel\": 5, \"damagePerLevel\": 1.25}, \"crossbow\": {\"chargeTicks\": 40}}");
		assertEquals(new GearConfig.Bleeding(DEFAULTS.bleeding().durationTicks(), 5, 1.25f), config.bleeding());
		assertEquals(40, config.crossbow().chargeTicks());
		assertEquals(DEFAULTS.crossbow().boltDamage(), config.crossbow().boltDamage());
		assertEquals(DEFAULTS.weapons(), config.weapons());
		assertEquals(DEFAULTS.loot(), config.loot());
	}

	@Test
	void outOfRangeValuesFallBackToTheirDefault() {
		GearConfig config = parse("{\"bleeding\": {\"maxLevel\": 0, \"durationTicks\": 10},"
				+ " \"weapons\": {\"sweepShare\": 5, \"rampWindowTicks\": 0},"
				+ " \"loot\": {\"eliteSet\": 1.5}, \"smoldering\": {\"fireReductionPerPiece\": 0.3}}");
		assertEquals(DEFAULTS.bleeding(), config.bleeding());
		assertEquals(DEFAULTS.weapons(), config.weapons());
		assertEquals(DEFAULTS.loot(), config.loot());
		assertEquals(DEFAULTS.smoldering(), config.smoldering());
	}

	@Test
	void boundsThemselvesAreAccepted() {
		GearConfig config = parse("{\"bleeding\": {\"maxLevel\": 10, \"durationTicks\": 20},"
				+ " \"smoldering\": {\"fireReductionPerPiece\": 0.25}, \"crossbow\": {\"chargeTicks\": 5}}");
		assertEquals(10, config.bleeding().maxLevel());
		assertEquals(20, config.bleeding().durationTicks());
		assertEquals(0.25f, config.smoldering().fireReductionPerPiece());
		assertEquals(5, config.crossbow().chargeTicks());
	}

	@Test
	void fractionalTickCountsAreRejected() {
		GearConfig config = parse("{\"weapons\": {\"harvestCooldown\": 12.5}, \"crossbow\": {\"pierce\": 1.0}}");
		assertEquals(DEFAULTS.weapons().harvestCooldown(), config.weapons().harvestCooldown());
		// A whole number written with a decimal point is still a whole number.
		assertEquals(1, config.crossbow().pierce());
	}

	@Test
	void wrongTypesFallBackToDefaults() {
		GearConfig config = parse("{\"bleeding\": \"lots\", \"crossbow\": {\"boltDamage\": \"high\", \"punch\": [1]},"
				+ " \"consumables\": {\"oilMinutes\": true}}");
		assertEquals(DEFAULTS.bleeding(), config.bleeding());
		assertEquals(DEFAULTS.crossbow(), config.crossbow());
		assertEquals(DEFAULTS.consumables(), config.consumables());
	}

	@Test
	void editedConfigSurvivesTheNetworkRoundTrip() {
		JsonObject edited = writtenDefaults();
		edited.getAsJsonObject("weapons").addProperty("lungeDash", 6.5);
		edited.getAsJsonObject("uniques").addProperty("novaCooldownTicks", 600);
		edited.getAsJsonObject("loot").addProperty("fodderUnique", 0.02);
		JsonObject root = new JsonObject();
		root.add("gear", edited);
		EmbervoidConfig first = EmbervoidConfig.fromNetworkString(root.toString());
		EmbervoidConfig second = EmbervoidConfig.fromNetworkString(EmbervoidConfig.toNetworkString(first));
		GearConfig config = second.section(GearConfig.SECTION);
		assertEquals(first.section(GearConfig.SECTION), config);
		assertEquals(6.5f, config.weapons().lungeDash());
		assertEquals(600, config.uniques().novaCooldownTicks());
		assertNotEquals(DEFAULTS, config);
		assertFalse(config.loot().equals(DEFAULTS.loot()));
	}
}
