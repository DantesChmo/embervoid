package com.embervoid.reach.citadel;

import com.embervoid.reach.citadel.CitadelLayout.Piece;
import com.embervoid.reach.citadel.CitadelLayout.Terrace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitadelLayoutTest {
	private static final double[] HILLS = {-1, -0.3, 0, 0.6, 1};

	@Test
	void piecesStandOnFlatGroundAtTheirFloor() {
		for (Piece piece : CitadelLayout.PIECES) {
			if (piece == CitadelLayout.GATEHOUSE) {
				continue; // cut into the hill; only its approach is a terrace
			}
			for (int x = piece.minX(); x <= piece.maxX(); x += 3) {
				for (int z = piece.minZ(); z <= piece.maxZ(); z += 3) {
					for (double hills : HILLS) {
						assertEquals(piece.floorY(), CitadelLayout.groundHeight(x, z, hills), 1e-9, piece.name() + " at " + x + ", " + z);
					}
					assertEquals(1, CitadelLayout.flatness(x, z), 1e-9);
				}
			}
		}
	}

	@Test
	void courtyardAndLandingAreFlat() {
		int r = CitadelLayout.COURTYARD_RADIUS;
		for (int dx = -r; dx <= r; dx += 4) {
			for (int dz = -r; dz <= r; dz += 4) {
				for (double hills : HILLS) {
					assertEquals(CitadelLayout.PLATEAU_Y, CitadelLayout.groundHeight(CitadelLayout.COURTYARD_X + dx,
							CitadelLayout.COURTYARD_Z + dz, hills), 1e-9);
				}
			}
		}
		for (int x = CitadelLayout.PLAZA_MIN_X; x <= CitadelLayout.PLAZA_MAX_X; x += 4) {
			for (int z = CitadelLayout.PLAZA_MIN_Z; z <= CitadelLayout.PLAZA_MAX_Z; z += 4) {
				for (double hills : HILLS) {
					assertEquals(CitadelLayout.LANDING_Y, CitadelLayout.groundHeight(x, z, hills), 1e-9);
				}
			}
		}
	}

	@Test
	void piecesNeverOverlapEachOtherOrTheCourtyard() {
		int r = CitadelLayout.COURTYARD_RADIUS;
		for (Piece a : CitadelLayout.PIECES) {
			assertFalse(overlaps(a.minX(), a.minZ(), a.maxX(), a.maxZ(), CitadelLayout.COURTYARD_X - r,
					CitadelLayout.COURTYARD_Z - r, CitadelLayout.COURTYARD_X + r, CitadelLayout.COURTYARD_Z + r), a.name());
			for (Piece b : CitadelLayout.PIECES) {
				if (a != b) {
					assertFalse(overlaps(a.minX(), a.minZ(), a.maxX(), a.maxZ(), b.minX(), b.minZ(), b.maxX(), b.maxZ()),
							a.name() + " / " + b.name());
				}
			}
		}
	}

	@Test
	void everyPieceIsWithinReachOfTheStartChunk() {
		int startX = Math.floorDiv(CitadelLayout.START_X, 16);
		int startZ = Math.floorDiv(CitadelLayout.START_Z, 16);
		for (Piece piece : CitadelLayout.PIECES) {
			for (int x : new int[]{piece.minX(), piece.maxX()}) {
				for (int z : new int[]{piece.minZ(), piece.maxZ()}) {
					assertTrue(Math.abs(Math.floorDiv(x, 16) - startX) <= CitadelLayout.START_REACH_CHUNKS
							&& Math.abs(Math.floorDiv(z, 16) - startZ) <= CitadelLayout.START_REACH_CHUNKS, piece.name());
				}
			}
		}
	}

	@Test
	void hillRisesAboveTheLowerCity() {
		// Off the terraces: the plateau beside the keep, the lower city east of the landing.
		assertTrue(Math.abs(CitadelLayout.groundHeight(70, -270, 0) - CitadelLayout.PLATEAU_Y) < 2);
		double lower = CitadelLayout.groundHeight(150, -40, 0);
		assertTrue(lower > CitadelLayout.LANDING_Y - 2 && lower < CitadelLayout.LANDING_Y + 6, "lower city at " + lower);
		// The hill's side is steep: from the rim to the foot within the cliff width.
		assertTrue(CitadelLayout.groundHeight(100, -200, 0) < CitadelLayout.PLATEAU_Y - 15);
		assertEquals(1, CitadelLayout.cliff(80, -200));
		assertEquals(0, CitadelLayout.cliff(0, -200));
	}

	@Test
	void realmAndPlanFramesAreInverse() {
		for (double v : new double[]{-300, -17.5, 0, 42, 400}) {
			assertEquals(v, CitadelLayout.realmX(CitadelLayout.planX(v)), 1e-9);
			assertEquals(v, CitadelLayout.planX(CitadelLayout.realmX(v)), 1e-9);
			assertEquals(v, CitadelLayout.realmZ(CitadelLayout.planZ(v)), 1e-9);
			assertEquals(v, CitadelLayout.planZ(CitadelLayout.realmZ(v)), 1e-9);
		}
	}

	@Test
	void theRouteRunsAwayFromTheLandingInTheRealm() {
		assertEquals(CitadelLayout.ORIGIN_Z, CitadelLayout.realmZ(0), 1e-9);
		double plaza = CitadelLayout.realmZ(CitadelLayout.PLAZA_MIN_Z);
		double courtyard = CitadelLayout.realmZ(CitadelLayout.COURTYARD_Z);
		double keep = CitadelLayout.realmZ(CitadelLayout.KEEP.minZ());
		assertTrue(CitadelLayout.ORIGIN_Z < plaza && plaza < courtyard && courtyard < keep);
		// The plan's east (+X) is the realm's west: the turn is 180°.
		assertEquals(-20, CitadelLayout.realmX(20), 1e-9);
	}

	@Test
	void pieceBoxesAreInclusiveAndCarryTheirFoundation() {
		Piece market = CitadelLayout.MARKET;
		assertEquals(market.sizeX(), market.maxX() - market.minX() + 1);
		assertEquals(market.sizeZ(), market.maxZ() - market.minZ() + 1);
		for (Piece piece : CitadelLayout.PIECES) {
			assertEquals(piece.floorY() - CitadelLayout.FOUNDATION, piece.originY(), piece.name());
			assertEquals("ashen_reach/citadel/" + piece.name(), piece.template().getPath());
		}
	}

	@Test
	void theRouteClimbsPieceByPiece() {
		int floor = CitadelLayout.LANDING_Y;
		int z = CitadelLayout.PLAZA_MIN_Z;
		for (Piece piece : List.of(CitadelLayout.OUTSKIRTS, CitadelLayout.STREET, CitadelLayout.MARKET, CitadelLayout.GATEHOUSE)) {
			assertTrue(piece.floorY() > floor, piece.name() + " is not above the piece before it");
			assertTrue(piece.maxZ() < z, piece.name() + " is not further north");
			floor = piece.floorY();
			z = piece.minZ();
		}
		assertTrue(CitadelLayout.PLATEAU_Y > floor);
	}

	@Test
	void terraceDistanceIsZeroInsideAndEuclideanOutside() {
		Terrace terrace = new Terrace(0, 0, 10, 10, 70);
		assertEquals(0, terrace.distance(5, 5), 1e-9);
		assertEquals(0, terrace.distance(10, 0), 1e-9);
		assertEquals(3, terrace.distance(13, 5), 1e-9);
		assertEquals(5, terrace.distance(-3, 14), 1e-9);
	}

	@Test
	void plateauDistanceIsNegativeInsideZeroOnTheRimPositiveOutside() {
		double centreX = (CitadelLayout.PLATEAU_MIN_X + CitadelLayout.PLATEAU_MAX_X) / 2.0;
		double centreZ = (CitadelLayout.PLATEAU_MIN_Z + CitadelLayout.PLATEAU_MAX_Z) / 2.0;
		assertTrue(CitadelLayout.plateauDistance(centreX, centreZ) < -50);
		assertEquals(0, CitadelLayout.plateauDistance(CitadelLayout.PLATEAU_MAX_X, centreZ), 1e-9);
		assertEquals(0, CitadelLayout.plateauDistance(centreX, CitadelLayout.PLATEAU_MIN_Z), 1e-9);
		assertEquals(10, CitadelLayout.plateauDistance(CitadelLayout.PLATEAU_MAX_X + 10, centreZ), 1e-9);
		// The corners are rounded: the rectangle's own corner lies outside.
		assertTrue(CitadelLayout.plateauDistance(CitadelLayout.PLATEAU_MAX_X, CitadelLayout.PLATEAU_MAX_Z) > 1);
		double previous = Double.NEGATIVE_INFINITY;
		for (double x = centreX; x < CitadelLayout.PLATEAU_MAX_X + 30; x += 2) {
			double d = CitadelLayout.plateauDistance(x, centreZ);
			assertTrue(d >= previous, "not rising outwards at x " + x);
			previous = d;
		}
	}

	@Test
	void naturalGroundIsThePlateauOnTheHillAndTheLowerCityOffIt() {
		double centreZ = (CitadelLayout.PLATEAU_MIN_Z + CitadelLayout.PLATEAU_MAX_Z) / 2.0;
		assertEquals(CitadelLayout.PLATEAU_Y, CitadelLayout.naturalHeight(0, centreZ, 0), 1e-9);
		// The hills noise moves the plateau less than the lower city.
		double plateauSwing = CitadelLayout.naturalHeight(0, centreZ, 1) - CitadelLayout.naturalHeight(0, centreZ, -1);
		double citySwing = CitadelLayout.naturalHeight(200, 100, 1) - CitadelLayout.naturalHeight(200, 100, -1);
		assertTrue(plateauSwing > 0 && plateauSwing < citySwing);
		// South of the plaza the lower city lies at the landing's height; it rises gently going north.
		assertEquals(CitadelLayout.LANDING_Y, CitadelLayout.naturalHeight(200, 100, 0), 1e-9);
		assertTrue(CitadelLayout.naturalHeight(200, -60, 0) > CitadelLayout.naturalHeight(200, 0, 0));
		assertTrue(CitadelLayout.naturalHeight(200, -60, 0) < CitadelLayout.LANDING_Y + 4 + 1e-9);
	}

	@Test
	void flatnessFadesOutAwayFromTheTerraces() {
		double previous = 1;
		for (int x = CitadelLayout.PLAZA_MAX_X; x <= CitadelLayout.PLAZA_MAX_X + 20; x++) {
			double flatness = CitadelLayout.flatness(x, -15);
			assertTrue(flatness <= previous + 1e-12, "flatness rises at x " + x);
			assertTrue(flatness >= 0 && flatness <= 1);
			previous = flatness;
		}
		assertEquals(1, CitadelLayout.flatness(CitadelLayout.PLAZA_MAX_X + 2, -15), 1e-9);
		assertEquals(0, previous, 1e-9);
		assertEquals(0, CitadelLayout.flatness(300, 300), 1e-9);
	}

	@Test
	void farFromTheTerracesTheGroundIsNatural() {
		for (double hills : HILLS) {
			assertEquals(CitadelLayout.naturalHeight(300, 200, hills), CitadelLayout.groundHeight(300, 200, hills), 1e-9);
			assertEquals(CitadelLayout.naturalHeight(-200, -60, hills), CitadelLayout.groundHeight(-200, -60, hills), 1e-9);
		}
	}

	@Test
	void groundNeverStepsMoreThanTheCliffAllows() {
		// Walking the route from the plaza to the keep's back wall down the middle line.
		double previous = CitadelLayout.groundHeight(0, 20, 0);
		for (int z = 19; z >= CitadelLayout.KEEP.maxZ() - 10; z--) {
			double ground = CitadelLayout.groundHeight(0, z, 0);
			assertTrue(Math.abs(ground - previous) < 5, "a step of " + (ground - previous) + " at z " + z);
			previous = ground;
		}
	}

	private static boolean overlaps(int ax0, int az0, int ax1, int az1, int bx0, int bz0, int bx1, int bz1) {
		return ax0 <= bx1 && bx0 <= ax1 && az0 <= bz1 && bz0 <= az1;
	}
}
