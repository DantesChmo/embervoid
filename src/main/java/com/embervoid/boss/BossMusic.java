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

	private BossMusic() {}
}
