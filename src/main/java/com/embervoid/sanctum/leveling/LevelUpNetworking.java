package com.embervoid.sanctum.leveling;

import com.embervoid.Embervoid;
import com.embervoid.sanctum.AshenMaidenEntity;
import com.embervoid.sanctum.SanctumSounds;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Level-up at the Ashen Maiden. The server opens the screen with a {@link StatSheet}; each click on a stat sends one
 * request, which the server validates (alive, near that Maiden, enough XP, below the cap) before spending XP, then
 * answers with a fresh sheet.
 */
public final class LevelUpNetworking {
	/** S2C: {@code boolean open, varint maidenId, StatSheet}. open = false only refreshes an open screen. */
	public static final Identifier SHEET = Embervoid.id("level_up_sheet");
	/** C2S: {@code varint maidenId, string statId}; {@link #REFRESH} as the stat only asks for a fresh sheet. */
	public static final Identifier LEVEL_UP = Embervoid.id("level_up");
	public static final String REFRESH = "";

	/** Opens the level-up screen for a player talking to {@code maiden}. */
	public static void open(ServerPlayerEntity player, AshenMaidenEntity maiden) {
		sendSheet(player, maiden, true);
	}

	private static void sendSheet(ServerPlayerEntity player, AshenMaidenEntity maiden, boolean open) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(open);
		buf.writeVarInt(maiden.getId());
		LevelingBridge.sheet(player).write(buf);
		ServerPlayNetworking.send(player, SHEET, buf);
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(LEVEL_UP, (server, player, handler, buf, sender) -> {
			int maidenId = buf.readVarInt();
			String statId = buf.readString(64);
			server.execute(() -> handle(player, maidenId, statId));
		});
	}

	private static void handle(ServerPlayerEntity player, int maidenId, String statId) {
		Entity entity = player.getServerWorld().getEntityById(maidenId);
		if (!player.isAlive() || !(entity instanceof AshenMaidenEntity maiden)
				|| player.squaredDistanceTo(maiden) > AshenMaidenEntity.REACH * AshenMaidenEntity.REACH) {
			return;
		}
		if (statId.equals(REFRESH)) {
			sendSheet(player, maiden, false);
			return;
		}
		Text failure = LevelingBridge.levelUp(player, statId);
		if (failure != null) {
			player.sendMessage(failure.copy().formatted(Formatting.RED), true);
		} else {
			player.getServerWorld().playSound(null, player.getBlockPos(), SanctumSounds.MAIDEN_LEVEL_UP,
					SoundCategory.PLAYERS, 0.6f, 1.4f);
		}
		sendSheet(player, maiden, false);
	}

	private LevelUpNetworking() {}
}
