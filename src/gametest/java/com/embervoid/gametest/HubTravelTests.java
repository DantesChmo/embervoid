package com.embervoid.gametest;

import com.embervoid.hub.Hub;
import com.embervoid.hub.HubLayout;
import com.embervoid.hub.HubTravel;
import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.RealmBlocks;
import com.embervoid.realm.RealmPlayerData;
import com.embervoid.realm.Realms;
import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.registry.ModEntities;
import com.embervoid.sanctum.AshenMaidenEntity;
import com.embervoid.sanctum.SanctumArena;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumEntities;
import com.embervoid.sanctum.VoidObeliskBlock.Glow;
import com.embervoid.sanctum.VoidObeliskBlockEntity;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Realm travel through the hub (PLAN 15.2, DESIGN §2): the obelisks lead into realms, the way out of a realm is the
 * hub, the hub's way out (with no lit stele) is the spot the player last left the overworld from, else the bed or the
 * world spawn. Every test touches the one shared hub, so each has its own batch.
 */
public final class HubTravelTests implements FabricGameTest {
	/** A recorded exit or a bed counts as reached within this distance (the bed: the wake-up spot beside it). */
	private static final double NEAR = 0.5;
	private static final double BESIDE_BED = 2.5;

	/**
	 * Overworld → hub (Realm 1 lights on the first visit, the spot is remembered) → the Realm 1 obelisk → the realm's
	 * landing → the return obelisk → the hub's landing; the spot stays the overworld one all along.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hub_travel_round_trip", tickLimit = 200)
	public void hubToRealmAndBack(TestContext context) {
		ServerWorld hub = hub(context);
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		step(context, () -> {
			Vec3d start = player.getPos();
			context.assertTrue(glow(player, HubLayout.OBELISKS.get(0)) == Glow.DARK, "Realm 1 lit before the hub");
			context.assertTrue(HubTravel.enter(player), "no hub");
			ServerPlayerEntity inHub = current(player);
			assertAtLanding(context, inHub, hub, "after entering");
			context.assertTrue(ProgressManager.isRealmUnlocked(inHub, ProgressManager.REALM_1), "Realm 1 not unlocked by the first visit");
			context.assertTrue(glow(inHub, HubLayout.OBELISKS.get(0)) == Glow.LIT, "Realm 1 obelisk not lit in the hub");
			assertExit(context, inHub, context.getWorld(), start);

			use(inHub, hub, HubLayout.OBELISKS.get(0).pos());
			ServerPlayerEntity inRealm = current(player);
			ServerWorld realm = inRealm.getServerWorld();
			context.assertTrue(realm.getRegistryKey() == Realms.ASHEN_REACH.world(), "the obelisk led to "
					+ realm.getRegistryKey().getValue());
			context.assertTrue(inRealm.getPos().distanceTo(RealmTeleports.landingSpot(realm)) < NEAR, "not at the realm's landing: "
					+ inRealm.getBlockPos().toShortString());
			assertExit(context, inRealm, context.getWorld(), start);

			useReturnObelisk(inRealm);
			ServerPlayerEntity back = current(player);
			assertAtLanding(context, back, hub, "after the return obelisk");
			assertExit(context, back, context.getWorld(), start);
		}, player);
		TestPlayers.complete(context, current(player));
	}

	/**
	 * Death in a realm still respawns at its landing (no shrine rested at), not in the hub or the overworld; the return
	 * obelisk then leads to the hub as before.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hub_travel_respawn", tickLimit = 200)
	public void realmRespawnIsUnchanged(TestContext context) {
		ServerWorld hub = hub(context);
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		ServerPlayerEntity[] respawned = {player};
		step(context, () -> {
			ProgressManager.unlockRealm(player, ProgressManager.REALM_1);
			context.assertTrue(RealmTeleports.enterRealm(player, ProgressManager.REALM_1, false), "the realm refused");
			ServerPlayerEntity inRealm = current(player);
			ServerWorld realm = inRealm.getServerWorld();
			inRealm.kill();
			respawned[0] = TestPlayers.respawn(inRealm);
			context.assertTrue(respawned[0].getServerWorld() == realm, "respawned in " + respawned[0].getServerWorld().getRegistryKey().getValue());
			context.assertTrue(respawned[0].getPos().distanceTo(RealmTeleports.landingSpot(realm)) < NEAR,
					"respawned away from the landing, at " + respawned[0].getBlockPos().toShortString());
			realm.getEntitiesByType(ModEntities.ASH_REMNANT, r -> player.getUuid().equals(r.getOwner())).forEach(r -> r.discard());

			useReturnObelisk(respawned[0]);
			respawned[0] = current(respawned[0]);
			assertAtLanding(context, respawned[0], hub, "after a death and the return obelisk");
		}, player);
		TestPlayers.complete(context, current(respawned[0]));
	}

	/**
	 * Two players: one goes through the hub, the other straight from an old world's Sanctum core (an obelisk outside the
	 * hub). Both come out of the realm in the hub, and each one's way out of the hub is their own overworld spot.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hub_travel_two_players", tickLimit = 200)
	public void twoPlayersKeepTheirOwnWayOut(TestContext context) {
		ServerWorld hub = hub(context);
		ServerPlayerEntity[] p = {TestPlayers.create(context, 2, 2), TestPlayers.create(context, 6, 4)};
		step(context, () -> {
			Vec3d[] start = {p[0].getPos(), p[1].getPos()};
			p[1].setYaw(90);
			HubTravel.enter(p[0]);
			use(current(p[0]), hub, HubLayout.OBELISKS.get(0).pos());
			// An old core: its obelisk takes a player who unlocked Realm 1 back in 0.4.x straight into the realm.
			ProgressManager.unlockRealm(p[1], ProgressManager.REALM_1);
			context.assertTrue(RealmTeleports.enterRealm(p[1], ProgressManager.REALM_1, false), "the old core's obelisk refused");
			for (int i = 0; i < 2; i++) {
				ServerPlayerEntity player = current(p[i]);
				context.assertTrue(Realms.isRealm(player.getWorld()), "player " + i + " not in the realm");
				assertExit(context, player, context.getWorld(), start[i]);
				useReturnObelisk(player);
				assertAtLanding(context, current(p[i]), hub, "player " + i + " after the return obelisk");
			}
			for (int i = 0; i < 2; i++) {
				HubTravel.leave(current(p[i]));
				ServerPlayerEntity player = current(p[i]);
				context.assertTrue(player.getServerWorld() == context.getWorld() && player.getPos().distanceTo(start[i]) < NEAR,
						"player " + i + " left the hub to " + player.getServerWorld().getRegistryKey().getValue() + " "
								+ player.getBlockPos().toShortString() + ", not their own spot");
			}
			context.assertTrue(Math.abs(current(p[1]).getYaw() - 90) < 1, "the facing was not kept: " + current(p[1]).getYaw());
		}, p);
		TestPlayers.complete(context, current(p[0]), current(p[1]));
	}

	/**
	 * A 0.4.x player file: progress with the attunement flag, realm data with a return point into a Sanctum. It loads
	 * without errors (Realm 1 stays unlocked, the shrine kept), the old fields are gone when saved again, no way out of
	 * the hub comes from the old return point, and the return obelisk leads to the hub, not to it.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hub_travel_old_data", tickLimit = 200)
	public void oldPlayerDataLoads(TestContext context) {
		ServerWorld hub = hub(context);
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		step(context, () -> {
			BlockPos oldSanctum = context.getAbsolutePos(new BlockPos(6, 1, 6));
			GlobalPos shrine = GlobalPos.create(Realms.ASHEN_REACH.world(), new BlockPos(40, 120, 40));
			NbtCompound nbt = player.writeNbt(new NbtCompound());
			NbtCompound attachments = nbt.getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY);
			NbtCompound progress = new NbtCompound();
			progress.putBoolean("attuned", true);
			NbtList unlocked = new NbtList();
			unlocked.add(NbtString.of(ProgressManager.REALM_1.toString()));
			progress.put("unlocked_realms", unlocked);
			attachments.put("embervoid:progress", progress);
			NbtCompound realmData = new NbtCompound();
			realmData.put("return_point", encode(GlobalPos.create(World.OVERWORLD, oldSanctum)));
			realmData.putFloat("return_yaw", 90);
			NbtCompound shrines = new NbtCompound();
			shrines.put(Realms.ASHEN_REACH.world().getValue().toString(), encode(shrine));
			realmData.put("shrines", shrines);
			attachments.put("embervoid:realm_player", realmData);
			nbt.put(AttachmentTarget.NBT_ATTACHMENT_KEY, attachments);

			player.readNbt(nbt);
			context.assertTrue(ProgressManager.isRealmUnlocked(player, ProgressManager.REALM_1), "Realm 1 lost: " + ProgressManager.get(player));
			context.assertTrue(shrine.equals(RealmPlayerData.get(player).shrines.get(Realms.ASHEN_REACH.world().getValue())),
					"realm data not loaded: " + RealmPlayerData.get(player));
			context.assertTrue(HubTravel.lastOverworldExit(player) == null, "the old return point became a way out: "
					+ HubTravel.lastOverworldExit(player));
			NbtCompound saved = player.writeNbt(new NbtCompound()).getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY);
			context.assertTrue(!saved.getCompound("embervoid:progress").contains("attuned"), "the attunement flag was saved again");
			context.assertTrue(!saved.getCompound("embervoid:realm_player").contains("return_point"), "the old return point was saved again");

			// Standing in the realm (the world was updated while they were there): the return obelisk leads to the hub.
			RealmTeleports.toLanding(player, Realms.ASHEN_REACH.world());
			ServerPlayerEntity inRealm = current(player);
			useReturnObelisk(inRealm);
			assertAtLanding(context, current(player), hub, "after the return obelisk with an old return point");
		}, player);
		TestPlayers.complete(context, current(player));
	}

	/**
	 * An old world's Sanctum core (PLAN 15.5): the 0.4.x core template placed far off in the test world brings its Ashen
	 * Maiden and four obelisks. The Realm 1 obelisk stays dark for a player without Realm 1 and takes one who has it
	 * into the realm; the return obelisk leads to the hub, and the hub's way out back to where the player stood.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hub_travel_old_core", tickLimit = 200)
	public void oldSanctumCoreStillLeadsToTheRealm(TestContext context) {
		ServerWorld hub = hub(context);
		ServerWorld world = context.getWorld();
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		step(context, () -> {
			StructureTemplate core = world.getStructureTemplateManager().getTemplate(SanctumArena.OLD_CORE)
					.orElseThrow(() -> new GameTestException("the old core template does not load"));
			BlockPos corner = context.getAbsolutePos(new BlockPos(4000, 0, 0));
			core.place(world, corner, corner, new StructurePlacementData(), world.getRandom(), Block.NOTIFY_LISTENERS);
			Box bounds = Box.from(BlockBox.create(corner, corner.add(core.getSize()))).expand(1);
			List<AshenMaidenEntity> maidens = world.getEntitiesByType(SanctumEntities.ASHEN_MAIDEN, bounds, maiden -> true);
			context.assertTrue(maidens.size() == 1, "the old core placed " + maidens.size() + " Maidens");
			BlockPos obelisk = null;
			for (StructureTemplate.StructureBlockInfo info : core.getInfosForBlock(corner, new StructurePlacementData(), SanctumBlocks.VOID_OBELISK)) {
				if (world.getBlockEntity(info.pos()) instanceof VoidObeliskBlockEntity entity && entity.getRealm().equals(ProgressManager.REALM_1)) {
					obelisk = info.pos();
				}
			}
			context.assertTrue(obelisk != null, "no Realm 1 obelisk in the old core");

			Vec3d start = player.getPos();
			use(player, world, obelisk);
			context.assertTrue(current(player).getServerWorld() == world, "a dark old obelisk let the player through");
			// Attuned in 0.4.x: Realm 1 is among the unlocked realms.
			ProgressManager.unlockRealm(player, ProgressManager.REALM_1);
			use(player, world, obelisk);
			ServerPlayerEntity inRealm = current(player);
			context.assertTrue(inRealm.getServerWorld().getRegistryKey() == Realms.ASHEN_REACH.world(), "the old obelisk led to "
					+ inRealm.getServerWorld().getRegistryKey().getValue());
			assertExit(context, inRealm, world, start);
			useReturnObelisk(inRealm);
			assertAtLanding(context, current(player), hub, "after the return obelisk");
			HubTravel.leave(current(player));
			context.assertTrue(current(player).getServerWorld() == world && current(player).getPos().distanceTo(start) < NEAR,
					"the hub's way out led to " + current(player).getBlockPos().toShortString() + ", not back by the old core");
			maidens.forEach(Entity::discard);
		}, player);
		TestPlayers.complete(context, current(player));
	}

	/**
	 * {@code /embervoid hub leave}: to the recorded spot; with none, to the player's bed; with no bed either, to the
	 * world spawn.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "hub_travel_leave", tickLimit = 200)
	public void hubLeaveCommand(TestContext context) {
		ServerWorld hub = hub(context);
		ServerWorld overworld = context.getWorld().getServer().getOverworld();
		ServerPlayerEntity traveller = TestPlayers.create(context, 2, 2);
		ServerPlayerEntity sleeper = TestPlayers.create(context, 8, 8);
		step(context, () -> {
			Vec3d start = traveller.getPos();
			HubTravel.enter(traveller);
			context.assertTrue(current(traveller).getServerWorld() == hub, "not in the hub");
			run(current(traveller), "embervoid hub leave");
			context.assertTrue(current(traveller).getServerWorld() == context.getWorld()
					&& current(traveller).getPos().distanceTo(start) < NEAR, "not back at the recorded spot: "
					+ current(traveller).getBlockPos().toShortString());

			// No recorded spot (sent by the hub's own landing, as a death-less 0.4.x player would be): the bed.
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					context.setBlockState(5 + dx, 0, 5 + dz, Blocks.STONE);
				}
			}
			context.setBlockState(5, 1, 5, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.FOOT));
			context.setBlockState(5, 1, 4, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD));
			BlockPos bed = context.getAbsolutePos(new BlockPos(5, 1, 4));
			sleeper.setSpawnPoint(context.getWorld().getRegistryKey(), bed, 0, false, false);
			Hub.sendToLanding(sleeper);
			context.assertTrue(HubTravel.lastOverworldExit(current(sleeper)) == null, "a spot was recorded without travel");
			run(current(sleeper), "embervoid hub leave");
			double fromBed = current(sleeper).getPos().distanceTo(Vec3d.ofBottomCenter(bed));
			context.assertTrue(current(sleeper).getServerWorld() == context.getWorld() && fromBed < BESIDE_BED,
					"not at the bed: " + fromBed + " blocks away in " + current(sleeper).getServerWorld().getRegistryKey().getValue());

			// No bed either: the world spawn.
			context.setBlockState(5, 1, 4, Blocks.AIR);
			context.setBlockState(5, 1, 5, Blocks.AIR);
			Hub.sendToLanding(current(sleeper));
			run(current(sleeper), "embervoid hub leave");
			double fromSpawn = current(sleeper).getPos().distanceTo(Vec3d.ofBottomCenter(overworld.getSpawnPos()));
			context.assertTrue(current(sleeper).getServerWorld() == overworld && fromSpawn < NEAR,
					"not at the world spawn: " + fromSpawn + " blocks away");
		}, traveller, sleeper);
		TestPlayers.complete(context, current(traveller), current(sleeper));
	}

	private static ServerWorld hub(TestContext context) {
		ServerWorld hub = Hub.world(context.getWorld().getServer());
		if (hub == null) {
			throw new GameTestException("no hub dimension in the test world");
		}
		return hub;
	}

	/** The player entity now on the server for this player (a dimension change can replace it). */
	private static ServerPlayerEntity current(ServerPlayerEntity player) {
		ServerPlayerEntity now = player.getServer().getPlayerManager().getPlayer(player.getUuid());
		return now == null ? player : now;
	}

