package com.embervoid.gametest;

import com.embervoid.reach.mob.CryptMothEntity;
import com.embervoid.reach.mob.GraveWardenEntity;
import com.embervoid.reach.mob.HollowSentinelEntity;
import com.embervoid.reach.mob.ReachMobs;
import com.embervoid.reach.mob.RestlessBonesEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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

	/** A sentinel facing +z (yaw 0). */
	private static HollowSentinelEntity sentinel(TestContext context, int x, int z) {
		HollowSentinelEntity sentinel = ReachTests.still(context, ReachMobs.HOLLOW_SENTINEL, x, 0, z);
		sentinel.setAiDisabled(true);
		sentinel.setYaw(0);
		sentinel.setBodyYaw(0);
		sentinel.setHeadYaw(0);
		return sentinel;
	}

	/** From the front, fists, axes and arrows do nothing; from behind a hit lands. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void sentinelShieldStopsFrontHits(TestContext context) {
		HollowSentinelEntity sentinel = sentinel(context, 4, 4);
		ServerPlayerEntity front = ReachTests.mockPlayer(context, 4, 7);
		ServerPlayerEntity back = ReachTests.mockPlayer(context, 4, 1);
		var sources = context.getWorld().getDamageSources();
		context.assertFalse(sentinel.damage(sources.playerAttack(front), 5), "a front punch landed");
		front.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
		context.assertFalse(sentinel.damage(sources.playerAttack(front), 5), "a front axe landed");
		ArrowEntity arrow = new ArrowEntity(context.getWorld(), front);
		arrow.setPosition(front.getEyePos());
		context.assertFalse(sentinel.damage(sources.arrow(arrow, front), 5), "a front arrow landed");
		context.assertTrue(sentinel.getHealth() == sentinel.getMaxHealth(), "hurt from the front");
		context.assertTrue(sentinel.damage(sources.playerAttack(back), 5), "a hit from behind was blocked");
		ReachTests.finish(context);
	}

	/** Shield planted 15 ticks, then a bash with knockback; for 30 ticks after it the front is open. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
	public void sentinelBashOpensTheShield(TestContext context) {
		HollowSentinelEntity sentinel = sentinel(context, 4, 2);
		// No goals but its AI on, so knockback moves it (a mob without AI never moves).
		CowEntity target = ReachTests.still(context, EntityType.COW, 4, 0, 4);
		target.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(200);
		target.setHealth(200);
		sentinel.setTarget(target);
		ServerPlayerEntity front = ReachTests.mockPlayer(context, 6, 6);
		Vec3d start = target.getPos();
		context.assertTrue(sentinel.startBash(), "the bash did not start");
		context.waitAndRun(HollowSentinelEntity.BASH_WINDUP - 1, () -> context.assertTrue(target.getHealth() == 200, "bashed early"));
		context.waitAndRun(HollowSentinelEntity.BASH_WINDUP + 4, () -> {
			context.assertTrue(target.getHealth() < 200, "the bash did no damage");
			context.assertTrue(target.getPos().distanceTo(start) > 0.5, "no knockback: moved " + target.getPos().distanceTo(start));
			context.assertTrue(sentinel.isOpen(), "shield not open after the bash");
			context.assertTrue(sentinel.damage(context.getWorld().getDamageSources().playerAttack(front), 5), "front hit blocked while open");
		});
		context.waitAndRun(HollowSentinelEntity.BASH_WINDUP + HollowSentinelEntity.OPEN_TICKS + 5, () -> {
			context.assertFalse(sentinel.isOpen(), "still open after 30 ticks");
			sentinel.timeUntilRegen = 0;
			context.assertFalse(sentinel.damage(context.getWorld().getDamageSources().playerAttack(front), 5), "front hit landed after the window");
			ReachTests.finish(context);
		});
	}

	/** The bell re-forms piles within 12 and staggers players within 4 (Slowness IV). */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 60)
	public void wardenBellReformsPilesAndStaggers(TestContext context) {
		GraveWardenEntity warden = ReachTests.still(context, ReachMobs.GRAVE_WARDEN, 2, 0, 2);
		warden.setAiDisabled(true);
		RestlessBonesEntity bones = bones(context, 8, 8);
		bones.damage(context.getWorld().getDamageSources().generic(), 1000);
		ServerPlayerEntity near = ReachTests.mockPlayer(context, 2, 5);
		context.assertTrue(warden.startBell(), "the bell did not start");
		context.waitAndRun(GraveWardenEntity.BELL_WINDUP - 1, () -> context.assertTrue(bones.isPile(), "re-formed before the ring"));
		context.waitAndRun(GraveWardenEntity.BELL_WINDUP + 2, () -> {
			context.assertFalse(bones.isPile(), "the pile did not re-form at the bell");
			StatusEffectInstance slow = near.getStatusEffect(StatusEffects.SLOWNESS);
			context.assertTrue(slow != null && slow.getAmplifier() == 3, "no Slowness IV: " + slow);
			ReachTests.finish(context);
		});
	}

	/** With no bones around, each ring raises two; never more than four of its own alive. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 120, batchId = "reach_warden_raise")
	public void wardenBellRaisesBonesUpToFour(TestContext context) {
		GraveWardenEntity warden = ReachTests.still(context, ReachMobs.GRAVE_WARDEN, 5, 0, 5);
		warden.setAiDisabled(true);
		// Its own batch, and only around the warden: no other test's bones counted.
		Box area = warden.getBoundingBox().expand(8);
		warden.startBell();
		context.waitAndRun(GraveWardenEntity.BELL_WINDUP + 2, () -> {
			context.assertTrue(count(context, area) == 2, "first ring raised " + count(context, area));
			// Bones around now: the next ring would re-form them instead of raising. Send them away to test the cap.
			context.getWorld().getEntitiesByClass(RestlessBonesEntity.class, area, b -> true)
					.forEach(ReachNecropolisTests::sendAway);
			warden.startBell();
		});
		context.waitAndRun(2 * GraveWardenEntity.BELL_WINDUP + 4, () -> {
			context.assertTrue(count(context, area) == 2, "second ring raised " + count(context, area) + " near");
			context.getWorld().getEntitiesByClass(RestlessBonesEntity.class, area, b -> true)
					.forEach(ReachNecropolisTests::sendAway);
			warden.startBell();
		});
		context.waitAndRun(3 * GraveWardenEntity.BELL_WINDUP + 6, () -> {
			context.assertTrue(count(context, area) == 0, "a third ring raised past the cap of four");
			// The raised bones are the warden's, not the test's: remove them too (own batch, so all are ours).
			context.getWorld().getEntitiesByClass(RestlessBonesEntity.class, warden.getBoundingBox().expand(100), b -> true)
					.forEach(RestlessBonesEntity::discard);
			ReachTests.finish(context);
		});
	}

	/** Out of the warden's reach and the counted area, still alive. */
	private static void sendAway(RestlessBonesEntity bones) {
		bones.setNoGravity(true);
		bones.setAiDisabled(true);
		bones.setPosition(bones.getX() + 60, bones.getY() + 5, bones.getZ());
	}

	private static int count(TestContext context, Box area) {
		return context.getWorld().getEntitiesByClass(RestlessBonesEntity.class, area, b -> b.isAlive()).size();
	}
}
