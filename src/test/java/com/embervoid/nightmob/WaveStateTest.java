package com.embervoid.nightmob;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WaveStateTest {
	private static final UUID ALICE = new UUID(1, 1);
	private static final UUID BOB = new UUID(2, 2);
	private static final UUID ZOMBIE = new UUID(10, 1);
	private static final UUID GHOST = new UUID(20, 1);

	private static WaveState twoWaves() {
		WaveState state = new WaveState();
		WaveState.Wave zombies = new WaveState.Wave(WaveRules.Kind.ZOMBIES);
		zombies.mobs.add(ZOMBIE);
		WaveState.Wave ghosts = new WaveState.Wave(WaveRules.Kind.GHOSTS);
		ghosts.mobs.add(GHOST);
		state.waves.put(ALICE, zombies);
		state.waves.put(BOB, ghosts);
		return state;
	}

	@Test
	void ownerOfFindsTheWaveAMobBelongsTo() {
		WaveState state = twoWaves();
		assertEquals(ALICE, state.ownerOf(ZOMBIE));
		assertEquals(BOB, state.ownerOf(GHOST));
		assertNull(state.ownerOf(new UUID(99, 99)));
	}
}
