package com.embervoid.client.realm;

import com.embervoid.boss.BossMusic;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Loops the boss music the server asks for, keeping the normal music quiet meanwhile. The track plays in the Master
 * category at the Boss music slider's volume ({@link BossMusicOption}), so players who keep Music at 0 % still hear it.
 */
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
			playing = new BossTrack(sound);
			client.getSoundManager().play(playing);
		}
	}

	/** The looping track; its volume follows the slider while it plays (the sound system re-reads it every tick). */
	private static final class BossTrack extends MovingSoundInstance {
		BossTrack(Identifier sound) {
			super(SoundEvent.of(sound), SoundCategory.MASTER, SoundInstance.createRandom());
			repeat = true;
			attenuationType = AttenuationType.NONE;
			relative = true;
			tick();
		}

		@Override
		public void tick() {
			volume = BossMusic.trackVolume(BossMusicOption.OPTION.getValue());
		}

		// Starts even with the slider at 0 %, so turning it up mid-fight brings the track in.
		@Override
		public boolean shouldAlwaysPlay() {
			return true;
		}
	}

	private BossMusicClient() {}
}
