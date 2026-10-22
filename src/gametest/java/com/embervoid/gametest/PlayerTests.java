package com.embervoid.gametest;

import com.embervoid.entity.AshRemnantEntity;
import com.embervoid.item.HealingFlaskItem;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.XpUtil;
import com.embervoid.registry.ModEntities;
import com.embervoid.registry.ModItems;
import com.embervoid.sanctum.SanctumBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** Player systems on a real server player: starting flask, shrine rest, death into an Ash Remnant and back. */
public final class PlayerTests implements FabricGameTest {
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void firstJoinGivesFullFlask(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		context.assertTrue(player.getInventory().count(ModItems.HEALING_FLASK) == 1, "no Healing Flask on first join");
		context.assertTrue(HealingFlaskItem.charges(player) == HealingFlaskItem.maxCharges(player),
				"flask not full: " + HealingFlaskItem.charges(player) + "/" + HealingFlaskItem.maxCharges(player));
		TestPlayers.complete(context, player);
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void shrineRestRefillsFlaskAndHealth(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		BlockPos shrine = new BlockPos(4, 1, 2);
		context.setBlockState(shrine, SanctumBlocks.EMBER_SHRINE);
		PlayerDataManager.get(player).flaskCharges = 0;
		player.setHealth(4);

		context.useBlock(shrine, player);
		context.assertTrue(HealingFlaskItem.charges(player) == HealingFlaskItem.maxCharges(player),
				"flask not refilled: " + HealingFlaskItem.charges(player));
		context.assertTrue(player.getHealth() == player.getMaxHealth(), "health not restored: " + player.getHealth());
		TestPlayers.complete(context, player);
	}

	/**
	 * Death puts items and XP into a remnant (the flask stays with the player and is full again after respawn);
	 * the owner gets everything back from it.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void deathLeavesRemnantThatGivesEverythingBack(TestContext context) {
		ServerPlayerEntity player = TestPlayers.create(context, 2, 2);
		player.getInventory().setStack(5, new ItemStack(Items.DIAMOND, 7));
		XpUtil.setPoints(player, 150);
		PlayerDataManager.get(player).flaskCharges = 0;

		player.kill();
		List<? extends AshRemnantEntity> remnants = context.getWorld().getEntitiesByType(ModEntities.ASH_REMNANT,
				remnant -> player.getUuid().equals(remnant.getOwner()));
		context.assertTrue(remnants.size() == 1, "expected one remnant, found " + remnants.size());
		List<ItemEntity> dropped = context.getWorld().getEntitiesByClass(ItemEntity.class,
				player.getBoundingBox().expand(8), item -> true);
		context.assertTrue(dropped.isEmpty(), "items dropped on the ground: " + dropped);

		ServerPlayerEntity respawned = TestPlayers.respawn(player);
		context.assertTrue(respawned.getInventory().count(ModItems.HEALING_FLASK) == 1, "flask lost on death");
		context.assertTrue(HealingFlaskItem.charges(respawned) == HealingFlaskItem.maxCharges(respawned),
				"flask not refilled on respawn");
		context.assertTrue(respawned.getInventory().count(Items.DIAMOND) == 0, "diamonds were kept, not in remnant");

		remnants.get(0).interact(respawned, Hand.MAIN_HAND);
		context.assertTrue(respawned.getInventory().getStack(5).isOf(Items.DIAMOND)
				&& respawned.getInventory().getStack(5).getCount() == 7, "diamonds not back in slot 5");
		context.assertTrue(XpUtil.getPoints(respawned) == 150, "XP back: " + XpUtil.getPoints(respawned) + ", expected 150");
		context.assertTrue(remnants.get(0).isRemoved(), "remnant still there after recovery");
		TestPlayers.complete(context, respawned);
	}
}
