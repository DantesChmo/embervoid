package com.embervoid.client.nightmob;

import com.embervoid.Embervoid;
import com.embervoid.nightmob.AshenZombieEntity;
import com.embervoid.nightmob.GhostEntity;
import com.embervoid.nightmob.NightMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Client side of the full moon's mobs: GeckoLib renderers. Assets follow GeckoLib's default layout (see
 * {@code MobRenderers}); models and textures come from {@code tools/overworld_mob_art.py}.
 */
public final class NightMobClient {
	public static void register() {
		EntityRendererRegistry.register(NightMobs.ASHEN_ZOMBIE, context -> {
			GeoEntityRenderer<AshenZombieEntity> renderer =
					new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<>(Embervoid.id("ashen_zombie"), true)) {
						@Override
						protected float getDeathMaxRotation(AshenZombieEntity zombie) {
							return 0; // its own death animation (PLAN 19.3)
						}
					};
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			return renderer;
		});
		EntityRendererRegistry.register(NightMobs.GHOST, NightMobClient::ghost);
	}

	/** Translucent and faintly lit, so it reads as a ghost in the dark and inside walls' edges. */
	private static GeoEntityRenderer<GhostEntity> ghost(EntityRendererFactory.Context context) {
		GeoEntityRenderer<GhostEntity> renderer = new GeoEntityRenderer<>(context,
				new DefaultedEntityGeoModel<>(Embervoid.id("ghost"))) {
			@Override
			public RenderLayer getRenderType(GhostEntity ghost, Identifier texture,
					@Nullable VertexConsumerProvider bufferSource, float partialTick) {
				return RenderLayer.getEntityTranslucent(texture);
			}

			@Override
			protected int getBlockLight(GhostEntity ghost, BlockPos pos) {
				return Math.max(8, super.getBlockLight(ghost, pos));
			}
		};
		renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
		return renderer;
	}

	private NightMobClient() {}
}
