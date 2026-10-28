package com.embervoid.reach.world;

import com.embervoid.realm.RealmLayout;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReachWorldRulesTest {
	@Test
	void theEdgeWallStandsNinetyFiveBlocksInsideTheRadius() {
		assertEquals(905, ReachWorldRules.wallRadius(1000), 1e-9);
		assertEquals(0, ReachWorldRules.wallRadius(95), 1e-9);
	}

	@Test
	void theBordersAreTheFinalSectorsTwoEdges() {
		// Three open zones of 100° from 30°, the final zone the last 60°: from 330° round to 30°.
		RealmLayout.Sectors sectors = RealmLayout.Sectors.degrees(30, 3, 60);
		double[] borders = ReachWorldRules.borders(sectors);
		assertEquals(2, borders.length);
		assertEquals(sectors.finalStart(), borders[0], 1e-9);
		assertEquals(sectors.startAngle(), borders[1], 1e-9);
		double width = Math.floorMod(Math.round(Math.toDegrees(borders[1] - borders[0])), 360);
		assertEquals(60, width, 1e-6);
	}

	@Test
	void aTemplateTurnsItsFrontOntoTheRoad() {
		double[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {0.9, 0.3}, {-0.2, -0.8}};
		for (double[] dir : directions) {
			BlockRotation rotation = ReachWorldRules.rotationAlong(dir[0], dir[1]);
			BlockPos front = new BlockPos(0, 0, 1).rotate(rotation);
			// The turned +Z side points along the road's main axis.
			assertTrue(front.getX() * dir[0] + front.getZ() * dir[1] > 0.5, rotation + " for " + dir[0] + ", " + dir[1]);
		}
	}

	@Test
	void outpostTemplatesAreNamedByKindAndZone() {
		assertEquals(new Identifier("embervoid", "ashen_reach/outpost/camp_woods"),
				ReachWorldRules.outpostId("camp", new Identifier("embervoid", "ashen_reach/zone_b")));
		assertThrows(IllegalArgumentException.class,
				() -> ReachWorldRules.outpostId("camp", new Identifier("embervoid", "ashen_reach/citadel")));
	}

	@Test
	void aFootprintTurnsWithItsTemplate() {
		// A 9 long (z) by 5 wide (x) template, half sizes 4 and 2.
		assertTrue(ReachWorldRules.onFootprint(2, 4, 0, 0, 5, 9, 0, 1));
		assertFalse(ReachWorldRules.onFootprint(4, 2, 0, 0, 5, 9, 0, 1));
		// Along x it lies the other way round.
		assertTrue(ReachWorldRules.onFootprint(4, 2, 0, 0, 5, 9, 1, 0));
		assertFalse(ReachWorldRules.onFootprint(2, 4, 0, 0, 5, 9, -1, 0));
		// Away from its centre.
		assertTrue(ReachWorldRules.onFootprint(100, 50, 100, 50, 5, 9, 0, -1));
		assertFalse(ReachWorldRules.onFootprint(100, 55, 100, 50, 5, 9, 0, -1));
	}
}
