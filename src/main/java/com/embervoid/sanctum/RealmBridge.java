package com.embervoid.sanctum;

import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.realm.api.ShrineHooks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * The Sanctum's only link to the realms track's {@code com.embervoid.realm.api} (COORDINATION.md contracts
 * {@code ShrineHooks} and {@code RealmTeleports}).
 */
final class RealmBridge {
	/** False for dormant arena shrines. */
	static boolean allowRest(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		return ShrineHooks.ALLOW_REST.invoker().allowRest(player, world, pos);
	}

	/** Sets the realm respawn point. */
	static void rested(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		ShrineHooks.RESTED.invoker().onRested(player, world, pos);
	}

	/** Returns false if the realm cannot be entered yet, so the obelisk says "the way is still forming". */
	static boolean enterRealm(ServerPlayerEntity player, Identifier realm, boolean toLastShrine) {
		return RealmTeleports.enterRealm(player, realm, toLastShrine);
	}

	static boolean hasRestedShrine(ServerPlayerEntity player, Identifier realm) {
		return RealmTeleports.hasRestedShrine(player, realm);
	}

	private RealmBridge() {}
}
