package com.embervoid.entity;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemnantRegistryTest {
	private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
	private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000b0b");
	private static final UUID FIRST = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID SECOND = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Test
	void newRemnantReplacesTheOldOne() {
		RemnantRegistry registry = new RemnantRegistry();
		registry.setRemnant(ALICE, FIRST);
		registry.setRemnant(ALICE, SECOND);
		assertEquals(SECOND, registry.getRemnant(ALICE));
	}

	@Test
	void eachPlayerHasTheirOwn() {
		RemnantRegistry registry = new RemnantRegistry();
		registry.setRemnant(ALICE, FIRST);
		assertNull(registry.getRemnant(BOB));
	}

	@Test
	void reclaimingTheCurrentRemnantForgetsIt() {
		RemnantRegistry registry = new RemnantRegistry();
		registry.setRemnant(ALICE, FIRST);
		registry.clearRemnant(ALICE, FIRST);
		assertNull(registry.getRemnant(ALICE));
	}

	@Test
	void reclaimingAnOldRemnantKeepsTheNewer() {
		RemnantRegistry registry = new RemnantRegistry();
		registry.setRemnant(ALICE, SECOND);
		registry.setDirty(false);
		registry.clearRemnant(ALICE, FIRST);
		assertEquals(SECOND, registry.getRemnant(ALICE));
		assertFalse(registry.isDirty());
	}

	@Test
	void changesMarkTheRegistryForSaving() {
		RemnantRegistry registry = new RemnantRegistry();
		registry.setRemnant(ALICE, FIRST);
		assertTrue(registry.isDirty());
		registry.setDirty(false);
		registry.clearRemnant(ALICE, FIRST);
		assertTrue(registry.isDirty());
	}

	@Test
	void savedByOwnerId() {
		RemnantRegistry registry = new RemnantRegistry();
		registry.setRemnant(ALICE, FIRST);
		registry.setRemnant(BOB, SECOND);
		NbtCompound owners = registry.writeNbt(new NbtCompound()).getCompound("Remnants");
		assertEquals(2, owners.getKeys().size());
		assertEquals(FIRST, owners.getUuid(ALICE.toString()));
		assertEquals(SECOND, owners.getUuid(BOB.toString()));
	}

	@Test
	void aSavedRegistryLoadsBackTheSameRemnants() {
		RemnantRegistry registry = new RemnantRegistry();
		registry.setRemnant(ALICE, FIRST);
		registry.setRemnant(BOB, SECOND);
		RemnantRegistry loaded = RemnantRegistry.fromNbt(registry.writeNbt(new NbtCompound()));
		assertEquals(FIRST, loaded.getRemnant(ALICE));
		assertEquals(SECOND, loaded.getRemnant(BOB));
		assertFalse(loaded.isDirty());
	}

	@Test
	void aWorldWithoutSavedRemnantsLoadsEmpty() {
		RemnantRegistry loaded = RemnantRegistry.fromNbt(new NbtCompound());
		assertNull(loaded.getRemnant(ALICE));
	}
}
