package com.embervoid.player;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Stores {@link PlayerData} on players and syncs it to the owning client. */
public final class PlayerDataManager {
	/** S2C: the full {@link PlayerData} of the receiving player. */
	public static final Identifier SYNC_PACKET = Embervoid.id("player_data");

	// Persistent (saved in player NBT) and copied on death; dimension changes keep the same player entity,
	// and the End exit portal respawns the player, which copies attachments as well.
	@SuppressWarnings("UnstableApiUsage")
	public static final AttachmentType<PlayerData> PLAYER_DATA = AttachmentRegistry.<PlayerData>builder()
			.persistent(PlayerData.CODEC)
			.initializer(PlayerData::createDefault)
			.copyOnDeath()
			.buildAndRegister(Embervoid.id("player_data"));

	@SuppressWarnings("UnstableApiUsage")
	public static PlayerData get(PlayerEntity player) {
		return player.getAttachedOrCreate(PLAYER_DATA);
	}

	/** Replaces the player's data; the client uses this when a sync packet arrives. */
	@SuppressWarnings("UnstableApiUsage")
	public static void set(PlayerEntity player, PlayerData data) {
		player.setAttached(PLAYER_DATA, data);
	}

	/** Sends the player's data to their client. Call after changing it on the server. */
	public static void sync(ServerPlayerEntity player) {
		PacketByteBuf buf = PacketByteBufs.create();
		get(player).write(buf);
		ServerPlayNetworking.send(player, SYNC_PACKET, buf);
	}

	public static void register() {
		// The client creates a new player entity on join, respawn and dimension change, so resend each time.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> sync(player));
	}

	private PlayerDataManager() {}
}
