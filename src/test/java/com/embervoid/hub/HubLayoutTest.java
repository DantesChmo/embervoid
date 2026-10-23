package com.embervoid.hub;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HubLayoutTest {
	@Test
	void steleSpotIsFiveWideSixTallAroundTheCrack() {
		BlockPos crack = HubLayout.STELE_CRACK_BOTTOM;
		int count = 0;
		for (BlockPos pos : BlockPos.iterate(crack.add(-4, -2, -4), crack.add(4, 8, 4))) {
			if (HubLayout.isSteleSpot(pos)) {
				count++;
			}
		}
		assertEquals(HubLayout.STELE_WIDTH * HubLayout.STELE_HEIGHT, count);
		assertTrue(HubLayout.isSteleSpot(crack.add(2, 5, 0)));
		assertFalse(HubLayout.isSteleSpot(crack.add(3, 0, 0)));
		assertFalse(HubLayout.isSteleSpot(crack.add(0, 6, 0)));
		assertFalse(HubLayout.isSteleSpot(crack.add(0, 0, 1)));
		assertFalse(HubLayout.isSteleSpot(crack.down()));
	}

	@Test
	void everythingStandsOnTheIslandAndApart() {
		List<BlockPos> spots = new ArrayList<>(List.of(HubLayout.SHRINE, HubLayout.MAIDEN, HubLayout.LANDING));
		HubLayout.OBELISKS.forEach(obelisk -> spots.add(obelisk.pos()));
		Set<BlockPos> seen = new HashSet<>();
		for (BlockPos pos : spots) {
			assertTrue(seen.add(pos), "two things at " + pos);
			assertEquals(HubLayout.FLOOR_Y + 1, pos.getY(), "not standing on the floor: " + pos);
			assertFalse(HubLayout.isSteleSpot(pos), "in the stele's spot: " + pos);
		}
		// The core's contents stay inside its walls; the landing is outside, between the core and the stele.
		for (BlockPos pos : List.of(HubLayout.SHRINE, HubLayout.MAIDEN)) {
			assertTrue(Math.abs(pos.getX()) < HubLayout.CORE_HALF && Math.abs(pos.getZ()) < HubLayout.CORE_HALF, "outside the core: " + pos);
		}
		HubLayout.OBELISKS.forEach(obelisk -> assertTrue(HubLayout.inCore(obelisk.pos().getX(), obelisk.pos().getZ())));
		assertFalse(HubLayout.inCore(HubLayout.LANDING.getX(), HubLayout.LANDING.getZ()));
		assertTrue(HubLayout.LANDING.getZ() > HubLayout.STELE_CRACK_BOTTOM.getZ());
		assertEquals(4, HubLayout.OBELISKS.size());
		assertEquals(4, HubLayout.OBELISKS.stream().map(HubLayout.Obelisk::realm).distinct().count());
	}

	@Test
	void islandFitsTheTemplate() {
		// The stele pad reaches one past the stele; the rim wobbles up to two blocks past the radius.
		int stelePad = Math.abs(HubLayout.STELE_CRACK_BOTTOM.getZ()) + 1;
		assertTrue(stelePad < HubLayout.ISLAND_RADIUS - 2, "stele pad off the island");
		assertTrue(HubLayout.ISLAND_RADIUS + 2 < HubLayout.TEMPLATE_HALF, "island wider than the template");
		assertTrue(Math.hypot(HubLayout.CORE_HALF, HubLayout.CORE_HALF) < HubLayout.ISLAND_RADIUS - 2, "core corners off the island");
		assertEquals(HubLayout.FLOOR_Y - HubLayout.DEPTH, HubLayout.TEMPLATE_ORIGIN.getY());
		assertTrue(HubLayout.STELE_HEIGHT < HubLayout.HEIGHT);
	}
}
