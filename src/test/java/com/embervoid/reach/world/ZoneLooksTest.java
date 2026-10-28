package com.embervoid.reach.world;

import com.embervoid.realm.fog.FogStack;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneLooksTest {
	private static final List<String> REALM_BIOMES = List.of("ashen_reach_landing", "ashen_reach_zone_a", "ashen_reach_zone_b",
			"ashen_reach_zone_c", "ashen_reach_abyss", "ashen_reach_citadel");

	private static FogStack.Layer fog(String biome) {
		return ZoneLooks.fog(new Identifier("embervoid", biome));
	}

	@Test
	void everyRealmBiomeHasFogThatAFightCanClear() {
		for (String biome : REALM_BIOMES) {
			FogStack.Layer layer = fog(biome);
			assertNotNull(layer, biome);
			assertTrue(layer.density() > 0 && layer.density() < 1, biome + " density " + layer.density());
			assertTrue(layer.clearable(), biome);
		}
	}

	@Test
	void otherBiomesHaveNoZoneFog() {
		assertNull(ZoneLooks.fog(new Identifier("minecraft", "plains")));
		assertNull(ZoneLooks.fog(new Identifier("other_mod", "ashen_reach_zone_a")));
	}

	@Test
	void theWoodsAreTheDensestZone() {
		float woods = fog("ashen_reach_zone_b").density();
		for (String zone : List.of("ashen_reach_zone_a", "ashen_reach_zone_c")) {
			assertTrue(woods > fog(zone).density(), zone);
		}
	}

	@Test
	void eachZoneFogHasItsOwnTint() {
		// Woods grey-green: green leads; Necropolis blue-grey: blue leads; Fields grey-ochre: red over blue.
		int woods = fog("ashen_reach_zone_b").color();
		assertTrue(green(woods) > red(woods) && green(woods) > blue(woods));
		int necropolis = fog("ashen_reach_zone_c").color();
		assertTrue(blue(necropolis) > red(necropolis) && blue(necropolis) > green(necropolis));
		int fields = fog("ashen_reach_zone_a").color();
		assertTrue(red(fields) > blue(fields));
	}

	@Test
	void zoneBossesNameTheirZones() {
		assertEquals("fields", ZoneLooks.zoneName(new Identifier("embervoid", "ashen_reach_zone_a")));
		assertEquals("woods", ZoneLooks.zoneName(new Identifier("embervoid", "ashen_reach_zone_b")));
		assertEquals("necropolis", ZoneLooks.zoneName(new Identifier("embervoid", "ashen_reach_zone_c")));
	}

	@Test
	void anythingElseIsNoZoneBoss() {
		assertThrows(IllegalArgumentException.class, () -> ZoneLooks.zoneName(new Identifier("embervoid", "ashen_reach_citadel")));
		assertThrows(IllegalArgumentException.class, () -> ZoneLooks.zoneName(new Identifier("embervoid", "fallen_sister")));
	}

	private static int red(int color) {
		return color >> 16 & 0xFF;
	}

	private static int green(int color) {
		return color >> 8 & 0xFF;
	}

	private static int blue(int color) {
		return color & 0xFF;
	}
}
