package com.embervoid.reliquary;

import com.embervoid.config.EmbervoidConfig;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The {@code reliquary} config section as README documents it, and the slot kinds it counts. */
class ReliquaryConfigTest {
	private static ReliquaryConfig read(String reliquaryJson) {
		return EmbervoidConfig.fromNetworkString("{\"reliquary\":" + reliquaryJson + "}").section(ReliquaryConfig.SECTION);
	}

	@Test
	void slotCountsComeFromTheConfig() {
		ReliquaryConfig config = read("{\"slots\":{\"artifact\":5,\"ring\":1}}");
		assertEquals(5, config.slots(SlotKinds.ARTIFACT));
		assertEquals(1, config.slots(SlotKinds.RING));
	}

	@Test
	void aKindTheConfigLeavesOutKeepsItsDefaultCount() {
		ReliquaryConfig config = read("{\"slots\":{\"artifact\":4}}");
		assertEquals(SlotKinds.RING.defaultCount(), config.slots(SlotKinds.RING));
		assertEquals(SlotKinds.ARTIFACT.defaultCount(), read("{}").slots(SlotKinds.ARTIFACT));
	}

	@Test
	void zeroHidesAKindAndEightIsTheMost() {
		assertEquals(0, read("{\"slots\":{\"ring\":0}}").slots(SlotKinds.RING));
		assertEquals(8, read("{\"slots\":{\"ring\":8}}").slots(SlotKinds.RING));
	}

	@Test
	void slotCountsOutsideZeroToEightFallBackToTheDefault() {
		for (String bad : new String[] {"9", "-1", "2.5", "\"two\"", "true"}) {
			assertEquals(SlotKinds.RING.defaultCount(), read("{\"slots\":{\"ring\":" + bad + "}}").slots(SlotKinds.RING), bad);
		}
	}

	@Test
	void artifactPointsAndDropChancesAreRead() {
		ReliquaryConfig config = read("{\"artifacts\":{\"points\":5,\"sealPoints\":2},"
				+ "\"drops\":{\"chestArtifact\":0.5,\"chestRing\":0,\"eliteArtifact\":1,\"eliteRing\":0.25}}");
		assertEquals(new ReliquaryConfig.Artifacts(5, 2), config.artifacts());
		assertEquals(new ReliquaryConfig.Drops(0.5f, 0, 1, 0.25f), config.drops());
	}

	@Test
	void outOfRangeNumbersFallBackToTheirDefaults() {
		ReliquaryConfig config = read("{\"artifacts\":{\"points\":100},\"drops\":{\"chestRing\":1.5},"
				+ "\"rings\":{\"featherSafeFall\":2,\"delverHasteLevel\":6}}");
		ReliquaryConfig defaults = read("{}");
		assertEquals(defaults.artifacts().points(), config.artifacts().points());
		assertEquals(defaults.drops().chestRing(), config.drops().chestRing());
		// A safe fall below vanilla's 3 blocks would make falls hurt more.
		assertEquals(defaults.rings().featherSafeFall(), config.rings().featherSafeFall());
		assertEquals(defaults.rings().delverHasteLevel(), config.rings().delverHasteLevel());
	}

	@Test
	void anEmptySectionIsTheDefault() {
		assertEquals(ReliquaryConfig.DEFAULT, read("{}"));
	}

	@Test
	void whatIsWrittenReadsBackTheSame() {
		String json = "{\"slots\":{\"artifact\":3,\"ring\":0},\"artifacts\":{\"points\":4,\"sealPoints\":2},"
				+ "\"rings\":{\"courierSprintBonus\":0.3,\"featherSafeFall\":10,\"featherMaxFallSpeed\":0.75,"
				+ "\"fogStalkerClearing\":3,\"delverHasteLevel\":2,\"embersFireSeconds\":5},"
				+ "\"drops\":{\"chestArtifact\":0.1,\"chestRing\":0.2,\"eliteArtifact\":0.3,\"eliteRing\":0.4}}";
		EmbervoidConfig custom = EmbervoidConfig.fromNetworkString("{\"reliquary\":" + json + "}");
		EmbervoidConfig again = EmbervoidConfig.fromNetworkString(EmbervoidConfig.toNetworkString(custom));
		assertEquals(custom.section(ReliquaryConfig.SECTION), again.section(ReliquaryConfig.SECTION));
		assertEquals(0, again.section(ReliquaryConfig.SECTION).slots(SlotKinds.RING));
	}

	@Test
	void kindsAreFoundByTheirSavedName() {
		assertSame(SlotKinds.ARTIFACT, SlotKinds.byName("artifact"));
		assertSame(SlotKinds.RING, SlotKinds.byName("ring"));
		assertNull(SlotKinds.byName("amulet"));
		assertNull(SlotKinds.byName("Ring"));
		Set<String> names = new HashSet<>();
		for (SlotKind kind : SlotKinds.ALL) {
			assertTrue(names.add(kind.name()), "two kinds named " + kind.name());
			assertSame(kind, SlotKinds.byName(kind.name()));
		}
	}

	@Test
	void eachKindHasItsOwnTag() {
		assertEquals("embervoid:artifacts", SlotKinds.ARTIFACT.tagId().toString());
		assertEquals("embervoid:rings", SlotKinds.RING.tagId().toString());
	}
}
