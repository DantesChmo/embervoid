package com.embervoid.player;

import com.embervoid.Embervoid;
import com.embervoid.config.EmbervoidConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Server-side stamina: spending, regeneration after a delay, and syncing to the owning client.
 * Pool size and regen come from the {@code stamina} config section.
 */
public final class StaminaManager {
	/** S2C: current stamina (float), max stamina (varint), whether a spend just failed (boolean). */
	public static final Identifier SYNC_PACKET = Embervoid.id("stamina");

	public static int getMax() {
		return EmbervoidConfig.get().stamina().max();
	}

	public static float get(ServerPlayerEntity player) {
		return PlayerDataManager.get(player).stamina;
	}

	/**
	 * Spends stamina for an action. Souls-style: the action is allowed while any stamina is left, and the pool
	 * bottoms out at zero. Creative and spectator players never spend.
	 *
	 * @return false if the player had no stamina left; the client is told so it can show feedback
	 */
	public static boolean tryConsume(ServerPlayerEntity player, float amount) {
		if (player.getAbilities().creativeMode || player.isSpectator()) {
			return true;
		}
		PlayerData data = PlayerDataManager.get(player);
		if (data.stamina <= 0) {
			sync(player, true);
			return false;
		}
		data.stamina = Math.max(0, data.stamina - amount);
		data.staminaRegenDelay = EmbervoidConfig.get().stamina().regenDelayTicks();
		sync(player, false);
		return true;
	}

	/** Sets stamina directly (clamped to the pool), e.g. from a debug command. */
	public static void set(ServerPlayerEntity player, float value) {
		PlayerDataManager.get(player).stamina = Math.max(0, Math.min(getMax(), value));
		sync(player, false);
	}

	public static void sync(ServerPlayerEntity player, boolean failedSpend) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeFloat(get(player));
		buf.writeVarInt(getMax());
		buf.writeBoolean(failedSpend);
		ServerPlayNetworking.send(player, SYNC_PACKET, buf);
	}

	private static void tick(ServerPlayerEntity player) {
		PlayerData data = PlayerDataManager.get(player);
		int max = getMax();
		if (data.stamina > max) {
			// The configured pool shrank since this player last played.
			data.stamina = max;
			sync(player, false);
		} else if (data.staminaRegenDelay > 0) {
			data.staminaRegenDelay--;
		} else if (data.stamina < max) {
			data.stamina = Math.min(max, data.stamina + EmbervoidConfig.get().stamina().regenPerTick());
			sync(player, false);
		}
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				tick(player);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				PlayerData data = PlayerDataManager.get(newPlayer);
				data.stamina = getMax();
				data.staminaRegenDelay = 0;
			}
			sync(newPlayer, false);
		});
		// The max is not part of PlayerData, so send it whenever the client gets a fresh player entity.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer(), false));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> sync(player, false));
	}

	private StaminaManager() {}
}
