package com.embervoid.reliquary;

import java.util.ArrayList;
import java.util.List;

/**
 * Where things sit on the Reliquary screen, in pixels from its top-left corner. Pure, so the screen handler (both
 * sides) and the client screen share it and it can be tested without rendering.
 *
 * <p>The player model stands in a box in the middle of the upper half, like on the inventory screen. Slot kinds
 * take turns left and right of it (the first kind left, the second right, the third left again further out...); a
 * kind fills columns of up to {@link #ROWS} slots from the model outward, each column centred vertically, so the
 * inventory title below stays clear. The player inventory and hotbar sit below as on every container screen. Like the
 * vanilla inventory, the screen draws no title of its own (it would run into a third row).
 */
public final class ReliquaryLayout {
	public static final int WIDTH = 176;
	public static final int HEIGHT = 166;
	/** Slot frame size; an item is drawn 1 px inside it. */
	public static final int SLOT = 18;
	public static final int ROWS = 3;
	/** Top of the first row of the slot area (item position). */
	public static final int SLOTS_TOP = 8;

	/** The player model box. */
	public static final int MODEL_LEFT = 62;
	public static final int MODEL_TOP = 8;
	public static final int MODEL_RIGHT = 114;
	public static final int MODEL_BOTTOM = 78;

	/** Item position of the first column left and right of the model. */
	public static final int LEFT_COLUMN_X = MODEL_LEFT - SLOT;
	public static final int RIGHT_COLUMN_X = MODEL_RIGHT + 2;

	public static final int INVENTORY_X = 8;
	public static final int INVENTORY_Y = 84;
	public static final int HOTBAR_Y = 142;
	public static final int INVENTORY_TITLE_Y = INVENTORY_Y - 12;

	/** Item position of one slot (the frame is 1 px up and left). */
	public record Pos(int x, int y) {}

	/**
	 * Positions of the accessory slots, kind after kind, slot after slot.
	 *
	 * @param counts slots per kind, in kind order
	 */
	public static List<Pos> slots(List<Integer> counts) {
		List<Pos> result = new ArrayList<>();
		// Columns already used on each side: 0 left, 1 right.
		int[] columns = new int[2];
		for (int kind = 0; kind < counts.size(); kind++) {
			int side = kind % 2;
			int remaining = counts.get(kind);
			while (remaining > 0) {
				int rows = Math.min(ROWS, remaining);
				int column = columns[side]++;
				int x = side == 0 ? LEFT_COLUMN_X - column * SLOT : RIGHT_COLUMN_X + column * SLOT;
				int top = SLOTS_TOP + (ROWS - rows) * SLOT / 2;
				for (int row = 0; row < rows; row++) {
					result.add(new Pos(x, top + row * SLOT));
				}
				remaining -= rows;
			}
		}
		return result;
	}

	/** Inventory slot {@code index} (0–26 main, 27–35 hotbar) of the player inventory part. */
	public static Pos inventorySlot(int index) {
		if (index >= 27) {
			return new Pos(INVENTORY_X + (index - 27) * SLOT, HOTBAR_Y);
		}
		return new Pos(INVENTORY_X + (index % 9) * SLOT, INVENTORY_Y + (index / 9) * SLOT);
	}

	/** Size of the Reliquary button on the inventory screen (as the recipe book button). */
	public static final int BUTTON_WIDTH = 20;
	public static final int BUTTON_HEIGHT = 18;

	/**
	 * The Reliquary button on the inventory screen: right of the recipe book button, on its row (free space below
	 * the crafting grid). Follows that button when the recipe book opens and shifts the screen.
	 */
	public static Pos inventoryButton(int recipeButtonX, int recipeButtonY) {
		return new Pos(recipeButtonX + BUTTON_WIDTH + 2, recipeButtonY);
	}

	private ReliquaryLayout() {}
}
