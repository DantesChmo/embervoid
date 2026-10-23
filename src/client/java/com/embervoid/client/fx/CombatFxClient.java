package com.embervoid.client.fx;

import com.embervoid.fx.CombatFx;
import com.embervoid.fx.ShakeCurve;
import com.embervoid.fx.SwingArc;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.math.Vec3d;

/**
 * Client side of the combat effects ({@link CombatFx}, PLAN 19.2): receives the packets and runs camera shake
 * ({@link CameraShake}, applied by the camera mixin), swing trails ({@link SwingTrails}) and hit flashes
 * ({@link HitFlashes}). Time is counted in client ticks that stop while the game is paused.
 */
public final class CombatFxClient {
	private static int ticks;

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(CombatFx.SHAKE_PACKET, (client, handler, buf, responseSender) -> {
			float strength = buf.readFloat();
			int duration = buf.readVarInt();
			boolean located = buf.readBoolean();
			Vec3d source = located ? new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
			float radius = located ? buf.readFloat() : 0;
			client.execute(() -> {
				if (client.player == null) {
					return;
				}
				float felt = source == null ? strength : strength * ShakeCurve.falloff(client.player.getEyePos().distanceTo(source), radius);
				CameraShake.start(felt, duration, ticks);
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(CombatFx.TRAIL_PACKET, (client, handler, buf, responseSender) -> {
			int entityId = buf.readVarInt();
			float yaw = buf.readFloat();
			Vec3d fallback = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
			SwingArc arc = SwingArc.read(buf);
			client.execute(() -> SwingTrails.start(entityId, yaw, fallback, arc, ticks));
		});
		ClientPlayNetworking.registerGlobalReceiver(CombatFx.FLASH_PACKET, (client, handler, buf, responseSender) -> {
			Vec3d pos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
			float size = buf.readFloat();
			int color = buf.readInt();
			client.execute(() -> {
				if (client.world != null) {
					HitFlashes.flash(client.world, pos, size, color);
				}
			});
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null || client.isPaused()) {
				return;
			}
			ticks++;
			CameraShake.tick(ticks);
			SwingTrails.tick(client.world, ticks);
		});
		WorldRenderEvents.AFTER_TRANSLUCENT.register(SwingTrails::render);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			CameraShake.clear();
			SwingTrails.clear();
		}));
	}

	/** Client ticks counted so far (not while paused). */
	static int ticks() {
		return ticks;
	}

	private CombatFxClient() {}
}
