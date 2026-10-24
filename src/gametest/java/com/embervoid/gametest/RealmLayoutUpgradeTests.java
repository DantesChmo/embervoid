package com.embervoid.gametest;

import com.embervoid.hub.Hub;
import com.embervoid.hub.HubLayout;
import com.embervoid.realm.RealmLayoutUpgrade;
import com.embervoid.realm.RealmPlayerData;
import com.embervoid.realm.Realms;
import com.embervoid.registry.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.storage.RegionFile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Worlds from 0.4.x (PLAN 21.4) on a made-up old save in a scratch folder: the realm and depths folders go, the other
 * dimensions stay, a remnant lying in the realm and a player who logged out there are kept for after the start; a world
 * already on the layout is left alone. Then a player's join: moved to the hub, old realm shrines forgotten once.
 */
public final class RealmLayoutUpgradeTests implements FabricGameTest {
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void oldWorldsLoseTheirRealmFoldersAndKeepWhatWasInside(TestContext context) {
		try {
			Path root = Files.createTempDirectory("embervoid-old-world");
			Path realm = root.resolve("dimensions/embervoid/ashen_reach");
			Path depths = root.resolve("dimensions/embervoid/ashen_reach_depths");
			Path hub = root.resolve("dimensions/embervoid/hub/region");
			Files.createDirectories(realm.resolve("region"));
			Files.createDirectories(depths.resolve("entities"));
			Files.createDirectories(hub);
			Files.writeString(realm.resolve("region/r.0.0.mca"), "old terrain");
			Files.writeString(hub.resolve("r.0.0.mca"), "hub");
			UUID remnant = UUID.randomUUID();
			writeEntities(depths.resolve("entities"), remnant);
			UUID inside = UUID.randomUUID();
			UUID outside = UUID.randomUUID();
			Files.createDirectories(root.resolve("playerdata"));
			writePlayer(root.resolve("playerdata"), inside, "embervoid:ashen_reach_depths");
			writePlayer(root.resolve("playerdata"), outside, "minecraft:overworld");

			context.assertTrue(RealmLayoutUpgrade.upgrade(root), "the old world was not upgraded");
			context.assertTrue(!Files.exists(realm) && !Files.exists(depths), "a realm folder is still there");
			context.assertTrue(Files.exists(hub.resolve("r.0.0.mca")), "the hub's folder was touched");
			context.assertTrue(Files.readString(root.resolve(RealmLayoutUpgrade.VERSION_FILE)).trim()
					.equals(Integer.toString(RealmLayoutUpgrade.CURRENT)), "no layout number written");
			NbtCompound pending = NbtIo.readCompressed(root.resolve(RealmLayoutUpgrade.PENDING_FILE).toFile());
			NbtList remnants = pending.getList("Remnants", NbtElement.COMPOUND_TYPE);
			context.assertTrue(remnants.size() == 1 && remnants.getCompound(0).getUuid("UUID").equals(remnant),
					"remnants kept: " + remnants);
			NbtList players = pending.getList("Players", NbtElement.COMPOUND_TYPE);
			context.assertTrue(players.size() == 1 && players.getCompound(0).getUuid("Player").equals(inside),
					"players to move: " + players);

			// Started again: nothing more to do.
			Files.createDirectories(realm);
			context.assertTrue(!RealmLayoutUpgrade.upgrade(root) && Files.exists(realm), "an upgraded world was cleared again");
			try (var walk = Files.walk(root)) {
				for (Path path : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
					Files.delete(path);
				}
			}
		} catch (IOException e) {
			throw new GameTestException("old world: " + e);
		}
		context.complete();
	}

