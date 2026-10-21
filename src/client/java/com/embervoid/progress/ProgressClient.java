package com.embervoid.progress;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client side of the {@code progress} API: keeps the local player's synced copy. */
public final class ProgressClient {
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ProgressManager.SYNC_PACKET, (client, handler, buf, responseSender) -> {
			PlayerProgress progress = PlayerProgress.read(buf);
			client.execute(() -> {
				if (client.player != null) {
					ProgressManager.set(client.player, progress);
				}
			});
		});
	}

	private ProgressClient() {}
}
