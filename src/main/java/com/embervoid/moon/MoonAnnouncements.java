package com.embervoid.moon;

import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights.EndReason;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * Title and chat line when a moon night starts and ends. Every player gets the chat line; players in the overworld
 * (where the night happens) also get the title.
 */
final class MoonAnnouncements {
	static void started(MinecraftServer server, MoonNight night) {
		Formatting color = night == MoonNight.BLOOD_NEW_MOON ? Formatting.DARK_RED : Formatting.GOLD;
		announce(server,
				Text.translatable("embervoid.moon.title." + night.id()).formatted(color),
				Text.translatable("embervoid.moon.subtitle." + night.id()),
				Text.translatable("embervoid.moon.started." + night.id()).formatted(color));
	}

	static void ended(MinecraftServer server, MoonNight night, EndReason reason) {
		Text chat = Text.translatable("embervoid.moon.ended." + night.id());
		if (reason == EndReason.TIMEOUT) {
			chat = Text.translatable("embervoid.moon.ended.timeout", chat);
		}
		announce(server,
				Text.translatable("embervoid.moon.title.dawn").formatted(Formatting.YELLOW),
				Text.empty(),
				chat.copy().formatted(Formatting.GRAY));
	}

	private static void announce(MinecraftServer server, Text title, Text subtitle, Text chat) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(chat, false);
			if (player.getWorld().getRegistryKey() == World.OVERWORLD) {
				player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
				player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
				player.networkHandler.sendPacket(new TitleS2CPacket(title));
			}
		}
	}

	private MoonAnnouncements() {}
}
