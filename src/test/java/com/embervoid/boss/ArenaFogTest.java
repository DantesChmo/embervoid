package com.embervoid.boss;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ArenaFogTest {
	private static final Identifier ZONE_A = new Identifier("embervoid", "ashen_reach/zone_a");

	@Test
	void fogSourceIsTheBossPathUnderTheModsArenaFolder() {
		assertEquals(new Identifier("embervoid", "arena/ashen_reach/zone_a"), ArenaFog.source(ZONE_A));
	}

	@Test
	void differentBossesHaveDifferentFogSources() {
		assertNotEquals(ArenaFog.source(ZONE_A), ArenaFog.source(new Identifier("embervoid", "ashen_reach/zone_b")));
	}
}
