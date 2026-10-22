package com.embervoid.player;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Item stores outside the vanilla inventory that follow it on death (e.g. the Reliquary's accessories, DESIGN §4.5).
 * A system registers its store once; on death the Ash Remnant takes the stash along with the inventory (mode
 * {@code xp_and_items}), or the stash drops like the inventory (modes {@code xp} and {@code off}). Under
 * {@code keepInventory} nothing happens, so the owner keeps it as it keeps the inventory. Reclaiming the remnant puts
 * each stack back through {@link Stash#restore}; what does not fit goes to the inventory or the ground.
 */
public final class DeathStashes {
	/** One store of items a player carries outside the vanilla inventory. Server side. */
	public interface Stash {
		/**
		 * Takes every stack out, in a stable order: the position in the list is the index {@link #restore} gets back.
		 * Empty positions are {@link ItemStack#EMPTY}.
		 */
		List<ItemStack> takeAll(ServerPlayerEntity player);

		/** Puts {@code stack} back at {@code index} if that place is free and still exists; returns what did not fit. */
		ItemStack restore(ServerPlayerEntity player, int index, ItemStack stack);

		/** Whether the player has nothing in this stash. */
		boolean isEmpty(ServerPlayerEntity player);
	}

	private static final Map<Identifier, Stash> STASHES = new LinkedHashMap<>();

	/** Registers a stash under a stable id (saved in remnants). Call during mod initialization. */
	public static void register(Identifier id, Stash stash) {
		if (STASHES.putIfAbsent(id, stash) != null) {
			throw new IllegalStateException("Death stash already registered: " + id);
		}
	}

	@Nullable
	public static Stash get(Identifier id) {
		return STASHES.get(id);
	}

	/** Whether any stash holds an item. */
	public static boolean anyItems(ServerPlayerEntity player) {
		for (Stash stash : STASHES.values()) {
			if (!stash.isEmpty(player)) {
				return true;
			}
		}
		return false;
	}

	/** Empties every stash; by stash id, only stashes that held something. */
	public static Map<Identifier, List<ItemStack>> takeAll(ServerPlayerEntity player) {
		Map<Identifier, List<ItemStack>> taken = new LinkedHashMap<>();
		STASHES.forEach((id, stash) -> {
			if (!stash.isEmpty(player)) {
				taken.put(id, new ArrayList<>(stash.takeAll(player)));
			}
		});
		return taken;
	}

	/** Drops everything in every stash at the player, as vanilla drops the inventory on death. */
	static void dropAll(ServerPlayerEntity player) {
		for (List<ItemStack> stacks : takeAll(player).values()) {
			for (ItemStack stack : stacks) {
				if (!stack.isEmpty()) {
					player.dropItem(stack, true, false);
				}
			}
		}
	}

	private DeathStashes() {}
}
