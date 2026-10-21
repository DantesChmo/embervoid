package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** GeckoLib renderer for the Ashen Maiden: her ember and eyes glow in the dark. */
public class AshenMaidenRenderer extends GeoEntityRenderer<AshenMaidenEntity> {
	public AshenMaidenRenderer(EntityRendererFactory.Context context) {
		super(context, new DefaultedEntityGeoModel<>(Embervoid.id("ashen_maiden"), true));
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
		shadowRadius = 0.5f;
	}
}
