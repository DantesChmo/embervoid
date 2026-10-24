package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.ArenaShrines;
import com.embervoid.boss.BossEntities;
import com.embervoid.boss.BossFight;
import com.embervoid.entity.AshRemnantEntity;
import com.embervoid.item.HealingFlaskItem;
import com.embervoid.mob.MobLevels;
import com.embervoid.moon.MoonNightManager;
import com.embervoid.moon.api.CurseOfTheNight;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import com.embervoid.nightmob.AshenZombieEntity;
import com.embervoid.nightmob.GhostEntity;
import com.embervoid.nightmob.NightMobConfig;
import com.embervoid.nightmob.NightWaves;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.XpUtil;
import com.embervoid.progress.ProgressManager;
import com.embervoid.reach.boss.AshOfRematch;
import com.embervoid.reach.boss.ReachBoss;
import com.embervoid.reach.boss.ReachBosses;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.reach.mob.ReachMobs;
import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.RealmBlocks;
import com.embervoid.realm.RealmPlayerData;
import com.embervoid.realm.Realms;
import com.embervoid.realm.CitadelGateBlock;
import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.realm.worldgen.RealmFeaturePiece;
import com.embervoid.realm.worldgen.RealmFeatures;
import com.embervoid.registry.ModEntities;
import com.embervoid.reliquary.ReliquaryManager;
import com.embervoid.reliquary.SlotKinds;
import com.embervoid.sanctum.SanctumBlocks;
import com.embervoid.sanctum.SanctumItems;
import com.embervoid.sanctum.VoidObeliskBlock.Glow;
import com.embervoid.stats.PlayerStats;
import com.embervoid.stats.Stat;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Multiplayer (PLAN R40.4a, R30.3): two or three real players joined through the player manager, in the test area and
 * in the shared realm dimension. Progress, boss credit and personal loot are per player (DESIGN §4.3); a boss fought
 * by more players scales at summon (§4.5 "Multiplayer"); deaths, syncs and moon waves stay with their own player.
 * Fights, rematches and the moon night are global, so each test has its own batch.
 */
public final class MultiplayerTests implements FabricGameTest {
	private static final int RADIUS = 6;

