package com.embervoid.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaHazardIdTest {
	private static class FireTrailHazard implements ArenaHazard {
		@Override
		public boolean tick(BossFight fight) {
			return false;
		}
	}

	@Test
	void builtInHazardsAreNamedInSnakeCaseWithoutTheSuffix() {
		assertEquals("area", new AreaHazard(null, 20, 5, null, 1, null, null).id());
		assertEquals("ash_storm", new AshStormHazard(null, 100, 2).id());
		assertEquals("flood", new FloodHazard(8, 1, 40).id());
	}

	@Test
	void aBossesOwnHazardFollowsTheSameRule() {
		assertEquals("fire_trail", new FireTrailHazard().id());
	}
}
