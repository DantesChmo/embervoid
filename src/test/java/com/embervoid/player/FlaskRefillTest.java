package com.embervoid.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlaskRefillTest {
	@Test
	void skippedNightRefills() {
		// Lay down at 13 000 on day 3, vanilla skipped to the next morning.
		assertTrue(FlaskRefill.nightSkipped(3 * 24000 + 13000, 4 * 24000));
	}

	@Test
	void gettingUpEarlyDoesNot() {
		assertFalse(FlaskRefill.nightSkipped(3 * 24000 + 13000, 3 * 24000 + 13100));
		// Woken by daylight at dawn of the same day: the night passed but was not skipped.
		assertFalse(FlaskRefill.nightSkipped(3 * 24000 + 13000, 3 * 24000 + 23460));
	}
}
