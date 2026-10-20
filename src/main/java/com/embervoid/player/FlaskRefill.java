package com.embervoid.player;

import com.embervoid.item.HealingFlaskItem;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Refills every healing flask in a player's inventory after a full night's sleep. */
public final class FlaskRefill {
	private static final long DAY_LENGTH = 24000;
	/** Vanilla wakes sleepers at dawn about here; earlier than this in the same day means they got up at night. */
	private static final long DAWN = 23000;
	/** Vanilla only counts a sleeper toward skipping the night after this many ticks in bed. */
	private static final int MIN_SLEEP_TICKS = 100;

	public static void register() {
		EntitySleepEvents.START_SLEEPING.register((entity, pos) -> {
			if (entity instanceof ServerPlayerEntity player) {
				// Sleeping is also allowed during daytime thunderstorms; that is not a night's rest.
				long timeOfDay = player.getWorld().getTimeOfDay();
				boolean night = timeOfDay % DAY_LENGTH >= 12000;
				PlayerDataManager.get(player).sleepStartTime = night ? timeOfDay : -1;
			}
		});
		// Fires before vanilla resets the sleep timer, so getSleepTimer still tells how long they slept.
		EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
			if (!(entity instanceof ServerPlayerEntity player)) {
				return;
			}
			PlayerData data = PlayerDataManager.get(player);
			long start = data.sleepStartTime;
			data.sleepStartTime = -1;
			if (start >= 0 && player.getSleepTimer() >= MIN_SLEEP_TICKS && isMorning(start, player.getWorld().getTimeOfDay())) {
				refillAll(player);
			}
		});
	}

	/**
	 * True if the night is over: either it was skipped (time jumped to the next day) or the player slept until dawn.
	 * World.isDay() is not used because on the tick the night is skipped it still reports night.
	 */
	private static boolean isMorning(long sleepStart, long now) {
		return now / DAY_LENGTH > sleepStart / DAY_LENGTH || now % DAY_LENGTH >= DAWN;
	}

	private static void refillAll(ServerPlayerEntity player) {
		PlayerInventory inventory = player.getInventory();
		boolean refilled = false;
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.getItem() instanceof HealingFlaskItem) {
				HealingFlaskItem.refill(stack, player);
				refilled = true;
			}
		}
		if (refilled) {
			player.sendMessage(Text.translatable("embervoid.flask.refilled").formatted(Formatting.GOLD), true);
		}
	}

	private FlaskRefill() {}
}
