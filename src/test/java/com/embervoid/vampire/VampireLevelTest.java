package com.embervoid.vampire;

import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmDefinition.LevelBand;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VampireLevelTest {
	@Test
	void freshPlayersMeetTheMinimum() {
		assertEquals(15, VampireLevel.of(List.of(1, 3), 15, 20));
	}

	@Test
	void highestPlayerLevelCounts() {
		assertEquals(18, VampireLevel.of(List.of(4, 18, 12), 15, 30));
	}

	@Test
	void cappedByTheHighestUnlockedRealm() {
		assertEquals(20, VampireLevel.of(List.of(40), 15, 20));
		assertEquals(30, VampireLevel.of(List.of(40), 15, 30));
	}

	@Test
	void capNeverBelowMinimum() {
		assertEquals(15, VampireLevel.of(List.of(40), 15, 10));
	}

	@Test
	void nobodyGivesTheMinimum() {
		assertEquals(15, VampireLevel.of(List.of(), 15, 20));
	}

	private static final Identifier FIRST = new Identifier("embervoid", "realm_1");
	private static final Identifier SECOND = new Identifier("embervoid", "realm_2");
	/** Realms as hand-made bands: the first tops out in its final zone at 30, the second in a zone at 44. */
	private static final List<RealmDefinition> REALMS = List.of(
			realm(FIRST, new LevelBand(15, 15), List.of(new LevelBand(15, 18), new LevelBand(21, 25)), new LevelBand(25, 30)),
			realm(SECOND, new LevelBand(30, 30), List.of(new LevelBand(31, 44), new LevelBand(35, 40)), new LevelBand(38, 42)));

	private static RealmDefinition realm(Identifier id, LevelBand landing, List<LevelBand> zones, LevelBand finalZone) {
		return new RealmDefinition(id, null, List.of(), id, null, zones, landing, finalZone, 1000);
	}

	@Test
	void withNoRealmUnlockedTheOverworldCapHolds() {
		assertEquals(20, VampireLevel.cap(REALMS, id -> false, 20));
		assertEquals(20, VampireLevel.cap(List.of(), id -> true, 20));
	}

	@Test
	void theCapIsTheTopOfTheRealmsHighestBand() {
		assertEquals(30, VampireLevel.cap(REALMS, Set.of(FIRST)::contains, 20));
	}

	@Test
	void theTopCanSitInAZone() {
		assertEquals(44, VampireLevel.cap(REALMS, Set.of(FIRST, SECOND)::contains, 20));
	}

	@Test
	void theLastUnlockedRealmInOrderCounts() {
		assertEquals(44, VampireLevel.cap(REALMS, Set.of(SECOND)::contains, 20));
	}

	@Test
	void aRealmsTopLevelLooksAtEveryBand() {
		assertEquals(9, VampireLevel.topLevel(realm(FIRST, new LevelBand(9, 9), List.of(new LevelBand(1, 2)), new LevelBand(3, 4))));
		assertEquals(7, VampireLevel.topLevel(realm(FIRST, new LevelBand(1, 1), List.of(), new LevelBand(5, 7))));
	}
}
