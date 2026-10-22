package com.embervoid.client.moon;

import com.embervoid.moon.MoonSync;
import com.embervoid.moon.api.MoonNight;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Client side of the moon nights; the one call from {@code EmbervoidClient}. Holds the night the server last sent
 * and fades a red tint over sky, fog and moon (the mixins in {@code mixin.client} ask {@link #tint}) on the blood
 * new moon, in the overworld only.
 */
public final class MoonClient {
	/** Ticks for the tint to fade fully in or out. */
	private static final float FADE_TICKS = 100;

	private static MoonNight night = MoonNight.NONE;
	private static float tint;
	private static float lastTint;

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(MoonSync.PACKET, (client, handler, buf, responseSender) -> {
			MoonNight received = MoonNight.byOrdinal(buf.readVarInt());
			client.execute(() -> night = received);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			night = MoonNight.NONE;
			tint = lastTint = 0;
		}));
		ClientTickEvents.END_CLIENT_TICK.register(MoonClient::tick);
	}

	public static MoonNight night() {
		return night;
	}

	private static void tick(MinecraftClient client) {
		lastTint = tint;
		boolean red = night == MoonNight.BLOOD_NEW_MOON && client.world != null
				&& client.world.getRegistryKey() == World.OVERWORLD;
		tint = MathHelper.clamp(tint + (red ? 1 : -1) / FADE_TICKS, 0, 1);
		if (client.world == null) {
			tint = lastTint = 0;
		}
	}

	/** How red the sky is right now, 0–1. */
	public static float tint(float tickDelta) {
		return MathHelper.lerp(tickDelta, lastTint, tint);
	}

	/** The sky colour, reddened by {@link #tint}. */
	public static Vec3d tintSky(Vec3d color, float tickDelta) {
		float t = tint(tickDelta);
		if (t <= 0) {
			return color;
		}
		return new Vec3d(
				MathHelper.lerp(t, color.x, color.x * 0.5 + 0.22),
				MathHelper.lerp(t, color.y, color.y * 0.2),
				MathHelper.lerp(t, color.z, color.z * 0.2));
	}

	/** Fog colour {@code rgb} (changed in place), reddened by {@link #tint}. */
	public static void tintFog(float[] rgb, float tickDelta) {
		float t = tint(tickDelta);
		if (t <= 0) {
			return;
		}
		rgb[0] = MathHelper.lerp(t, rgb[0], rgb[0] * 0.5f + 0.16f);
		rgb[1] = MathHelper.lerp(t, rgb[1], rgb[1] * 0.25f);
		rgb[2] = MathHelper.lerp(t, rgb[2], rgb[2] * 0.25f);
	}

	private MoonClient() {}
}
