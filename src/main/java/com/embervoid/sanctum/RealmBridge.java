package com.embervoid.sanctum;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * The Sanctum's only link to the realms track's {@code com.embervoid.realm.api} (COORDINATION.md contracts
 * {@code ShrineHooks} and {@code RealmTeleports}).
 *
 * <p>STUB until realms merges {@code realm.api}: then each method forwards to the real call noted on it and
 * nothing else in this package changes.
 */
final class RealmBridge {
	/** {@code ShrineHooks.ALLOW_REST.invoker().allowRest(player, world, pos)}; false for dormant arena shrines. */
	static boolean allowRest(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		return true;
	}

	/** {@code ShrineHooks.RESTED.invoker().onRested(player, world, pos)}: sets the realm respawn point. */
	static void rested(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
	}

	/**
	 * {@code RealmTeleports.enterRealm(player, realm, toLastShrine)}. Returns false if the realm cannot be entered
	 * yet (no realm framework, or the realm has no dimension), so the obelisk says "the way is still forming".
	 */
	static boolean enterRealm(ServerPlayerEntity player, Identifier realm, boolean toLastShrine) {
		return false;
	}

	/** {@code RealmTeleports.hasRestedShrine(player, realm)}. */
	static boolean hasRestedShrine(ServerPlayerEntity player, Identifier realm) {
		return false;
	}

	private RealmBridge() {}
}
