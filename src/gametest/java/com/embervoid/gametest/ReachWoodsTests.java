package com.embervoid.gametest;

import com.embervoid.reach.mob.BarkHollowEntity;
import com.embervoid.reach.mob.FireFlaskEntity;
import com.embervoid.reach.mob.LanternHagEntity;
import com.embervoid.reach.mob.ReachMobs;
import com.embervoid.reach.mob.WitheredThrallEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Royal Woods mobs: Withered Thrall and its hanging cage, Lantern Hag, Bark Hollow. Tests where a
 * mob reacts to players or moves on its own run in a batch of their own.
 */
public final class ReachWoodsTests implements FabricGameTest {
	/** A thrall hung in a cage at relative (x, y, z): placed with the cage tag. */
	private static WitheredThrallEntity cagedThrall(TestContext context, int x, int y, int z) {
		WitheredThrallEntity thrall = ReachTests.still(context, ReachMobs.WITHERED_THRALL, x, y, z);
		thrall.addCommandTag(WitheredThrallEntity.CAGE_TAG);
		thrall.markPlaced();
		return thrall;
	}

	/**
	 * A caged thrall hangs while a player stands off to the side; a player passing under sets off a 10-tick chain
	 * rattle (it still hangs), then it drops to the ground with that player as its target.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100, batchId = "reach_thrall_drop")
	public void cagedThrallDropsOnAPlayerPassingUnder(TestContext context) {
		ServerPlayerEntity player = ReachTests.player(context, 6, 3);
		WitheredThrallEntity thrall = cagedThrall(context, 3, 6, 3);
		double hangY = thrall.getY();
		context.waitAndRun(20, () -> {
			context.assertTrue(thrall.isHanging() && !thrall.isRattling(), "set off by a player 3 blocks to the side");
			context.assertTrue(Math.abs(thrall.getY() - hangY) < 0.01, "a hanging thrall moved");
			ReachTests.teleport(context, player, 3.5, 1, 3.5);
		});
		context.waitAndRun(22, () -> context.assertTrue(thrall.isRattling(), "no rattle with a player under the cage"));
		context.waitAndRun(30, () -> context.assertTrue(thrall.isHanging() && Math.abs(thrall.getY() - hangY) < 0.01,
				"dropped before the 10-tick rattle ended"));
		context.waitAndRun(60, () -> {
			context.assertFalse(thrall.isHanging(), "still hanging after the rattle");
			context.assertTrue(thrall.getY() < hangY - 3, "did not fall: y " + thrall.getY());
			context.assertTrue(thrall.getTarget() == player, "does not go for the player it dropped on");
			ReachTests.finish(context);
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100, batchId = "reach_thrall_trapdoor")
	public void cagedThrallOpensTheTrapdoorUnderIt(TestContext context) {
		context.setBlockState(3, 5, 3, Blocks.SPRUCE_TRAPDOOR);
		WitheredThrallEntity thrall = cagedThrall(context, 3, 6, 3);
		ServerPlayerEntity player = ReachTests.player(context, 3, 6);
		context.waitAndRun(5, () -> ReachTests.teleport(context, player, 3.5, 1, 3.5));
		context.waitAndRun(60, () -> {
			context.assertTrue(context.getBlockState(new BlockPos(3, 5, 3)).get(TrapdoorBlock.OPEN), "trapdoor still shut");
			context.assertTrue(thrall.getY() < context.getAbsolute(new Vec3d(0, 5, 0)).y, "did not fall through the trapdoor");
			ReachTests.finish(context);
		});
	}

	/** The hag raises her lantern for 15 ticks, then a flask flies; it hurts and sets alight what it lands on. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 120)
	public void lanternHagThrowsAFlaskThatBurns(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 2, 9);
		LanternHagEntity hag = ReachTests.still(context, ReachMobs.LANTERN_HAG, 2, 0, 1);
		hag.setAiDisabled(true);
		hag.setTarget(target);
		context.assertTrue(hag.startThrow(), "the throw did not start");
		Box area = new Box(context.getAbsolutePos(BlockPos.ORIGIN)).expand(16);
		context.waitAndRun(LanternHagEntity.THROW_WINDUP - 1, () -> context.assertTrue(
				context.getWorld().getEntitiesByClass(FireFlaskEntity.class, area, flask -> true).isEmpty(), "a flask before the lantern was up"));
		context.waitAndRun(LanternHagEntity.THROW_WINDUP + 1, () -> context.assertFalse(
				context.getWorld().getEntitiesByClass(FireFlaskEntity.class, area, flask -> true).isEmpty(), "no flask after the windup"));
		for (int tick = 20; tick <= 100; tick++) {
			context.waitAndRun(tick, () -> {
				if (target.getHealth() < target.getMaxHealth() && target.isOnFire()) {
					ReachTests.finish(context);
				}
			});
		}
	}

	/** A target 3 blocks away: the hag backs off to at least 6. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 140, batchId = "reach_hag_distance")
	public void lanternHagKeepsHerDistance(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 8, 8);
		LanternHagEntity hag = ReachTests.mob(context, ReachMobs.LANTERN_HAG, 8, 0, 11);
		hag.setTarget(target);
		context.waitAndRun(120, () -> {
			context.assertTrue(hag.distanceTo(target) >= 6, "hag still " + hag.distanceTo(target) + " blocks from her target");
			ReachTests.finish(context);
		});
	}

	/** Dormant until a player is within 6, then 20 ticks of creaking and it is up with that player as target. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100, batchId = "reach_bark_wake")
	public void barkHollowWakesWhenAPlayerComesClose(TestContext context) {
		ServerPlayerEntity player = ReachTests.player(context, 3, 12);
		BarkHollowEntity hollow = ReachTests.still(context, ReachMobs.BARK_HOLLOW, 3, 0, 2);
		hollow.goDormant();
		context.waitAndRun(10, () -> {
			context.assertTrue(hollow.isDormant() && !hollow.showsBone("eyes"), "not dormant with a player 10 blocks away");
			ReachTests.teleport(context, player, 3.5, 1, 6.5);
		});
		context.waitAndRun(13, () -> context.assertTrue(hollow.isAwakening() && hollow.showsBone("eyes"), "did not stir"));
		context.waitAndRun(40, () -> {
			context.assertFalse(hollow.isDormant() || hollow.isAwakening(), "still not up");
			context.assertTrue(hollow.getTarget() == player, "not after the player that woke it");
			ReachTests.finish(context);
		});
	}

	/** An axe in a melee hit and fire do ×1.5; a bare hand does the plain amount. Armor is off to compare. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void barkHollowTakesMoreFromAxesAndFire(TestContext context) {
		ServerPlayerEntity player = ReachTests.mockPlayer(context, 1, 1);
		BarkHollowEntity plain = hollow(context, 4, 2);
		BarkHollowEntity axed = hollow(context, 4, 6);
		BarkHollowEntity burnt = hollow(context, 8, 6);
		var sources = context.getWorld().getDamageSources();
		plain.damage(sources.playerAttack(player), 4);
		player.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
		axed.damage(sources.playerAttack(player), 4);
		burnt.damage(sources.inFire(), 4);
		assertLost(context, plain, 4, "bare hand");
		assertLost(context, axed, 6, "axe");
		assertLost(context, burnt, 6, "fire");
		ReachTests.finish(context);
	}

	private static BarkHollowEntity hollow(TestContext context, int x, int z) {
		BarkHollowEntity hollow = ReachTests.still(context, ReachMobs.BARK_HOLLOW, x, 0, z);
		hollow.setAiDisabled(true);
		hollow.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(0);
		return hollow;
	}

	private static void assertLost(TestContext context, BarkHollowEntity hollow, float expected, String what) {
		float lost = hollow.getMaxHealth() - hollow.getHealth();
		context.assertTrue(Math.abs(lost - expected) < 0.01, what + ": lost " + lost + ", expected " + expected);
	}

	/** The sweep lands 20 ticks after the windup on what stands in front within 3 blocks, not behind it. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 60)
	public void barkHollowSweepsInFront(TestContext context) {
		BarkHollowEntity hollow = ReachTests.still(context, ReachMobs.BARK_HOLLOW, 4, 0, 4);
		hollow.setAiDisabled(true);
		hollow.setYaw(0);
		hollow.setBodyYaw(0);
		// Yaw 0 faces +z.
		CowEntity front = ReachTests.dummy(context, 4, 7);
		CowEntity side = ReachTests.dummy(context, 6, 6);
		CowEntity behind = ReachTests.dummy(context, 4, 1);
		hollow.setTarget(front);
		context.assertTrue(hollow.startMove(hollow.meleeMove()), "the sweep did not start");
		context.waitAndRun(BarkHollowEntity.SWEEP_WINDUP - 1, () -> context.assertTrue(front.getHealth() == front.getMaxHealth(), "hit early"));
		context.waitAndRun(BarkHollowEntity.SWEEP_WINDUP + 2, () -> {
			context.assertTrue(front.getHealth() < front.getMaxHealth(), "the one in front was not hit");
			context.assertTrue(side.getHealth() < side.getMaxHealth(), "the one front-left was not hit");
			context.assertTrue(behind.getHealth() == behind.getMaxHealth(), "the one behind was hit");
			ReachTests.finish(context);
		});
	}
}
