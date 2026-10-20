package com.embervoid.client;

import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;

public class EmbervoidClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Entity renderers, HUD (boss bar), keybinds.
		ClientStamina.register();

		ClientPlayNetworking.registerGlobalReceiver(PlayerDataManager.SYNC_PACKET, (client, handler, buf, responseSender) -> {
			PlayerData data = PlayerData.read(buf);
			client.execute(() -> {
				if (client.player != null) {
					PlayerDataManager.set(client.player, data);
				}
			});
		});

		// /embervoid_client data: show the player data as the client sees it (debug).
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommandManager.literal("embervoid_client")
						.then(ClientCommandManager.literal("data").executes(context -> {
							context.getSource().sendFeedback(
									Text.literal("[client] " + PlayerDataManager.get(context.getSource().getPlayer())
											+ ", maxStamina=" + ClientStamina.max()));
							return 1;
						}))));
	}
}
