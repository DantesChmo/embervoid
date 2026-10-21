package com.embervoid.sanctum;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** What talking to the Ashen Maiden does. */
final class MaidenTalk {
	static void talk(ServerPlayerEntity player, AshenMaidenEntity maiden) {
		// The level-up screen needs player-core's levelUp and stat API; until then she only greets.
		player.sendMessage(Text.translatable("embervoid.maiden.greeting").formatted(Formatting.GOLD), false);
	}

	private MaidenTalk() {}
}
