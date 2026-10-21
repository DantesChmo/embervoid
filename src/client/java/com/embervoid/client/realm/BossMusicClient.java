package com.embervoid.client.realm;

import com.embervoid.boss.BossMusic;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** Loops the boss music the server asks for, keeping the normal music quiet meanwhile. */
final class BossMusicClient {
	@Nullable
	private static SoundInstance playing;

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(BossMusic.PACKET, (client, handler, buf, responseSender) -> {
			String sound = buf.readString();
			client.execute(() -> play(client, sound.isEmpty() ? null : Identifier.tryParse(sound)));
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (playing != null) {
				client.getMusicTracker().stop();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> play(client, null)));
	}

	private static void play(MinecraftClient client, @Nullable Identifier sound) {
		if (playing != null) {
			client.getSoundManager().stop(playing);
			playing = null;
		}
		if (sound != null) {
			client.getMusicTracker().stop();
			playing = new PositionedSoundInstance(sound, SoundCategory.MUSIC, 1.0f, 1.0f, SoundInstance.createRandom(), true, 0,
					SoundInstance.AttenuationType.NONE, 0, 0, 0, true);
			client.getSoundManager().play(playing);
		}
	}

	private BossMusicClient() {}
}
