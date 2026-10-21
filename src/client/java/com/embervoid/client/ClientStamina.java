package com.embervoid.client;

import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.StaminaManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/** The local player's stamina as last sent by the server; read by the HUD. */
public final class ClientStamina {
	private static int max = 100;
	private static long lastFailedSpendTime = Long.MIN_VALUE;

	public static float current() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.player == null ? max : PlayerDataManager.get(client.player).stamina;
	}

	public static int max() {
		return max;
	}

	/** World time of the last failed spend, for flashing the bar. */
	public static long lastFailedSpendTime() {
		return lastFailedSpendTime;
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(StaminaManager.SYNC_PACKET, (client, handler, buf, responseSender) -> {
			float stamina = buf.readFloat();
			int newMax = buf.readVarInt();
			boolean failedSpend = buf.readBoolean();
			client.execute(() -> {
				max = newMax;
				if (client.player != null) {
					PlayerDataManager.get(client.player).stamina = stamina;
					if (failedSpend) {
						lastFailedSpendTime = client.player.getWorld().getTime();
					}
				}
			});
		});
	}

	private ClientStamina() {}
}
