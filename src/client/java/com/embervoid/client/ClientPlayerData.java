package com.embervoid.client;

import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * The local player's {@link PlayerData} as last sent by the server. Kept here rather than on the player entity, which
 * the client recreates on respawn and dimension change; {@link PlayerDataManager#get} reads it on the client.
 */
public final class ClientPlayerData {
	private static PlayerData data = PlayerData.createDefault();

	public static PlayerData get() {
		return data;
	}

	static void register() {
		PlayerDataManager.setClientAccess(ClientPlayerData::get, () -> MinecraftClient.getInstance().player);
		ClientPlayNetworking.registerGlobalReceiver(PlayerDataManager.SYNC_PACKET, (client, handler, buf, responseSender) -> {
			PlayerData received = PlayerData.read(buf);
			client.execute(() -> data = received);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> data = PlayerData.createDefault()));
	}

	private ClientPlayerData() {}
}
