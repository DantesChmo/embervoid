package com.embervoid.reliquary;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReliquaryLayoutTest {
	@Test
	void defaultSlotsFlankTheModel() {
		List<ReliquaryLayout.Pos> slots = ReliquaryLayout.slots(List.of(2, 2));
		assertEquals(List.of(
				new ReliquaryLayout.Pos(44, 17), new ReliquaryLayout.Pos(44, 35),
				new ReliquaryLayout.Pos(116, 17), new ReliquaryLayout.Pos(116, 35)), slots);
	}

	@Test
	void slotsNeverOverlapTheModelInventoryOrEachOther() {
		for (int artifacts = 0; artifacts <= ReliquaryConfig.MAX_SLOTS_PER_KIND; artifacts++) {
			for (int rings = 0; rings <= ReliquaryConfig.MAX_SLOTS_PER_KIND; rings++) {
				List<ReliquaryLayout.Pos> slots = ReliquaryLayout.slots(List.of(artifacts, rings));
				assertEquals(artifacts + rings, slots.size());
				Set<ReliquaryLayout.Pos> seen = new HashSet<>();
				for (ReliquaryLayout.Pos pos : slots) {
					assertTrue(seen.add(pos), "two slots at " + pos);
					int left = pos.x() - 1;
					int top = pos.y() - 1;
					int right = left + ReliquaryLayout.SLOT;
					int bottom = top + ReliquaryLayout.SLOT;
					assertTrue(left >= 0 && right <= ReliquaryLayout.WIDTH, "off the panel: " + pos);
					assertTrue(top >= 0 && bottom <= ReliquaryLayout.INVENTORY_TITLE_Y, "into the inventory: " + pos);
					boolean overlapsModel = right > ReliquaryLayout.MODEL_LEFT - 1 && left < ReliquaryLayout.MODEL_RIGHT + 1
							&& bottom > ReliquaryLayout.MODEL_TOP - 1 && top < ReliquaryLayout.MODEL_BOTTOM + 1;
					assertFalse(overlapsModel, "over the model: " + pos);
				}
			}
		}
	}

	@Test
	void inventoryMatchesVanillaContainers() {
		assertEquals(new ReliquaryLayout.Pos(8, 84), ReliquaryLayout.inventorySlot(0));
		assertEquals(new ReliquaryLayout.Pos(152, 120), ReliquaryLayout.inventorySlot(26));
		assertEquals(new ReliquaryLayout.Pos(8, 142), ReliquaryLayout.inventorySlot(27));
	}

	@Test
	void buttonSitsRightOfTheRecipeBookButton() {
		// Vanilla: recipe book button at (x + 104, height / 2 - 22), 20×18; the crafting grid ends above it.
		ReliquaryLayout.Pos pos = ReliquaryLayout.inventoryButton(204, 98);
		assertEquals(new ReliquaryLayout.Pos(226, 98), pos);
	}
}
