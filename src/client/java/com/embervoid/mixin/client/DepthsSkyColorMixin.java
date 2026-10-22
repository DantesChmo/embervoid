package com.embervoid.mixin.client;

import com.embervoid.client.reach.world.DepthsSky;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dark red sky over the realm depths (PLAN 13.1). */
@Mixin(ClientWorld.class)
abstract class DepthsSkyColorMixin {
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void embervoid$depthsSky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
		cir.setReturnValue(DepthsSky.tint((ClientWorld) (Object) this, cir.getReturnValue()));
	}
}
