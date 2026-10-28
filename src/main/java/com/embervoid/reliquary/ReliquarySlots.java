package com.embervoid.reliquary;

import com.embervoid.reliquary.ReliquaryManager.SlotRef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * The Reliquary's slot rules over any kind of stored stack, with slot counts passed in, so they hold without items or
 * a config. {@link ReliquaryManager} runs them on a player's {@link Reliquary}.
 */
final class ReliquarySlots {
	/** Stacks stored by kind name and slot index, as in {@link Reliquary}. */
	interface Store<T> {
		/** Kind names that hold stacks, including kinds that no longer exist. */
		Iterable<String> kindNames();

		/** Slots stored for a kind, filled or not. */
		int size(String kind);

		T get(String kind, int slot);
	}

	/** The current slots: {@code kinds} in order, {@code count} slots of each. */
	static List<SlotRef> slots(List<SlotKind> kinds, ToIntFunction<SlotKind> count) {
		List<SlotRef> slots = new ArrayList<>();
		for (SlotKind kind : kinds) {
			for (int i = 0; i < count.applyAsInt(kind); i++) {
				slots.add(new SlotRef(kind, i));
			}
		}
		return slots;
	}

	/** Stacks outside the current slots: in kinds that are not among {@code kinds}, or past their kind's count. */
	static <T> List<T> leftovers(Store<T> store, List<SlotKind> kinds, ToIntFunction<SlotKind> count, Predicate<T> isEmpty) {
		List<T> leftovers = new ArrayList<>();
		for (String name : store.kindNames()) {
			int from = kinds.stream().filter(kind -> kind.name().equals(name)).findFirst()
					.map(count::applyAsInt).orElse(0);
			for (int i = from; i < store.size(name); i++) {
				T stack = store.get(name, i);
				if (!isEmpty.test(stack)) {
					leftovers.add(stack);
				}
			}
		}
		return leftovers;
	}

	/**
	 * Whether slot {@code slot} of {@code kind} exists ({@code count} of them) and no other of the current
	 * {@code slots} holds a stack {@code sameItem} matches: each accessory counts once.
	 */
	static <T> boolean canTake(Store<T> store, List<SlotRef> slots, SlotKind kind, int slot, int count, Predicate<T> sameItem) {
		if (slot < 0 || slot >= count) {
			return false;
		}
		for (SlotRef ref : slots) {
			if ((ref.kind() != kind || ref.slot() != slot) && sameItem.test(store.get(ref.kind().name(), ref.slot()))) {
				return false;
			}
		}
		return true;
	}

	private ReliquarySlots() {}
}
