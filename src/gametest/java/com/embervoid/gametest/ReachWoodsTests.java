package com.embervoid.gametest;

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
 * Royal Woods mobs (PLAN 10.4, 10.5): Withered Thrall and its hanging cage, Lantern Hag, Bark Hollow. Tests where a
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

	/** A cage with a trapdoor floor: the thrall opens it and falls through. */
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
}
