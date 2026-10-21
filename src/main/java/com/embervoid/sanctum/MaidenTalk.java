package com.embervoid.sanctum;

import com.embervoid.sanctum.leveling.LevelUpNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/** What talking to the Ashen Maiden does: opens the level-up screen. */
final class MaidenTalk {
	static void talk(ServerPlayerEntity player, AshenMaidenEntity maiden) {
		LevelUpNetworking.open(player, maiden);
	}

	private MaidenTalk() {}
}
