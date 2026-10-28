package com.embervoid.gametest;

import com.embervoid.reach.mob.CinderHoundEntity;
import com.embervoid.reach.mob.FurnaceGolemEntity;
import com.embervoid.reach.mob.ReachMobs;
import com.embervoid.reach.mob.SmolderingKnightEntity;
import com.embervoid.reach.mob.SparkingMarksmanEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Smoldering Citadel mobs: Cinder Hound, Sparking Marksman, Furnace Golem, Smoldering Knight. */
public final class ReachCitadelTests implements FabricGameTest {
	/** A hound that finds a target howls: idle hounds within 24 take the same target, one 30 blocks off does not. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "reach_hound_howl")
	public void houndHowlPullsThePack(TestContext context) {
		CowEntity prey = ReachTests.dummy(context, 8, 8);
		CinderHoundEntity caller = ReachTests.still(context, ReachMobs.CINDER_HOUND, 2, 0, 2);
		CinderHoundEntity near = ReachTests.still(context, ReachMobs.CINDER_HOUND, 5, 0, 2);
		CinderHoundEntity close = ReachTests.still(context, ReachMobs.CINDER_HOUND, 2, 0, 6);
		CinderHoundEntity far = ReachTests.still(context, ReachMobs.CINDER_HOUND, 34, 0, 2);
		caller.setTarget(prey);
		context.assertTrue(near.getTarget() == prey && close.getTarget() == prey, "the pack did not answer the howl");
		context.assertTrue(far.getTarget() == null, "a hound 32 blocks off answered");
		ReachTests.finish(context);
	}

	/** The bite lands 6 ticks after the lunge starts and sets the target alight. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void houndBiteSetsAlight(TestContext context) {
		CowEntity prey = ReachTests.dummy(context, 2, 3);
		CinderHoundEntity hound = ReachTests.still(context, ReachMobs.CINDER_HOUND, 2, 0, 2);
		hound.setAiDisabled(true);
		hound.setTarget(prey);
		context.assertTrue(hound.startMove(hound.meleeMove()), "the bite did not start");
		context.waitAndRun(5, () -> context.assertTrue(prey.getHealth() == prey.getMaxHealth(), "bit early"));
		context.waitAndRun(8, () -> {
			context.assertTrue(prey.getHealth() < prey.getMaxHealth(), "no bite");
			context.assertTrue(prey.isOnFire(), "the bite did not set it alight");
			ReachTests.finish(context);
		});
	}

	/** The spark shows for 20 ticks, then a burning bolt flies and hits. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 80)
	public void marksmanSparksThenFiresABurningBolt(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 2, 12);
		SparkingMarksmanEntity marksman = ReachTests.still(context, ReachMobs.SPARKING_MARKSMAN, 2, 0, 2);
		marksman.setAiDisabled(true);
		marksman.setTarget(target);
		context.assertTrue(marksman.startAim(), "the aim did not start");
		context.waitAndRun(SparkingMarksmanEntity.AIM_TICKS - 2, () -> {
			context.assertTrue(marksman.showsBone("spark"), "no spark during the aim");
			context.assertTrue(bolts(context, marksman).isEmpty(), "a bolt before the spark burnt down");
		});
		context.waitAndRun(SparkingMarksmanEntity.AIM_TICKS + 1, () -> {
			List<ArrowEntity> bolts = bolts(context, marksman);
			context.assertTrue(bolts.size() == 1 && bolts.get(0).isOnFire(), "no burning bolt: " + bolts);
			context.assertFalse(marksman.showsBone("spark"), "spark still lit");
		});
		for (int tick = 25; tick <= 70; tick++) {
			context.waitAndRun(tick, () -> {
				if (target.getHealth() < target.getMaxHealth()) {
					bolts(context, marksman).forEach(ArrowEntity::discard);
					ReachTests.finish(context);
				}
			});
		}
	}

	private static List<ArrowEntity> bolts(TestContext context, SparkingMarksmanEntity marksman) {
		return context.getWorld().getEntitiesByClass(ArrowEntity.class, marksman.getBoundingBox().expand(24),
				arrow -> arrow.getOwner() == marksman);
	}

	/** A placed marksman with a target keeps its spot: no walking, no knockback. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 120, batchId = "reach_marksman_post")
	public void marksmanNeverLeavesItsPost(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 2, 8);
		SparkingMarksmanEntity marksman = ReachTests.mob(context, ReachMobs.SPARKING_MARKSMAN, 2, 0, 2);
		marksman.markPlaced();
		marksman.setTarget(target);
		Vec3d post = marksman.getPos();
		context.waitAndRun(5, () -> marksman.takeKnockback(2, 0, 1));
		context.waitAndRun(100, () -> {
			context.assertTrue(marksman.getPos().multiply(1, 0, 1).distanceTo(post.multiply(1, 0, 1)) < 0.2,
					"the marksman left its post: " + post + " -> " + marksman.getPos());
			bolts(context, marksman).forEach(ArrowEntity::discard);
			ReachTests.finish(context);
		});
	}

	/** Three hits only stoke it; the fourth makes it glow, and 25 ticks later a fire wave hits within 4, not at 7. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 60)
	public void furnaceGolemBlastsAfterFourHits(TestContext context) {
		FurnaceGolemEntity golem = ReachTests.still(context, ReachMobs.FURNACE_GOLEM, 4, 0, 4);
		golem.setAiDisabled(true);
		CowEntity near = ReachTests.dummy(context, 4, 7);
		CowEntity far = ReachTests.dummy(context, 4, 11);
		var sources = context.getWorld().getDamageSources();
		for (int hit = 1; hit <= 3; hit++) {
			golem.timeUntilRegen = 0;
			golem.damage(sources.generic(), 1);
		}
		context.assertTrue(golem.heat() == 3 && !golem.isHeating(), "heating after 3 hits (heat " + golem.heat() + ")");
		golem.timeUntilRegen = 0;
		golem.damage(sources.generic(), 1);
		context.assertTrue(golem.isHeating() && golem.showsBone("heat"), "not glowing after the 4th hit");
		context.waitAndRun(FurnaceGolemEntity.HEAT_TICKS - 2, () ->
				context.assertTrue(near.getHealth() == near.getMaxHealth(), "the wave came early"));
		context.waitAndRun(FurnaceGolemEntity.HEAT_TICKS + 2, () -> {
			context.assertTrue(near.getHealth() < near.getMaxHealth() && near.isOnFire(), "the wave missed the cow 3 blocks off");
			context.assertTrue(far.getHealth() == far.getMaxHealth(), "the wave reached the cow 7 blocks off");
			context.assertTrue(golem.heat() == 0 && !golem.isHeating(), "not cold again after the wave");
			ReachTests.finish(context);
		});
	}

	/** The combo's three blows all land, inside the target's hurt cooldown, within 19 ticks. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 60)
	public void knightComboLandsThreeBlows(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 2, 3);
		SmolderingKnightEntity knight = ReachTests.still(context, ReachMobs.SMOLDERING_KNIGHT, 2, 0, 2);
		knight.setAiDisabled(true);
		knight.setTarget(target);
		context.assertTrue(knight.startMove(knight.meleeMove()), "the combo did not start");
		float[] last = {target.getHealth()};
		int[] blows = {0};
		for (int tick = 1; tick <= 30; tick++) {
			context.waitAndRun(tick, () -> {
				if (target.getHealth() < last[0]) {
					blows[0]++;
					last[0] = target.getHealth();
				}
			});
		}
		context.waitAndRun(31, () -> {
			context.assertTrue(blows[0] == 3, "the combo landed " + blows[0] + " blows");
			ReachTests.finish(context);
		});
	}

	/**
	 * Crouched for 15 ticks (not moving), then it dashes and hits a target 6 blocks off. Its own batch: the dash must
	 * not run into another test's mobs.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 60, batchId = "reach_knight_lunge")
	public void knightLungesAfterACrouch(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 2, 8);
		SmolderingKnightEntity knight = ReachTests.still(context, ReachMobs.SMOLDERING_KNIGHT, 2, 0, 2);
		knight.setTarget(target);
		Vec3d start = knight.getPos();
		context.assertTrue(knight.startLunge(), "the lunge did not start");
		context.waitAndRun(SmolderingKnightEntity.CROUCH_TICKS - 1, () ->
				context.assertTrue(knight.getPos().distanceTo(start) < 0.3, "moved during the crouch"));
		context.waitAndRun(SmolderingKnightEntity.CROUCH_TICKS + 12, () -> {
			context.assertTrue(knight.getPos().distanceTo(start) > 3, "no dash: moved " + knight.getPos().distanceTo(start));
			context.assertTrue(target.getHealth() < target.getMaxHealth(), "the lunge did not hit");
			ReachTests.finish(context);
		});
	}
}
