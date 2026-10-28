package com.embervoid.client.reliquary;

import com.embervoid.Embervoid;
import com.embervoid.client.realm.RealmFog;
import com.embervoid.reliquary.Reliquary;
import com.embervoid.reliquary.ReliquaryLayout;
import com.embervoid.reliquary.ReliquaryManager;
import com.embervoid.reliquary.ReliquaryScreenHandler;
import com.embervoid.reliquary.RingEffects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Client side of the Reliquary: the synced copy of the local player's accessories, the screen, the button next to
 * the recipe book on the inventory screen and the keybind (default R), both asking the server to open the menu; and
 * the client half of the rings.
 */
public final class ReliquaryClient {
	public static final KeyBinding OPEN_KEY = new KeyBinding("key.embervoid.reliquary", InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_R, "key.categories.embervoid");

	/** Fog API source of the Ring of the Fog Stalker. */
	private static final Identifier FOG_STALKER = Embervoid.id("ring/fog_stalker");
	private static float fogClearing = 1;

	public static void register() {
		KeyBindingHelper.registerKeyBinding(OPEN_KEY);
		HandledScreens.register(ReliquaryScreenHandler.TYPE, ReliquaryScreen::new);

		ClientPlayNetworking.registerGlobalReceiver(ReliquaryManager.SYNC_PACKET, (client, handler, buf, responseSender) -> {
			Reliquary reliquary = Reliquary.read(buf);
			client.execute(() -> {
				if (client.player != null) {
					ReliquaryManager.set(client.player, reliquary);
					RingEffects.updateSprint(client.player);
				}
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_KEY.wasPressed()) {
				if (client.player != null && client.currentScreen == null) {
					requestOpen();
				}
			}
			tickRings(client.player);
		});

		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen)) {
				return;
			}
			ClickableWidget recipeButton = recipeBookButton(screen);
			if (recipeButton == null) {
				return;
			}
			ReliquaryButton button = new ReliquaryButton();
			follow(button, recipeButton);
			Screens.getButtons(screen).add(button);
			// The recipe book button moves when the book opens or closes; keep ours beside it.
			ScreenEvents.beforeRender(screen).register((s, context, mouseX, mouseY, delta) -> follow(button, recipeButton));
		});
	}

	/**
	 * Client side of the rings that act on the local player's movement and view: Ash Feathers caps the fall speed
	 * (the client moves its own player), the Fog Stalker pushes the realm fog out through the fog API.
	 */
	private static void tickRings(@Nullable ClientPlayerEntity player) {
		float clearing = player != null ? RingEffects.fogClearing(player) : 1;
		if (clearing != fogClearing) {
			fogClearing = clearing;
			RealmFog.setClearing(FOG_STALKER, clearing);
		}
		if (player == null) {
			return;
		}
		double maxFall = RingEffects.maxFallSpeed(player);
		Vec3d velocity = player.getVelocity();
		if (maxFall > 0 && velocity.y < -maxFall) {
			player.setVelocity(velocity.x, -maxFall, velocity.z);
		}
	}

	public static void requestOpen() {
		ClientPlayNetworking.send(ReliquaryManager.OPEN_PACKET, PacketByteBufs.empty());
	}

	/** The inventory screen's only textured button is the recipe book toggle. */
	@Nullable
	private static ClickableWidget recipeBookButton(Screen screen) {
		for (ClickableWidget widget : Screens.getButtons(screen)) {
			if (widget instanceof TexturedButtonWidget) {
				return widget;
			}
		}
		return null;
	}

	private static void follow(ButtonWidget button, ClickableWidget recipeButton) {
		ReliquaryLayout.Pos pos = ReliquaryLayout.inventoryButton(recipeButton.getX(), recipeButton.getY());
		button.setPosition(pos.x(), pos.y());
	}

	/** The inventory screen button: a vanilla button with the Reliquary icon. */
	private static final class ReliquaryButton extends ButtonWidget {
		private static final Identifier ICON = Embervoid.id("textures/gui/reliquary_button.png");

		ReliquaryButton() {
			super(0, 0, ReliquaryLayout.BUTTON_WIDTH, ReliquaryLayout.BUTTON_HEIGHT, Text.empty(),
					b -> requestOpen(), DEFAULT_NARRATION_SUPPLIER);
			setTooltip(Tooltip.of(Text.translatable("embervoid.reliquary.button",
					OPEN_KEY.getBoundKeyLocalizedText())));
		}

		@Override
		protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
			super.renderButton(context, mouseX, mouseY, delta);
			context.drawTexture(ICON, getX() + 2, getY() + 1, 0, 0, 16, 16, 16, 16);
		}
	}

	private ReliquaryClient() {}
}
