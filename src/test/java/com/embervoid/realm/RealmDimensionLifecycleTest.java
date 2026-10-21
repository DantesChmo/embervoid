package com.embervoid.realm;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmDimensionLifecycleTest {
	private static final Identifier OVERWORLD = new Identifier("overworld");
	private static final Identifier NETHER = new Identifier("the_nether");
	private static final Identifier END = new Identifier("the_end");
	private static final Identifier REALM = new Identifier("embervoid", "ashen_reach");
	private static final Identifier DEPTHS = new Identifier("embervoid", "ashen_reach_depths");
	private static final Set<Identifier> OURS = Set.of(REALM, DEPTHS);

	@Test
	void vanillaPlusRealmsIsStable() {
		assertTrue(RealmDimensionLifecycle.onlyVanillaBesides(List.of(OVERWORLD, NETHER, END, REALM, DEPTHS), OURS));
	}

	@Test
	void duplicatesFromBothRegistriesCountOnce() {
		assertTrue(RealmDimensionLifecycle.onlyVanillaBesides(List.of(OVERWORLD, NETHER, END, OVERWORLD, REALM, REALM), OURS));
	}

	@Test
	void anotherModsDimensionStillWarns() {
		assertFalse(RealmDimensionLifecycle.onlyVanillaBesides(
				List.of(OVERWORLD, NETHER, END, REALM, DEPTHS, new Identifier("othermod", "moon")), OURS));
	}

	@Test
	void missingVanillaDimensionStillWarns() {
		assertFalse(RealmDimensionLifecycle.onlyVanillaBesides(List.of(OVERWORLD, END, REALM, DEPTHS), OURS));
	}
}
