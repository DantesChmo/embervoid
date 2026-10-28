package com.embervoid.client.boss;

import com.embervoid.boss.ArenaFog;
import com.embervoid.client.realm.RealmFog;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

/**
 * Client side of the arena fog ({@link ArenaFog}): pushes the fog the server asks for into the realm fog API as
 * fog that clearing (Ring of the Fog Stalker) does not thin out.
 */
public final class ArenaFogClient {
	private static final Set<Identifier> PUSHED = new HashSet<>();

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ArenaFog.PACKET, (client, handler, buf, responseSender) -> {
			Identifier source = buf.readIdentifier();
			float density = buf.readFloat();
			int color = buf.readInt();
			client.execute(() -> {
				if (density < 0) {
					clear(source);
				} else {
					push(source, density, color);
				}
			});
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> Set.copyOf(PUSHED).forEach(ArenaFogClient::clear)));
	}

	private static void push(Identifier source, float density, int color) {
		PUSHED.add(source);
		RealmFog.push(source, density, color, false);
	}

	private static void clear(Identifier source) {
		PUSHED.remove(source);
		RealmFog.clear(source);
	}

	private ArenaFogClient() {}
}
