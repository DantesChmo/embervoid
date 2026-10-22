package com.embervoid.client.reach.world;

import com.embervoid.realm.Realms;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

/** The Smoldering Citadel's dark red sky (PLAN 13.1): the dusk sky of the depths, pulled towards a smouldering red. */
public final class DepthsSky {
	private static final Vec3d RED = new Vec3d(0x5A / 255.0, 0x16 / 255.0, 0x0C / 255.0);
	private static final double STRENGTH = 0.85;

	/** The sky colour of {@code world}: unchanged outside a realm's depths. */
	public static Vec3d tint(ClientWorld world, Vec3d sky) {
		if (Realms.ALL.stream().noneMatch(realm -> realm.depths().equals(world.getRegistryKey()))) {
			return sky;
		}
		return sky.lerp(RED, STRENGTH);
	}

	private DepthsSky() {}
}
