package com.embervoid.client.reliquary;

import com.embervoid.Embervoid;
import com.embervoid.reliquary.ReliquaryLayout;
import com.embervoid.reliquary.ReliquaryScreenHandler;
import com.embervoid.reliquary.SlotKind;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The Reliquary screen: the player model in the middle, accessory slots left and right of it (positions from
 * {@link ReliquaryLayout}), the inventory below. Background from {@link #TEXTURE} ({@code tools/reach_gui_art.py}):
 * the panel with the model alcove, and a slot sprite drawn at every slot (bronze-framed for accessory slots), since
 * slot kinds and counts are data. Closes with the inventory key or the Reliquary key.
 */
public class ReliquaryScreen extends HandledScreen<ReliquaryScreenHandler> {
	public static final Identifier TEXTURE = Embervoid.id("textures/gui/reliquary.png");
	/** Slot sprites in the texture: the vanilla slot, then the accessory slot. */
	private static final int SLOT_U = 176;
	private static final int SLOT_V = 0;
	private static final int ACCESSORY_SLOT_V = 18;
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
		context.drawTexture(TEXTURE, left, top, 0, 0, backgroundWidth, backgroundHeight);
		for (Slot slot : handler.slots) {
			int v = handler.kindOf(slot.id) != null ? ACCESSORY_SLOT_V : SLOT_V;
			context.drawTexture(TEXTURE, left + slot.x - 1, top + slot.y - 1, SLOT_U, v, ReliquaryLayout.SLOT,
					ReliquaryLayout.SLOT);
		}
		if (client != null && client.player != null) {
			int modelLeft = left + ReliquaryLayout.MODEL_LEFT;
			int modelWidth = ReliquaryLayout.MODEL_RIGHT - ReliquaryLayout.MODEL_LEFT;
			int centreX = modelLeft + modelWidth / 2;
			int feetY = top + ReliquaryLayout.MODEL_BOTTOM - 5;
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
}
