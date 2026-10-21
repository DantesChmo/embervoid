package com.embervoid.client.mob;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidClientConfig;
import com.embervoid.mob.MobLevels;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Level display (PLAN 3.7): keeps the levels the server sends for tracked entities, finds the leveled entity under
 * the crosshair (up to {@link #LOOK_RANGE} blocks, stopped by blocks), and builds the "Name Lv. N" label that
 * {@code EntityRendererMobLevelMixin} draws above it. Mobs that already show a name tag get the level appended.
 */
public final class MobLevelClient {
	private static final double LOOK_RANGE = 24;

	/** @param showMobLevel show levels next to the names of looked-at mobs */
	public record Hud(boolean showMobLevel) {
		public static final ConfigSection<Hud> SECTION = EmbervoidClientConfig.register(ConfigSection.of(
				"hud", new Hud(true),
				in -> new Hud(in.readBoolean("showMobLevel", true)),
				(value, out) -> out.addProperty("showMobLevel", value.showMobLevel())));
	}

	private static int lookedAt = -1;

	public static boolean enabled() {
		return EmbervoidClientConfig.get(Hud.SECTION).showMobLevel();
	}

	/** Whether to draw a level label above {@code entity} although vanilla shows no name tag for it. */
	public static boolean isLookedAt(Entity entity) {
		return entity.getId() == lookedAt && enabled() && !entity.isInvisible() && MobLevels.getLevel(entity) > 0;
	}

	/** {@code name} followed by the entity's level, or {@code name} unchanged if it has none or display is off. */
	public static Text withLevel(Entity entity, Text name) {
		int level = MobLevels.getLevel(entity);
		if (level <= 0 || !enabled()) {
			return name;
		}
		return Text.empty().append(name).append(" ")
				.append(Text.translatable("embervoid.mob_level", level).formatted(Formatting.GOLD));
	}

	private static void updateLookedAt(MinecraftClient client) {
		lookedAt = -1;
		Entity camera = client.getCameraEntity();
		if (camera == null || client.world == null || !enabled()) {
			return;
		}
		Vec3d eye = camera.getCameraPosVec(1.0f);
		Vec3d look = camera.getRotationVec(1.0f);
		HitResult block = camera.raycast(LOOK_RANGE, 1.0f, false);
		double maxSquared = block.getType() == HitResult.Type.MISS ? LOOK_RANGE * LOOK_RANGE : block.getPos().squaredDistanceTo(eye);
		Box box = camera.getBoundingBox().stretch(look.multiply(LOOK_RANGE)).expand(1.0);
		EntityHitResult hit = ProjectileUtil.raycast(camera, eye, eye.add(look.multiply(LOOK_RANGE)), box,
				entity -> !entity.isSpectator() && entity.canHit() && MobLevels.getLevel(entity) > 0, maxSquared);
		if (hit != null) {
			lookedAt = hit.getEntity().getId();
		}
	}

	public static void register() {
		// Registers the "hud" section now, so a fresh embervoid-client.json lists it.
		EmbervoidClientConfig.get(Hud.SECTION);
		ClientPlayNetworking.registerGlobalReceiver(MobLevels.SYNC_PACKET, (client, handler, buf, responseSender) -> {
			int entityId = buf.readVarInt();
			int level = buf.readVarInt();
			client.execute(() -> MobLevels.ClientLevels.put(entityId, level));
		});
		ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> MobLevels.ClientLevels.remove(entity.getId()));
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(MobLevels.ClientLevels::clear));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(MobLevels.ClientLevels::clear));
		ClientTickEvents.END_CLIENT_TICK.register(MobLevelClient::updateLookedAt);
	}

	private MobLevelClient() {}
}
