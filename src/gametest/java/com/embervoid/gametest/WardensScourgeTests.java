package com.embervoid.gametest;

import com.embervoid.gametest.mixin.LivingEntityAccessor;
import com.embervoid.reach.gear.Bleeding;
import com.embervoid.reach.gear.GearAbilities;
import com.embervoid.reach.gear.GearConfig;
import com.embervoid.reach.gear.GearReach;
import com.embervoid.reach.gear.GearSounds;
import com.embervoid.reach.gear.GearWeapons;
import com.embervoid.reach.gear.WardensScourgeItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/**
 * The Warden's Scourge as a whip (PLAN 17.4): a swing hits every mob along the lash and nothing off it, the tip cracks
 * for more, a wall stops the lash, Lash still pulls, and one swing costs one durability. The player stands at (2, 2)
 * looking south (+z), so a mob at (2, z) is z − 2 blocks ahead on the crosshair.
 */
public final class WardensScourgeTests implements FabricGameTest {
	/** A swing at the air hits the three husks on the line (2, 3 and 5 blocks ahead, each bleeding), not one beside or behind. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void swingHitsEveryMobOnTheLash(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		GearTests.withPlayers(context, () -> {
			HuskEntity near = GearTests.husk(context, 2, 4);
			HuskEntity middle = GearTests.husk(context, 2, 5);
			HuskEntity far = GearTests.husk(context, 2, 7);
			HuskEntity beside = GearTests.husk(context, 5, 5);
			HuskEntity behind = GearTests.husk(context, 2, 0);
			ready(player);
			GearReach.onSwing(player, Hand.MAIN_HAND);
			for (HuskEntity husk : new HuskEntity[] {near, middle, far}) {
				context.assertTrue(hurt(husk), "a husk " + (husk.getZ() - player.getZ()) + " blocks ahead was not hit");
				context.assertTrue(Bleeding.level(husk) == 1, "a hit did not bleed: " + Bleeding.level(husk));
			}
			context.assertFalse(hurt(beside), "the husk 3 blocks aside was hit");
			context.assertFalse(hurt(behind), "the husk behind was hit");
		}, player);
		TestPlayers.complete(context, player);
	}

	/**
	 * A click on the nearest mob is a crack too: the attack event cancels the single vanilla hit and the lash hits the
	 * mob behind it as well.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void clickCracksTheWholeLash(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		GearTests.withPlayers(context, () -> {
			HuskEntity clicked = GearTests.husk(context, 2, 4);
			HuskEntity behindIt = GearTests.husk(context, 2, 6);
			ready(player);
			context.assertTrue(GearReach.onAttackEntityForTest(player, clicked) == ActionResult.FAIL,
					"the vanilla attack was not replaced");
			context.assertTrue(hurt(clicked) && hurt(behindIt), "the click did not crack along the lash");
		}, player);
		TestPlayers.complete(context, player);
	}

	/**
	 * Only the tip cracks: an iron golem 2 blocks ahead takes the plain hit, one 5 blocks ahead (in the last 1.5 blocks)
	 * ×1.6 of it with the crack's sound, which a swing that reaches no mob with the tip does not make.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void onlyTheTipCracks(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		GearTests.withPlayers(context, () -> {
			ServerWorld world = context.getWorld();
			IronGolemEntity alone = golem(context, 2, 4);
			SoundLog.forget(world, player.getPos(), 16);
			ready(player);
			WardensScourgeItem.crack(player, null);
			context.assertTrue(hurt(alone), "the golem 2 blocks ahead was not hit");
			context.assertFalse(SoundLog.heard(world, GearSounds.WHIP_CRACK, player.getPos(), 16), "cracked without the tip");
			alone.discard();

			IronGolemEntity near = golem(context, 2, 4);
			IronGolemEntity tip = golem(context, 2, 7);
			ready(player);
			WardensScourgeItem.crack(player, null);
			float plain = near.getMaxHealth() - near.getHealth();
			float cracked = tip.getMaxHealth() - tip.getHealth();
			float multiplier = GearConfig.get().weapons().whipTipMultiplier();
			context.assertTrue(plain > 0 && Math.abs(cracked - plain * multiplier) < 0.01,
					"the tip dealt " + cracked + ", the lash " + plain + ", not ×" + multiplier);
			context.assertTrue(SoundLog.heard(world, GearSounds.WHIP_CRACK, tip.getPos(), 2), "no crack at the tip");
		}, player);
		TestPlayers.complete(context, player);
	}

	/** A wall 3 blocks ahead stops the lash: the husk before it is hit, the one behind it is not. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void wallStopsTheLash(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		GearTests.withPlayers(context, () -> {
			HuskEntity before = GearTests.husk(context, 2, 4);
			HuskEntity beyond = GearTests.husk(context, 2, 7);
			for (int x = 0; x <= 4; x++) {
				for (int y = 1; y <= 4; y++) {
					context.setBlockState(x, y, 5, Blocks.STONE);
				}
			}
			ready(player);
			WardensScourgeItem.crack(player, null);
			context.assertTrue(hurt(before), "the husk before the wall was not hit");
			context.assertFalse(hurt(beyond), "the lash went through the wall");
		}, player);
		TestPlayers.complete(context, player);
	}

	/**
	 * Lash is as it was: 20 stamina, 5 s, pulls a mob 5 blocks away toward the player without hurting it, and the
	 * swing packet that follows the cast is no crack.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void lashStillPulls(TestContext context) {
		context.assertTrue(GearAbilities.LASH.staminaCost() == 20 && GearAbilities.LASH.cooldownTicks() == 100,
				"Lash costs " + GearAbilities.LASH.staminaCost() + " stamina, cools down " + GearAbilities.LASH.cooldownTicks());
		context.assertTrue(GearConfig.get().weapons().lashPull() == 4, "Lash pulls " + GearConfig.get().weapons().lashPull());
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		GearTests.withPlayers(context, () -> {
			HuskEntity far = GearTests.husk(context, 2, 7);
			ready(player);
			GearAbilities.LASH.perform(context.getWorld(), player, player.getMainHandStack());
			context.assertTrue(far.getVelocity().z < -0.5, "Lash did not pull: velocity " + far.getVelocity());
			GearReach.onSwing(player, Hand.MAIN_HAND);
			context.assertFalse(hurt(far), "the swing after Lash cracked");
		}, player);
		TestPlayers.complete(context, player);
	}

	/** One swing through three husks costs one durability, as a single hit does. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void oneSwingWearsOnce(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		GearTests.withPlayers(context, () -> {
			HuskEntity[] husks = {GearTests.husk(context, 2, 4), GearTests.husk(context, 2, 5), GearTests.husk(context, 2, 6)};
			ready(player);
			WardensScourgeItem.crack(player, null);
			for (HuskEntity husk : husks) {
				context.assertTrue(hurt(husk), "a husk on the lash was not hit");
			}
			int wear = player.getMainHandStack().getDamage();
			context.assertTrue(wear == 1, "one swing cost " + wear + " durability");
			for (HuskEntity husk : husks) {
				husk.timeUntilRegen = 0;
			}
			ready(player);
			WardensScourgeItem.crack(player, null);
			context.assertTrue(player.getMainHandStack().getDamage() == 2, "a second swing cost "
					+ (player.getMainHandStack().getDamage() - wear));
		}, player);
		TestPlayers.complete(context, player);
	}

	/** The Scourge in hand, the attack fully charged (test players never tick). */
	private static void ready(ServerPlayerEntity player) {
		if (!player.getMainHandStack().isOf(GearWeapons.WARDENS_SCOURGE)) {
			GearTests.hold(player, GearWeapons.WARDENS_SCOURGE);
		}
		((LivingEntityAccessor) player).embervoid$setLastAttackedTicks(100);
		player.setOnGround(true);
		player.fallDistance = 0;
	}

	private static IronGolemEntity golem(TestContext context, int x, int z) {
		context.setBlockState(x, 0, z, Blocks.STONE);
		IronGolemEntity golem = context.spawnEntity(EntityType.IRON_GOLEM, x, 1, z);
		golem.setAiDisabled(true);
		return golem;
	}

	private static boolean hurt(LivingEntity entity) {
		return entity.getHealth() < entity.getMaxHealth();
	}
}
