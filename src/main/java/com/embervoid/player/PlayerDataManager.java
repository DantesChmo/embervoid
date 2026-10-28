package com.embervoid.player;

import com.embervoid.Embervoid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** Stores {@link PlayerData} on players and syncs it to the owning client. */
public final class PlayerDataManager {
	/** S2C: the full {@link PlayerData} of the receiving player. */
	public static final Identifier SYNC_PACKET = Embervoid.id("player_data");

	private static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PlayerData.FIELDS_CODEC.forGetter(data -> data),
			FlaskKeeper.Kept.CODEC.listOf().optionalFieldOf("kept_on_death", List.of()).forGetter(data -> List.copyOf(data.keptOnDeath))
	).apply(instance, (data, kept) -> {
		data.keptOnDeath.addAll(kept);
		return data;
	}));

	// Persistent (saved in player NBT) and copied on death; dimension changes keep the same player entity,
	// and the End exit portal respawns the player, which copies attachments as well.
	@SuppressWarnings("UnstableApiUsage")
	public static final AttachmentType<PlayerData> PLAYER_DATA = AttachmentRegistry.<PlayerData>builder()
			.persistent(CODEC)
			.initializer(PlayerData::createDefault)
			.copyOnDeath()
			.buildAndRegister(Embervoid.id("player_data"));

	/**
	 * Client side: the local player's synced data, kept outside the player entity because the client recreates that
	 * entity on respawn and dimension change. Set by the client initializer; null on a dedicated server.
	 */
	@Nullable
	private static Supplier<PlayerData> clientData;
	@Nullable
	private static Supplier<PlayerEntity> clientPlayer;

	/**
	 * The player's data. On the server, the attachment; on a client, the synced copy for the local player and
	 * defaults for anyone else (other players' data is not sent).
	 */
	@SuppressWarnings("UnstableApiUsage")
	public static PlayerData get(PlayerEntity player) {
		if (player.getWorld().isClient && clientData != null) {
			return player == clientPlayer.get() ? clientData.get() : PlayerData.createDefault();
		}
		return player.getAttachedOrCreate(PLAYER_DATA);
	}

	/** Client side: the local player, or null when there is none (dedicated server, main menu). */
	@Nullable
	public static PlayerEntity getLocalPlayer() {
		return clientPlayer != null ? clientPlayer.get() : null;
	}

	/** Called once by the client initializer. */
	public static void setClientAccess(Supplier<PlayerData> data, Supplier<PlayerEntity> player) {
		clientData = data;
		clientPlayer = player;
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
