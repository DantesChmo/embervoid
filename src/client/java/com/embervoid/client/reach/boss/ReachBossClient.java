package com.embervoid.client.reach.boss;

import com.embervoid.Embervoid;
import com.embervoid.reach.boss.AshColossusEntity;
import com.embervoid.reach.boss.ReachBoss;
import com.embervoid.reach.boss.ReachBossEntities;
import com.embervoid.reach.boss.SmolderingLordEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.function.Predicate;

/**
 * GeckoLib renderers for the Ashen Reach bosses: models from {@code tools/reach_boss_art.py}, glowmask layer, an ember
 * halo ({@link EmberHaloLayer}) where a body burns, bones the boss shows only at times ({@link ReachBoss#toggledBones})
 * hidden or shown every frame, and no vanilla death tilt (every boss has its own death animation, PLAN 19.4).
 */
public final class ReachBossClient {
	public static void register() {
		renderer(ReachBossEntities.CRYPT_KEEPER, null);
		// The Colossus's core shimmers once it is bare; the Lord burns under his plate, harder once it is gone.
		renderer(ReachBossEntities.ASH_COLOSSUS, r -> new EmberHaloLayer<>(r, AshColossusEntity::coreExposed, 1.05f, 0.35f));
		renderer(ReachBossEntities.GRIEVING_TREE, null);
		renderer(ReachBossEntities.SMOLDERING_LORD, r -> new EmberHaloLayer<>(r, lord -> !lord.isOnWall(), 1.06f, 0.3f));
		EntityRendererRegistry.register(ReachBossEntities.TREE_LANTERN, TreeLanternRenderer::new);
	}

	private static <T extends ReachBoss> void renderer(EntityType<T> type, java.util.function.Function<GeoEntityRenderer<T>, GeoRenderLayer<T>> extra) {
		String name = Registries.ENTITY_TYPE.getId(type).getPath();
		EntityRendererRegistry.register(type, context -> {
			GeoEntityRenderer<T> renderer = new GeoEntityRenderer<>(context, new Model<>(name)) {
				@Override
				protected float getDeathMaxRotation(T boss) {
					return 0;
				}
			};
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			if (extra != null) {
				renderer.addRenderLayer(extra.apply(renderer));
			}
			return renderer;
		});
	}

	/**
	 * Heat off a burning body: the glowmask drawn once more over the model, scaled up a little, translucent and
	 * pulsing, so the embers get a soft halo. Only while {@code active} says the body burns.
	 */
	private static class EmberHaloLayer<T extends ReachBoss> extends GeoRenderLayer<T> {
		private final Predicate<T> active;
		private final float scale;
		private final float alpha;

		EmberHaloLayer(GeoRenderer<T> renderer, Predicate<T> active, float scale, float alpha) {
			super(renderer);
			this.active = active;
			this.scale = scale;
			this.alpha = alpha;
		}

		@Override
		public void render(MatrixStack poseStack, T boss, BakedGeoModel bakedModel, RenderLayer renderType, VertexConsumerProvider bufferSource,
				VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
			if (!active.test(boss)) {
				return;
			}
			Identifier texture = getRenderer().getGeoModel().getTextureResource(boss);
			Identifier glow = new Identifier(texture.getNamespace(), texture.getPath().replace(".png", "_glowmask.png"));
			RenderLayer halo = RenderLayer.getEntityTranslucentEmissive(glow);
			float pulse = 0.7f + 0.3f * MathHelper.sin((boss.age + partialTick) * 0.25f);
			poseStack.push();
			poseStack.scale(scale, scale, scale);
			getRenderer().reRender(bakedModel, poseStack, bufferSource, boss, halo, bufferSource.getBuffer(halo), partialTick,
					LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1, 1, 1, alpha * pulse);
			poseStack.pop();
		}
	}

	private static class Model<T extends ReachBoss> extends DefaultedEntityGeoModel<T> {
		Model(String name) {
			super(Embervoid.id(name), true);
		}

		@Override
		public void setCustomAnimations(T boss, long instanceId, AnimationState<T> state) {
			super.setCustomAnimations(boss, instanceId, state);
			for (String name : boss.toggledBones()) {
				CoreGeoBone bone = getAnimationProcessor().getBone(name);
				if (bone != null) {
					bone.setHidden(!boss.showsBone(name));
				}
			}
		}
	}

	private ReachBossClient() {}
}
