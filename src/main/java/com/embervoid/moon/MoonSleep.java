package com.embervoid.moon;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * Beds in the overworld refuse during a moon night (and on its evening), with a message. The respawn point is still
 * set, as vanilla does when a bed refuses at daytime.
 */
final class MoonSleep {
	static void register() {
		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
			if (!(player instanceof ServerPlayerEntity serverPlayer)
					|| player.getWorld().getRegistryKey() != World.OVERWORLD
					|| !MoonNightManager.sleepBlocked(serverPlayer.server)) {
				return null;
			}
			serverPlayer.setSpawnPoint(World.OVERWORLD, pos, serverPlayer.getYaw(), false, true);
			player.sendMessage(Text.translatable("embervoid.moon.no_sleep"), true);
			return PlayerEntity.SleepFailureReason.OTHER_PROBLEM;
		});
	}

	private MoonSleep() {}
}