	/**
	 * Three players, a veteran (A) and a newcomer (B) with Realm 1 unlocked, one without (C). A beats the three zone
	 * bosses alone; then A helps B with one. Each keeps their own beaten bosses and obelisks, only the owner hears of
	 * a change, and the Citadel Gate lets A through while it stays sealed for B and C.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "mp_progress", tickLimit = 400)
	public void progressAndTheGateArePerPlayer(TestContext context) {
		// The local config of the test run dir may be older than 0.4.0 (depths closed).
		BossArena arena = new BossArena(context, "{\"realm\": {\"depthsOpen\": true}}");
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			arena.floor(30, 4, 2);
			Recorder[] sync = {new Recorder(), new Recorder(), new Recorder()};
			ServerPlayerEntity a = arena.join(4, 6, sync[0]);
			ServerPlayerEntity b = arena.join(30, 3, sync[1]);
			ServerPlayerEntity c = arena.join(30, 5, sync[2]);
			ProgressManager.unlockRealm(a, ProgressManager.REALM_1);
			ProgressManager.unlockRealm(b, ProgressManager.REALM_1);
			context.assertTrue(glow(a, ProgressManager.REALM_1) == Glow.LIT && glow(b, ProgressManager.REALM_1) == Glow.LIT,
					"Realm 1 not lit for the players who unlocked it");
			context.assertTrue(glow(c, ProgressManager.REALM_1) == Glow.DARK, "Realm 1 lit for a player who never unlocked it");

			int bBefore = sync[1].count(ProgressManager.SYNC_PACKET);
			int cBefore = sync[2].count(ProgressManager.SYNC_PACKET);
			int aBefore = sync[0].count(ProgressManager.SYNC_PACKET);
			for (Identifier boss : Realms.ASHEN_REACH.zoneBosses()) {
				BossRewardTests.kill(arena.start(boss, BossEntities.TEST_BOSS, 4, 4, RADIUS, 1));
			}
			context.assertTrue(sync[0].count(ProgressManager.SYNC_PACKET) == aBefore + 3,
					"A's progress syncs: " + (sync[0].count(ProgressManager.SYNC_PACKET) - aBefore));
			context.assertTrue(sync[1].count(ProgressManager.SYNC_PACKET) == bBefore, "B was sent A's progress");
			context.assertTrue(sync[2].count(ProgressManager.SYNC_PACKET) == cBefore, "C was sent A's progress");
			for (Identifier boss : Realms.ASHEN_REACH.zoneBosses()) {
				context.assertTrue(ProgressManager.hasBeatenBoss(a, boss), "A did not beat " + boss);
				context.assertTrue(!ProgressManager.hasBeatenBoss(b, boss) && !ProgressManager.hasBeatenBoss(c, boss),
						"a player outside the arena got credit for " + boss);
			}

			// A helps B: B's first kill, A's repeat; C (outside) still nothing.
			b.teleport(context.getWorld(), a.getX() + 2, a.getY(), a.getZ(), 0, 0);
			BossRewardTests.kill(arena.start(ReachBosses.FIELDS, BossEntities.TEST_BOSS, 4, 4, RADIUS, 1));
			context.assertTrue(ProgressManager.hasBeatenBoss(b, ReachBosses.FIELDS), "B not credited with a helper");
			context.assertTrue(b.getInventory().count(GearItems.ASH_COLOSSUS_SOUL) == 1, "B: no soul of their own");
			context.assertTrue(a.getInventory().count(SanctumItems.FLASK_SHARD) == 3, "A: shards " + a.getInventory().count(SanctumItems.FLASK_SHARD));
			context.assertTrue(b.getInventory().count(SanctumItems.FLASK_SHARD) == 1, "B: shards " + b.getInventory().count(SanctumItems.FLASK_SHARD));
			context.assertTrue(ProgressManager.get(c).beatenBosses().isEmpty(), "C got credit: " + ProgressManager.get(c).beatenBosses());

			context.assertTrue(CitadelGateBlock.remainingZoneBosses(a, Realms.ASHEN_REACH) == 0, "gate sealed for A");
			context.assertTrue(CitadelGateBlock.remainingZoneBosses(b, Realms.ASHEN_REACH) == 2, "B's locks: "
					+ CitadelGateBlock.remainingZoneBosses(b, Realms.ASHEN_REACH));
			context.assertTrue(CitadelGateBlock.remainingZoneBosses(c, Realms.ASHEN_REACH) == 3, "C's locks");

			// The gate itself, used by each: B and C stay, A goes to the depths.
			ServerWorld realm = arena.server.getWorld(Realms.ASHEN_REACH.world());
			BlockPos gate = new BlockPos(0, 100, 10);
			for (ServerPlayerEntity player : List.of(b, c)) {
				useGate(realm, gate, player);
				context.assertTrue(player.getServerWorld() == context.getWorld(), player.getName().getString() + " went through a sealed gate");
			}
			useGate(realm, gate, a);
			context.assertTrue(a.getServerWorld().getRegistryKey() == Realms.ASHEN_REACH.depths(),
					"A did not reach the depths: " + a.getServerWorld().getRegistryKey().getValue());
			arena.finish();
		});
	}

	/**
	 * The summon rule in the real Necropolis arena with a veteran (V), a newcomer (N) and an outsider (O): V alone
	 * walks through an empty arena; N stepping in with V summons the Crypt Keeper for both (HP ×1.5, one Restless
	 * Bones). O is kept out by the fog wall, also after getting inside some other way. The kill credits both
	 * fighters, each with their own soul (V at the repeat chance, 1 here), and nothing for O.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "mp_boss", tickLimit = PlacedFight.LOAD_LIMIT + 1200)
	public void zoneBossForTwoWithAnOutsider(TestContext context) {
		BossArena arena = new BossArena(context, "{\"boss\": {\"repeatSoulChance\": 1}}");
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		RealmFeaturePiece[] piece = new RealmFeaturePiece[1];
		ServerPlayerEntity[] p = new ServerPlayerEntity[3];
		arena.step(() -> piece[0] = placeArena(arena, realm, ReachBosses.NECROPOLIS));
		arena.await(() -> arenaTicks(realm, piece[0]), PlacedFight.LOAD_LIMIT, "the arena chunks to tick", () -> {
			for (int i = 0; i < 3; i++) {
				p[i] = arena.join(2 + 2 * i, 2);
				p[i].addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 4000, 4));
				moveTo(p[i], realm, piece[0].shrinePos().add(1 + i, 0, 1));
			}
			ProgressManager.markBossBeaten(p[0], ReachBosses.NECROPOLIS);
			moveTo(p[0], realm, inside(piece[0], -2));
			arena.after(20, () -> {
				context.assertTrue(ArenaManager.fightAt(realm, piece[0].center()) == null, "the boss rose for the veteran alone");
				moveTo(p[1], realm, inside(piece[0], 2));
				arena.await(() -> ArenaManager.fightAt(realm, piece[0].center()) != null, 40, "the newcomer to summon the boss",
						() -> fightWithAnOutsider(context, arena, realm, piece[0], p));
			});
		});
	}

	private static void fightWithAnOutsider(TestContext context, BossArena arena, ServerWorld realm, RealmFeaturePiece piece,
			ServerPlayerEntity[] p) {
		BossFight fight = ArenaManager.fightAt(realm, piece.center());
		arena.track(fight);
		MobEntity boss = heldBoss(fight);
		context.assertTrue(Set.copyOf(fight.fighters()).equals(Set.of(p[0], p[1])), "fighters: " + names(fight.fighters()));
		assertScaled(context, realm, fight, boss, 1.5, 1);
		// O comes to the wall from outside: pushed back out.
		Vec3d c = fight.centerPos();
		int r = fight.spec().radius();
		moveTo(p[2], realm, BlockPos.ofFloored(c.x + r + 0.3, c.y + 2, c.z));
		arena.after(2, () -> {
			assertPushedOut(context, running(fight, p), p[2], "at the wall");
			// O gets inside past the wall (a pearl, a teleport): still pushed out, still not a fighter.
			moveTo(p[2], realm, inside(piece, 6));
			arena.after(2, () -> {
				assertPushedOut(context, running(fight, p), p[2], "inside the arena");
				Vec3d at = boss.getPos();
				boss.kill();
				context.assertTrue(fight.isEnded(), "the kill did not end the fight");
				context.assertTrue(ProgressManager.hasBeatenBoss(p[1], ReachBosses.NECROPOLIS), "the newcomer was not credited");
				context.assertTrue(!ProgressManager.hasBeatenBoss(p[2], ReachBosses.NECROPOLIS), "the outsider was credited");
				context.assertTrue(p[0].getInventory().count(GearItems.CRYPT_KEEPER_SOUL) == 1, "veteran: no soul at chance 1");
				context.assertTrue(p[0].getInventory().count(SanctumItems.FLASK_SHARD) == 0, "veteran: a second Flask Shard");
				context.assertTrue(p[1].getInventory().count(GearItems.CRYPT_KEEPER_SOUL) == 1, "newcomer: no soul");
				context.assertTrue(p[1].getInventory().count(SanctumItems.FLASK_SHARD) == 1, "newcomer: no Flask Shard");
				context.assertTrue(p[2].getInventory().count(GearItems.CRYPT_KEEPER_SOUL) == 0, "outsider: a soul");
				boolean soulOnGround = !realm.getEntitiesByClass(ItemEntity.class, new Box(BlockPos.ofFloored(at)).expand(r),
						item -> item.getStack().isOf(GearItems.CRYPT_KEEPER_SOUL)).isEmpty();
				context.assertTrue(!soulOnGround, "a soul dropped on the ground for anyone to take");
				// Both have beaten it now: walking about inside summons nothing (O, who has not, is out again).
				moveTo(p[2], realm, piece.shrinePos().add(3, 0, 1));
				arena.after(20, () -> {
					context.assertTrue(ArenaManager.fightAt(realm, piece.center()) == null, "the boss rose again for players who beat it");
					arena.finish();
				});
			});
		});
	}

	/**
	 * A rematch primed by one veteran, entered by three: the primer, another veteran and a newcomer. HP ×2 and two
	 * Restless Bones; the kill gives the veterans no soul (a rematch, even at a repeat chance of 1) and the newcomer
	 * their first-kill soul and Flask Shard.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "mp_rematch", tickLimit = PlacedFight.LOAD_LIMIT + 1200)
	public void rematchPrimedByOneForThree(TestContext context) {
		BossArena arena = new BossArena(context, "{\"boss\": {\"repeatSoulChance\": 1}}");
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		RealmFeaturePiece[] piece = new RealmFeaturePiece[1];
		ServerPlayerEntity[] p = new ServerPlayerEntity[3];
		arena.step(() -> piece[0] = placeArena(arena, realm, ReachBosses.NECROPOLIS));
		arena.await(() -> arenaTicks(realm, piece[0]), PlacedFight.LOAD_LIMIT, "the arena chunks to tick", () -> {
			for (int i = 0; i < 3; i++) {
				p[i] = arena.join(2 + 2 * i, 2);
				p[i].addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 4000, 4));
				moveTo(p[i], realm, piece[0].shrinePos().add(1 + i, 0, 1));
			}
			ProgressManager.markBossBeaten(p[0], ReachBosses.NECROPOLIS);
			ProgressManager.markBossBeaten(p[1], ReachBosses.NECROPOLIS);
			ItemStack ash = new ItemStack(GearItems.ASH_OF_REMATCH);
			ActionResult used = AshOfRematch.use(p[0], realm, piece[0].shrinePos(), ash);
			context.assertTrue(used == ActionResult.SUCCESS, "the ash failed: " + used + ", fight " + ArenaManager.fightAt(realm, piece[0].center())
					+ ", primed " + ArenaManager.isRematchPrimed(realm, piece[0].center()) + ", shrine " + piece[0].shrinePos().toShortString()
					+ " " + realm.getBlockState(piece[0].shrinePos()) + ", " + featureAt(realm, piece[0].shrinePos()));
			for (int i = 0; i < 3; i++) {
				moveTo(p[i], realm, inside(piece[0], 2 * i - 2));
			}
			arena.await(() -> ArenaManager.fightAt(realm, piece[0].center()) != null, 40, "the rematch to start", () -> {
				BossFight fight = ArenaManager.fightAt(realm, piece[0].center());
				arena.track(fight);
				context.assertTrue(fight.isRematch(), "not a rematch");
				MobEntity boss = heldBoss(fight);
				context.assertTrue(fight.fighters().size() == 3, "fighters: " + names(fight.fighters()));
				assertScaled(context, realm, fight, boss, 2.0, 2);
				boss.kill();
				context.assertTrue(fight.isEnded(), "the kill did not end the rematch");
				for (int i = 0; i < 2; i++) {
					context.assertTrue(p[i].getInventory().count(GearItems.CRYPT_KEEPER_SOUL) == 0, "veteran " + i + ": a soul from a rematch");
					context.assertTrue(p[i].getInventory().count(SanctumItems.FLASK_SHARD) == 0, "veteran " + i + ": a Flask Shard");
				}
				context.assertTrue(ProgressManager.hasBeatenBoss(p[2], ReachBosses.NECROPOLIS), "the newcomer was not credited");
				context.assertTrue(p[2].getInventory().count(GearItems.CRYPT_KEEPER_SOUL) == 1, "newcomer: no first-kill soul");
				context.assertTrue(p[2].getInventory().count(SanctumItems.FLASK_SHARD) == 1, "newcomer: no Flask Shard");
				arena.finish();
			});
		});
	}

	/**
	 * Two players in the realm, each rested at their own shrine, die on the same spot: two remnants, each only its
	 * owner's (the other cannot take from it); each respawns at their own shrine and gets their own things back.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "mp_death", tickLimit = PlacedFight.LOAD_LIMIT + 200)
	public void realmDeathsKeepToTheirOwners(TestContext context) {
		BossArena arena = new BossArena(context, null);
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		// High above the realm's terrain, away from the landing, arenas and roads.
		BlockPos base = new BlockPos(-320, 230, 280);
		BlockPos[] shrines = {base, base.add(24, 0, 0)};
		BlockPos deathSpot = base.add(12, 0, 0);
		ServerPlayerEntity[] p = new ServerPlayerEntity[2];
		arena.step(() -> arena.forceChunks(realm, deathSpot, 32));
		arena.await(() -> realm.shouldTickEntity(base) && realm.shouldTickEntity(shrines[1]), PlacedFight.LOAD_LIMIT,
				"the realm platform to tick", () -> {
			for (int dx = -3; dx <= 27; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					realm.setBlockState(base.add(dx, -1, dz), Blocks.STONE.getDefaultState());
				}
			}
			for (BlockPos shrine : shrines) {
				realm.setBlockState(shrine, SanctumBlocks.EMBER_SHRINE.getDefaultState());
			}
			p[0] = arena.join(2, 2);
			p[1] = arena.join(4, 2);
			for (int i = 0; i < 2; i++) {
				moveTo(p[i], realm, shrines[i].south());
				BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(shrines[i]), Direction.SOUTH, shrines[i], false);
				realm.getBlockState(shrines[i]).onUse(realm, p[i], Hand.MAIN_HAND, hit);
				context.assertTrue(RealmTeleports.hasRestedShrine(p[i], ProgressManager.REALM_1), "player " + i + " rest not recorded: "
						+ RealmPlayerData.get(p[i]) + " in " + p[i].getServerWorld().getRegistryKey().getValue());
			}
			p[0].getInventory().setStack(5, new ItemStack(Items.DIAMOND, 5));
			XpUtil.setPoints(p[0], 120);
			p[1].getInventory().setStack(5, new ItemStack(Items.EMERALD, 3));
			XpUtil.setPoints(p[1], 40);
			for (ServerPlayerEntity player : p) {
				moveTo(player, realm, deathSpot);
				player.kill();
			}
			AshRemnantEntity[] remnants = {remnantOf(realm, p[0]), remnantOf(realm, p[1])};
			context.assertTrue(remnants[0] != remnants[1], "one remnant for two players");

			ServerPlayerEntity[] back = {TestPlayers.respawn(p[0]), TestPlayers.respawn(p[1])};
			for (int i = 0; i < 2; i++) {
				context.assertTrue(back[i].getServerWorld() == realm, "player " + i + " respawned outside the realm: "
						+ back[i].getServerWorld().getRegistryKey().getValue());
				double d = back[i].getPos().distanceTo(Vec3d.ofBottomCenter(shrines[i]));
				context.assertTrue(d < 4, "player " + i + " respawned " + d + " blocks from their own shrine, at "
						+ back[i].getBlockPos().toShortString() + ", " + RealmPlayerData.get(back[i]));
			}
			// The second player tries the first one's remnant: nothing moves.
			moveTo(back[1], realm, deathSpot);
			remnants[0].interact(back[1], Hand.MAIN_HAND);
			context.assertTrue(!remnants[0].isRemoved() && back[1].getInventory().count(Items.DIAMOND) == 0
					&& XpUtil.getPoints(back[1]) == 0, "someone else's remnant gave its things away");
			remnants[1].interact(back[1], Hand.MAIN_HAND);
			moveTo(back[0], realm, deathSpot);
			remnants[0].interact(back[0], Hand.MAIN_HAND);
			context.assertTrue(back[0].getInventory().count(Items.DIAMOND) == 5 && XpUtil.getPoints(back[0]) == 120,
					"first player's things: " + back[0].getInventory().count(Items.DIAMOND) + " diamonds, " + XpUtil.getPoints(back[0]) + " XP");
			context.assertTrue(back[1].getInventory().count(Items.EMERALD) == 3 && XpUtil.getPoints(back[1]) == 40,
					"second player's things: " + back[1].getInventory().count(Items.EMERALD) + " emeralds, " + XpUtil.getPoints(back[1]) + " XP");
			context.assertTrue(back[0].getInventory().count(Items.EMERALD) == 0, "first player got the second one's emeralds");
			arena.finish();
		});
	}

	/**
	 * One player's Reliquary, stats, flask and progress change: the sync packets go to that player's client only
	 * (another player in sight of them gets none).
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "mp_sync", tickLimit = 100)
	public void syncGoesToItsOwnerOnly(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, 3);
			Recorder ownerSync = new Recorder();
			Recorder otherSync = new Recorder();
			ServerPlayerEntity owner = arena.join(3, 4, ownerSync);
			ServerPlayerEntity other = arena.join(5, 4, otherSync);
			int before = otherSync.channels.size();
			int ownerBefore = ownerSync.channels.size();

			ReliquaryManager.setStack(owner, SlotKinds.ARTIFACT, 0, new ItemStack(Items.RABBIT_FOOT));
			PlayerStats.set(owner, Stat.VITALITY, 14);
			PlayerDataManager.get(owner).flaskCharges = 0;
			HealingFlaskItem.refill(owner);
			ProgressManager.unlockRealm(owner, ProgressManager.REALM_1);
			ProgressManager.markBossBeaten(owner, ReachBosses.WOODS);

			List<Identifier> toOwner = ownerSync.channels.subList(ownerBefore, ownerSync.channels.size());
			for (Identifier channel : List.of(ReliquaryManager.SYNC_PACKET, ProgressManager.SYNC_PACKET, PlayerDataManager.SYNC_PACKET)) {
				context.assertTrue(toOwner.contains(channel), "owner not sent " + channel + ": " + toOwner);
			}
			List<Identifier> leaked = otherSync.channels.subList(before, otherSync.channels.size()).stream()
					.filter(channel -> channel.getNamespace().equals(Embervoid.MOD_ID)).toList();
			context.assertTrue(leaked.isEmpty(), "another player was sent " + leaked);
			context.assertTrue(ReliquaryManager.get(other).get(SlotKinds.ARTIFACT, 0).isEmpty(), "the other player wears the artifact");
			context.assertTrue(PlayerStats.get(other, Stat.VITALITY) == 10, "the other player's Vitality changed");
			context.assertTrue(!ProgressManager.isRealmUnlocked(other, ProgressManager.REALM_1), "the other player got Realm 1");
			arena.finish();
		});
	}

	/**
	 * A full moon with two players: the cursed one (2 stacks) gets two Ashen Zombies, the other five Ghosts, each
	 * wave after its own player and holding the night on its own. Clearing one wave leaves the night on; the second
	 * ends it, and only the Ghost wave gives a Flask Shard.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "mp_moon", tickLimit = 200)
	public void moonWavesArePerPlayer(TestContext context) {
		BossArena arena = new BossArena(context, null);
		MinecraftServer server = context.getWorld().getServer();
		ServerPlayerEntity[] p = new ServerPlayerEntity[2];
		List<List<MobEntity>> waves = new ArrayList<>();
		Runnable stop = () -> MoonNightManager.forceStop(server);
		moonStep(arena, stop, () -> {
			MoonNightManager.forceStop(server);
			arena.floor(3, 3, 2);
			arena.floor(12, 12, 2);
			p[0] = arena.join(3, 3);
			p[1] = arena.join(12, 12);
			curse(context, p[0], 2);
			curse(context, p[1], 0);
			MoonNightManager.forceStart(server, MoonNight.FULL_MOON);
			context.assertTrue(MoonNights.current(server) == MoonNight.FULL_MOON, "full moon did not start");
			for (ServerPlayerEntity player : p) {
				List<MobEntity> wave = new ArrayList<>();
				for (UUID uuid : NightWaves.waveOf(server, player.getUuid())) {
					if (server.getOverworld().getEntity(uuid) instanceof MobEntity mob) {
						wave.add(mob);
					}
				}
				waves.add(wave);
				context.assertTrue(MoonNights.holds(server).contains(NightWaves.holdId(player.getUuid())), "no hold for a wave");
			}
			context.assertTrue(waves.get(0).size() == 2 && waves.get(0).stream().allMatch(m -> m instanceof AshenZombieEntity),
					"cursed player's wave: " + types(waves.get(0)));
			context.assertTrue(waves.get(1).size() == NightMobConfig.get().ghostCount() && waves.get(1).stream().allMatch(m -> m instanceof GhostEntity),
					"uncursed player's wave: " + types(waves.get(1)));
			for (int i = 0; i < 2; i++) {
				for (MobEntity mob : waves.get(i)) {
					context.assertTrue(mob.getTarget() == p[i], "a wave mob is after " + (mob.getTarget() == null ? "nobody" : mob.getTarget().getName().getString()));
				}
			}
			killNear(waves.get(0), p[0]);
		});
		// The bodies stay for their death animations before they go and the waves count as cleared.
		int zombiesGone = AshenZombieEntity.DEATH_TICKS + 10;
		arena.after(zombiesGone, () -> moonStep(arena, stop, () -> {
			context.assertTrue(!MoonNights.holds(server).contains(NightWaves.holdId(p[0].getUuid())), "cleared wave still holds");
			context.assertTrue(MoonNights.current(server) == MoonNight.FULL_MOON, "the night ended with a wave still out");
			context.assertTrue(p[0].getInventory().count(SanctumItems.FLASK_SHARD) == 0, "a shard for the zombie wave");
			killNear(waves.get(1), p[1]);
		}));
		arena.after(zombiesGone + GhostEntity.DEATH_TICKS + 10, () -> moonStep(arena, stop, () -> {
			context.assertTrue(MoonNights.current(server) == MoonNight.NONE, "night still on after both waves");
			context.assertTrue(p[1].getInventory().count(SanctumItems.FLASK_SHARD) == 1,
					"Ghost wave shards: " + p[1].getInventory().count(SanctumItems.FLASK_SHARD));
			context.assertTrue(p[0].getInventory().count(SanctumItems.FLASK_SHARD) == 0, "the zombie player got the other's shard");
			stop.run();
			arena.finish();
		}));
	}

	// --- helpers -----------------------------------------------------------------------------------------------

	/** A step of a moon test: a failure stops the night before the arena cleans up. */
	private static void moonStep(BossArena arena, Runnable stop, Runnable step) {
		arena.step(() -> {
			try {
				step.run();
			} catch (RuntimeException | Error e) {
				stop.run();
				throw e;
			}
		});
	}

