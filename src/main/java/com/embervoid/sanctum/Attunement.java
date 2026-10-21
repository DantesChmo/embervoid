package com.embervoid.sanctum;

import com.embervoid.progress.ProgressManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Attunes a player the first time they reach a Sanctum core, which is detected by standing on the core's
 * {@link SanctumBlocks#SANCTUM_FLAGSTONE} floor. Attunement is per player: from then on any Sanctum greets them.
 */
public final class Attunement {
	private static final int CHECK_INTERVAL = 10;

	/** Players standing in a core at the last check, so the greeting shows once per visit. */
	private static final Set<UUID> IN_CORE = new HashSet<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % CHECK_INTERVAL != 0) {
				return;
			}
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				check(player);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> IN_CORE.remove(handler.getPlayer().getUuid()));
	}

	public static boolean isInCore(ServerPlayerEntity player) {
		return player.getWorld().getBlockState(player.getBlockPos().down()).isOf(SanctumBlocks.SANCTUM_FLAGSTONE)
				|| player.getWorld().getBlockState(player.getSteppingPos()).isOf(SanctumBlocks.SANCTUM_FLAGSTONE);
	}

	private static void check(ServerPlayerEntity player) {
		if (player.isSpectator() || !isInCore(player)) {
			IN_CORE.remove(player.getUuid());
			return;
		}
		if (!IN_CORE.add(player.getUuid())) {
			return;
		}
		if (ProgressManager.attune(player)) {
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(20, 80, 30));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(
					Text.translatable("embervoid.sanctum.attuned.subtitle").formatted(Formatting.GRAY)));
			player.networkHandler.sendPacket(new TitleS2CPacket(
					Text.translatable("embervoid.sanctum.attuned").formatted(Formatting.GOLD)));
			player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE,
					SoundCategory.PLAYERS, 1.0f, 0.7f);
		} else {
			player.sendMessage(Text.translatable("embervoid.sanctum.welcome").formatted(Formatting.GOLD), true);
		}
	}

	private Attunement() {}
}
