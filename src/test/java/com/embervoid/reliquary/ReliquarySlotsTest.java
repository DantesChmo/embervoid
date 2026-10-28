package com.embervoid.reliquary;

import com.embervoid.reliquary.ReliquaryManager.SlotRef;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slot rules (DESIGN §4.5) on stacks named by strings, "" being an empty slot. */
class ReliquarySlotsTest {
	private static final SlotKind ARTIFACT = new SlotKind("artifact", new Identifier("test", "artifacts"), 2);
	private static final SlotKind RING = new SlotKind("ring", new Identifier("test", "rings"), 2);
	private static final List<SlotKind> KINDS = List.of(ARTIFACT, RING);

	private final Map<String, List<String>> stored = new LinkedHashMap<>();
	private final ReliquarySlots.Store<String> store = new ReliquarySlots.Store<>() {
		@Override
		public Iterable<String> kindNames() {
			return stored.keySet();
		}

		@Override
		public int size(String kind) {
			return stored.getOrDefault(kind, List.of()).size();
		}

		@Override
		public String get(String kind, int slot) {
			List<String> list = stored.getOrDefault(kind, List.of());
			return slot < list.size() ? list.get(slot) : "";
		}
	};

	private void put(String kind, int slot, String stack) {
		List<String> list = stored.computeIfAbsent(kind, k -> new ArrayList<>());
		while (list.size() <= slot) {
			list.add("");
		}
		list.set(slot, stack);
	}

	private static ToIntFunction<SlotKind> counts(int artifacts, int rings) {
		return kind -> kind == ARTIFACT ? artifacts : rings;
	}

	private List<String> leftovers(int artifacts, int rings) {
		return ReliquarySlots.leftovers(store, KINDS, counts(artifacts, rings), String::isEmpty);
	}

	private boolean canTake(SlotKind kind, int slot, String stack) {
		List<SlotRef> slots = ReliquarySlots.slots(KINDS, counts(2, 2));
		return ReliquarySlots.canTake(store, slots, kind, slot, 2, stack::equals);
	}

	@Test
	void slotsComeKindAfterKind() {
		assertEquals(List.of(new SlotRef(ARTIFACT, 0), new SlotRef(ARTIFACT, 1), new SlotRef(RING, 0)),
				ReliquarySlots.slots(KINDS, counts(2, 1)));
	}

	@Test
	void aKindWithNoSlotsIsLeftOut() {
		assertEquals(List.of(new SlotRef(RING, 0)), ReliquarySlots.slots(KINDS, counts(0, 1)));
	}

	@Test
	void stacksInCurrentSlotsAreNoLeftovers() {
		put("artifact", 0, "ember_heart");
		put("artifact", 1, "ash_eye");
		put("ring", 1, "couriers_ring");
		assertEquals(List.of(), leftovers(2, 2));
	}

	@Test
	void stacksPastALoweredCountAreLeftovers() {
		put("artifact", 0, "ember_heart");
		put("artifact", 1, "ash_eye");
		put("artifact", 4, "seal");
		assertEquals(List.of("ash_eye", "seal"), leftovers(1, 2));
	}

	@Test
	void aHiddenKindIsAllLeftovers() {
		put("ring", 0, "couriers_ring");
		put("ring", 1, "delvers_ring");
		assertEquals(List.of("couriers_ring", "delvers_ring"), leftovers(2, 0));
	}

	@Test
	void aKindThatNoLongerExistsIsAllLeftovers() {
		put("amulet", 0, "old_amulet");
		put("artifact", 0, "ember_heart");
		assertEquals(List.of("old_amulet"), leftovers(2, 2));
	}

	@Test
	void emptySlotsAreNoLeftovers() {
		put("artifact", 5, "seal");
		assertEquals(List.of("seal"), leftovers(1, 2));
	}

	@Test
	void anEmptyReliquaryTakesAnythingIntoAnExistingSlot() {
		assertTrue(canTake(ARTIFACT, 0, "ember_heart"));
		assertTrue(canTake(RING, 1, "couriers_ring"));
	}

	@Test
	void slotsOutsideTheCountDoNotExist() {
		assertFalse(canTake(ARTIFACT, -1, "ember_heart"));
		assertFalse(canTake(ARTIFACT, 2, "ember_heart"));
	}

	@Test
	void anAccessoryCountsOnce() {
		put("artifact", 0, "ember_heart");
		assertFalse(canTake(ARTIFACT, 1, "ember_heart"));
		assertTrue(canTake(ARTIFACT, 1, "ash_eye"));
	}

	@Test
	void theSameItemIsAlsoRefusedInAnotherKind() {
		put("ring", 0, "odd_charm");
		assertFalse(canTake(ARTIFACT, 0, "odd_charm"));
	}

	@Test
	void anAccessoryMayReplaceItself() {
		put("artifact", 0, "ember_heart");
		assertTrue(canTake(ARTIFACT, 0, "ember_heart"));
	}

	@Test
	void aCopyPastTheCurrentSlotsDoesNotBlock() {
		// A leftover is handed back on the next join; until then it is not worn.
		put("artifact", 3, "ember_heart");
		assertTrue(canTake(ARTIFACT, 0, "ember_heart"));
	}
}
