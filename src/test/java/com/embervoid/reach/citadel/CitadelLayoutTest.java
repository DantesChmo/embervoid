package com.embervoid.reach.citadel;

import com.embervoid.reach.citadel.CitadelLayout.Piece;
import org.junit.jupiter.api.Test;

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

	private static boolean overlaps(int ax0, int az0, int ax1, int az1, int bx0, int bz0, int bx1, int bz1) {
		return ax0 <= bx1 && bx0 <= ax1 && az0 <= bz1 && bz0 <= az1;
	}
}
