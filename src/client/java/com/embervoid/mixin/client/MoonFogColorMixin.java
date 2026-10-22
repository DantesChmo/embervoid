package com.embervoid.mixin.client;

import com.embervoid.client.moon.MoonClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blood new moon: red fog and clear colour, except under water, lava or powder snow (PLAN 8.3). */
@Mixin(BackgroundRenderer.class)
abstract class MoonFogColorMixin {
	@Shadow
	private static float red;
	@Shadow
	private static float green;
	@Shadow
	private static float blue;

	@Inject(method = "render", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V"))
	private static void embervoid$bloodFog(Camera camera, float tickDelta, ClientWorld world, int viewDistance,
			float skyDarkness, CallbackInfo ci) {
		if (camera.getSubmersionType() != CameraSubmersionType.NONE) {
			return;
		}
		float[] rgb = {red, green, blue};
		MoonClient.tintFog(rgb, tickDelta);
		red = rgb[0];
		green = rgb[1];
		blue = rgb[2];
	}
}