	private static void assertAtLanding(TestContext context, ServerPlayerEntity player, ServerWorld hub, String when) {
		context.assertTrue(player.getServerWorld() == hub, when + ": in " + player.getServerWorld().getRegistryKey().getValue()
				+ ", not the hub");
		context.assertTrue(player.getPos().distanceTo(Vec3d.ofBottomCenter(HubLayout.LANDING)) < NEAR, when + ": at "
				+ player.getBlockPos().toShortString() + ", not the hub's landing");
	}

	private static void assertExit(TestContext context, ServerPlayerEntity player, ServerWorld world, Vec3d pos) {
		HubTravel.Exit exit = HubTravel.lastOverworldExit(player);
		context.assertTrue(exit != null && exit.dimension() == world.getRegistryKey() && exit.pos().distanceTo(pos) < NEAR,
				"way out of the hub: " + exit + ", expected " + pos);
	}

	/**
	 * A right click on a return obelisk at the player's realm landing. The test world's realms have no landing feature, so
	 * one is set beside the landing for the click and taken away after.
	 */
	private static void useReturnObelisk(ServerPlayerEntity player) {
		ServerWorld realm = player.getServerWorld();
		BlockPos obelisk = BlockPos.ofFloored(RealmTeleports.landingSpot(realm)).east(3);
		realm.setBlockState(obelisk, RealmBlocks.RETURN_OBELISK.getDefaultState());
		use(player, realm, obelisk);
		realm.setBlockState(obelisk, Blocks.AIR.getDefaultState());
	}

	private static NbtElement encode(GlobalPos pos) {
		return GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow(false, s -> {});
	}

	private static void run(ServerPlayerEntity player, String command) {
		player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(2), command);
	}

	private static Glow glow(ServerPlayerEntity player, HubLayout.Obelisk obelisk) {
		return Glow.of(ProgressManager.get(player), obelisk.realm());
	}

	/** A right click on the top of a block with whatever the player holds in the main hand. */
	private static ActionResult use(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos).add(0, 0.5, 0), Direction.UP, pos, false);
		return player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
	}

	/** Runs a test's checks; on a failure the players leave the server before the runner sees it. */
	private static void step(TestContext context, Runnable checks, ServerPlayerEntity... players) {
		try {
			checks.run();
		} catch (RuntimeException e) {
			for (ServerPlayerEntity player : players) {
				ServerPlayerEntity now = current(player);
				if (now.getServer().getPlayerManager().getPlayer(now.getUuid()) == now) {
					now.getServer().getPlayerManager().remove(now);
				}
			}
			throw e;
		}
	}
}
