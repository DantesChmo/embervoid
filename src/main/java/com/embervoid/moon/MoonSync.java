package com.embervoid.moon;

import com.embervoid.Embervoid;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Tells clients which moon night is on, for the blood-red sky. Sent on join, respawn, dimension change
 * and whenever a night starts or ends; the client decides from its own dimension whether to tint.
 */
public final class MoonSync {
	/** S2C: {@link MoonNight} ordinal (varint). */
	public static final Identifier PACKET = Embervoid.id("moon_night");

	static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer(), MoonNights.current(server)));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				send(newPlayer, MoonNights.current(newPlayer.server)));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
				send(player, MoonNights.current(player.server)));
		MoonNights.NIGHT_STARTED.register((server, night) -> sendAll(server, night));
		MoonNights.NIGHT_ENDED.register((server, night, reason) -> sendAll(server, MoonNight.NONE));
	}

	private static void sendAll(MinecraftServer server, MoonNight night) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			send(player, night);
		}
	}

	private static void send(ServerPlayerEntity player, MoonNight night) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(night.ordinal());
		ServerPlayNetworking.send(player, PACKET, buf);
	}

	private MoonSync() {}
}
