package com.embervoid.mixin;

import com.embervoid.reach.gear.GearArmor;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Drowned Sabatons (track reach-gear): walking in water at full speed, as with Depth Strider III. */
@Mixin(EnchantmentHelper.class)
abstract class GearEnchantmentHelperMixin {
	@Inject(method = "getDepthStrider", at = @At("RETURN"), cancellable = true)
	private static void embervoid$drownedSabatons(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
		if (GearArmor.wears(entity, GearArmor.DROWNED_SABATONS)) {
			cir.setReturnValue(Math.max(3, cir.getReturnValue()));
		}
	}
}
