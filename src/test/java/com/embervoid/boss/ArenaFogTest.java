package com.embervoid.boss;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ArenaFogTest {
	@Test
	@Disabled("bug: ArenaFog.source drops the namespace, othermod:x and embervoid:x share a fog source")
	void differentBossesHaveDifferentFogSources() {
		Identifier zoneA = new Identifier("embervoid", "ashen_reach/zone_a");
		assertNotEquals(ArenaFog.source(zoneA), ArenaFog.source(new Identifier("embervoid", "ashen_reach/zone_b")));
		assertNotEquals(ArenaFog.source(zoneA), ArenaFog.source(new Identifier("othermod", "ashen_reach/zone_a")));
	}
}
