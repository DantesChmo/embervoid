package com.embervoid.sanctum;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Hook for the realms track: a player used a lit (or kindled) Void Obelisk. Return a non-PASS result if the
 * realm was handled (teleported, or a choice screen opened); if every listener passes, the player is told
 * "the way is still forming".
 */
@FunctionalInterface
public interface ObeliskActivation {
	Event<ObeliskActivation> EVENT = EventFactory.createArrayBacked(ObeliskActivation.class,
			listeners -> (player, realm, obeliskPos) -> {
				for (ObeliskActivation listener : listeners) {
					ActionResult result = listener.onActivate(player, realm, obeliskPos);
					if (result != ActionResult.PASS) {
						return result;
					}
				}
				return ActionResult.PASS;
			});

	ActionResult onActivate(ServerPlayerEntity player, Identifier realm, BlockPos obeliskPos);
}
