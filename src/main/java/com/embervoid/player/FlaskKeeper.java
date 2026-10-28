package com.embervoid.player;

import com.embervoid.item.HealingFlaskItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Healing Flasks stay with the player through death (DESIGN §7): before anything drops or goes into the Ash Remnant,
 * every flask is taken out of the inventory and saved in {@link PlayerData#keptOnDeath}, then put back into the
 * same slot on respawn. This holds in every {@code remnant.mode} and ignores Curse of Vanishing. Saved with the
 * player, so quitting on the death screen does not lose them.
 */
public final class FlaskKeeper {
	public record Kept(int slot, ItemStack stack) {
		public static final Codec<Kept> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("slot").forGetter(Kept::slot),
				ItemStack.CODEC.fieldOf("stack").forGetter(Kept::stack)
		).apply(instance, Kept::new));
	}

	public static boolean isKeptOnDeath(ItemStack stack) {
		return stack.getItem() instanceof HealingFlaskItem;
	}

	/** Called on death before vanishing curses, the remnant and vanilla drops; takes every flask out. */
	public static void takeFlasks(ServerPlayerEntity player) {
		PlayerData data = PlayerDataManager.get(player);
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			if (isKeptOnDeath(inventory.getStack(i))) {
				data.keptOnDeath.add(new Kept(i, inventory.removeStack(i)));
			}
		}
	}

	/** Puts kept stacks back: into their old slot if it is free, else anywhere (or at the player's feet). */
	public static void restore(ServerPlayerEntity player) {
		PlayerData data = PlayerDataManager.get(player);
		if (data.keptOnDeath.isEmpty()) {
			return;
		}
		List<Kept> kept = List.copyOf(data.keptOnDeath);
		data.keptOnDeath.clear();
		PlayerInventory inventory = player.getInventory();
		for (Kept entry : kept) {
			if (entry.slot() >= 0 && entry.slot() < inventory.size() && inventory.getStack(entry.slot()).isEmpty()) {
				inventory.setStack(entry.slot(), entry.stack());
			} else {
				inventory.offerOrDrop(entry.stack());
			}
		}
	}

	public static void register() {
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> restore(newPlayer));
		// Safety net: a player who is alive with flasks still kept (e.g. the server stopped between death and
		// respawn and the player came back alive) gets them back on join.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (!handler.getPlayer().isDead()) {
				restore(handler.getPlayer());
			}
		});
	}

	private FlaskKeeper() {}
}
