package com.embervoid.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaminaBarTest {
	private static final int WIDTH = 79;
	private static final long NO_FAIL = 1000;

	@Test
	void hiddenWhenFullUnlessTheOptionIsOff() {
		assertFalse(StaminaBar.look(100, 100, NO_FAIL, true, WIDTH).visible());
		StaminaBar.Look shown = StaminaBar.look(100, 100, NO_FAIL, false, WIDTH);
		assertTrue(shown.visible());
		assertEquals(WIDTH, shown.filled());
	}

	@Test
	void fillFollowsStamina() {
		assertEquals(0, StaminaBar.look(0, 100, NO_FAIL, true, WIDTH).filled());
		assertEquals(40, StaminaBar.look(50, 100, NO_FAIL, true, WIDTH).filled());
		assertEquals(55, StaminaBar.look(70, 100, NO_FAIL, true, WIDTH).filled());
		// Out-of-range values from a late sync are clamped.
		assertEquals(0, StaminaBar.look(-5, 100, NO_FAIL, true, WIDTH).filled());
		assertEquals(WIDTH, StaminaBar.look(120, 100, NO_FAIL, false, WIDTH).filled());
		assertEquals(0, StaminaBar.look(0, 0, NO_FAIL, false, WIDTH).filled());
	}

	@Test
	void failedSpendFlashesEvenWhenFull() {
		// A creative-like full pool that just failed (e.g. a race with the sync) still shows the flash.
		assertTrue(StaminaBar.look(100, 100, 0, true, WIDTH).visible());
		boolean[] on = new boolean[StaminaBar.FLASH_TICKS];
		for (int t = 0; t < StaminaBar.FLASH_TICKS; t++) {
			on[t] = StaminaBar.look(0, 100, t, true, WIDTH).flashOn();
		}
		assertTrue(on[0] && on[1] && !on[2] && !on[3] && on[4], "blinks every 2 ticks");
		assertFalse(StaminaBar.look(0, 100, StaminaBar.FLASH_TICKS, true, WIDTH).flashOn(), "flash over");
		assertFalse(StaminaBar.look(0, 100, -1, true, WIDTH).flashOn(), "a failure in the future of the clock is ignored");
	}
}
