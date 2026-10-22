package com.embervoid.mixin;

import com.embervoid.reach.gear.GearWeapons;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Realm melee weapons take the sword enchantments of GearWeapons on the anvil, like a sword (track reach-gear). */
@Mixin(Enchantment.class)
abstract class GearEnchantmentMixin {
	@Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
	private void embervoid$realmWeapons(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (GearWeapons.takesSwordEnchantment((Enchantment) (Object) this, stack.getItem())) {
			cir.setReturnValue(true);
		}
	}
}
