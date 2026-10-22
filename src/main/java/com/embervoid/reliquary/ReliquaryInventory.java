package com.embervoid.reliquary;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Server side: the player's Reliquary slots as a vanilla {@link Inventory}, one index per {@link
 * ReliquaryManager.SlotRef}, for the screen handler. Every change goes through {@link ReliquaryManager#setStack},
 * so it is synced and its effects applied at once.
 */
final class ReliquaryInventory implements Inventory {
	private final ServerPlayerEntity player;
	private final List<ReliquaryManager.SlotRef> slots;

	ReliquaryInventory(ServerPlayerEntity player, List<ReliquaryManager.SlotRef> slots) {
		this.player = player;
		this.slots = List.copyOf(slots);
	}

	@Override
	public int size() {
		return slots.size();
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < size(); i++) {
			if (!getStack(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		if (slot < 0 || slot >= size()) {
			return ItemStack.EMPTY;
		}
		ReliquaryManager.SlotRef ref = slots.get(slot);
		return ReliquaryManager.get(player).get(ref.kind(), ref.slot());
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack stack = getStack(slot);
		if (stack.isEmpty() || amount <= 0) {
			return ItemStack.EMPTY;
		}
		if (amount >= stack.getCount()) {
			return removeStack(slot);
		}
		ItemStack rest = stack.copy();
		ItemStack taken = rest.split(amount);
		setStack(slot, rest);
		return taken;
	}

	@Override
	public ItemStack removeStack(int slot) {
		ItemStack stack = getStack(slot);
		if (!stack.isEmpty()) {
			setStack(slot, ItemStack.EMPTY);
		}
		return stack;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		if (slot < 0 || slot >= size()) {
			return;
		}
		ReliquaryManager.SlotRef ref = slots.get(slot);
		ReliquaryManager.setStack(player, ref.kind(), ref.slot(), stack);
	}

	@Override
	public int getMaxCountPerStack() {
		return 1;
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		return slot >= 0 && slot < size() && slots.get(slot).kind().accepts(stack);
	}

	/** Every change is applied as it happens. */
	@Override
	public void markDirty() {
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return player == this.player && player.isAlive();
	}

	@Override
	public void clear() {
		for (int i = 0; i < size(); i++) {
			setStack(i, ItemStack.EMPTY);
		}
	}
}
