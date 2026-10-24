package com.embervoid.gametest;

import com.embervoid.entity.mob.AshGhoulEntity;
import com.embervoid.entity.mob.MobAnimations;
import com.embervoid.entity.mob.MobEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * The hit and death animations of the overworld mobs (PLAN 19.3, track overworld-mobs): a kill keeps the body for its
 * death animation and then removes it, and a flinch plays once per cooldown. Written and compiled here; the coordinator
 * runs them.
 */
public final class OverworldMobAnimationTests implements FabricGameTest {
	/** Killed, the Ghoul bursts into its blinding cloud at once; its body lies there for its death animation, then goes. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "overworld_anim_death", tickLimit = 200)
	public void ghoulBodyStaysForItsDeathAnimation(TestContext context) {
		floor(context, 2, 2);
		AshGhoulEntity ghoul = context.spawnEntity(MobEntities.ASH_GHOUL, 2, 1, 2);
		ghoul.setAiDisabled(true);
		int deathTicks = ghoul.deathTicks();
		context.assertTrue(deathTicks >= 30, "death animation of " + deathTicks + " ticks is too short to see");
		context.runAtTick(5, () -> {
			ghoul.kill();
			context.assertTrue(ghoul.isDead() && !ghoul.isRemoved(), "the body went at once");
			context.runAtTick(5 + deathTicks / 2, () -> {
				context.assertTrue(!ghoul.isRemoved(), "the body went before its death animation");
				context.runAtTick(5 + deathTicks + 10, () -> {
					context.assertTrue(ghoul.isRemoved(), "the body is still there after " + (deathTicks + 10) + " ticks");
					context.complete();
				});
			});
		});
	}

	/** A hit shows a flinch; a second inside the cooldown shows none; one after it does. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "overworld_anim_hurt", tickLimit = 100)
	public void ghoulFlinchesOncePerCooldown(TestContext context) {
		floor(context, 2, 2);
		AshGhoulEntity ghoul = context.spawnEntity(MobEntities.ASH_GHOUL, 2, 1, 2);
		ghoul.setAiDisabled(true);
		context.runAtTick(5, () -> {
			hit(ghoul);
			context.assertTrue(ghoul.hurtFlinches() == 1, "no flinch on a hit (" + ghoul.hurtFlinches() + ")");
			context.runAtTick(7, () -> {
				hit(ghoul);
				context.assertTrue(ghoul.hurtFlinches() == 1, "a second flinch inside the cooldown");
				context.runAtTick(7 + MobAnimations.HURT_COOLDOWN + 1, () -> {
					hit(ghoul);
					context.assertTrue(ghoul.hurtFlinches() == 2, "no flinch after the cooldown");
					context.assertTrue(ghoul.isAlive(), "the test hits killed it");
					ghoul.discard();
					context.complete();
				});
			});
		});
	}

	/** A 3×3 stone floor under relative {@code (x, 1, z)}. */
	static void floor(TestContext context, int x, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				context.setBlockState(x + dx, 0, z + dz, Blocks.STONE);
			}
		}
	}

	/** A small magic hit that counts (no hurt cooldown in the way). */
	static void hit(LivingEntity mob) {
		mob.timeUntilRegen = 0;
		mob.hurtTime = 0;
		mob.damage(mob.getDamageSources().magic(), 1);
	}
}
