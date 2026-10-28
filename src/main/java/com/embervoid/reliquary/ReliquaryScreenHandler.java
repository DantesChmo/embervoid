package com.embervoid.reliquary;

import com.embervoid.Embervoid;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Reliquary menu: the accessory slots (kind after kind, positions from {@link ReliquaryLayout}) followed by the
 * player's main inventory and hotbar. The server backs the accessory slots with the player's {@link Reliquary}; the
 * client gets a plain inventory that vanilla keeps in sync. Slot counts travel in the screen opening data, so both
 * sides build the same slots.
 */
public class ReliquaryScreenHandler extends ScreenHandler {
	public static final ExtendedScreenHandlerType<ReliquaryScreenHandler> TYPE = Registry.register(
			Registries.SCREEN_HANDLER, Embervoid.id("reliquary"), new ExtendedScreenHandlerType<>(ReliquaryScreenHandler::new));

	public record KindCount(SlotKind kind, int count) {}

	private final List<KindCount> counts;
	private final Inventory accessories;
	private final int accessoryCount;

	/** Client side: counts from the opening data. */
	public ReliquaryScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
		this(syncId, playerInventory, readCounts(buf), null);
	}

	ReliquaryScreenHandler(int syncId, PlayerInventory playerInventory, List<KindCount> counts, @Nullable Inventory accessories) {
		super(TYPE, syncId);
		this.counts = List.copyOf(counts);
		int total = counts.stream().mapToInt(KindCount::count).sum();
		this.accessories = accessories != null ? accessories : new SimpleInventory(total);
		this.accessoryCount = total;

		List<ReliquaryLayout.Pos> positions = ReliquaryLayout.slots(counts.stream().map(KindCount::count).toList());
		int index = 0;
		for (KindCount count : counts) {
			for (int i = 0; i < count.count(); i++, index++) {
				ReliquaryLayout.Pos pos = positions.get(index);
				addSlot(new AccessorySlot(count.kind(), this.accessories, index, pos.x(), pos.y()));
			}
		}
		for (int i = 0; i < 36; i++) {
			ReliquaryLayout.Pos pos = ReliquaryLayout.inventorySlot(i);
			// Layout index 0–26 is the main inventory (player slots 9–35), 27–35 the hotbar (player slots 0–8).
			addSlot(new Slot(playerInventory, i < 27 ? i + 9 : i - 27, pos.x(), pos.y()));
		}
	}

	public List<KindCount> counts() {
		return counts;
	}

	public int accessoryCount() {
		return accessoryCount;
	}

	/** The kind of accessory slot {@code index}, or null for an inventory slot. */
	@Nullable
	public SlotKind kindOf(int index) {
		return index >= 0 && index < accessoryCount && slots.get(index) instanceof AccessorySlot slot ? slot.kind : null;
	}

	/** Slot counts of the current config, as the opening data carries them. */
	static List<KindCount> currentCounts() {
		List<KindCount> counts = new ArrayList<>();
		for (SlotKind kind : SlotKinds.ALL) {
			counts.add(new KindCount(kind, kind.count()));
		}
		return counts;
	}

	static void writeCounts(PacketByteBuf buf) {
		List<KindCount> counts = currentCounts();
		buf.writeVarInt(counts.size());
		for (KindCount count : counts) {
			buf.writeString(count.kind().name());
			buf.writeVarInt(count.count());
		}
	}

	private static List<KindCount> readCounts(PacketByteBuf buf) {
		List<KindCount> counts = new ArrayList<>();
		int size = buf.readVarInt();
		for (int i = 0; i < size; i++) {
			SlotKind kind = SlotKinds.byName(buf.readString());
			int count = buf.readVarInt();
			if (kind == null) {
				throw new IllegalStateException("Unknown Reliquary slot kind from the server");
			}
			counts.add(new KindCount(kind, count));
		}
		return counts;
	}

	private boolean holdsElsewhere(Item item, Slot except) {
		for (int i = 0; i < accessoryCount; i++) {
			Slot slot = slots.get(i);
			if (slot != except && slot.getStack().isOf(item)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasStack()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getStack();
		ItemStack original = stack.copy();
		int mainStart = accessoryCount;
		int hotbarStart = mainStart + 27;
		int end = hotbarStart + 9;
		if (index < accessoryCount) {
			if (!insertItem(stack, mainStart, end, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!equipOne(stack)) {
			// Not an accessory (or no free slot): move between main inventory and hotbar, like vanilla.
			boolean moved = index < hotbarStart
					? insertItem(stack, hotbarStart, end, false)
					: insertItem(stack, mainStart, hotbarStart, false);
			if (!moved) {
				return ItemStack.EMPTY;
			}
		}
		if (stack.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTakeItem(player, stack);
		return original;
	}

	/** Puts one item of {@code stack} into the first free accessory slot that takes it. */
	private boolean equipOne(ItemStack stack) {
		for (int i = 0; i < accessoryCount; i++) {
			Slot slot = slots.get(i);
			if (!slot.hasStack() && slot.canInsert(stack)) {
				slot.setStack(stack.split(1));
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return accessories.canPlayerUse(player);
	}

	/** One accessory slot: takes one item of its kind that no other accessory slot holds. */
	private final class AccessorySlot extends Slot {
		private final SlotKind kind;

		AccessorySlot(SlotKind kind, Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
			this.kind = kind;
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return kind.accepts(stack) && !holdsElsewhere(stack.getItem(), this);
		}

		@Override
		public int getMaxItemCount() {
			return 1;
		}

		@Override
		public Pair<Identifier, Identifier> getBackgroundSprite() {
			return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, kind.emptyIcon());
		}
	}
}
