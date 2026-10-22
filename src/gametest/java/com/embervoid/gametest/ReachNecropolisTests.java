package com.embervoid.gametest;

import com.embervoid.reach.mob.CryptMothEntity;
import com.embervoid.reach.mob.ReachMobs;
import com.embervoid.reach.mob.RestlessBonesEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

/** Sunken Necropolis mobs (PLAN 10.6, 10.7): Restless Bones, Crypt Moth, Hollow Sentinel, Grave Warden. */
public final class ReachNecropolisTests implements FabricGameTest {
	private static RestlessBonesEntity bones(TestContext context, int x, int z) {
		RestlessBonesEntity bones = ReachTests.still(context, ReachMobs.RESTLESS_BONES, x, 0, z);
		bones.setAiDisabled(true);
		return bones;
	}

	/** A lethal blow leaves a bone pile (6/16 of max health); 5 s later it stands again at half health. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 140)
	public void restlessBonesCollapseAndReform(TestContext context) {
		RestlessBonesEntity bones = bones(context, 3, 3);
		bones.damage(context.getWorld().getDamageSources().generic(), 1000);
		context.assertTrue(bones.isAlive() && bones.isPile(), "the lethal blow did not leave a pile");
		float pileHealth = bones.getMaxHealth() * RestlessBonesEntity.PILE_HEALTH / RestlessBonesEntity.BASE_HEALTH;
		context.assertTrue(Math.abs(bones.getHealth() - pileHealth) < 0.01, "pile health " + bones.getHealth() + ", expected " + pileHealth);
		context.assertTrue(bones.getHeight() < 1, "a pile is " + bones.getHeight() + " tall");
		context.waitAndRun(RestlessBonesEntity.REFORM_TICKS - 5, () -> context.assertTrue(bones.isPile(), "re-formed early"));
		context.waitAndRun(RestlessBonesEntity.REFORM_TICKS + 5, () -> {
			context.assertFalse(bones.isPile(), "no re-forming after 5 s");
			context.assertTrue(Math.abs(bones.getHealth() - bones.getMaxHealth() / 2) < 0.01, "re-formed at " + bones.getHealth());
			ReachTests.finish(context);
		});
	}

	/** Finishing the pile kills it for good; /kill kills a standing skeleton outright. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void finishedPileAndKillStayDead(TestContext context) {
		RestlessBonesEntity finished = bones(context, 2, 2);
		RestlessBonesEntity killed = bones(context, 6, 6);
		var sources = context.getWorld().getDamageSources();
		finished.damage(sources.generic(), 1000);
		finished.timeUntilRegen = 0;
		finished.damage(sources.generic(), 1000);
		context.assertTrue(finished.isDead(), "a finished pile is not dead");
		killed.kill();
		context.assertTrue(killed.isDead(), "/kill left a pile");
		ReachTests.finish(context);
	}

	/** A pile in torch light (block light ≥ 8) crumbles at once and never stands up. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 160)
	public void litPileStaysDead(TestContext context) {
		RestlessBonesEntity bones = bones(context, 3, 3);
		context.setBlockState(4, 0, 3, Blocks.TORCH);
		context.waitAndRun(5, () -> {
			bones.damage(context.getWorld().getDamageSources().generic(), 1000);
			context.assertTrue(bones.isPile(), "no pile");
		});
		context.waitAndRun(10, () -> {
			context.assertTrue(bones.isDead(), "a lit pile did not crumble");
			ReachTests.finish(context);
		});
	}

	/** With mobGriefing on, a Crypt Moth flies to a torch within 12 and snuffs it. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 260, batchId = "reach_moth_griefing")
	public void cryptMothSnuffsTorches(TestContext context) {
		testMothAndTorch(context, true);
	}

	/** With mobGriefing off, it is drawn to the torch but leaves it burning. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 260, batchId = "reach_moth_no_griefing")
	public void cryptMothLeavesTorchesWithoutGriefing(TestContext context) {
		testMothAndTorch(context, false);
	}

	private static void testMothAndTorch(TestContext context, boolean griefing) {
		GameRules.BooleanRule rule = context.getWorld().getGameRules().get(GameRules.DO_MOB_GRIEFING);
		boolean before = rule.get();
		rule.set(griefing, context.getWorld().getServer());
		BlockPos torch = new BlockPos(7, 0, 7);
		context.setBlockState(torch, Blocks.TORCH);
		// Closer to the torch than to the test's own corner blocks (the test frame has a light source there).
		CryptMothEntity moth = ReachTests.mob(context, ReachMobs.CRYPT_MOTH, 4, 3, 4);
		moth.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE).setBaseValue(1);
		Vec3d torchCenter = context.getAbsolute(Vec3d.ofCenter(torch));
		boolean[] done = {false};
		for (int tick = 10; tick <= 240; tick += 5) {
			int now = tick;
			context.waitAndRun(tick, () -> {
				if (done[0]) {
					return;
				}
				boolean lit = context.getBlockState(torch).isOf(Blocks.TORCH);
				boolean near = moth.getPos().isInRange(torchCenter, 2.5);
				String failure;
				if (griefing && !lit || !griefing && lit && near && now >= 200) {
					failure = "";
				} else if (!griefing && !lit) {
					failure = "torch snuffed with mobGriefing off";
				} else if (now >= 240) {
					failure = (griefing ? "torch still lit" : "moth never came to the torch") + "; moth at " + moth.getPos()
							+ ", drawn to " + moth.light();
				} else {
					return;
				}
				done[0] = true;
				rule.set(before, context.getWorld().getServer());
				context.assertTrue(failure.isEmpty(), failure);
				ReachTests.finish(context);
			});
		}
	}

	/** The wings glow for 15 ticks, then players within 3 are blinded for 3 s. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 60)
	public void cryptMothPollenBurstBlinds(TestContext context) {
		ServerPlayerEntity near = ReachTests.mockPlayer(context, 3, 4);
		ServerPlayerEntity far = ReachTests.mockPlayer(context, 8, 8);
		CryptMothEntity moth = ReachTests.still(context, ReachMobs.CRYPT_MOTH, 3, 1, 2);
		moth.setAiDisabled(true);
		moth.setTarget(near);
		context.assertTrue(moth.startBurst(), "the burst did not start");
		context.waitAndRun(CryptMothEntity.BURST_WINDUP - 2, () -> {
			context.assertTrue(moth.showsBone("glow_left"), "wings not glowing during the windup");
			context.assertFalse(near.hasStatusEffect(StatusEffects.BLINDNESS), "blinded before the burst");
		});
		context.waitAndRun(CryptMothEntity.BURST_WINDUP + 2, () -> {
			context.assertTrue(near.hasStatusEffect(StatusEffects.BLINDNESS), "the near player is not blind");
			context.assertFalse(far.hasStatusEffect(StatusEffects.BLINDNESS), "a player 8 blocks off is blind");
			context.assertFalse(moth.showsBone("glow_left"), "wings still glowing");
			ReachTests.finish(context);
		});
	}
}
