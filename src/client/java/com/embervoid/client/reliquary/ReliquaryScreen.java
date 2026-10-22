package com.embervoid.client.reliquary;

import com.embervoid.reliquary.ReliquaryLayout;
import com.embervoid.reliquary.ReliquaryScreenHandler;
import com.embervoid.reliquary.SlotKind;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/**
 * The Reliquary screen: the player model in the middle, accessory slots left and right of it (positions from
 * {@link ReliquaryLayout}), the inventory below. Drawn with plain fills in the vanilla container colours; a texture
 * comes with the art pass. Closes with the inventory key or the Reliquary key.
 */
public class ReliquaryScreen extends HandledScreen<ReliquaryScreenHandler> {
	private static final int PANEL = 0xFFC6C6C6;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF555555;
	private static final int SLOT_FILL = 0xFF8B8B8B;
	private static final int SLOT_DARK = 0xFF373737;
	private static final int MODEL_FILL = 0xFF000000;
	private static final int TEXT = 0x404040;

	public ReliquaryScreen(ReliquaryScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		backgroundWidth = ReliquaryLayout.WIDTH;
		backgroundHeight = ReliquaryLayout.HEIGHT;
		playerInventoryTitleY = ReliquaryLayout.INVENTORY_TITLE_Y;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);
		super.render(context, mouseX, mouseY, delta);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int left = x;
		int top = y;
		bevel(context, left, top, backgroundWidth, backgroundHeight, PANEL, LIGHT, SHADOW);
		for (Slot slot : handler.slots) {
			bevel(context, left + slot.x - 1, top + slot.y - 1, ReliquaryLayout.SLOT, ReliquaryLayout.SLOT,
					SLOT_FILL, SLOT_DARK, LIGHT);
		}
		int modelLeft = left + ReliquaryLayout.MODEL_LEFT;
		int modelTop = top + ReliquaryLayout.MODEL_TOP;
		int modelWidth = ReliquaryLayout.MODEL_RIGHT - ReliquaryLayout.MODEL_LEFT;
		int modelHeight = ReliquaryLayout.MODEL_BOTTOM - ReliquaryLayout.MODEL_TOP;
		bevel(context, modelLeft - 1, modelTop - 1, modelWidth + 2, modelHeight + 2, MODEL_FILL, SLOT_DARK, LIGHT);
		if (client != null && client.player != null) {
			int centreX = modelLeft + modelWidth / 2;
			int feetY = modelTop + modelHeight - 5;
			InventoryScreen.drawEntity(context, centreX, feetY, 30, centreX - mouseX, feetY - 50 - mouseY, client.player);
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, TEXT, false);
	}

	/** Hovering an empty accessory slot names its kind. */
	@Override
	protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
		if (focusedSlot != null && !focusedSlot.hasStack() && handler.getCursorStack().isEmpty()) {
			SlotKind kind = handler.kindOf(focusedSlot.id);
			if (kind != null) {
				context.drawTooltip(textRenderer, Text.translatable(kind.translationKey()), mouseX, mouseY);
				return;
			}
		}
		super.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (ReliquaryClient.OPEN_KEY.matchesKey(keyCode, scanCode)) {
			close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	/** A rectangle with a 1 px bevel: light top-left, dark bottom-right (or the reverse for sunken slots). */
	private static void bevel(DrawContext context, int x, int y, int width, int height, int fill, int topLeft, int bottomRight) {
		context.fill(x, y, x + width, y + height, fill);
		context.fill(x, y, x + width - 1, y + 1, topLeft);
		context.fill(x, y, x + 1, y + height - 1, topLeft);
		context.fill(x + 1, y + height - 1, x + width, y + height, bottomRight);
		context.fill(x + width - 1, y + 1, x + width, y + height, bottomRight);
	}
}
