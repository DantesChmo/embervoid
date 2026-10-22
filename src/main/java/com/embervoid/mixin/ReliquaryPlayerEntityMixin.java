package com.embervoid.mixin;

import com.embervoid.reliquary.RingEffects;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reliquary rings (PLAN 11.12) on players. See {@link RingEffects}. */
@Mixin(PlayerEntity.class)
abstract class ReliquaryPlayerEntityMixin {
	// Delver's Ring: Haste on realm ores; both sides compute the breaking speed.
	@Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
	private void embervoid$delverHaste(BlockState state, CallbackInfoReturnable<Float> cir) {
		float multiplier = RingEffects.miningMultiplier((PlayerEntity) (Object) this, state);
		if (multiplier != 1) {
			cir.setReturnValue(cir.getReturnValue() * multiplier);
		}
	}
}
