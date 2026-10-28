package com.embervoid.reach.boss.arena;

import com.embervoid.reach.boss.arena.ReachArenas.Arena;
import com.embervoid.reach.gear.GearLootTables;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReachArenasTest {
	private static double flat(BlockPos pos) {
		return Math.hypot(pos.getX(), pos.getZ());
	}

	@Test
	void fogWallFitsInsideItsTemplate() {
		for (Arena arena : ReachArenas.ALL) {
			assertTrue(arena.radius() < arena.site(), arena.zone());
			assertEquals(2 * arena.site() + 1, arena.size(), arena.zone());
		}
	}

	@Test
	void shrineAndChestStandOutsideTheFogWallOnTheTemplate() {
		for (Arena arena : ReachArenas.ALL) {
			BlockPos shrine = arena.shrine();
			assertEquals(0, shrine.getX(), arena.zone());
			assertTrue(-shrine.getZ() > arena.radius(), arena.zone() + " shrine inside the fog wall");
			assertTrue(-shrine.getZ() <= arena.site(), arena.zone() + " shrine off the template");
			BlockPos chest = arena.chest();
			assertEquals(shrine.getZ(), chest.getZ(), arena.zone());
			assertEquals(shrine.getY(), chest.getY(), arena.zone());
			assertTrue(chest.getX() > 1 && chest.getX() <= arena.site(), arena.zone() + " chest not beside the shrine");
		}
	}

	@Test
	void zoneArenaChestsUseTheZonesArenaLootTable() {
		List<Arena> zoneArenas = ReachArenas.ALL.stream().filter(Arena::hasChest).toList();
		assertEquals(GearLootTables.ZONES, zoneArenas.stream().map(Arena::zone).toList());
		for (Arena arena : zoneArenas) {
			assertEquals(GearLootTables.arena(arena.zone()), arena.chestLoot());
		}
		assertFalse(ReachArenas.CITADEL.hasChest());
	}

	@Test
	void granaryPillarsRingTheFieldsArenaAndLeaveTheEntryFree() {
		List<BlockPos> pillars = ReachArenas.pillars();
		assertEquals(ReachArenas.PILLAR_COUNT, pillars.size());
		assertEquals(pillars.size(), new HashSet<>(pillars).size());
		for (BlockPos pillar : pillars) {
			assertEquals(ReachArenas.PILLAR_RING, flat(pillar), 1, pillar.toShortString());
			// A 3×3 pillar stays clear of the fog wall and of the path in from the shrine on the north axis.
			assertTrue(flat(pillar) + 1.5 < ReachArenas.FIELDS.radius(), pillar.toShortString());
			assertTrue(Math.abs(pillar.getX()) > 2 || pillar.getZ() > 0, pillar + " blocks the entry");
		}
	}

	@Test
	void granaryPillarsNeverTouchEachOther() {
		List<BlockPos> pillars = ReachArenas.pillars();
		for (int i = 0; i < pillars.size(); i++) {
			for (int j = i + 1; j < pillars.size(); j++) {
				BlockPos a = pillars.get(i);
				BlockPos b = pillars.get(j);
				assertTrue(Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ())) > 3, a + " / " + b);
			}
		}
	}

	@Test
	void granaryPillarsAreMirroredAcrossTheNorthAxis() {
		Set<BlockPos> pillars = new HashSet<>(ReachArenas.pillars());
		for (BlockPos pillar : pillars) {
			assertTrue(pillars.contains(new BlockPos(-pillar.getX(), pillar.getY(), pillar.getZ())), pillar.toShortString());
		}
	}

	@Test
	void groveWallStandsBetweenTheFogWallAndTheTemplateEdge() {
		assertTrue(ReachArenas.GROVE_WALL > ReachArenas.WOODS.radius());
		assertTrue(ReachArenas.GROVE_WALL < ReachArenas.WOODS.site());
	}

	@Test
	void necropolisDaisCrossRingAndPedestalsAreDry() {
		assertTrue(ReachArenas.isWalkway(0, 0));
		assertTrue(ReachArenas.isWalkway(ReachArenas.DAIS_RADIUS, 0));
		assertTrue(ReachArenas.isWalkway(3, 3));
		// The cross walkways out to the wall.
		assertTrue(ReachArenas.isWalkway(0, 25));
		assertTrue(ReachArenas.isWalkway(-25, 1));
		// The ring walkway.
		assertTrue(ReachArenas.isWalkway(12, 4));
		assertTrue(ReachArenas.isWalkway(-9, -9));
		for (BlockPos brazier : ReachArenas.braziers()) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					assertTrue(ReachArenas.isWalkway(brazier.getX() + dx, brazier.getZ() + dz), brazier.toShortString());
				}
			}
		}
	}

	@Test
	void necropolisHasWaterBetweenTheWalkways() {
		assertFalse(ReachArenas.isWalkway(6, 6));
		assertFalse(ReachArenas.isWalkway(-3, 8));
		assertFalse(ReachArenas.isWalkway(20, 15));
		// Next to a pedestal, not on it.
		assertFalse(ReachArenas.isWalkway(ReachArenas.BRAZIER_OFFSET + 2, ReachArenas.BRAZIER_OFFSET + 2));
	}

	@Test
	void necropolisWalkwaysStopShortOfTheFogWall() {
		int r = ReachArenas.NECROPOLIS.radius();
		assertFalse(ReachArenas.isWalkway(0, r));
		assertFalse(ReachArenas.isWalkway(r - 1, 0));
		assertTrue(ReachArenas.isWalkway(0, r - 2));
	}

	@Test
	void braziersStandOnTheirPedestalsTopsOnTheDiagonals() {
		List<BlockPos> braziers = ReachArenas.braziers();
		assertEquals(4, braziers.size());
		assertEquals(4, new HashSet<>(braziers).size());
		for (BlockPos brazier : braziers) {
			assertEquals(Math.abs(brazier.getX()), Math.abs(brazier.getZ()));
			assertEquals(ReachArenas.WALKWAY_TOP + 1, brazier.getY());
			assertTrue(flat(brazier) < ReachArenas.NECROPOLIS.radius());
		}
	}

	@Test
	void necropolisRoofCoversTheOuterRingInsideTheFogWall() {
		assertTrue(ReachArenas.ROOF_INNER < ReachArenas.ROOF_OUTER);
		assertTrue(ReachArenas.ROOF_OUTER < ReachArenas.NECROPOLIS.radius());
		assertTrue(ReachArenas.ROOF_Y < ReachArenas.NECROPOLIS.height());
		assertTrue(ReachArenas.WALKWAY_TOP < ReachArenas.ROOF_Y);
	}

	@Test
	void keepWallRunsFromInsideTheFogWallToTheTemplatesSouthEdge() {
		Arena citadel = ReachArenas.CITADEL;
		assertTrue(ReachArenas.KEEP_WALL_Z < citadel.radius());
		assertEquals(citadel.site(), ReachArenas.KEEP_WALL_Z + ReachArenas.KEEP_WALL_DEPTH - 1);
		assertTrue(ReachArenas.KEEP_WALL_HEIGHT < citadel.height());
		assertTrue(ReachArenas.KEEP_GATE_HEIGHT < ReachArenas.KEEP_WALL_HEIGHT);
	}

	@Test
	void lordAndMarksmenStartOnTopOfTheKeepWallInsideTheFight() {
		BlockPos lord = ReachArenas.lordOnWall();
		List<BlockPos> marksmen = ReachArenas.marksmenOnWall();
		assertEquals(2, marksmen.size());
		for (BlockPos pos : concat(lord, marksmen)) {
			assertEquals(ReachArenas.KEEP_WALL_HEIGHT + 1, pos.getY(), pos.toShortString());
			assertTrue(pos.getZ() > ReachArenas.KEEP_WALL_Z, pos + " is not on the wall");
			assertTrue(flat(pos) < ReachArenas.CITADEL.radius(), pos + " is outside the fog wall");
		}
		assertEquals(0, lord.getX());
		assertEquals(-marksmen.get(0).getX(), marksmen.get(1).getX());
		assertTrue(Math.abs(marksmen.get(0).getX()) > ReachArenas.KEEP_GATE_HALF_WIDTH);
	}

	@Test
	void signalBraziersStandInTheCourtyardInFrontOfTheKeepWall() {
		List<BlockPos> braziers = ReachArenas.signalBraziers();
		assertEquals(3, new HashSet<>(braziers).size());
		for (BlockPos brazier : braziers) {
			assertTrue(flat(brazier) + 1.5 < ReachArenas.CITADEL.radius(), brazier.toShortString());
			assertTrue(brazier.getZ() + 1 < ReachArenas.KEEP_WALL_Z, brazier.toShortString());
		}
	}

	private static List<BlockPos> concat(BlockPos first, List<BlockPos> rest) {
		List<BlockPos> all = new java.util.ArrayList<>(rest);
		all.add(0, first);
		return all;
	}
}
