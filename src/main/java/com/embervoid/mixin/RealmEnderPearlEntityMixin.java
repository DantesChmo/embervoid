package com.embervoid.mixin;

import com.embervoid.realm.FinalZoneLock;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The final zone's lock: a locked-out player's pearl drops where it flies into the final zone. */
@Mixin(EnderPearlEntity.class)
abstract class RealmEnderPearlEntityMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void embervoid$finalZoneLock(CallbackInfo ci) {
		FinalZoneLock.pearlTick((EnderPearlEntity) (Object) this);
	}
}
