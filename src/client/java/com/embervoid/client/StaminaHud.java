package com.embervoid.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.MathHelper;

/**
 * Stamina bar above the hunger row, drawn with plain fills (no texture). Hidden while stamina is full,
 * and flashes red for a moment when the server reports a spend that failed for lack of stamina.
 *
 * <p>Drawn from {@code InGameHudMixin} right after the vanilla status bars, so it only shows when they do
 * (not in creative or spectator) and layers under the chat like they do.
 */
public final class StaminaHud {
	private static final int WIDTH = 81;
	private static final int HEIGHT = 5;
	private static final int FLASH_TICKS = 10;

	private static final int FRAME = 0xFF1A1210;
	private static final int FRAME_FLASH = 0xFFC8321E;
	private static final int BACKGROUND = 0xFF3A2C26;
	private static final int FILL = 0xFFD8702A;
	private static final int FILL_HIGHLIGHT = 0xFFF2A65A;

	public static void render(DrawContext context) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) {
			return;
		}

		float max = ClientStamina.max();
		float current = MathHelper.clamp(ClientStamina.current(), 0, max);
		long sinceFail = player.getWorld().getTime() - ClientStamina.lastFailedSpendTime();
		boolean flashing = sinceFail >= 0 && sinceFail < FLASH_TICKS;
		if (current >= max && !flashing) {
			return;
		}

		// Same column as the hunger row (vanilla draws it at height - 39), one row above it,
		// moved further up when air bubbles or mount hearts take that space.
		int x = context.getScaledWindowWidth() / 2 + 10;
		int y = context.getScaledWindowHeight() - 39 - 10;
		if (player.getVehicle() instanceof LivingEntity mount) {
			y -= 10 * Math.max(0, MathHelper.ceil(mount.getMaxHealth() / 20.0f) - 1);
		}
		if (player.isSubmergedIn(FluidTags.WATER) || player.getAir() < player.getMaxAir()) {
			y -= 10;
		}
		y += 2;

		boolean flashOn = flashing && (sinceFail / 2) % 2 == 0;
		context.fill(x, y, x + WIDTH, y + HEIGHT, flashOn ? FRAME_FLASH : FRAME);
		context.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, BACKGROUND);
		int filled = Math.round((WIDTH - 2) * current / max);
		if (filled > 0) {
			context.fill(x + 1, y + 1, x + 1 + filled, y + HEIGHT - 1, FILL);
			context.fill(x + 1, y + 1, x + 1 + filled, y + 2, FILL_HIGHLIGHT);
		}
	}

	private StaminaHud() {}
}
