package com.embervoid.mixin;

import com.embervoid.realm.FinalZoneLock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ChorusFruitItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The final zone's lock: a chorus fruit never takes a locked-out player into the final zone. A spot inside
 * counts as a failed try, so the fruit tries another one as it does for a spot without room.
 */
@Mixin(ChorusFruitItem.class)
abstract class RealmChorusFruitItemMixin {
	@WrapOperation(method = "finishUsing", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/LivingEntity;teleport(DDDZ)Z"))
	private boolean embervoid$finalZoneLock(LivingEntity user, double x, double y, double z, boolean particles,
			Operation<Boolean> original) {
		if (user instanceof PlayerEntity player && FinalZoneLock.isLockedAt(player, x, z)) {
			return false;
		}
		return original.call(user, x, y, z, particles);
	}
}
