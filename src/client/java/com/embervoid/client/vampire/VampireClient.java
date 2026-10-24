package com.embervoid.client.vampire;

import com.embervoid.Embervoid;
import com.embervoid.vampire.VampireBossEntity;
import com.embervoid.vampire.VampireEntities;
import com.embervoid.vampire.VampireEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Client side of the blood new moon's Vampires: GeckoLib renderers (models by tools/overworld_mob_art.py, glowing eyes). */
public final class VampireClient {
	public static void register() {
		EntityRendererRegistry.register(VampireEntities.VAMPIRE, context -> {
			GeoEntityRenderer<VampireEntity> renderer =
					new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<>(Embervoid.id("vampire"), true)) {
						@Override
						protected float getDeathMaxRotation(VampireEntity vampire) {
							return 0; // its own death animation (PLAN 19.3)
						}
					};
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			return renderer;
		});
		EntityRendererRegistry.register(VampireEntities.VAMPIRE_BOSS, context -> {
			GeoEntityRenderer<VampireBossEntity> renderer =
					new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<>(Embervoid.id("vampire_boss"), true)) {
						@Override
						protected float getDeathMaxRotation(VampireBossEntity boss) {
							return 0; // its own death animation
						}
					};
			renderer.withScale(1.2f);
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			return renderer;
		});
	}

	private VampireClient() {}
}
