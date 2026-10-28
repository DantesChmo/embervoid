package com.embervoid.sanctum.boss;

import com.embervoid.sanctum.boss.CryingSoulFlight.State;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryingSoulFlightTest {
	private static final double FAR = 10;
	private static final Vec3d FROM = new Vec3d(0, 66, 0);
	private static final Vec3d TO = new Vec3d(8, 62, -4);

	private static CryingSoulFlight diving() {
		CryingSoulFlight flight = new CryingSoulFlight();
		flight.startWindup();
		flight.dive(FROM, TO);
		return flight;
	}

	private static void tickTimes(CryingSoulFlight flight, int times, double distanceToOrbit) {
		for (int i = 0; i < times; i++) {
			flight.tick(distanceToOrbit);
		}
	}

	@Test
	void aSoulStartsCirclingAndStaysSoUntilSent() {
		CryingSoulFlight flight = new CryingSoulFlight();
		assertEquals(State.CIRCLING, flight.state());
		tickTimes(flight, 200, FAR);
		assertEquals(State.CIRCLING, flight.state());
	}

	@Test
	void onlyACirclingSoulCanBeSent() {
		CryingSoulFlight flight = new CryingSoulFlight();
		assertTrue(flight.startWindup());
		assertEquals(State.WINDUP, flight.state());
		assertFalse(flight.startWindup());
		flight.dive(FROM, TO);
		assertFalse(flight.startWindup());
		assertEquals(State.DIVING, flight.state());
	}

	@Test
	void theWindupLastsUntilTheDiveBegins() {
		CryingSoulFlight flight = new CryingSoulFlight();
		flight.startWindup();
		tickTimes(flight, 100, FAR);
		assertEquals(State.WINDUP, flight.state());
	}

	@Test
	void theDiveRunsFromItsStartToWhereItLandsInItsTime() {
		CryingSoulFlight flight = diving();
		assertEquals(State.DIVING, flight.tick(FAR));
		Vec3d first = flight.divePosition();
		assertTrue(first.distanceTo(FROM) > 0 && first.distanceTo(TO) < FROM.distanceTo(TO));
		double previous = first.distanceTo(TO);
		for (int t = 2; t < CryingSoulFlight.DIVE_TICKS; t++) {
			assertEquals(State.DIVING, flight.tick(FAR));
			double left = flight.divePosition().distanceTo(TO);
			assertTrue(left < previous, "the dive moves on every tick");
			previous = left;
		}
		assertEquals(State.DIVING, flight.tick(FAR), "the last dive tick is still a dive");
		assertEquals(0, flight.divePosition().distanceTo(TO), 1e-9);
		assertEquals(State.DAZED, flight.state());
	}

	@Test
	void theDiveFollowsItsLine() {
		CryingSoulFlight flight = diving();
		Vec3d line = TO.subtract(FROM);
		assertEquals(line, flight.diveDirection());
		for (int t = 0; t < CryingSoulFlight.DIVE_TICKS; t++) {
			flight.tick(FAR);
			Vec3d off = flight.divePosition().subtract(FROM);
			assertEquals(0, off.crossProduct(line).length(), 1e-6);
		}
	}

	@Test
	void aDiveHitsOnce() {
		CryingSoulFlight flight = diving();
		assertFalse(flight.hasStruck());
		flight.markStruck();
		assertTrue(flight.hasStruck());
		tickTimes(flight, CryingSoulFlight.DIVE_TICKS + CryingSoulFlight.DAZED_TICKS, FAR);
		tickTimes(flight, 1, 0);
		flight.startWindup();
		flight.dive(FROM, TO);
		assertFalse(flight.hasStruck(), "the next dive may hit again");
	}

	@Test
	void afterTheDiveItHangsDazedWhereItLanded() {
		CryingSoulFlight flight = diving();
		tickTimes(flight, CryingSoulFlight.DIVE_TICKS, FAR);
		assertEquals(TO, flight.diveTarget());
		for (int t = 1; t < CryingSoulFlight.DAZED_TICKS; t++) {
			assertEquals(State.DAZED, flight.tick(0));
			assertEquals(State.DAZED, flight.state(), "being near the circle does not end the daze");
		}
		assertEquals(State.DAZED, flight.tick(0));
		assertEquals(State.RETURNING, flight.state());
	}

	@Test
	void aReturningSoulCirclesAgainOnceBackOnTheCircle() {
		CryingSoulFlight flight = diving();
		tickTimes(flight, CryingSoulFlight.DIVE_TICKS + CryingSoulFlight.DAZED_TICKS, FAR);
		assertEquals(State.RETURNING, flight.state());
		tickTimes(flight, 300, FAR);
		assertEquals(State.RETURNING, flight.state(), "far from the circle it keeps flying back");
		flight.tick(CryingSoulFlight.ARRIVE_DISTANCE * 2);
		assertEquals(State.RETURNING, flight.state());
		flight.tick(CryingSoulFlight.ARRIVE_DISTANCE / 2);
		assertEquals(State.CIRCLING, flight.state());
		assertTrue(flight.startWindup(), "back on the circle it can be sent again");
	}

	@Test
	void everyStateCountsItsOwnTicks() {
		CryingSoulFlight flight = new CryingSoulFlight();
		tickTimes(flight, 5, FAR);
		assertEquals(5, flight.ticks());
		flight.startWindup();
		assertEquals(0, flight.ticks());
		tickTimes(flight, 3, FAR);
		assertEquals(3, flight.ticks());
		flight.dive(FROM, TO);
		assertEquals(0, flight.ticks());
	}
}
