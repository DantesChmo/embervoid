package com.embervoid.mixin.client;

import com.embervoid.client.fx.CameraShake;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraShakeMixin {
	@Shadow
	public abstract float getYaw();

	@Shadow
	public abstract float getPitch();

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	// After the camera has placed itself for the frame: the view turns, the player's own facing does not.
	@Inject(method = "update", at = @At("TAIL"))
	private void embervoid$shake(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta,
			CallbackInfo ci) {
		float[] offsets = CameraShake.offsets(tickDelta);
		if (offsets != null) {
			setRotation(getYaw() + offsets[0], getPitch() + offsets[1]);
		}
	}
}
