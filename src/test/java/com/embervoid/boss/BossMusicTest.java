package com.embervoid.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossMusicTest {
	@Test
	void musicSliderPlaysNoPart() {
		// Music at 0 % is not an input at all: the track is heard at full Boss music.
		assertEquals(1.0f, BossMusic.heardVolume(1.0, 1.0), 1e-6f);
	}

	@Test
	void bossSliderAtZeroIsSilent() {
		assertEquals(0.0f, BossMusic.trackVolume(0.0), 1e-6f);
		assertEquals(0.0f, BossMusic.heardVolume(1.0, 0.0), 1e-6f);
	}

	@Test
	void masterScalesIt() {
		assertEquals(0.5f, BossMusic.heardVolume(0.5, 1.0), 1e-6f);
		assertEquals(0.25f, BossMusic.heardVolume(0.5, 0.5), 1e-6f);
		assertEquals(0.0f, BossMusic.heardVolume(0.0, 1.0), 1e-6f);
	}

	@Test
	void slidersAreClamped() {
		assertEquals(1.0f, BossMusic.trackVolume(1.5), 1e-6f);
		assertEquals(0.0f, BossMusic.trackVolume(-0.5), 1e-6f);
	}
}
