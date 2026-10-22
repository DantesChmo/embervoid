package com.embervoid.mixin;

import com.embervoid.reliquary.RingEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reliquary rings (PLAN 11.12) on living entities; only players wear rings. See {@link RingEffects}. */
@Mixin(LivingEntity.class)
abstract class ReliquaryLivingEntityMixin {
	// Courier's Ring: its speed modifier follows sprinting, on both sides like vanilla's.
	@Inject(method = "setSprinting", at = @At("TAIL"))
	private void embervoid$courierSprint(boolean sprinting, CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity player) {
			RingEffects.updateSprint(player);
		}
	}

	// Ring of Ash Feathers: longer falls before any damage.
	@ModifyVariable(method = "computeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float embervoid$featherFall(float fallDistance) {
		return fallDistance - RingEffects.ignoredFallDistance((LivingEntity) (Object) this);
	}

	// Drowned Man's Ring: no air lost under water.
	@Inject(method = "canBreatheInWater", at = @At("HEAD"), cancellable = true)
	private void embervoid$drownedBreath(CallbackInfoReturnable<Boolean> cir) {
		if (RingEffects.breathesUnderwater((LivingEntity) (Object) this)) {
			cir.setReturnValue(true);
		}
	}
}
