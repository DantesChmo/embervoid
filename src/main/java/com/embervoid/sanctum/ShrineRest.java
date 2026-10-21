package com.embervoid.sanctum;

import com.embervoid.item.HealingFlaskItem;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** What resting at an Ember Shrine does. Realm respawn points hook in through {@link RealmBridge}. */
final class ShrineRest {
	static void rest(ServerPlayerEntity player, BlockPos pos) {
		if (!RealmBridge.allowRest(player, player.getServerWorld(), pos)) {
			player.sendMessage(Text.translatable("embervoid.shrine.dormant").formatted(Formatting.DARK_GRAY), true);
			return;
		}
		refillFlasks(player);
		player.getServerWorld().playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 0.6f, 0.8f);
		player.sendMessage(Text.translatable("embervoid.shrine.rested").formatted(Formatting.GOLD), true);
		RealmBridge.rested(player, player.getServerWorld(), pos);
	}

	/** Fills every healing flask in the inventory through the flask's own API. */
	private static void refillFlasks(ServerPlayerEntity player) {
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.getItem() instanceof HealingFlaskItem) {
				HealingFlaskItem.refill(stack, player);
			}
		}
	}

	private ShrineRest() {}
}
