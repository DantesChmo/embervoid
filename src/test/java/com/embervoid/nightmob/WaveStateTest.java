package com.embervoid.nightmob;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveStateTest {
	private static final UUID ALICE = new UUID(0, 1);
	private static final UUID BOB = new UUID(0, 2);
	private static final UUID ZOMBIE_1 = new UUID(1, 1);
	private static final UUID ZOMBIE_2 = new UUID(1, 2);
	private static final UUID GHOST = new UUID(2, 1);

	private static WaveState roundTrip(WaveState state) {
		return WaveState.fromNbt(state.writeNbt(new NbtCompound()));
	}

	@Test
	void wavesSurviveASave() {
		WaveState state = new WaveState();
		state.night = 4;
		WaveState.Wave zombies = new WaveState.Wave(WaveRules.Kind.ZOMBIES);
		zombies.mobs.add(ZOMBIE_1);
		zombies.mobs.add(ZOMBIE_2);
		state.waves.put(ALICE, zombies);
		WaveState.Wave ghosts = new WaveState.Wave(WaveRules.Kind.GHOSTS);
		ghosts.mobs.add(GHOST);
		state.waves.put(BOB, ghosts);
		state.served.add(ALICE);
		state.served.add(BOB);
		state.pendingShards.add(BOB);

		WaveState loaded = roundTrip(state);
		assertEquals(4, loaded.night);
		assertEquals(WaveRules.Kind.ZOMBIES, loaded.waves.get(ALICE).kind);
		assertEquals(Set.of(ZOMBIE_1, ZOMBIE_2), loaded.waves.get(ALICE).mobs);
		assertEquals(WaveRules.Kind.GHOSTS, loaded.waves.get(BOB).kind);
		assertEquals(Set.of(GHOST), loaded.waves.get(BOB).mobs);
		assertEquals(Set.of(ALICE, BOB), loaded.served);
		assertEquals(Set.of(BOB), loaded.pendingShards);
		assertEquals(BOB, loaded.ownerOf(GHOST));
		assertEquals(ALICE, loaded.ownerOf(ZOMBIE_2));
	}

	@Test
	void anEmptyStateSurvivesASave() {
		WaveState loaded = roundTrip(new WaveState());
		assertEquals(0, loaded.night);
		assertTrue(loaded.waves.isEmpty());
		assertTrue(loaded.served.isEmpty());
		assertTrue(loaded.pendingShards.isEmpty());
		assertNull(loaded.ownerOf(GHOST));
	}

	@Test
	void aWaveWhoseMobsAllDiedStaysAWave() {
		WaveState state = new WaveState();
		state.waves.put(ALICE, new WaveState.Wave(WaveRules.Kind.GHOSTS));
		WaveState loaded = roundTrip(state);
		assertEquals(WaveRules.Kind.GHOSTS, loaded.waves.get(ALICE).kind);
		assertTrue(loaded.waves.get(ALICE).mobs.isEmpty());
	}
}
