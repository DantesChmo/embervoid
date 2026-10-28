package com.embervoid.boss;

import com.embervoid.boss.FloodPlan.Space;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloodPlanTest {
	private static final BlockPos CENTER = new BlockPos(40, 30, -12);

	/** A flat solid floor at the centre's level with air above, plus whatever a test sets. */
	private static final class TestGround implements FloodPlan.Ground {
		final Set<BlockPos> holes = new HashSet<>();
		final Map<BlockPos, Space> above = new HashMap<>();

		@Override
		public boolean isSolidFloor(BlockPos pos) {
			return pos.getY() == CENTER.getY() && !holes.contains(pos);
		}

		@Override
		public Space space(BlockPos pos) {
			return above.getOrDefault(pos, Space.AIR);
		}
	}

	@Test
	void anOpenFloorFillsEveryColumnInTheCircleToTheDepth() {
		List<BlockPos> spots = FloodPlan.columns(CENTER, 1, 2, new TestGround(), 7);
		// Radius 1: the centre and its four neighbours, two blocks of water each.
		assertEquals(10, spots.size());
		assertEquals(10, new HashSet<>(spots).size());
		for (BlockPos spot : spots) {
			int dx = spot.getX() - CENTER.getX();
			int dz = spot.getZ() - CENTER.getZ();
			assertTrue(dx * dx + dz * dz <= 1);
			assertTrue(spot.getY() == CENTER.getY() + 1 || spot.getY() == CENTER.getY() + 2);
		}
	}

	@Test
	void nothingOutsideTheRadius() {
		for (BlockPos spot : FloodPlan.columns(CENTER, 5, 1, new TestGround(), 1)) {
			int dx = spot.getX() - CENTER.getX();
			int dz = spot.getZ() - CENTER.getZ();
			assertTrue(dx * dx + dz * dz <= 25, spot.toShortString());
		}
		assertFalse(FloodPlan.columns(CENTER, 5, 1, new TestGround(), 1).contains(CENTER.add(4, 1, 4)), "corner of the square, outside the circle");
		assertTrue(FloodPlan.columns(CENTER, 5, 1, new TestGround(), 1).contains(CENTER.add(3, 1, 4)), "on the circle");
	}

	@Test
	void raisedGroundStaysDry() {
		TestGround ground = new TestGround();
		BlockPos walkway = CENTER.add(1, 1, 0);
		ground.above.put(walkway, Space.BLOCKED);
		List<BlockPos> spots = FloodPlan.columns(CENTER, 1, 2, ground, 3);
		assertFalse(spots.contains(walkway));
		assertFalse(spots.contains(walkway.up()), "nothing on top of the walkway either");
		assertEquals(8, spots.size());
	}

	@Test
	void aBlockHigherUpStopsOnlyTheWaterAboveIt() {
		TestGround ground = new TestGround();
		BlockPos overhang = CENTER.up(2);
		ground.above.put(overhang, Space.BLOCKED);
		List<BlockPos> spots = FloodPlan.columns(CENTER, 0, 3, ground, 3);
		assertEquals(List.of(CENTER.up()), spots);
	}

	@Test
	void waterAlreadyThereIsPassedOver() {
		TestGround ground = new TestGround();
		ground.above.put(CENTER.up(), Space.WATER);
		List<BlockPos> spots = FloodPlan.columns(CENTER, 0, 2, ground, 3);
		assertEquals(List.of(CENTER.up(2)), spots);
	}

	@Test
	void noWaterOverAHoleInTheFloor() {
		TestGround ground = new TestGround();
		ground.holes.add(CENTER);
		List<BlockPos> spots = FloodPlan.columns(CENTER, 1, 1, ground, 3);
		assertFalse(spots.contains(CENTER.up()));
		assertEquals(4, spots.size());
	}

	@Test
	void theLowestLayerComesLastSoItIsFilledFirst() {
		List<BlockPos> spots = FloodPlan.columns(CENTER, 3, 2, new TestGround(), 11);
		for (int i = 1; i < spots.size(); i++) {
			assertTrue(spots.get(i - 1).getY() >= spots.get(i).getY());
		}
		assertEquals(CENTER.getY() + 1, spots.get(spots.size() - 1).getY());
	}

	@Test
	void theSameSeedPlansTheSameFlood() {
		assertEquals(FloodPlan.columns(CENTER, 4, 2, new TestGround(), 99), FloodPlan.columns(CENTER, 4, 2, new TestGround(), 99));
	}

	@Test
	void theRiseFinishesExactlyInItsTime() {
		for (int total : new int[]{0, 1, 7, 50, 333}) {
			for (int ticks : new int[]{1, 3, 40, 100}) {
				int pending = total;
				for (int age = 0; age < ticks; age++) {
					int now = FloodPlan.perTick(pending, ticks - age);
					assertTrue(now >= 0);
					pending -= Math.min(pending, now);
					if (age < ticks - 1 && total >= ticks) {
						assertTrue(pending > 0, "not done before the last tick: " + total + " over " + ticks);
					}
				}
				assertEquals(0, pending, total + " blocks over " + ticks + " ticks");
			}
		}
	}

	@Test
	void theRiseIsEven() {
		int pending = 50;
		int ticks = 40;
		int smallest = Integer.MAX_VALUE;
		int largest = 0;
		for (int age = 0; age < ticks; age++) {
			int now = FloodPlan.perTick(pending, ticks - age);
			smallest = Math.min(smallest, now);
			largest = Math.max(largest, now);
			pending -= now;
		}
		assertTrue(largest - smallest <= 1, "between " + smallest + " and " + largest + " a tick");
	}

	@Test
	void runningOverTimeFillsTheRestAtOnce() {
		assertEquals(12, FloodPlan.perTick(12, 0));
		assertEquals(12, FloodPlan.perTick(12, -5));
	}
}
