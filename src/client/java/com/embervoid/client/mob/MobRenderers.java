package com.embervoid.client.mob;

import com.embervoid.Embervoid;
import com.embervoid.entity.mob.AshGhoulEntity;
import com.embervoid.entity.mob.EmberWispEntity;
import com.embervoid.entity.mob.HollowEntity;
import com.embervoid.entity.mob.MobEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * GeckoLib renderers for the overworld mobs. Models, animations and textures follow GeckoLib's default layout:
 * {@code geo/entity/<name>.geo.json}, {@code animations/entity/<name>.animation.json},
 * {@code textures/entity/<name>.png}, plus {@code <name>_glowmask.png} for the parts that glow in the dark.
 */
public final class MobRenderers {
	public static void register() {
		EntityRendererRegistry.register(MobEntities.ASH_GHOUL, context -> {
			GeoEntityRenderer<AshGhoulEntity> renderer =
					new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<>(Embervoid.id("ash_ghoul"), true)) {
						@Override
						protected float getDeathMaxRotation(AshGhoulEntity ghoul) {
							return 0; // its own death animation (PLAN 19.3)
						}
					};
			renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
			return renderer;
		});
		EntityRendererRegistry.register(MobEntities.HOLLOW, MobRenderers::hollow);
		EntityRendererRegistry.register(MobEntities.EMBER_WISP, MobRenderers::emberWisp);
	}

	/** Slightly larger than a player, with its weapon drawn at the {@code right_item} bone. */
	private static GeoEntityRenderer<HollowEntity> hollow(EntityRendererFactory.Context context) {
		GeoEntityRenderer<HollowEntity> renderer =
				new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<>(Embervoid.id("hollow"), true)) {
					@Override
					protected float getDeathMaxRotation(HollowEntity hollow) {
						return 0; // its own death animation (PLAN 19.3)
					}
				};
		renderer.withScale(1.1f);
		renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
		renderer.addRenderLayer(new BlockAndItemGeoLayer<>(renderer) {
			@Nullable
			@Override
			protected ItemStack getStackForBone(GeoBone bone, HollowEntity hollow) {
				return bone.getName().equals("right_item") && !hollow.getMainHandStack().isEmpty() ? hollow.getMainHandStack() : null;
			}

			@Override
			protected ModelTransformationMode getTransformTypeForStack(GeoBone bone, ItemStack stack, HollowEntity hollow) {
				return ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
			}

			@Override
			protected void renderStackForBone(MatrixStack poseStack, GeoBone bone, ItemStack stack, HollowEntity hollow,
					VertexConsumerProvider bufferSource, float partialTick, int packedLight, int packedOverlay) {
				// The blade lies in the Hollow's front-back plane; turning about the bone's X axis swings it within that
				// plane, and a larger angle turns it from up towards the back. Measured in game: +30 points the blade
				// ~35 degrees behind vertical, +125 ~30 degrees below horizontal pointing back. A half turn from +125
				// reverses it: blade in front, ~30 degrees above horizontal.
				poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55));
				super.renderStackForBone(poseStack, bone, stack, hollow, bufferSource, partialTick, packedLight, packedOverlay);
			}
		});
		return renderer;
	}

	/** Translucent, always fully lit. */
	private static GeoEntityRenderer<EmberWispEntity> emberWisp(EntityRendererFactory.Context context) {
		GeoEntityRenderer<EmberWispEntity> renderer = new GeoEntityRenderer<>(context,
				new DefaultedEntityGeoModel<>(Embervoid.id("ember_wisp"))) {
			@Override
			public RenderLayer getRenderType(EmberWispEntity wisp, Identifier texture,
					@Nullable VertexConsumerProvider bufferSource, float partialTick) {
				return RenderLayer.getEntityTranslucent(texture);
			}

			@Override
			protected int getBlockLight(EmberWispEntity wisp, BlockPos pos) {
				return 15;
			}
		};
		renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
		return renderer;
	}

	private MobRenderers() {}
}