	/**
	 * After the start, the kept remnant lies on the hub's landing, the same entity, and the pending file is gone. Its own
	 * batch: the pending file is the server's one, and the remnant may take a few ticks to show up in the hub.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "realm_layout_remnants", tickLimit = 200)
	public void keptRemnantsLieOnTheHubLanding(TestContext context) {
		MinecraftServer server = context.getWorld().getServer();
		UUID remnant = UUID.randomUUID();
		Path pending = server.getSavePath(WorldSavePath.ROOT).resolve(RealmLayoutUpgrade.PENDING_FILE);
		try {
			NbtCompound entity = new NbtCompound();
			entity.putString("id", EntityType.getId(ModEntities.ASH_REMNANT).toString());
			entity.putUuid("UUID", remnant);
			NbtList remnants = new NbtList();
			remnants.add(entity);
			NbtCompound data = new NbtCompound();
			data.put("Remnants", remnants);
			data.put("Players", new NbtList());
			NbtIo.writeCompressed(data, pending.toFile());
		} catch (IOException e) {
			throw new GameTestException("pending file: " + e);
		}
		// The landing's chunk held loaded, so the remnant shows up in the hub's entity lookup.
		ServerWorld hub = Hub.world(server);
		ChunkPos landing = new ChunkPos(HubLayout.LANDING);
		hub.setChunkForced(landing.x, landing.z, true);
		RealmLayoutUpgrade.finish(server);
		context.assertTrue(!Files.exists(pending), "the pending file is still there");
		context.addInstantFinalTask(() -> {
			Entity found = hub.getEntity(remnant);
			context.assertTrue(found != null && found.getType() == ModEntities.ASH_REMNANT
					&& found.getBlockPos().isWithinDistance(HubLayout.LANDING, 4), "remnant on the landing: " + found
					+ "; remnants in the hub: " + hub.getEntitiesByType(ModEntities.ASH_REMNANT, r -> true));
			hub.setChunkForced(landing.x, landing.z, false);
			found.discard();
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void aPlayerFromTheOldRealmJoinsInTheHub(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		RealmPlayerData data = RealmPlayerData.get(player);
		data.shrines.put(Realms.ASHEN_REACH.world().getValue(),
				GlobalPos.create(Realms.ASHEN_REACH.world(), new BlockPos(100, 70, 100)));
		data.layout = 0;
		RealmLayoutUpgrade.moveOnJoin(player.getServer(), player.getUuid());
		RealmLayoutUpgrade.onJoin(player);
		ServerPlayerEntity now = player.getServer().getPlayerManager().getPlayer(player.getUuid());
		now = now == null ? player : now;
		context.assertTrue(Hub.isHub(now.getWorld()), "not moved to the hub: " + now.getWorld().getRegistryKey().getValue());
		context.assertTrue(RealmPlayerData.get(now).shrines.isEmpty() && RealmPlayerData.get(now).layout == RealmLayoutUpgrade.CURRENT,
				"old shrines kept: " + RealmPlayerData.get(now));
		// A second join moves nothing and keeps a shrine rested at since.
		RealmPlayerData.get(now).shrines.put(Realms.ASHEN_REACH.world().getValue(),
				GlobalPos.create(Realms.ASHEN_REACH.world(), new BlockPos(50, 70, 50)));
		RealmLayoutUpgrade.onJoin(now);
		context.assertTrue(RealmPlayerData.get(now).shrines.size() == 1, "a new shrine was forgotten");
		TestPlayers.complete(context, now);
	}

	/** One entity region file with a single Ash Remnant in chunk 0, 0. */
	private static void writeEntities(Path entities, UUID remnant) throws IOException {
		NbtCompound entity = new NbtCompound();
		entity.putString("id", EntityType.getId(ModEntities.ASH_REMNANT).toString());
		entity.putUuid("UUID", remnant);
		NbtList list = new NbtList();
		list.add(entity);
		NbtCompound chunk = new NbtCompound();
		chunk.put("Entities", list);
		try (RegionFile region = new RegionFile(entities.resolve("r.0.0.mca"), entities, false);
				DataOutputStream out = region.getChunkOutputStream(new ChunkPos(0, 0))) {
			NbtIo.write(chunk, out);
		}
	}

	private static void writePlayer(Path playerdata, UUID player, String dimension) throws IOException {
		NbtCompound data = new NbtCompound();
		data.putString("Dimension", dimension);
		NbtIo.writeCompressed(data, playerdata.resolve(player + ".dat").toFile());
	}
}
