package com.embervoid.gametest;

import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.ArenaSpec;
import com.embervoid.boss.BossEntities;
import com.embervoid.boss.BossFight;
import com.embervoid.progress.PlayerProgress;
import com.embervoid.progress.ProgressManager;
import com.embervoid.reach.boss.ReachBosses;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.realm.Realms;
import com.embervoid.realm.SealedGateBlock;
import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.sanctum.SanctumItems;
import com.embervoid.sanctum.VoidObeliskBlock.Glow;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * The whole Realm 1 loop as progress sees it (PLAN 13.5): unlocking Realm 1 (the first hub visit) lights its obelisk; the three zone
 * bosses open the Sealed Gate; the Smoldering Lord, beaten in the depths, completes the realm (its obelisk kindled)
 * and lights the Realm 2 obelisk, which leads nowhere yet. The framework's test boss stands in for every boss (rewards
 * and completion follow the progress id and the world); each boss's own fight has its own tests.
 */
public final class RealmLoopTests implements FabricGameTest {
	private static final int RADIUS = 6;
	private static final Identifier REALM_2 = ProgressManager.nextRealm(ProgressManager.REALM_1);
	/** A spot in the depths away from the landing, the Citadel and its courtyard. */
	private static final BlockPos DEPTHS_CENTER = new BlockPos(240, 160, 240);

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "realm_loop", tickLimit = 1200)
	public void realmOneLoopLightsTheNextObelisk(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			context.assertTrue(REALM_2 != null, "no realm after Realm 1");
			arena.floor(4, 4, RADIUS + 1);
			ServerPlayerEntity player = arena.join(4, 6);

			context.assertTrue(glow(player, ProgressManager.REALM_1) == Glow.DARK, "Realm 1 obelisk lit before the hub");
			ProgressManager.unlockRealm(player, ProgressManager.REALM_1);
			context.assertTrue(glow(player, ProgressManager.REALM_1) == Glow.LIT, "Realm 1 obelisk not lit when unlocked");
			context.assertTrue(glow(player, REALM_2) == Glow.DARK, "Realm 2 obelisk lit too early");
			context.assertTrue(SealedGateBlock.remainingZoneBosses(player, Realms.ASHEN_REACH) == 3, "gate not sealed by three bosses");

			// The three zone bosses, in any order: each leaves its soul and a Flask Shard and unseals one more lock.
			for (Identifier boss : Realms.ASHEN_REACH.zoneBosses()) {
				BossRewardTests.kill(arena.start(boss, BossEntities.TEST_BOSS, 4, 4, RADIUS, 1));
				context.assertTrue(ProgressManager.hasBeatenBoss(player, boss), "zone boss not beaten: " + boss);
			}
			context.assertTrue(SealedGateBlock.remainingZoneBosses(player, Realms.ASHEN_REACH) == 0, "gate still sealed");
			context.assertTrue(player.getInventory().count(SanctumItems.FLASK_SHARD) == 3, "zone boss Flask Shards: "
					+ player.getInventory().count(SanctumItems.FLASK_SHARD));
			context.assertTrue(!ProgressManager.isRealmCompleted(player, ProgressManager.REALM_1), "completed before the Lord");

			// Through the gate: the Lord's fight in the depths.
			ServerWorld depths = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.depths());
			context.assertTrue(depths != null, "no depths dimension in the test world");
			for (int dx = -RADIUS - 1; dx <= RADIUS + 1; dx++) {
				for (int dz = -RADIUS - 1; dz <= RADIUS + 1; dz++) {
					depths.setBlockState(DEPTHS_CENTER.add(dx, 0, dz), Blocks.STONE.getDefaultState());
				}
			}
			arena.forceChunks(depths, DEPTHS_CENTER, RADIUS + 16);
			player.teleport(depths, DEPTHS_CENTER.getX() + 0.5, DEPTHS_CENTER.getY() + 1, DEPTHS_CENTER.getZ() + 2.5, 0, 0);

			arena.await(() -> depths.shouldTickEntity(DEPTHS_CENTER), PlacedFight.LOAD_LIMIT, "the depths arena to tick", () -> {
				BossFight lord = ArenaManager.start(depths, new ArenaSpec(ReachBosses.CITADEL, BossEntities.TEST_BOSS, 1, RADIUS),
						DEPTHS_CENTER);
				context.assertTrue(lord != null, "the Lord's fight did not start in the depths");
				arena.track(lord);
				BossRewardTests.kill(lord);

				context.assertTrue(ProgressManager.isRealmCompleted(player, ProgressManager.REALM_1), "Realm 1 not completed");
				context.assertTrue(glow(player, ProgressManager.REALM_1) == Glow.KINDLED, "Realm 1 obelisk not kindled");
				context.assertTrue(glow(player, REALM_2) == Glow.LIT, "Realm 2 obelisk not lit");
				context.assertTrue(player.getInventory().count(GearItems.SMOLDERING_LORD_SOUL) == 1, "no Lord soul");
				context.assertTrue(player.getInventory().count(GearItems.FLASK_EMBER) == 1, "no Flask Ember");

				// Realm 2 is lit but not built: the obelisk keeps the player where they are.
				context.assertTrue(!RealmTeleports.enterRealm(player, REALM_2, false), "entered a realm that does not exist");
				context.assertTrue(player.getServerWorld() == depths, "moved by an obelisk that leads nowhere");
				arena.finish();
			});
		});
	}

	private static Glow glow(ServerPlayerEntity player, Identifier realm) {
		PlayerProgress progress = ProgressManager.get(player);
		return Glow.of(progress, realm);
	}
}
