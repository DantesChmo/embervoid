package com.embervoid.mixin;

import com.embervoid.moon.FullMoonMobs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Full moon: a hostile does not lose a player target that stays within its follow range (PLAN 8.4). */
@Mixin(TrackTargetGoal.class)
abstract class TrackTargetGoalMixin {
	@Shadow
	@Final
	protected MobEntity mob;

	@Shadow
	protected abstract double getFollowRange();

	@Inject(method = "shouldContinue", at = @At("RETURN"), cancellable = true)
	private void embervoid$keepTargetOnFullMoon(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			return;
		}
		// Only a target the mob still has: one cleared on purpose (e.g. an Ember Wisp fleeing light) stays cleared.
		LivingEntity current = mob.getTarget();
		if (current != null && FullMoonMobs.keepsTarget(mob, current, getFollowRange())) {
			cir.setReturnValue(true);
		}
	}
}
