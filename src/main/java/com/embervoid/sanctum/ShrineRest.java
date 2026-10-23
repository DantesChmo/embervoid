package com.embervoid.sanctum;

import com.embervoid.item.HealingFlaskItem;
import com.embervoid.realm.api.ShrineHooks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * What resting at an Ember Shrine does: full health and flasks. The realms track hooks in through {@link ShrineHooks}
 * (dormant arena shrines, realm respawn points).
 */
final class ShrineRest {
	static void rest(ServerPlayerEntity player, BlockPos pos) {
		ServerWorld world = player.getServerWorld();
		// A listener that refuses (a dormant arena shrine) tells the player why.
		if (!ShrineHooks.ALLOW_REST.invoker().allowRest(player, world, pos)) {
			return;
		}
		HealingFlaskItem.refill(player);
		player.setHealth(player.getMaxHealth());
		world.playSound(null, pos, SanctumSounds.SHRINE_REST, SoundCategory.BLOCKS, 0.6f, 0.8f);
		player.sendMessage(Text.translatable("embervoid.shrine.rested").formatted(Formatting.GOLD), true);
		ShrineHooks.RESTED.invoker().onRested(player, world, pos);
	}

	private ShrineRest() {}
}
