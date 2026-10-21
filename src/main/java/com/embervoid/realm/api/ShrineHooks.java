package com.embervoid.realm.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Hooks for the Ember Shrine block (owned by the sanctum track). The shrine calls {@link #ALLOW_REST} before a
 * player rests and {@link #RESTED} after; the realm code uses them for realm respawn points and dormant arena shrines
 * (DESIGN §10.8).
 */
public final class ShrineHooks {
	/** Whether the player may rest at the shrine at {@code pos}. On false the shrine tells the player it is cold to them. */
	public static final Event<AllowRest> ALLOW_REST = EventFactory.createArrayBacked(AllowRest.class,
			listeners -> (player, world, pos) -> {
				for (AllowRest listener : listeners) {
					if (!listener.allowRest(player, world, pos)) {
						return false;
					}
				}
				return true;
			});

	/** The player rested at the shrine at {@code pos} (after the shrine's own effects, e.g. refilled flasks). */
	public static final Event<Rested> RESTED = EventFactory.createArrayBacked(Rested.class,
			listeners -> (player, world, pos) -> {
				for (Rested listener : listeners) {
					listener.onRested(player, world, pos);
				}
			});

	@FunctionalInterface
	public interface AllowRest {
		boolean allowRest(ServerPlayerEntity player, ServerWorld world, BlockPos pos);
	}

	@FunctionalInterface
	public interface Rested {
		void onRested(ServerPlayerEntity player, ServerWorld world, BlockPos pos);
	}

	private ShrineHooks() {}
}
