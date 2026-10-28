package com.embervoid.mixin;

import com.embervoid.reach.gear.GearArmor;
import com.embervoid.reach.gear.GearWeapons;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
abstract class GearEnchantmentHelperMixin {
	// Drowned Sabatons: walking in water at full speed, as with Depth Strider III.
	@Inject(method = "getDepthStrider", at = @At("RETURN"), cancellable = true)
	private static void embervoid$drownedSabatons(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
		if (GearArmor.wears(entity, GearArmor.DROWNED_SABATONS)) {
			cir.setReturnValue(Math.max(3, cir.getReturnValue()));
		}
	}

	// The enchanting table offers the realm melee weapons the sword enchantments they take (GearWeapons).
	@WrapOperation(method = "getPossibleEntries", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/enchantment/EnchantmentTarget;isAcceptableItem(Lnet/minecraft/item/Item;)Z"))
	private static boolean embervoid$realmWeaponsOnTheTable(EnchantmentTarget target, Item item, Operation<Boolean> original,
			@Local Enchantment enchantment) {
		return original.call(target, item) || GearWeapons.takesSwordEnchantment(enchantment, item);
	}
}
