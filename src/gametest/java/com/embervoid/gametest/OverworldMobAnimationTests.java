package com.embervoid.gametest;

import com.embervoid.entity.mob.AshGhoulEntity;
import com.embervoid.entity.mob.HollowEntity;
import com.embervoid.entity.mob.MobAnimations;
import com.embervoid.entity.mob.MobEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;

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

	/** A hit two ticks into the Hollow's wind-up: the wind-up goes on and lands on time, no flinch plays; one after it does. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "overworld_anim_windup", tickLimit = 200)
	public void hollowHitDuringTheWindupKeepsTheWindup(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, 4);
			// As in HollowTests: goals cleared (left to its AI next to the player it swings on its own through the join
			// wait), and a sword, since all of its damage comes from the weapon.
			HollowEntity hollow = context.spawnMob(MobEntities.HOLLOW, new BlockPos(4, 1, 3));
			hollow.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			ServerPlayerEntity player = arena.join(4, 4);
			arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				hollow.setTarget(player);
				context.assertTrue(startSwing(hollow), "the swing did not start");
				int windup = hollow.windup();
				arena.after(2, () -> {
					hit(hollow);
					context.assertTrue(hollow.windup() == windup - 2, "the hit changed the wind-up: " + hollow.windup());
					context.assertTrue(hollow.hurtFlinches() == 0, "a flinch played over the wind-up");
					float health = player.getHealth();
					arena.after(windup + 3, () -> {
						context.assertTrue(hollow.windup() < 0 && player.getHealth() < health, "the swing never landed");
						// It may raise its guard at random between swings, and a guarded hit shows no flinch (by design).
						lowerGuard(hollow);
						hit(hollow);
						context.assertTrue(hollow.hurtFlinches() == 1, "no flinch between swings (" + hollow.hurtFlinches() + ")");
						hollow.discard();
						arena.finish();
					});
				});
			});
		});
	}

	/** {@code HollowEntity.startSwing} is package-private. */
	private static boolean startSwing(HollowEntity hollow) {
		try {
			Method method = HollowEntity.class.getDeclaredMethod("startSwing");
			method.setAccessible(true);
			return (boolean) method.invoke(hollow);
		} catch (ReflectiveOperationException e) {
			throw new GameTestException("startSwing: " + e);
		}
	}

	/** Drops a raised guard: {@code HollowEntity.setStance} is private. */
	private static void lowerGuard(HollowEntity hollow) {
		try {
			Method method = HollowEntity.class.getDeclaredMethod("setStance", byte.class, int.class);
			method.setAccessible(true);
			method.invoke(hollow, (byte) 0, 0);
		} catch (ReflectiveOperationException e) {
			throw new GameTestException("setStance: " + e);
		}
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
