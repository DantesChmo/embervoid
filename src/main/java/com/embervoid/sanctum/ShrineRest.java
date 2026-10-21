package com.embervoid.sanctum;

import com.embervoid.item.HealingFlaskItem;
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
		HealingFlaskItem.refill(player);
		player.getServerWorld().playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 0.6f, 0.8f);
		player.sendMessage(Text.translatable("embervoid.shrine.rested").formatted(Formatting.GOLD), true);
		RealmBridge.rested(player, player.getServerWorld(), pos);
	}

	private ShrineRest() {}
}
