package com.embervoid.config;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Sends the server's config to each joining client, which uses it for the session (see EmbervoidConfig). */
public final class ConfigSync {
	/** S2C: the server config as compact JSON. */
	public static final Identifier SYNC_PACKET = Embervoid.id("config");

	public static void send(ServerPlayerEntity player, EmbervoidConfig config) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(EmbervoidConfig.toNetworkString(config));
		ServerPlayNetworking.send(player, SYNC_PACKET, buf);
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer(), EmbervoidConfig.get()));
	}

	private ConfigSync() {}
}
