package com.embervoid.hub.stele;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SteleStateTest {
	private static final UUID ALICE = new UUID(1, 1);
	private static final UUID BOB = new UUID(2, 2);

	@Test
	void replacingTheSteleHandsBackItsUsersOnceAndForgetsThem() {
		SteleState state = new SteleState();
		state.used(ALICE);
		state.used(BOB);
		state.used(ALICE);
		assertEquals(Set.of(ALICE, BOB), state.replace(null));
		assertEquals(Set.of(), state.replace(null));
		assertNull(state.lit());
	}
}
