package com.embervoid.client.sanctum.boss;

import com.embervoid.Embervoid;
import com.embervoid.sanctum.boss.CryingSoulEntity;
import com.embervoid.sanctum.boss.FallenSisterEntity;
import com.embervoid.sanctum.boss.SanctumBossEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * GeckoLib renderers of the Fallen Sister and her crying souls (models from {@code tools/sanctum_boss_art.py}, with
 * glowmasks): her veil is hidden once it has burnt, and she gets no vanilla death tilt (her own death animation sinks
 * her to her knees).
 */
public final class SanctumBossClient {
	public static void register() {
		EntityRendererRegistry.register(SanctumBossEntities.FALLEN_SISTER, context -> {
			GeoEntityRenderer<FallenSisterEntity> renderer = new GeoEntityRenderer<>(context, new SisterModel()) {
				@Override
				protected float getDeathMaxRotation(FallenSisterEntity sister) {
					return 0;
				}
			};
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			return renderer;
		});
		EntityRendererRegistry.register(SanctumBossEntities.CRYING_SOUL, context -> {
			GeoEntityRenderer<CryingSoulEntity> renderer = new GeoEntityRenderer<>(context,
					new DefaultedEntityGeoModel<CryingSoulEntity>(Embervoid.id("crying_soul")));
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			return renderer;
		});
	}

	private static class SisterModel extends DefaultedEntityGeoModel<FallenSisterEntity> {
		SisterModel() {
			super(Embervoid.id("fallen_sister"), true);
		}

		@Override
		public void setCustomAnimations(FallenSisterEntity sister, long instanceId, AnimationState<FallenSisterEntity> state) {
			super.setCustomAnimations(sister, instanceId, state);
			for (String name : sister.toggledBones()) {
				CoreGeoBone bone = getAnimationProcessor().getBone(name);
				if (bone != null) {
					bone.setHidden(!sister.showsBone(name));
				}
			}
		}
	}

	private SanctumBossClient() {}
}
