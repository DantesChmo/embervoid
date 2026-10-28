package com.embervoid.realm;

import com.embervoid.reach.citadel.CitadelLayout;
import com.embervoid.realm.worldgen.RealmRoads;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Citadel in the Ashen Reach's final sector, against the realm's data files. */
class CitadelPlacementTest {
	private static final Path STRUCTURE_SETS = Path.of("src", "main", "resources", "data", "embervoid", "worldgen", "structure_set");

	@Test
	void structureSetsPlaceTheCitadelAndCourtyardWhereTheLayoutSays() throws IOException {
		for (long seed : new long[]{0, 1, -7, 123456789}) {
			assertArrayEquals(new int[]{(int) CitadelLayout.realmX(CitadelLayout.START_X), (int) CitadelLayout.realmZ(CitadelLayout.START_Z)},
					position(seed, "ashen_reach_citadel"));
			assertArrayEquals(new int[]{(int) CitadelLayout.realmX(CitadelLayout.COURTYARD_X), (int) CitadelLayout.realmZ(CitadelLayout.COURTYARD_Z)},
					position(seed, "ashen_reach_arena_final"));
		}
		// An arena stands on its start chunk's corner.
		assertEquals(0, Math.floorMod((int) CitadelLayout.realmZ(CitadelLayout.COURTYARD_Z), 16));
		assertEquals(0, Math.floorMod((int) CitadelLayout.realmX(CitadelLayout.COURTYARD_X), 16));
	}

	@Test
	void theCityBeginsAboutAHundredBlocksPastTheGateRoad() {
		double outskirts = CitadelLayout.realmZ(CitadelLayout.OUTSKIRTS.maxZ());
		double fromGate = outskirts - RealmRoads.GATE_ROAD_END;
		assertTrue(fromGate >= 80 && fromGate <= 120, "the outskirts start " + fromGate + " blocks past the gate road");
	}

	@Test
	void everyTerraceIsWhollyFinalZoneTerrain() {
		RealmLayout.Sectors sectors = ReachLayoutFiles.sectors();
		JsonObject blend = ReachLayoutFiles.densityFunctions("embervoid:zone_blend").get(0);
		double landingRadius = blend.get("landing_radius").getAsDouble();
		double landingBlend = blend.get("landing_blend").getAsDouble();
		double blendBlocks = blend.get("blend").getAsDouble();
		double warp = Math.toRadians(blend.get("warp_degrees").getAsDouble());
		int finalIndex = sectors.index(RealmLayout.FINAL);
		for (CitadelLayout.Terrace terrace : CitadelLayout.TERRACES) {
			for (int[] column : columns(terrace)) {
				double x = CitadelLayout.realmX(column[0]);
				double z = CitadelLayout.realmZ(column[1]);
				double distance = Math.hypot(x, z);
				String where = terrace + " at " + x + ", " + z;
				assertEquals(0, RealmLayout.landingWeight(distance, landingRadius, landingBlend), 1e-9, where);
				for (double w : new double[]{-1, 0, 1}) {
					double local = sectors.warpedLocal(RealmLayout.angle(x, z), w, warp);
					assertEquals(1, RealmLayout.zoneWeights(local, sectors, distance, blendBlocks)[finalIndex], 1e-9, where);
				}
			}
		}
	}

	/** Every second column of a terrace, its edges included. */
	private static List<int[]> columns(CitadelLayout.Terrace t) {
		List<int[]> columns = new ArrayList<>();
		for (int x = t.minX(); x <= t.maxX() + 1; x += 2) {
			for (int z = t.minZ(); z <= t.maxZ() + 1; z += 2) {
				columns.add(new int[]{Math.min(x, t.maxX()), Math.min(z, t.maxZ())});
			}
		}
		return columns;
	}

	private static int[] position(long seed, String set) throws IOException {
		JsonObject placement = JsonParser.parseString(Files.readString(STRUCTURE_SETS.resolve(set + ".json")))
				.getAsJsonObject().getAsJsonObject("placement");
		double start = Math.toRadians(placement.get("start_angle").getAsDouble());
		double width = Math.toRadians(placement.get("sector_width").getAsDouble());
		int sector = placement.has("sector") ? placement.get("sector").getAsInt() : 0;
		return RealmLayout.featurePosition(seed, placement.get("salt").getAsInt(), start + sector * width, width,
				placement.get("angle_spread").getAsDouble(), placement.get("min_distance").getAsInt(),
				placement.get("max_distance").getAsInt());
	}
}
