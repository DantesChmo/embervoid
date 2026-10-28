package com.embervoid.reach.mob;

import com.embervoid.reach.mob.SentinelShield.Stance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentinelShieldTest {
	@Test
	void theShieldIsUpWhileItHasATarget() {
		assertEquals(Stance.GUARD, SentinelShield.next(Stance.LOWERED, 0, true));
		assertEquals(Stance.GUARD, SentinelShield.next(Stance.GUARD, 0, true));
		assertEquals(Stance.LOWERED, SentinelShield.next(Stance.GUARD, 0, false));
	}

	@Test
	void anOpenShieldStaysOpenUntilItsTimeRunsOutTargetOrNot() {
		assertEquals(Stance.OPEN, SentinelShield.next(Stance.OPEN, 5, true));
		assertEquals(Stance.OPEN, SentinelShield.next(Stance.OPEN, 1, false));
		assertEquals(Stance.LOWERED, SentinelShield.next(Stance.OPEN, 0, true));
	}

	@Test
	void aRaisedShieldTurnsMeleeAndArrowsFromTheFront() {
		// Facing +z; a hit from straight ahead and from 80° to the side.
		assertTrue(SentinelShield.blocks(false, false, true, 0, 3, 0, 1));
		assertTrue(SentinelShield.blocks(false, false, true, 3, 0.5, 0, 1));
	}

	@Test
	void hitsFromBehindGetThrough() {
		assertFalse(SentinelShield.blocks(false, false, true, 0, -3, 0, 1));
		assertFalse(SentinelShield.blocks(false, false, true, 3, -0.5, 0, 1));
	}

	@Test
	void anOpenShieldTurnsNothing() {
		assertFalse(SentinelShield.blocks(true, false, true, 0, 3, 0, 1));
	}

	@Test
	void damageThatBypassesShieldsAndOtherDamageGetThrough() {
		assertFalse(SentinelShield.blocks(false, true, true, 0, 3, 0, 1), "bypasses shields");
		assertFalse(SentinelShield.blocks(false, false, false, 0, 3, 0, 1), "neither melee nor a projectile");
	}

	@Test
	void aHitFromRightOnTopOfItIsBlocked() {
		assertTrue(SentinelShield.blocks(false, false, true, 0, 0, 0, 1));
	}
}
