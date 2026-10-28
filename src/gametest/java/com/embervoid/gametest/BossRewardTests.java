package com.embervoid.gametest;

import com.embervoid.boss.BossEntities;
import com.embervoid.boss.BossFight;
import com.embervoid.boss.BossItems;
import com.embervoid.progress.ProgressManager;
import com.embervoid.reach.boss.ReachBosses;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.sanctum.SanctumItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * Souls and Flask Shards: a Realm 1 zone boss's first kill gives its soul and a Flask Shard, personal, no
 * Ember Heart; a repeat kill gives the soul at {@code boss.repeatSoulChance}. The framework's test boss stands in
 * for the boss (the reward follows the progress id).
 */
public final class BossRewardTests implements FabricGameTest {
	private static final int RADIUS = 6;

	/** First kill: the Crypt Keeper's soul and one Flask Shard straight into the inventory; the boss counts as beaten. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "reward_first", tickLimit = 100)
	public void firstKillGivesSoulAndShard(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			ServerPlayerEntity player = arena.join(4, 6);
			kill(arena.start(ReachBosses.NECROPOLIS, BossEntities.TEST_BOSS, 4, 4, RADIUS, 1));
			context.assertTrue(ProgressManager.hasBeatenBoss(player, ReachBosses.NECROPOLIS), "not beaten");
			context.assertTrue(player.getInventory().count(GearItems.CRYPT_KEEPER_SOUL) == 1, "no soul");
			context.assertTrue(player.getInventory().count(SanctumItems.FLASK_SHARD) == 1, "shards " + player.getInventory().count(SanctumItems.FLASK_SHARD));
			context.assertTrue(player.getInventory().count(BossItems.EMBER_HEART) == 0, "an Ember Heart for a Realm 1 boss");
			arena.finish();
		});
	}

	/**
	 * A veteran helping a newcomer: the newcomer gets soul and shard; the veteran the soul only, at the repeat chance
	 * (1 here), and nothing at chance 0.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "reward_repeat", tickLimit = 100)
	public void repeatKillGivesTheSoulAtItsChance(TestContext context) {
		BossArena always = new BossArena(context, "{\"boss\": {\"repeatSoulChance\": 1}}");
		always.step(() -> {
			always.floor(4, 4, RADIUS + 1);
			ServerPlayerEntity veteran = always.join(3, 6);
			ServerPlayerEntity newcomer = always.join(5, 6);
			ProgressManager.markBossBeaten(veteran, ReachBosses.FIELDS);
			kill(always.start(ReachBosses.FIELDS, BossEntities.TEST_BOSS, 4, 4, RADIUS, 1));
			context.assertTrue(newcomer.getInventory().count(GearItems.ASH_COLOSSUS_SOUL) == 1, "newcomer: no soul");
			context.assertTrue(newcomer.getInventory().count(SanctumItems.FLASK_SHARD) == 1, "newcomer: no shard");
			context.assertTrue(veteran.getInventory().count(GearItems.ASH_COLOSSUS_SOUL) == 1, "veteran: no soul at chance 1");
			context.assertTrue(veteran.getInventory().count(SanctumItems.FLASK_SHARD) == 0, "veteran: a second shard");
			always.finish();
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "reward_repeat_never", tickLimit = 100)
	public void repeatKillAtChanceZeroGivesNoSoul(TestContext context) {
		BossArena never = new BossArena(context, "{\"boss\": {\"repeatSoulChance\": 0}}");
		never.step(() -> {
			never.floor(4, 4, RADIUS + 1);
			ServerPlayerEntity veteran = never.join(4, 6);
			ProgressManager.markBossBeaten(veteran, ReachBosses.WOODS);
			kill(never.start(ReachBosses.WOODS, BossEntities.TEST_BOSS, 4, 4, RADIUS, 1));
			context.assertTrue(veteran.getInventory().count(GearItems.GRIEVING_TREE_SOUL) == 0, "a soul at chance 0");
			context.assertTrue(veteran.getInventory().count(SanctumItems.FLASK_SHARD) == 0, "a shard on a repeat kill");
			never.finish();
		});
	}

	/** The Smoldering Lord's first kill: his soul and a Flask Ember, no Flask Shard. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "reward_lord", tickLimit = 100)
	public void lordGivesSoulAndFlaskEmber(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			ServerPlayerEntity player = arena.join(4, 6);
			kill(arena.start(ReachBosses.CITADEL, BossEntities.TEST_BOSS, 4, 4, RADIUS, 1));
			context.assertTrue(player.getInventory().count(GearItems.SMOLDERING_LORD_SOUL) == 1, "no soul");
			context.assertTrue(player.getInventory().count(GearItems.FLASK_EMBER) == 1, "no Flask Ember");
			context.assertTrue(player.getInventory().count(SanctumItems.FLASK_SHARD) == 0, "a Flask Shard from the Lord");
			arena.finish();
		});
	}

	static void kill(BossFight fight) {
		MobEntity boss = fight.boss();
		boss.setAiDisabled(true);
		boss.kill();
		if (!fight.isEnded()) {
			throw new net.minecraft.test.GameTestException("the kill did not end the fight");
		}
	}
}
