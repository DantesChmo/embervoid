package com.embervoid.boss;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Arena fog (DESIGN §4.5): the server tells a fighter's client to push or clear a fog layer. The client applies it
 * through the realm fog API (PLAN 9.1) as fog the Ring of the Fog Stalker does not thin out.
 */
public final class ArenaFog {
	/** S2C: source id, density (float, 0–1; negative clears the source), colour (int, 0xRRGGBB). */
	public static final Identifier PACKET = Embervoid.id("arena_fog");

	/** Fog source of an arena: {@code embervoid:arena/<boss id path>}. */
	public static Identifier source(Identifier bossId) {
		return Embervoid.id("arena/" + bossId.getPath());
	}

	public static void push(ServerPlayerEntity player, Identifier source, float density, int color) {
		send(player, source, Math.max(0, Math.min(1, density)), color);
	}

	public static void clear(ServerPlayerEntity player, Identifier source) {
		send(player, source, -1, 0);
	}

	private static void send(ServerPlayerEntity player, Identifier source, float density, int color) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeIdentifier(source);
		buf.writeFloat(density);
		buf.writeInt(color);
		ServerPlayNetworking.send(player, PACKET, buf);
	}

	private ArenaFog() {}
}
