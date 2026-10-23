package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModEntities;
import com.embervoid.progress.ProgressManager;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.reach.world.ReachZones;
import com.embervoid.reach.world.ZoneTitles;
import com.embervoid.realm.RealmBlocks;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.RealmLayout;
import com.embervoid.realm.RealmMobLevels;
import com.embervoid.realm.RealmPlayerData;
import com.embervoid.realm.Realms;
import com.embervoid.realm.RoadLayout;
import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.realm.worldgen.RealmBiomeSource;
import com.embervoid.realm.worldgen.RealmFeaturePiece;
import com.embervoid.realm.worldgen.RealmRoads;
import com.embervoid.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.List;

/**
 * The realm's four sectors (PLAN 21.1): three open zones and the final zone's sector in one dimension, the Citadel Gate
 * on the landing plaza facing that sector with the gate road out to it, and realm respawn as before.
 */
public final class RealmSectorTests implements FabricGameTest {
	private static final double NEAR = 2.5;

	/** The dimension's biome source and {@link RealmDefinition#sectors} agree, and so does the radius. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void sectorsMatchTheDefinition(TestContext context) {
		ServerWorld realm = realm(context);
		RealmDefinition definition = Realms.ASHEN_REACH;
		context.assertTrue(realm.getChunkManager().getChunkGenerator().getBiomeSource() instanceof RealmBiomeSource,
				"the realm has no realm_zones biome source");
		RealmBiomeSource source = (RealmBiomeSource) realm.getChunkManager().getChunkGenerator().getBiomeSource();
		RealmLayout.Sectors sectors = source.sectors();
		context.assertTrue(sectors.equals(definition.sectors()), "biome source " + sectors + ", definition " + definition.sectors());
		context.assertTrue(sectors.hasFinal() && Math.abs(Math.toDegrees(sectors.finalWidth()) - 60) < 1e-6
				&& Math.abs(Math.toDegrees(sectors.openWidth()) - 100) < 1e-6, "not 3 × 100° + 60°: " + sectors);
		context.assertTrue(source.radius() == definition.radius() && definition.radius() == 1300,
				"radius " + source.radius() + " / " + definition.radius());
		context.complete();
	}

	/**
	 * In the middle of each sector the zone, the biome, the level band and the zone title all resolve: the open zones as
	 * before, the final sector to the Citadel biome and the final band (25–30).
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void zonesAndTheFinalSectorResolve(TestContext context) {
		ServerWorld realm = realm(context);
		RealmDefinition definition = Realms.ASHEN_REACH;
		RealmLayout.Sectors sectors = definition.sectors();
		List<RegistryKey<Biome>> expected = List.of(ReachZones.FIELDS, ReachZones.WOODS, ReachZones.NECROPOLIS);
		ServerPlayerEntity player = TestPlayers.create(context, 1, 1);
		try {
			for (int zone = 0; zone <= sectors.openCount(); zone++) {
				boolean isFinal = zone == sectors.openCount();
				double angle = isFinal ? sectors.finalCenter() : sectors.openCenter(zone);
				for (int distance : new int[]{120, 600, 1150}) {
					BlockPos column = BlockPos.ofFloored(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
					int found = RealmMobLevels.zoneAt(realm, column);
					int want = isFinal ? RealmLayout.FINAL : zone;
					context.assertTrue(found == want, "zone " + found + " at " + column.toShortString() + ", expected " + want);
					RegistryEntry<Biome> biome = biomeAt(realm, column);
					RegistryKey<Biome> wantBiome = isFinal ? ReachZones.CITADEL : expected.get(zone);
					context.assertTrue(biome.matchesKey(wantBiome),
							"biome " + biome.getKey().map(k -> k.getValue().toString()).orElse("?") + " at " + column.toShortString());
					RealmDefinition.LevelBand band = RealmMobLevels.bandAt(realm, column);
					RealmDefinition.LevelBand wantBand = isFinal ? definition.finalLevels() : definition.zoneLevels().get(zone);
					context.assertTrue(wantBand.equals(band), "band " + band + " at " + column.toShortString());
				}
				BlockPos column = BlockPos.ofFloored(Math.cos(angle) * 300, 0, Math.sin(angle) * 300);
				realm.getChunk(column);
				BlockPos top = realm.getTopPosition(Heightmap.Type.MOTION_BLOCKING, column);
				player.teleport(realm, top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5, 0, 0);
				String title = ZoneTitles.zoneKey(player);
				String wantTitle = "embervoid.zone.ashen_reach." + (isFinal ? "final" : "zone_" + (char) ('a' + zone));
				context.assertTrue(wantTitle.equals(title), "title " + title + " at " + column.toShortString());
			}
			context.assertTrue(definition.finalLevels().min() == 25 && definition.finalLevels().max() == 30,
					"final band " + definition.finalLevels());
		} finally {
			TestPlayers.complete(context, player);
		}
	}

	/**
	 * The landing plaza, built the way worldgen does: the Citadel Gate stands on it on the side facing the final
	 * sector, the return obelisk opposite, and the plaza wall opens with two lantern posts onto the gate road, which is
	 * paved out to the sector.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 1200)
	public void citadelGateStandsOnThePlazaFacingTheFinalSector(TestContext context) {
		ServerWorld realm = realm(context);
		Structure structure = realm.getRegistryManager().get(RegistryKeys.STRUCTURE).get(Embervoid.id("ashen_reach_landing"));
		ChunkGenerator generator = realm.getChunkManager().getChunkGenerator();
		StructureStart start = structure.createStructureStart(realm.getRegistryManager(), generator,
				generator.getBiomeSource(), realm.getChunkManager().getNoiseConfig(), realm.getStructureTemplateManager(),
				realm.getSeed(), new ChunkPos(0, 0), 0, realm, biome -> true);
		BlockBox box = start.getBoundingBox();
		ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4))
				.forEach(c -> start.place(realm, realm.getStructureAccessor(), generator, realm.getRandom(),
						new BlockBox(c.getStartX(), realm.getBottomY(), c.getStartZ(), c.getEndX(), realm.getTopY(), c.getEndZ()), c));
		RealmFeaturePiece piece = (RealmFeaturePiece) start.getChildren().get(0);
		BlockPos center = piece.center();
		double toFinal = Realms.ASHEN_REACH.sectors().finalCenter();

		List<BlockPos> gates = new ArrayList<>();
		int r = RealmFeaturePiece.LANDING_RADIUS;
		for (BlockPos pos : BlockPos.iterate(center.add(-r, 1, -r), center.add(r, 6, r))) {
			if (realm.getBlockState(pos).isOf(RealmBlocks.CITADEL_GATE)) {
				gates.add(pos.toImmutable());
			}
		}
		context.assertTrue(gates.size() == 3, "expected a 3-high Citadel Gate on the plaza, found " + gates.size());
		for (BlockPos gate : gates) {
			double dx = gate.getX() - center.getX();
			double dz = gate.getZ() - center.getZ();
			context.assertTrue(Math.hypot(dx, dz) <= r, "gate off the plaza at " + gate.toShortString());
			double off = Math.abs(Math.toDegrees(RealmLayout.normalize(Math.atan2(dz, dx) - toFinal + Math.PI) - Math.PI));
			context.assertTrue(off < 45, "the gate at " + gate.toShortString() + " is " + off + "° off the final sector");
		}
		BlockPos obelisk = center.up().offset(RealmFeaturePiece.gateSide(realm).getOpposite(), RealmFeaturePiece.GATE_DISTANCE);
		context.assertTrue(realm.getBlockState(obelisk).isOf(RealmBlocks.RETURN_OBELISK), "no return obelisk opposite the gate");

		RoadLayout.Road gateRoad = RealmRoads.gate(realm);
		context.assertTrue(gateRoad != null, "no gate road");
		BlockPos rim = center.add((int) Math.round(Math.cos(toFinal) * r), 1, (int) Math.round(Math.sin(toFinal) * r));
		context.assertTrue(realm.getBlockState(rim).isAir(), "the plaza wall blocks the gate road at " + rim);
		int lanterns = 0;
		for (BlockPos pos : BlockPos.iterate(rim.add(-7, 0, -7), rim.add(7, 3, 7))) {
			if (realm.getBlockState(pos).isOf(ModBlocks.EMBER_LANTERN)) {
				lanterns++;
			}
		}
		context.assertTrue(lanterns == 2, "the gate road's exit has " + lanterns + " lantern posts");

		int paved = 0;
		int checked = 0;
		for (int k = 3; k < gateRoad.points().size(); k += 3) {
			double[] p = gateRoad.points().get(k);
			BlockPos column = BlockPos.ofFloored(p[0], 0, p[1]);
			realm.getChunk(column);
			checked++;
			int top = realm.getTopY(Heightmap.Type.WORLD_SURFACE, column.getX(), column.getZ());
			for (int y = top; y > top - 12; y--) {
				if (realm.getBlockState(column.withY(y)).isOf(ReachBlocks.KINGSROAD_FLAGSTONES)) {
					paved++;
					break;
				}
			}
		}
		context.assertTrue(checked > 0 && paved >= checked - 1, "gate road paved at only " + paved + " of " + checked + " points");
		context.complete();
	}

	/**
	 * A death in the final sector follows the open-zone rules until PLAN 21.3 gives it its own shrines: with no shrine
	 * rested at, back at the landing; with one rested at in the realm, at that shrine.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "realm_sector_respawn", tickLimit = 200)
	public void realmRespawnIsUnchangedInTheFinalSector(TestContext context) {
		ServerWorld realm = realm(context);
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		ServerPlayerEntity[] now = {player};
		try {
			ProgressManager.unlockRealm(player, ProgressManager.REALM_1);
			now[0] = dieInTheFinalSector(context, realm, current(player));
			context.assertTrue(now[0].getServerWorld() == realm, "respawned in " + now[0].getServerWorld().getRegistryKey().getValue());
			context.assertTrue(now[0].getPos().distanceTo(RealmTeleports.landingSpot(realm)) < NEAR,
					"respawned away from the landing, at " + now[0].getBlockPos().toShortString());

			// A shrine rested at in an open zone: the final-sector death goes back there.
			double angle = Realms.ASHEN_REACH.sectors().openCenter(1);
			BlockPos column = BlockPos.ofFloored(Math.cos(angle) * 200, 0, Math.sin(angle) * 200);
			realm.getChunk(column);
			BlockPos shrine = realm.getTopPosition(Heightmap.Type.MOTION_BLOCKING, column);
			realm.setBlockState(shrine.down(), Blocks.STONE.getDefaultState());
			RealmPlayerData.get(now[0]).shrines.put(realm.getRegistryKey().getValue(), GlobalPos.create(realm.getRegistryKey(), shrine));
			now[0] = dieInTheFinalSector(context, realm, now[0]);
			context.assertTrue(now[0].getServerWorld() == realm && now[0].getPos().distanceTo(Vec3d.ofBottomCenter(shrine)) < 5,
					"respawned at " + now[0].getBlockPos().toShortString() + ", not beside the shrine at " + shrine.toShortString());
		} finally {
			TestPlayers.complete(context, current(now[0]));
		}
	}

	private static ServerPlayerEntity dieInTheFinalSector(TestContext context, ServerWorld realm, ServerPlayerEntity player) {
		double angle = Realms.ASHEN_REACH.sectors().finalCenter();
		BlockPos column = BlockPos.ofFloored(Math.cos(angle) * 400, 0, Math.sin(angle) * 400);
		realm.getChunk(column);
		BlockPos top = realm.getTopPosition(Heightmap.Type.MOTION_BLOCKING, column);
		ServerPlayerEntity moved = RealmTeleports.teleport(player, realm, Vec3d.ofBottomCenter(top.up()), 0);
		ServerPlayerEntity inRealm = current(moved == null ? player : moved);
		context.assertTrue(RealmMobLevels.zoneAt(realm, inRealm.getBlockPos()) == RealmLayout.FINAL,
				"not in the final sector at " + inRealm.getBlockPos().toShortString());
		inRealm.kill();
		ServerPlayerEntity respawned = TestPlayers.respawn(inRealm);
		realm.getEntitiesByType(ModEntities.ASH_REMNANT, r -> player.getUuid().equals(r.getOwner())).forEach(r -> r.discard());
		return respawned;
	}

	private static RegistryEntry<Biome> biomeAt(ServerWorld world, BlockPos column) {
		return world.getChunkManager().getChunkGenerator().getBiomeSource().getBiome(BiomeCoords.fromBlock(column.getX()), 16,
				BiomeCoords.fromBlock(column.getZ()), world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
	}

	private static ServerWorld realm(TestContext context) {
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		context.assertTrue(realm != null, "the Ashen Reach dimension is missing from the test world");
		return realm;
	}

	private static ServerPlayerEntity current(ServerPlayerEntity player) {
		ServerPlayerEntity now = player.getServer().getPlayerManager().getPlayer(player.getUuid());
		return now == null ? player : now;
	}
}
