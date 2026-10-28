package com.embervoid.gametest;

import com.embervoid.reach.mob.CinderHuskEntity;
import com.embervoid.reach.mob.ReachMob;
import com.embervoid.reach.mob.ReachMobs;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * The hit, stagger and death animations of the Realm 1 mobs (on the Cinder Husk for all of
 * {@link ReachMob}): a hit during a windup leaves the windup running and shows no flinch (the tell stays
 * readable), a hit between moves shows one with a cooldown, and a kill keeps the body for its death animation
 * ({@link ReachMob#deathTicks}) and then removes it. The AI is off; moves are started by the test.
 */
public final class MobAnimationTests implements FabricGameTest {
	/** A hit two ticks into the swing's windup: the windup goes on unchanged and no flinch plays; one after it does, with a cooldown. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void hurtNeverCancelsAWindup(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 2, 3);
		CinderHuskEntity husk = ReachTests.still(context, ReachMobs.CINDER_HUSK, 2, 1, 2);
		husk.setAiDisabled(true);
		husk.setTarget(target);
		context.assertTrue(husk.startMove(husk.meleeMove()), "the swing did not start");
		context.waitAndRun(2, () -> {
			hit(husk);
			context.assertTrue(husk.isWindingUp(), "the hit cancelled the windup");
			context.assertTrue(husk.moveTicksLeft() == CinderHuskEntity.SWING_WINDUP - 2, "the windup changed: " + husk.moveTicksLeft() + " ticks left");
			context.assertTrue(husk.hurtFlinches() == 0, "a flinch played over the windup");
		});
		context.waitAndRun(CinderHuskEntity.SWING_WINDUP + 3, () -> {
			context.assertFalse(husk.isWindingUp(), "the swing never landed");
			context.assertTrue(target.getHealth() < target.getMaxHealth(), "the swing did not hit after the flinch-free windup");
			hit(husk);
			context.assertTrue(husk.hurtFlinches() == 1, "no flinch between moves (" + husk.hurtFlinches() + ")");
			context.waitAndRun(2, () -> {
				hit(husk);
				context.assertTrue(husk.hurtFlinches() == 1, "a second flinch inside the cooldown");
				context.waitAndRun(ReachMob.HURT_COOLDOWN, () -> {
					hit(husk);
					context.assertTrue(husk.hurtFlinches() == 2, "no flinch after the cooldown");
					ReachTests.finish(context);
				});
			});
		});
	}

	/** Killed, the Husk's body lies there for its death animation, then goes; a windup running at the death is dropped. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void deathAnimationDelaysRemoval(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 2, 3);
		CinderHuskEntity husk = ReachTests.still(context, ReachMobs.CINDER_HUSK, 2, 1, 2);
		husk.setAiDisabled(true);
		husk.setTarget(target);
		int deathTicks = husk.deathTicks();
		context.assertTrue(deathTicks >= 30, "death animation of " + deathTicks + " ticks is too short to see");
		husk.startMove(husk.meleeMove());
		context.waitAndRun(2, () -> {
			husk.kill();
			context.assertTrue(husk.isDead() && !husk.isRemoved(), "the body went at once");
			context.assertFalse(husk.isWindingUp(), "a windup survived the death");
		});
		context.waitAndRun(2 + deathTicks / 2, () -> {
			context.assertTrue(!husk.isRemoved(), "the body went before its death animation");
			context.assertTrue(target.getHealth() == target.getMaxHealth(), "the cancelled swing landed after the death");
		});
		context.waitAndRun(2 + deathTicks + 10, () -> {
			context.assertTrue(husk.isRemoved(), "the body is still there after " + (deathTicks + 10) + " ticks");
			ReachTests.finish(context);
		});
	}

	/** Hits the mob for 1 (magic, outside the hurt cooldown). */
	private static void hit(ReachMob mob) {
		mob.timeUntilRegen = 0;
		mob.hurtTime = 0;
		mob.damage(mob.getDamageSources().magic(), 1);
	}
}
