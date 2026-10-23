package com.embervoid.player;

import com.embervoid.item.HealingFlaskItem;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * When the player's flask charges come back: after a night skipped while they were in bed, and on respawn after
 * death (souls-style, dying sends you back rested). Also hands out the starting flask on first join.
 */
public final class FlaskRefill {
	private static final long DAY_LENGTH = 24000;
	/** Vanilla allows sleeping from about here (or in a thunderstorm); before it, it is not a night's rest. */
	private static final long NIGHT_START = 12000;

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> HealingFlaskItem.giveStartingFlask(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				HealingFlaskItem.refill(newPlayer);
			}
		});

		EntitySleepEvents.START_SLEEPING.register((entity, pos) -> {
			if (entity instanceof ServerPlayerEntity player) {
				// Sleeping is also allowed during daytime thunderstorms; that is not a night's rest.
				long timeOfDay = player.getWorld().getTimeOfDay();
				PlayerDataManager.get(player).sleepStartTime = timeOfDay % DAY_LENGTH >= NIGHT_START ? timeOfDay : -1;
			}
		});
		EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
			if (!(entity instanceof ServerPlayerEntity player)) {
				return;
			}
			PlayerData data = PlayerDataManager.get(player);
			long start = data.sleepStartTime;
			data.sleepStartTime = -1;
			if (start >= 0 && nightSkipped(start, player.getWorld().getTimeOfDay())) {
				HealingFlaskItem.refill(player);
				player.sendMessage(Text.translatable("embervoid.flask.refilled").formatted(Formatting.GOLD), true);
				player.playSound(PlayerSounds.FLASK_REFILL, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
		});
	}

	/**
	 * True if the day rolled over while the player was in bed. Sleeping is only possible at night, and vanilla
	 * wakes sleepers at dawn of the same day, so the day only changes in bed when the night was skipped (by this
	 * player's sleep or by enough other sleepers: vanilla sets the time to the next morning, then wakes everyone).
	 * Getting up early, or being woken by daylight, stays on the same day. With {@code doDaylightCycle} off the
	 * time does not jump, so there is no refill.
	 */
	static boolean nightSkipped(long sleepStart, long now) {
		return now / DAY_LENGTH > sleepStart / DAY_LENGTH;
	}

	private FlaskRefill() {}
}
