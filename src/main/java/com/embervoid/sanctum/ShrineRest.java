package com.embervoid.sanctum;

import com.embervoid.item.HealingFlaskItem;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** What resting at an Ember Shrine does, and the hook for systems that add to it. */
public final class ShrineRest {
	/**
	 * Fired on the server after a player rested at a shrine. The realms track sets the realm respawn point here
	 * (milestone 6); {@code player.getServerWorld()} is the shrine's world.
	 */
	public static final Event<Rested> RESTED = EventFactory.createArrayBacked(Rested.class,
			listeners -> (player, shrinePos) -> {
				for (Rested listener : listeners) {
					listener.onRested(player, shrinePos);
				}
			});

	@FunctionalInterface
	public interface Rested {
		void onRested(ServerPlayerEntity player, BlockPos shrinePos);
	}

	static void rest(ServerPlayerEntity player, BlockPos pos) {
		refillFlasks(player);
		player.getServerWorld().playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 0.6f, 0.8f);
		player.sendMessage(Text.translatable("embervoid.shrine.rested").formatted(Formatting.GOLD), true);
		RESTED.invoker().onRested(player, pos);
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
