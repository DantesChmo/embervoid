package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.reach.citadel.CitadelLayout;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.reach.world.ReachZones;
import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmMobLevels;
import com.embervoid.realm.Realms;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.List;

/** The depths "Burning City" and the Smoldering Citadel (PLAN 13.1–13.2, track citadel). */
public final class CitadelWorldTests implements FabricGameTest {
	private static ServerWorld depths(TestContext context) {
		ServerWorld depths = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.depths());
		context.assertTrue(depths != null, "the depths dimension is missing from the test world");
		return depths;
	}

	/** Own dimension type: the same dusk as the open zones, ultrawarm, dark red sky. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void depthsHaveTheirOwnDuskAndRedSky(TestContext context) {
		ServerWorld depths = depths(context);
		context.assertTrue(depths.getDimensionKey().getValue().equals(Embervoid.id("ashen_reach_depths")),
				"depths dimension type is " + depths.getDimensionKey().getValue());
		context.assertTrue(depths.getDimension().fixedTime().orElse(-1) == 13500, "depths time is not fixed at dusk");
		context.assertTrue(depths.getDimension().ultrawarm(), "the Burning City is not ultrawarm");
		Biome biome = depths.getRegistryManager().get(RegistryKeys.BIOME).get(ReachZones.DEPTHS);
		int sky = biome.getSkyColor();
		context.assertTrue((sky >> 16 & 0xff) > 2 * (sky & 0xff) && (sky >> 16 & 0xff) > 2 * (sky >> 8 & 0xff),
				"sky is not dark red: " + Integer.toHexString(sky));
		RealmDefinition.LevelBand band = RealmMobLevels.bandAt(depths, new BlockPos(0, 80, -100));
		context.assertTrue(band != null && band.min() == 25 && band.max() == 30, "depths levels " + band);
		context.complete();
	}

	/** The ground matches the layout: the landing and the courtyard flat at their heights, the hill above the city. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 400)
	public void depthsTerrainFollowsTheLayout(TestContext context) {
		ServerWorld depths = depths(context);
		ChunkGenerator generator = depths.getChunkManager().getChunkGenerator();
		for (int dx = -24; dx <= 24; dx += 8) {
			for (int dz = -24; dz <= 24; dz += 8) {
				assertGround(context, depths, generator, dx, dz, CitadelLayout.LANDING_Y);
				assertGround(context, depths, generator, CitadelLayout.COURTYARD_X + dx, CitadelLayout.COURTYARD_Z + dz,
						CitadelLayout.PLATEAU_Y);
			}
		}
		for (CitadelLayout.Piece piece : List.of(CitadelLayout.OUTSKIRTS, CitadelLayout.MARKET, CitadelLayout.KEEP)) {
			assertGround(context, depths, generator, (piece.minX() + piece.maxX()) / 2, (piece.minZ() + piece.maxZ()) / 2,
					piece.floorY());
		}
		int below = height(depths, generator, 130, -200);
		context.assertTrue(below < CitadelLayout.PLATEAU_Y - 12, "the hill's foot east of the plateau is at " + below);
		context.complete();
	}

	/** Ember cracks and smouldering logs lie in the Burning City, and nowhere in the open zones. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 2400)
	public void emberCracksAndSmolderingLogsOnlyInTheDepths(TestContext context) {
		ServerWorld depths = depths(context);
		int[] inDepths = count(depths, 120, -40, 3);
		context.assertTrue(inDepths[0] > 0 && inDepths[1] > 0,
				"depths: " + inDepths[0] + " ember cracks, " + inDepths[1] + " smouldering logs");
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		for (double angle : new double[]{-30, 90, 210}) {
			double a = Math.toRadians(angle);
			int[] inZone = count(realm, (int) (Math.cos(a) * 300), (int) (Math.sin(a) * 300), 2);
			context.assertTrue(inZone[0] == 0 && inZone[1] == 0,
					"zone at " + angle + "°: " + inZone[0] + " ember cracks, " + inZone[1] + " smouldering logs");
		}
		context.complete();
	}

	/** The Smoldering Lord's courtyard is the depths arena, fixed north of the landing on the plateau. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void courtyardIsTheDepthsArena(TestContext context) {
		ServerWorld depths = depths(context);
		List<RealmArenas.Location> arenas = RealmArenas.all(depths);
		context.assertTrue(arenas.size() == 1, "expected one depths arena, found " + arenas.size());
		RealmArenas.Location arena = arenas.get(0);
		context.assertTrue(arena.pos().getX() == CitadelLayout.COURTYARD_X && arena.pos().getZ() == CitadelLayout.COURTYARD_Z,
				"courtyard at " + arena.pos());
		context.assertTrue(arena.siteRadius() == CitadelLayout.COURTYARD_RADIUS, "courtyard site radius " + arena.siteRadius());
		context.assertTrue(arena.spec().bossId().equals(Realms.ASHEN_REACH.finalBoss()) && arena.spec().level() == 30,
				"courtyard boss " + arena.spec());
		context.complete();
	}

	private static void assertGround(TestContext context, ServerWorld world, ChunkGenerator generator, int x, int z, int expected) {
		int y = height(world, generator, x, z);
		context.assertTrue(y == expected, "ground at " + x + ", " + z + " is " + y + ", expected " + expected);
	}

	private static int height(ServerWorld world, ChunkGenerator generator, int x, int z) {
		return generator.getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, world.getChunkManager().getNoiseConfig());
	}

	/** Ember cracks and smouldering logs in a square of (2r + 1)² chunks around a column. */
	private static int[] count(ServerWorld world, int x, int z, int r) {
		int[] counts = new int[2];
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int cx = (x >> 4) - r; cx <= (x >> 4) + r; cx++) {
			for (int cz = (z >> 4) - r; cz <= (z >> 4) + r; cz++) {
				WorldChunk chunk = world.getChunk(cx, cz);
				for (int dx = 0; dx < 16; dx++) {
					for (int dz = 0; dz < 16; dz++) {
						int top = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, dx, dz);
						for (int y = top - 4; y <= top + 1; y++) {
							Block block = chunk.getBlockState(pos.set(dx, y, dz)).getBlock();
							if (block == ReachBlocks.EMBER_CRACK) {
								counts[0]++;
							} else if (block == ReachBlocks.SMOLDERING_LOG) {
								counts[1]++;
							}
						}
					}
				}
			}
		}
		return counts;
	}
}
