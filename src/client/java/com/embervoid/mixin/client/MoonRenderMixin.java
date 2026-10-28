package com.embervoid.mixin.client;

import com.embervoid.client.moon.MoonClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blood new moon: the moon is drawn red. The stars after it set their own colour. */
@Mixin(WorldRenderer.class)
abstract class MoonRenderMixin {
	// Ordinal 1 is the moon texture (0 is the sun).
	@Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
			at = @At(value = "INVOKE",
					target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V",
					ordinal = 1))
	private void embervoid$bloodMoon(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera,
			boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
		float t = MoonClient.tint(tickDelta);
		if (t > 0) {
			float[] color = RenderSystem.getShaderColor();
			RenderSystem.setShaderColor(color[0], MathHelper.lerp(t, color[1], 0.25f),
					MathHelper.lerp(t, color[2], 0.2f), color[3]);
		}
	}
}
