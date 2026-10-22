package com.embervoid.client.reach.mob;

import com.embervoid.Embervoid;
import com.embervoid.reach.mob.FireFlaskEntity;
import com.embervoid.reach.mob.ReachMob;
import com.embervoid.reach.mob.ReachMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * GeckoLib renderers for the Realm 1 mobs. Assets follow GeckoLib's default layout (see {@code MobRenderers});
 * greybox models come from {@code tools/mob_placeholders.py}. Bones a mob shows only at times
 * ({@link ReachMob#toggledBones}) are hidden or shown every frame.
 */
public final class ReachMobClient {
	public static void register() {
		renderer(ReachMobs.CINDER_HUSK, true);
		renderer(ReachMobs.ASH_STALKER, true);
		renderer(ReachMobs.ASHEN_CARRION, true);
		renderer(ReachMobs.WITHERED_THRALL, true);
		renderer(ReachMobs.LANTERN_HAG, true);
		renderer(ReachMobs.BARK_HOLLOW, false);
		renderer(ReachMobs.RESTLESS_BONES, true);
		renderer(ReachMobs.CRYPT_MOTH, true);
		renderer(ReachMobs.HOLLOW_SENTINEL, true);
		renderer(ReachMobs.GRAVE_WARDEN, true);
		// The flask flies as an item; once it is a fire patch it is invisible and only its particles show.
		EntityRendererRegistry.register(ReachMobs.FIRE_FLASK, context -> new FlyingItemEntityRenderer<FireFlaskEntity>(context) {
			@Override
			public void render(FireFlaskEntity flask, float yaw, float tickDelta, MatrixStack matrices,
					VertexConsumerProvider vertices, int light) {
				if (!flask.isInvisible()) {
					super.render(flask, yaw, tickDelta, matrices, vertices, light);
				}
			}
		});
	}

	/** A GeckoLib renderer with the glowmask layer; {@code turnsHead}: the {@code head} bone follows the look. */
	private static <T extends ReachMob> void renderer(EntityType<T> type, boolean turnsHead) {
		String name = Registries.ENTITY_TYPE.getId(type).getPath();
		EntityRendererRegistry.register(type, context -> {
			GeoEntityRenderer<T> renderer = new GeoEntityRenderer<>(context, new Model<>(name, turnsHead));
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			return renderer;
		});
	}

	private static class Model<T extends ReachMob> extends DefaultedEntityGeoModel<T> {
		Model(String name, boolean turnsHead) {
			super(Embervoid.id(name), turnsHead);
		}

		@Override
		public void setCustomAnimations(T mob, long instanceId, AnimationState<T> state) {
			super.setCustomAnimations(mob, instanceId, state);
			for (String name : mob.toggledBones()) {
				CoreGeoBone bone = getAnimationProcessor().getBone(name);
				if (bone != null) {
					bone.setHidden(!mob.showsBone(name));
				}
			}
		}
	}

	private ReachMobClient() {}
}
