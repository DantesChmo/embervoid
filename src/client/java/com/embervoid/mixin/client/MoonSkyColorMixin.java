package com.embervoid.mixin.client;

import com.embervoid.client.moon.MoonClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blood new moon: red sky. */
@Mixin(ClientWorld.class)
abstract class MoonSkyColorMixin {
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void embervoid$bloodSky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
		cir.setReturnValue(MoonClient.tintSky(cir.getReturnValue(), tickDelta));
	}
}
