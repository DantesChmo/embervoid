package com.embervoid.reliquary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A player's accessories: the stacks in their Reliquary slots, by slot kind name and slot index. Lives on the player
 * as its own data attachment ({@link ReliquaryManager}). Mutate through {@link ReliquaryManager} on the server so
 * effects and the client follow; reads are safe on both sides.
 *
 * <p>Kinds are kept by name, so a save never loses items when a kind or slot goes away: those items are simply
 * outside the current slots ({@link ReliquaryManager#slots()}) until they are handed back.
 */
public final class Reliquary {
	/** One saved stack. */
	private record Entry(String kind, int slot, ItemStack stack) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("kind").forGetter(Entry::kind),
				Codec.INT.fieldOf("slot").forGetter(Entry::slot),
				ItemStack.CODEC.fieldOf("stack").forGetter(Entry::stack)
		).apply(instance, Entry::new));
	}

	public static final Codec<Reliquary> CODEC = Entry.CODEC.listOf().xmap(Reliquary::fromEntries, Reliquary::entries);

	private final Map<String, List<ItemStack>> stacks = new LinkedHashMap<>();

	public static Reliquary createDefault() {
		return new Reliquary();
	}

	public ItemStack get(SlotKind kind, int slot) {
		return get(kind.name(), slot);
	}

	public ItemStack get(String kind, int slot) {
		List<ItemStack> list = stacks.get(kind);
		return list != null && slot >= 0 && slot < list.size() ? list.get(slot) : ItemStack.EMPTY;
	}

	/** Puts {@code stack} into a slot, replacing what was there. No checks: callers decide what fits. */
	public void set(SlotKind kind, int slot, ItemStack stack) {
		set(kind.name(), slot, stack);
	}

	void set(String kind, int slot, ItemStack stack) {
		if (slot < 0) {
			return;
		}
		List<ItemStack> list = stacks.computeIfAbsent(kind, k -> new ArrayList<>());
		while (list.size() <= slot) {
			list.add(ItemStack.EMPTY);
		}
		list.set(slot, stack);
	}

	/** Whether any slot holds {@code item}. */
	public boolean contains(Item item) {
		return stacks.values().stream().flatMap(List::stream).anyMatch(stack -> stack.isOf(item));
	}

	/** Whether any slot of {@code kind} holds {@code item}. */
	public boolean contains(SlotKind kind, Item item) {
		return stacks.getOrDefault(kind.name(), List.of()).stream().anyMatch(stack -> stack.isOf(item));
	}

	/** Non-empty stacks of one kind, in slot order (including slots past the current count). */
	public List<ItemStack> items(SlotKind kind) {
		return stacks.getOrDefault(kind.name(), List.of()).stream().filter(stack -> !stack.isEmpty()).toList();
	}

	public boolean isEmpty() {
		return stacks.values().stream().flatMap(List::stream).allMatch(ItemStack::isEmpty);
	}

	/** Kind names that hold stacks, including kinds that no longer exist. */
	Iterable<String> kindNames() {
		return List.copyOf(stacks.keySet());
	}

	int size(String kind) {
		return stacks.getOrDefault(kind, List.of()).size();
	}

	public Reliquary copy() {
		Reliquary copy = new Reliquary();
		stacks.forEach((kind, list) -> copy.stacks.put(kind, new ArrayList<>(list.stream().map(ItemStack::copy).toList())));
		return copy;
	}

	private static Reliquary fromEntries(List<Entry> entries) {
		Reliquary reliquary = new Reliquary();
		for (Entry entry : entries) {
			reliquary.set(entry.kind, entry.slot, entry.stack);
		}
		return reliquary;
	}

	private List<Entry> entries() {
		List<Entry> entries = new ArrayList<>();
		stacks.forEach((kind, list) -> {
			for (int i = 0; i < list.size(); i++) {
				if (!list.get(i).isEmpty()) {
					entries.add(new Entry(kind, i, list.get(i)));
				}
			}
		});
		return entries;
	}

	public void write(PacketByteBuf buf) {
		List<Entry> entries = entries();
		buf.writeVarInt(entries.size());
		for (Entry entry : entries) {
			buf.writeString(entry.kind);
			buf.writeVarInt(entry.slot);
			buf.writeItemStack(entry.stack);
		}
	}

	public static Reliquary read(PacketByteBuf buf) {
		Reliquary reliquary = new Reliquary();
		int size = buf.readVarInt();
		for (int i = 0; i < size; i++) {
			reliquary.set(buf.readString(), buf.readVarInt(), buf.readItemStack());
		}
		return reliquary;
	}

	@Override
	public String toString() {
		StringBuilder text = new StringBuilder();
		for (Entry entry : entries()) {
			if (!text.isEmpty()) {
				text.append(", ");
			}
			text.append(entry.kind).append('[').append(entry.slot).append("]=").append(entry.stack);
		}
		return text.isEmpty() ? "empty" : text.toString();
	}
}
