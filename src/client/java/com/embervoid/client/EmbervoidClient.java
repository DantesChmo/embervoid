package com.embervoid.client;

import com.embervoid.Embervoid;
import com.embervoid.item.HealingFlaskItem;
import com.embervoid.player.PlayerData;
import com.embervoid.registry.ModItems;
import com.embervoid.player.PlayerDataManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.text.Text;

public class EmbervoidClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Entity renderers, HUD (boss bar), keybinds.
		ClientStamina.register();
		FabricModelPredicateProviderRegistry.register(ModItems.HEALING_FLASK, Embervoid.id("empty"),
				(stack, world, entity, seed) -> HealingFlaskItem.getMaxCharges(stack) > 0 && HealingFlaskItem.getCharges(stack) == 0 ? 1 : 0);

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
