package com.embervoid.gametest;

import com.embervoid.reach.gear.GearArmor;
import com.embervoid.reach.mob.AshStalkerEntity;
import com.embervoid.reach.mob.AshenCarrionEntity;
import com.embervoid.reach.mob.CinderHuskEntity;
import com.embervoid.reach.mob.ReachMobs;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * Ashen Fields mobs: Cinder Husk, Ash Stalker, Ashen Carrion. Tests where a mob reacts to players
 * or chases a target run in a batch of their own, so no other test's player is around.
 */
public final class ReachFieldsTests implements FabricGameTest {
	/** With its AI on, a Cinder Husk walks up to its target and the hit lands no sooner than 10 ticks after the swing starts. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 120, batchId = "reach_husk_chase")
	public void cinderHuskChasesAndHitsAfterTheWindup(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 2, 6);
		CinderHuskEntity husk = ReachTests.mob(context, ReachMobs.CINDER_HUSK, 2, 0, 2);
		husk.setTarget(target);
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
					ReachTests.finish(context);
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
		}));
		context.waitAndRun(12, ReachTests.guard(context, () -> context.assertTrue(stalker.isBoiling(),
				"a player 3 blocks away did not set it off")));
		context.waitAndRun(29, ReachTests.guard(context, () -> {
			context.assertTrue(stalker.isBuried(), "out before the 20-tick boil ended");
			context.assertFalse(stalker.damage(context.getWorld().getDamageSources().playerAttack(player), 5), "hit while boiling");
		}));
		context.waitAndRun(33, ReachTests.guard(context, () -> {
			context.assertFalse(stalker.isBuried() || stalker.isInvisible(), "still in the ground after the boil");
			context.assertTrue(stalker.getTarget() == player, "it did not go for the player: " + stalker.getTarget());
			ReachTests.finish(context);
		}));
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 80, batchId = "reach_stalker_immune")
	public void scarecrowMaskKeepsTheStalkerBuried(TestContext context) {
		ServerPlayerEntity player = ReachTests.player(context, 2, 4);
		player.equipStack(EquipmentSlot.HEAD, new ItemStack(GearArmor.SCARECROW_MASK));
		AshStalkerEntity stalker = ReachTests.mob(context, ReachMobs.ASH_STALKER, 2, 0, 2);
		stalker.bury();
		context.waitAndRun(40, ReachTests.guard(context, () -> {
			context.assertTrue(stalker.isBuried() && !stalker.isBoiling(), "the stalker ambushed a masked player");
			ReachTests.finish(context);
		}));
	}

	/** Out of the ground with nothing to hunt, an Ash Stalker digs back in after 10 s. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 260, batchId = "reach_stalker_rebury")
	public void ashStalkerReburiesWhenAlone(TestContext context) {
		// Its own batch (another test's player could set it off again) and no goals (nothing becomes its target).
		AshStalkerEntity stalker = ReachTests.still(context, ReachMobs.ASH_STALKER, 2, 0, 2);
		context.waitAndRun(150, () -> context.assertFalse(stalker.isBuried(), "buried too early"));
		context.waitAndRun(215, () -> {
			context.assertTrue(stalker.isBuried(), "still out after 10 s alone; target " + stalker.getTarget()
					+ ", on ground " + stalker.isOnGround());
			ReachTests.finish(context);
		});
	}

	/** A carrion over a target at full health: the first shriek comes 8–12 s in, the dive 15 ticks later, then the hit. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 340, batchId = "reach_carrion_schedule")
	public void carrionDivesEveryEightToTwelveSeconds(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 4, 4);
		AshenCarrionEntity carrion = ReachTests.still(context, ReachMobs.ASHEN_CARRION, 4, 7, 4);
		carrion.setTarget(target);
		int[] shriek = {-1};
		int[] dive = {-1};
		for (int tick = 1; tick <= 320; tick++) {
			int now = tick;
			context.waitAndRun(tick, () -> {
				if (shriek[0] < 0 && carrion.isShrieking()) {
					shriek[0] = now;
					context.assertTrue(now >= AshenCarrionEntity.DIVE_MIN_TICKS - 5, "first shriek after only " + now + " ticks");
				}
				if (dive[0] < 0 && carrion.isDiving()) {
					dive[0] = now;
					context.assertTrue(shriek[0] > 0 && now - shriek[0] >= AshenCarrionEntity.SHRIEK_TICKS - 1,
							"dived " + (now - shriek[0]) + " ticks after the shriek");
				}
				if (target.getHealth() < target.getMaxHealth()) {
					context.assertTrue(dive[0] > 0, "hit without a dive");
					ReachTests.finish(context);
				}
			});
		}
	}

	/** A target below half health draws a dive at once. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 60, batchId = "reach_carrion_wounded")
	public void carrionDivesAtAWoundedTarget(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 4, 4);
		target.setHealth(target.getMaxHealth() * 0.4f);
		AshenCarrionEntity carrion = ReachTests.still(context, ReachMobs.ASHEN_CARRION, 4, 7, 4);
		carrion.setTarget(target);
		context.waitAndRun(10, () -> {
			context.assertTrue(carrion.isShrieking(), "no shriek at a target under half health");
			ReachTests.finish(context);
		});
	}

	/** Without a target it comes down and perches; a target makes it take off again. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 240, batchId = "reach_carrion_perch")
	public void carrionPerchesAndTakesOff(TestContext context) {
		AshenCarrionEntity carrion = ReachTests.still(context, ReachMobs.ASHEN_CARRION, 4, 4, 4);
		carrion.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE).setBaseValue(1);
		context.waitAndRun(60, () -> context.assertFalse(carrion.isPerched(), "perched while it should still circle"));
		double[] perchY = {0};
		context.waitAndRun(200, () -> {
			context.assertTrue(carrion.isPerched(), "not perched after 10 s without a target");
			perchY[0] = carrion.getY();
			carrion.setTarget(ReachTests.dummy(context, 1, 1));
		});
		context.waitAndRun(215, () -> {
			context.assertFalse(carrion.isPerched(), "still perched with a target");
			context.assertTrue(carrion.getY() > perchY[0] + 0.5, "did not take off: y " + carrion.getY());
			ReachTests.finish(context);
		});
	}
}
