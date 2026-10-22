package com.embervoid.gametest;

import com.embervoid.reach.gear.GearArmor;
import com.embervoid.reach.mob.AshStalkerEntity;
import com.embervoid.reach.mob.CinderHuskEntity;
import com.embervoid.reach.mob.ReachMobs;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * Ashen Fields mobs (PLAN 10.2, 10.3): Cinder Husk, Ash Stalker, Ashen Carrion. Tests where a mob reacts to players
 * or chases a target run in a batch of their own, so no other test's player is around.
 */
public final class ReachFieldsTests implements FabricGameTest {
	/** With its AI on, a Cinder Husk walks up to its target and the hit lands no sooner than 10 ticks after the swing starts. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 120, batchId = "reach_husk_chase")
	public void cinderHuskChasesAndHitsAfterTheWindup(TestContext context) {
		IronGolemEntity target = ReachMobTests.dummy(context, 2, 6);
		CinderHuskEntity husk = ReachTests.mob(context, ReachMobs.CINDER_HUSK, 2, 0, 2);
		husk.setTarget(target);
		// Its own batch: no other test's player around to draw it off.
		int[] windupStart = {-1};
		for (int tick = 1; tick <= 100; tick++) {
			int now = tick;
			context.waitAndRun(tick, () -> {
				if (windupStart[0] < 0 && husk.isWindingUp()) {
					windupStart[0] = now;
				}
				if (target.getHealth() < target.getMaxHealth()) {
					context.assertTrue(windupStart[0] > 0, "hit without a windup");
					// The windup is seen the tick after it starts (entities tick after the test's tasks): 10 - 1.
					context.assertTrue(now - windupStart[0] >= 9, "hit " + (now - windupStart[0]) + " ticks after the windup started");
					context.complete();
				}
			});
		}
	}

	/**
	 * A buried Ash Stalker is invisible and takes no damage; a player 7 blocks away does not set it off, one within
	 * 5 does: it boils for 20 ticks (still invulnerable), then comes out with the player as its target.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 120, batchId = "reach_stalker_ambush")
	public void ashStalkerAmbushesAPlayerWithinFive(TestContext context) {
		ServerPlayerEntity player = ReachTests.player(context, 2, 9);
		AshStalkerEntity stalker = ReachTests.mob(context, ReachMobs.ASH_STALKER, 2, 0, 2);
		stalker.bury();
		context.waitAndRun(10, ReachTests.guard(context, () -> {
			context.assertTrue(stalker.isBuried() && !stalker.isBoiling() && stalker.isInvisible(), "not lying buried");
			context.assertFalse(stalker.damage(context.getWorld().getDamageSources().playerAttack(player), 5), "a buried stalker took a hit");
			context.assertTrue(stalker.getHealth() == stalker.getMaxHealth(), "buried stalker hurt");
			ReachTests.teleport(context, player, 2.5, 1, 5.5);
		}, player));
		context.waitAndRun(12, ReachTests.guard(context, () -> context.assertTrue(stalker.isBoiling(),
				"a player 3 blocks away did not set it off"), player));
		context.waitAndRun(29, ReachTests.guard(context, () -> {
			context.assertTrue(stalker.isBuried(), "out before the 20-tick boil ended");
			context.assertFalse(stalker.damage(context.getWorld().getDamageSources().playerAttack(player), 5), "hit while boiling");
		}, player));
		context.waitAndRun(33, ReachTests.guard(context, () -> {
			context.assertFalse(stalker.isBuried() || stalker.isInvisible(), "still in the ground after the boil");
			context.assertTrue(stalker.getTarget() == player, "it did not go for the player: " + stalker.getTarget());
			TestPlayers.complete(context, player);
		}, player));
	}

	/** A player wearing the Scarecrow's Mask walks right past a buried Ash Stalker. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 80, batchId = "reach_stalker_immune")
	public void scarecrowMaskKeepsTheStalkerBuried(TestContext context) {
		ServerPlayerEntity player = ReachTests.player(context, 2, 4);
		player.equipStack(EquipmentSlot.HEAD, new ItemStack(GearArmor.SCARECROW_MASK));
		AshStalkerEntity stalker = ReachTests.mob(context, ReachMobs.ASH_STALKER, 2, 0, 2);
		stalker.bury();
		context.waitAndRun(40, ReachTests.guard(context, () -> {
			context.assertTrue(stalker.isBuried() && !stalker.isBoiling(), "the stalker ambushed a masked player");
			TestPlayers.complete(context, player);
		}, player));
	}

	/** Out of the ground with nothing to hunt, an Ash Stalker digs back in after 10 s. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 260, batchId = "reach_stalker_rebury")
	public void ashStalkerReburiesWhenAlone(TestContext context) {
		// Its own batch (another test's player could set it off again) and no goals (nothing becomes its target).
		AshStalkerEntity stalker = context.spawnMob(ReachMobs.ASH_STALKER, 2, 0, 2);
		context.waitAndRun(150, () -> context.assertFalse(stalker.isBuried(), "buried too early"));
		context.waitAndRun(215, () -> {
			context.assertTrue(stalker.isBuried(), "still out after 10 s alone; target " + stalker.getTarget()
					+ ", on ground " + stalker.isOnGround());
			context.complete();
		});
	}
}
