package com.embervoid.client.mob;

import com.embervoid.Embervoid;
import com.embervoid.entity.mob.AshGhoulEntity;
import com.embervoid.entity.mob.MobEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * GeckoLib renderers for the overworld mobs. Models, animations and textures follow GeckoLib's default layout:
 * {@code geo/entity/<name>.geo.json}, {@code animations/entity/<name>.animation.json},
 * {@code textures/entity/<name>.png}, plus {@code <name>_glowmask.png} for the parts that glow in the dark.
 */
public final class MobRenderers {
	public static void register() {
		EntityRendererRegistry.register(MobEntities.ASH_GHOUL, context -> {
			GeoEntityRenderer<AshGhoulEntity> renderer =
					new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<>(Embervoid.id("ash_ghoul"), true));
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			return renderer;
		});
	}

	private MobRenderers() {}
}
