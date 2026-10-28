package com.embervoid.client.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.client.realm.RealmFog;
import com.embervoid.reach.world.ReachZones;
import com.embervoid.realm.fog.FogStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/** Per-zone fog: the zone of the biome under the camera, pushed as the {@code embervoid:zone} source. */
public final class ZoneFog {
	public static final Identifier SOURCE = Embervoid.id("zone");

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ZoneFog::tick);
	}

	private static void tick(MinecraftClient client) {
		if (client.world == null || client.gameRenderer == null) {
			RealmFog.clear(SOURCE);
			return;
		}
		FogStack.Layer fog = client.world.getBiome(client.gameRenderer.getCamera().getBlockPos()).getKey()
				.map(ReachZones::fog).orElse(null);
		if (fog == null) {
			RealmFog.clear(SOURCE);
		} else {
			RealmFog.push(SOURCE, fog.density(), fog.color(), fog.clearable());
		}
	}

	private ZoneFog() {}
}
