package com.embervoid.client.nightmob;

import com.embervoid.Embervoid;
import com.embervoid.nightmob.AshenZombieEntity;
import com.embervoid.nightmob.NightMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Client side of the full moon's mobs: GeckoLib renderers. Assets follow GeckoLib's default layout (see
 * {@code MobRenderers}); placeholders come from {@code tools/nightmob_placeholders.py}.
 */
public final class NightMobClient {
	public static void register() {
		EntityRendererRegistry.register(NightMobs.ASHEN_ZOMBIE, context -> {
			GeoEntityRenderer<AshenZombieEntity> renderer =
					new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<>(Embervoid.id("ashen_zombie"), true));
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			return renderer;
		});
	}

	private NightMobClient() {}
}
