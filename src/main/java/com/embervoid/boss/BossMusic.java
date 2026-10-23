package com.embervoid.boss;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** Boss music hooks: the server tells a fighter's client which track to loop, or to stop. */
public final class BossMusic {
	/** S2C: a sound event id to loop as music, or an empty string to stop. */
	public static final Identifier PACKET = Embervoid.id("boss_music");

	public static void play(ServerPlayerEntity player, @Nullable Identifier sound) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(sound == null ? "" : sound.toString());
		ServerPlayNetworking.send(player, PACKET, buf);
	}

	public static void stop(ServerPlayerEntity player) {
		play(player, null);
	}

	/**
	 * The volume the client gives the boss track: the Boss music slider (0..1). The track plays in the Master
	 * category, so the Music slider never mutes it.
	 */
	public static float trackVolume(double bossMusic) {
		return (float) Math.max(0.0, Math.min(1.0, bossMusic));
	}

	/** How loud the track is heard: the sound engine scales Master sounds by the Master slider alone. */
	public static float heardVolume(double master, double bossMusic) {
		return (float) Math.max(0.0, Math.min(1.0, master)) * trackVolume(bossMusic);
	}

	private BossMusic() {}
}
