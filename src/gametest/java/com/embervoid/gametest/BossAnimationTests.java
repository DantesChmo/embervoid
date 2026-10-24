package com.embervoid.gametest;

import com.embervoid.boss.BossFight;
import com.embervoid.boss.Telegraph;
import com.embervoid.reach.boss.AshColossusEntity;
import com.embervoid.reach.boss.ReachBoss;
import com.embervoid.reach.boss.ReachBossEntities;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;

/**
 * The hit, stagger and death animations of the bosses (PLAN 19.3, 19.4): a hit during a windup leaves the windup
 * running and shows no flinch (the tell stays readable), a hit between moves shows one, and a kill keeps the body for
 * its death animation and then removes it, the fight having ended and credited at once. On a stone floor with fights
 * started directly; the brain is held and the AI off, so moves are started by the test.
 */
public final class BossAnimationTests implements FabricGameTest {
	private static final int RADIUS = 7;

	/** A hit two ticks into the fist's windup: the windup goes on unchanged and no flinch plays; one after it does, with a cooldown. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "boss_anim_hurt", tickLimit = 200)
	public void hurtNeverCancelsAWindup(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			ServerPlayerEntity player = arena.join(4, 7);
			player.setInvulnerable(true); // stands inside the fist's cone
			BossFight fight = arena.start(ReachBossEntities.ASH_COLOSSUS, 4, 4, RADIUS, 18);
			AshColossusEntity boss = (AshColossusEntity) fight.boss();
			boss.holdMoves(Integer.MAX_VALUE);
			boss.setAiDisabled(true);
			arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				boss.fist(player);
				context.assertTrue(Telegraph.windups(boss).equals(List.of(AshColossusEntity.FIST_WINDUP)), "windups " + Telegraph.windups(boss));
				arena.after(2, () -> {
					hit(player, boss);
					context.assertTrue(Telegraph.isWindingUp(boss), "the hit cancelled the windup");
					context.assertTrue(Telegraph.windups(boss).equals(List.of(AshColossusEntity.FIST_WINDUP)), "the windup changed: " + Telegraph.windups(boss));
					context.assertTrue(boss.hurtFlinches() == 0, "a flinch played over the windup");
					arena.after(AshColossusEntity.FIST_WINDUP + 2, () -> {
						context.assertTrue(!Telegraph.isWindingUp(boss), "the fist never landed");
						hit(player, boss);
						context.assertTrue(boss.hurtFlinches() == 1, "no flinch between moves (" + boss.hurtFlinches() + ")");
						arena.after(2, () -> {
							hit(player, boss);
							context.assertTrue(boss.hurtFlinches() == 1, "a second flinch inside the cooldown");
							arena.after(ReachBoss.HURT_COOLDOWN, () -> {
								hit(player, boss);
								context.assertTrue(boss.hurtFlinches() == 2, "no flinch after the cooldown");
								arena.finish();
							});
						});
					});
				});
			});
		});
	}

	/** Killed, the Colossus's fight ends at once; its body lies there for its death animation, then goes. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "boss_anim_death_colossus", tickLimit = 300)
	public void colossusBodyStaysForItsDeathAnimation(TestContext context) {
		bodyStays(context, ReachBossEntities.ASH_COLOSSUS, 18);
	}

	/** The same for the Crypt Keeper (its lantern bone goes with the body, not before). */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "boss_anim_death_keeper", tickLimit = 300)
	public void keeperBodyStaysForItsDeathAnimation(TestContext context) {
		bodyStays(context, ReachBossEntities.CRYPT_KEEPER, 24);
	}

	private static void bodyStays(TestContext context, net.minecraft.entity.EntityType<? extends ReachBoss> type, int level) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			arena.join(4, 7);
			BossFight fight = arena.start(type, 4, 4, RADIUS, level);
			ReachBoss boss = (ReachBoss) fight.boss();
			boss.holdMoves(Integer.MAX_VALUE);
			int deathTicks = boss.deathTicks();
			context.assertTrue(deathTicks >= 40, "death animation of " + deathTicks + " ticks is too short to see");
			arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				BossRewardTests.kill(fight);
				context.assertTrue(boss.isDead() && !boss.isRemoved(), "the body went with the fight");
				arena.after(deathTicks / 2, () -> {
					context.assertTrue(!boss.isRemoved(), "the body went before its death animation");
					context.assertTrue(!Telegraph.isWindingUp(boss), "a windup survived the death");
					arena.after(deathTicks / 2 + 10, () -> {
						context.assertTrue(boss.isRemoved(), "the body is still there after " + (deathTicks + 10) + " ticks");
						arena.finish();
					});
				});
			});
		});
	}

	/** Hits the boss for 4 (magic, from the player's position: the Colossus scales it by direction, the Keeper by light). */
	private static void hit(ServerPlayerEntity player, ReachBoss boss) {
		boss.timeUntilRegen = 0;
		boss.hurtTime = 0;
		boss.damage(player.getDamageSources().indirectMagic(player, player), 4);
	}
}
