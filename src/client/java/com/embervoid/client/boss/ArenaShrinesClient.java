package com.embervoid.client.boss;

import com.embervoid.boss.ArenaShrines;
import com.embervoid.sanctum.EmberShrineBlock;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Client side of {@link ArenaShrines}: keeps the arena shrines the server named for the current dimension and draws
 * the ones whose boss the local player has not beaten cold.
 */
public final class ArenaShrinesClient {
	private static List<ArenaShrines.Entry> shrines = List.of();
	private static Identifier dimension;

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ArenaShrines.PACKET, (client, handler, buf, responseSender) -> {
			Identifier world = buf.readIdentifier();
			int count = buf.readVarInt();
			List<ArenaShrines.Entry> received = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				received.add(new ArenaShrines.Entry(buf.readBlockPos(), buf.readIdentifier()));
			}
			client.execute(() -> {
				dimension = world;
				shrines = List.copyOf(received);
			});
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> shrines = List.of()));
		EmberShrineBlock.coldForViewer = pos -> {
			MinecraftClient client = MinecraftClient.getInstance();
			return client.player != null && client.world != null && !shrines.isEmpty()
					&& client.world.getRegistryKey().getValue().equals(dimension)
					&& ArenaShrines.isCold(client.player, shrines, pos);
		};
	}

	private ArenaShrinesClient() {}
}
