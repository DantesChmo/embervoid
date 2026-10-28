package com.embervoid.reach.mob;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.reach.mob.ReachMobConfig.Spawn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReachMobConfigTest {
	/** Registered before any parse, or the parse would not read the section at all. */
	private static final ConfigSection<ReachMobConfig> SECTION = ReachMobConfig.SECTION;

	private static ReachMobConfig parse(String spawnsJson) {
		return EmbervoidConfig.fromNetworkString("{\"reachMobs\": {\"spawns\": " + spawnsJson + "}}").section(SECTION);
	}

	@Test
	void emptySpawnsReadAsDefaults() {
		assertEquals(ReachMobConfig.DEFAULT, parse("{}"));
		assertEquals(ReachMobConfig.DEFAULT, EmbervoidConfig.fromNetworkString("{}").section(SECTION));
	}

	@Test
	void editedSpawnsSurviveTheNetworkRoundTrip() {
		EmbervoidConfig edited = EmbervoidConfig.fromNetworkString(
				"{\"reachMobs\": {\"spawns\": {\"crypt_moth\": {\"weight\": 7, \"minGroup\": 2, \"maxGroup\": 9}}}}");
		ReachMobConfig config = EmbervoidConfig.fromNetworkString(EmbervoidConfig.toNetworkString(edited)).section(SECTION);
		assertEquals(edited.section(SECTION), config);
		assertEquals(new Spawn(7, 2, 9), config.spawns().get("crypt_moth"));
	}

	@Test
	void oneMobsEntryChangesOnlyThatMob() {
		ReachMobConfig config = parse("{\"cinder_hound\": {\"weight\": 20, \"minGroup\": 1, \"maxGroup\": 2}}");
		assertEquals(new Spawn(20, 1, 2), config.spawns().get("cinder_hound"));
		assertEquals(ReachMobConfig.DEFAULT.spawns().get("cinder_husk"), config.spawns().get("cinder_husk"));
		assertEquals(ReachMobConfig.DEFAULT.spawns().size(), config.spawns().size());
	}

	@Test
	void weightZeroTurnsASpawnOff() {
		assertEquals(0, parse("{\"ashen_carrion\": {\"weight\": 0}}").spawns().get("ashen_carrion").weight());
	}

	@Test
	void raisingMinGroupAboveTheDefaultMaxRaisesTheMaxWithIt() {
		// cinder_husk groups are 2–4 by default.
		Spawn spawn = parse("{\"cinder_husk\": {\"minGroup\": 6}}").spawns().get("cinder_husk");
		assertEquals(6, spawn.minGroup());
		assertEquals(6, spawn.maxGroup());
	}

	@Test
	void maxGroupBelowMinGroupFallsBackToTheDefaultMaxButNeverBelowTheMin() {
		Spawn def = ReachMobConfig.DEFAULT.spawns().get("cinder_husk");
		int min = def.maxGroup() - 1;
		Spawn kept = parse("{\"cinder_husk\": {\"minGroup\": " + min + ", \"maxGroup\": " + (min - 1) + "}}").spawns().get("cinder_husk");
		assertEquals(new Spawn(def.weight(), min, def.maxGroup()), kept);
		int high = def.maxGroup() + 3;
		Spawn raised = parse("{\"cinder_husk\": {\"minGroup\": " + high + ", \"maxGroup\": 1}}").spawns().get("cinder_husk");
		assertEquals(new Spawn(def.weight(), high, high), raised);
	}

	@Test
	void outOfRangeGroupSizesAndWeightsAreRejected() {
		Spawn def = ReachMobConfig.DEFAULT.spawns().get("restless_bones");
		Spawn spawn = parse("{\"restless_bones\": {\"minGroup\": 17, \"maxGroup\": 40, \"weight\": 1001}}").spawns().get("restless_bones");
		assertEquals(def, spawn);
		Spawn negative = parse("{\"restless_bones\": {\"minGroup\": 0, \"weight\": -1}}").spawns().get("restless_bones");
		assertEquals(def, negative);
	}

	@Test
	void unknownMobsAreIgnored() {
		ReachMobConfig config = parse("{\"zombie\": {\"weight\": 5}}");
		assertFalse(config.spawns().containsKey("zombie"));
		assertEquals(ReachMobConfig.DEFAULT, config);
	}
}