	private static void curse(TestContext context, ServerPlayerEntity player, int stacks) {
		MinecraftServer server = player.getServer();
		server.getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(2).withSilent(), "embervoid moon curse @s " + stacks);
		context.assertTrue(CurseOfTheNight.stacks(player) == stacks, "curse " + CurseOfTheNight.stacks(player) + ", wanted " + stacks);
	}

	/** Kills wave mobs next to their player, where the chunks surely tick (see NightMobTests). */
	private static void killNear(List<MobEntity> wave, ServerPlayerEntity player) {
		for (MobEntity mob : wave) {
			mob.refreshPositionAndAngles(player.getX() + 2, player.getY(), player.getZ() + 2, 0, 0);
			mob.kill();
		}
	}

	private static List<String> types(List<MobEntity> mobs) {
		return mobs.stream().map(m -> m.getType().getUntranslatedName()).toList();
	}

	private static List<String> names(List<ServerPlayerEntity> players) {
		return players.stream().map(p -> p.getUuid().toString().substring(0, 8)).toList();
	}

	private static Glow glow(ServerPlayerEntity player, Identifier realm) {
		return Glow.of(ProgressManager.get(player), realm);
	}

	private static void useGate(ServerWorld realm, BlockPos gate, ServerPlayerEntity player) {
		BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(gate), Direction.NORTH, gate, false);
		RealmBlocks.CITADEL_GATE.getDefaultState().onUse(realm, player, Hand.MAIN_HAND, hit);
	}

	/** The fight's boss with its moves held: only the framework acts. */
	private static MobEntity heldBoss(BossFight fight) {
		MobEntity boss = fight.boss();
		if (!(boss instanceof ReachBoss reachBoss)) {
			throw new GameTestException("no zone boss in the fight: " + boss);
		}
		reachBoss.holdMoves(Integer.MAX_VALUE);
		return boss;
	}

	/** Max HP {@code multiplier} × a lone fighter's boss at the same level, at full health, with {@code adds} Restless Bones. */
	private static void assertScaled(TestContext context, ServerWorld world, BossFight fight, MobEntity boss, double multiplier, int adds) {
		Entity created = fight.spec().entity().create(world);
		if (!(created instanceof MobEntity lone)) {
			throw new GameTestException("cannot create " + fight.spec().entity());
		}
		MobLevels.setLevel(lone, fight.spec().level());
		double expected = lone.getMaxHealth() * multiplier;
		lone.discard();
		context.assertTrue(Math.abs(boss.getMaxHealth() - expected) < 0.01, "max HP " + boss.getMaxHealth() + ", expected " + expected);
		context.assertTrue(boss.getHealth() == boss.getMaxHealth(), "not at full health: " + boss.getHealth());
		List<MobEntity> living = fight.adds();
		context.assertTrue(living.size() == adds && living.stream().allMatch(m -> m.getType() == ReachMobs.RESTLESS_BONES),
				"adds: " + types(living) + ", expected " + adds + " Restless Bones");
	}

	private static String featureAt(ServerWorld realm, BlockPos pos) {
		RealmFeatures.Placed placed = RealmFeatures.at(realm, pos);
		WorldChunk chunk = realm.getWorldChunk(pos);
		return "feature " + (placed == null ? null : placed.structure().kind() + " shrine " + placed.piece().shrinePos().toShortString()
				+ " box " + placed.piece().getBoundingBox()) + ", loaded " + realm.isChunkLoaded(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4))
				+ ", refs " + chunk.getStructureReferences().values() + ", starts here " + chunk.getStructureStarts().size();
	}

	/** The fight, still running; otherwise a failure telling why it ended. */
	private static BossFight running(BossFight fight, ServerPlayerEntity[] players) {
		if (fight.isEnded()) {
			StringBuilder why = new StringBuilder("the fight ended: " + fight + ", boss " + fight.boss());
			for (ServerPlayerEntity player : players) {
				ServerPlayerEntity now = player.getServer().getPlayerManager().getPlayer(player.getUuid());
				why.append("; player ").append(player.getUuid().toString(), 0, 8).append(now == player ? "" : " (replaced)")
						.append(" alive ").append(player.isAlive()).append(" hp ").append(player.getHealth())
						.append(" in ").append(player.getServerWorld().getRegistryKey().getValue())
						.append(" at ").append(player.getBlockPos().toShortString())
						.append(" inside ").append(fight.contains(player.getPos(), 2));
			}
			throw new GameTestException(why.toString());
		}
		return fight;
	}

	/** The outsider is not a fighter and is being pushed away from the arena centre. */
	private static void assertPushedOut(TestContext context, BossFight fight, ServerPlayerEntity outsider, String where) {
		context.assertTrue(!fight.fighters().contains(outsider), "the outsider became a fighter " + where);
		Vec3d away = outsider.getPos().subtract(fight.centerPos()).multiply(1, 0, 1).normalize();
		Vec3d velocity = outsider.getVelocity().multiply(1, 0, 1);
		context.assertTrue(velocity.dotProduct(away) > 0.3, "the outsider " + where + " is not pushed out: velocity " + velocity);
	}

	private static AshRemnantEntity remnantOf(ServerWorld world, ServerPlayerEntity player) {
		List<? extends AshRemnantEntity> remnants = world.getEntitiesByType(ModEntities.ASH_REMNANT, r -> player.getUuid().equals(r.getOwner()));
		if (remnants.size() != 1) {
			throw new GameTestException("expected one remnant of " + player.getUuid() + " in the realm, found " + remnants.size());
		}
		return remnants.get(0);
	}

	/** A spot inside the arena, {@code dx} blocks east and 6 south of its centre, a little above the floor. */
	private static BlockPos inside(RealmFeaturePiece piece, int dx) {
		return piece.center().up(3).add(dx, 0, 6);
	}

	/**
	 * Whether the arena's chunks tick entities, and the shrine's and centre's chunks are fully ticking too: entity
	 * ticking starts before a chunk's ticking future is ready, and until then {@code RealmFeatures.at} sees no arena.
	 */
	private static boolean arenaTicks(ServerWorld realm, RealmFeaturePiece piece) {
		BlockPos c = piece.center();
		int r = piece.radius();
		return realm.isChunkLoaded(ChunkPos.toLong(c.getX() >> 4, c.getZ() >> 4))
				&& realm.isChunkLoaded(ChunkPos.toLong(piece.shrinePos().getX() >> 4, piece.shrinePos().getZ() >> 4))
				&& realm.shouldTickEntity(c.up()) && realm.shouldTickEntity(piece.shrinePos())
				&& realm.shouldTickEntity(c.add(r, 1, 0)) && realm.shouldTickEntity(c.add(-r, 1, 0))
				&& realm.shouldTickEntity(c.add(0, 1, r)) && realm.shouldTickEntity(c.add(0, 1, -r));
	}

	/**
	 * Makes the arena of {@code bossId} at its real spot in the realm, attaches it to its chunks (forced) and places it,
	 * the way worldgen would (the test world does not generate structures), as RematchTests does.
	 */
	private static RealmFeaturePiece placeArena(BossArena arena, ServerWorld realm, Identifier bossId) {
		RealmArenas.Location location = RealmArenas.all(realm).stream()
				.filter(a -> a.spec().bossId().equals(bossId)).findFirst().orElseThrow();
		String name = bossId.equals(ReachBosses.FIELDS) ? "ashen_reach_arena_a" : bossId.equals(ReachBosses.WOODS)
				? "ashen_reach_arena_b" : "ashen_reach_arena_c";
		Registry<Structure> structures = realm.getRegistryManager().get(RegistryKeys.STRUCTURE);
		Structure structure = structures.get(Embervoid.id(name));
		ChunkGenerator generator = realm.getChunkManager().getChunkGenerator();
		ChunkPos chunk = new ChunkPos(location.pos());
		StructureStart start = structure.createStructureStart(realm.getRegistryManager(), generator, generator.getBiomeSource(),
				realm.getChunkManager().getNoiseConfig(), realm.getStructureTemplateManager(), realm.getSeed(), chunk, 0, realm, biome -> true);
		if (!start.hasChildren()) {
			throw new GameTestException("no arena pieces at " + chunk);
		}
		RealmFeaturePiece piece = (RealmFeaturePiece) start.getChildren().get(0);
		BlockBox box = start.getBoundingBox();
		arena.forceChunks(realm, piece.center(), Math.max(box.getBlockCountX(), box.getBlockCountZ()) / 2 + 1);
		realm.getChunk(chunk.x, chunk.z).setStructureStart(structure, start);
		ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4))
				.forEach(c -> {
					WorldChunk part = realm.getChunk(c.x, c.z);
					part.addStructureReference(structure, chunk.toLong());
					start.place(realm, realm.getStructureAccessor(), generator, realm.getRandom(),
							new BlockBox(c.getStartX(), realm.getBottomY(), c.getStartZ(), c.getEndX(), realm.getTopY(), c.getEndZ()), c);
				});
		return piece;
	}

	/** Moves a player (into the realm if needed), as a real client confirming the teleport. */
	private static void moveTo(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		player.onTeleportationDone();
	}

	/**
	 * DESIGN §10.8, PLAN L7: two players at the Fields arena's shrine are both told where it is and whose arena it is;
	 * it is cold for the one who has not beaten the Ash Colossus and lights for them as soon as they have. Leaving the
	 * realm empties the list.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "mp_shrine", tickLimit = PlacedFight.LOAD_LIMIT + 200)
	public void arenaShrineIsColdForNewcomersOnly(TestContext context) {
		BossArena arena = new BossArena(context, null);
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		RealmFeaturePiece[] piece = new RealmFeaturePiece[1];
		arena.step(() -> piece[0] = placeArena(arena, realm, ReachBosses.FIELDS));
		arena.await(() -> arenaTicks(realm, piece[0]), PlacedFight.LOAD_LIMIT, "the arena chunks to tick", () -> {
			BlockPos shrine = piece[0].shrinePos();
			ShrineRecorder veteranSees = new ShrineRecorder();
			ShrineRecorder newcomerSees = new ShrineRecorder();
			ServerPlayerEntity veteran = arena.join(2, 2, veteranSees);
			ServerPlayerEntity newcomer = arena.join(4, 2, newcomerSees);
			ProgressManager.markBossBeaten(veteran, ReachBosses.FIELDS);
			moveTo(veteran, realm, shrine.add(1, 0, 1));
			moveTo(newcomer, realm, shrine.add(2, 0, 1));
			ArenaShrines.update(veteran);
			ArenaShrines.update(newcomer);
			ArenaShrines.Entry expected = new ArenaShrines.Entry(shrine, ReachBosses.FIELDS);
			for (ShrineRecorder seen : List.of(veteranSees, newcomerSees)) {
				context.assertTrue(seen.last != null && seen.last.contains(expected), "not told about the shrine: " + seen.last);
				context.assertTrue(realm.getRegistryKey().getValue().equals(seen.dimension), "wrong dimension: " + seen.dimension);
			}
			context.assertTrue(!ArenaShrines.isCold(veteran, veteranSees.last, shrine), "cold for the veteran");
			context.assertTrue(ArenaShrines.isCold(newcomer, newcomerSees.last, shrine), "lit for the newcomer");
			context.assertTrue(!ArenaShrines.isCold(newcomer, newcomerSees.last, shrine.up(3)), "cold where there is no shrine");
			ProgressManager.markBossBeaten(newcomer, ReachBosses.FIELDS);
			context.assertTrue(!ArenaShrines.isCold(newcomer, newcomerSees.last, shrine), "still cold after the kill");

			moveTo(newcomer, context.getWorld(), context.getAbsolutePos(new BlockPos(4, 1, 2)));
			ArenaShrines.update(newcomer);
			context.assertTrue(newcomerSees.last.isEmpty(), "shrines sent outside the realm: " + newcomerSees.last);
			arena.finish();
		});
	}

	/** A connection that keeps the last arena shrine list sent to its player. */
	static final class ShrineRecorder extends ClientConnection {
		@Nullable List<ArenaShrines.Entry> last;
		@Nullable Identifier dimension;

		ShrineRecorder() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {
			send(packet, null);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof CustomPayloadS2CPacket payload && payload.getChannel().equals(ArenaShrines.PACKET)) {
				PacketByteBuf buf = new PacketByteBuf(payload.getData().copy());
				dimension = buf.readIdentifier();
				List<ArenaShrines.Entry> entries = new ArrayList<>();
				for (int i = buf.readVarInt(); i > 0; i--) {
					entries.add(new ArenaShrines.Entry(buf.readBlockPos(), buf.readIdentifier()));
				}
				last = entries;
			}
		}
	}

	/** A connection that records the channel of every custom packet sent to it instead of sending anything. */
	static final class Recorder extends ClientConnection {
		final List<Identifier> channels = new ArrayList<>();

		Recorder() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {
			send(packet, null);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof CustomPayloadS2CPacket payload) {
				channels.add(payload.getChannel());
			}
		}

		int count(Identifier channel) {
			return (int) channels.stream().filter(channel::equals).count();
		}
	}
}
