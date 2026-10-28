package com.embervoid.mixin.client;

import com.embervoid.client.realm.RealmFog;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Realm fog overrides ({@link RealmFog}): fog colour and terrain fog distance in the open air. */
@Mixin(BackgroundRenderer.class)
abstract class RealmFogMixin {
	@Shadow
	private static float red;
	@Shadow
	private static float green;
	@Shadow
	private static float blue;

	@Inject(method = "render", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V"))
	private static void embervoid$realmFogColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance,
			float skyDarkness, CallbackInfo ci) {
		RealmFog.frame();
		if (camera.getSubmersionType() != CameraSubmersionType.NONE) {
			return;
		}
		float[] rgb = {red, green, blue};
		if (RealmFog.tint(rgb)) {
			red = rgb[0];
			green = rgb[1];
			blue = rgb[2];
		}
	}

	@Inject(method = "applyFog", at = @At("TAIL"))
	private static void embervoid$realmFogDistance(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance,
			boolean thickFog, float tickDelta, CallbackInfo ci) {
		if (fogType != BackgroundRenderer.FogType.FOG_TERRAIN
				|| camera.getSubmersionType() != CameraSubmersionType.NONE) {
			return;
		}
		float[] startEnd = {RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd()};
		if (RealmFog.distances(startEnd)) {
			RenderSystem.setShaderFogStart(startEnd[0]);
			RenderSystem.setShaderFogEnd(startEnd[1]);
		}
	}
}
