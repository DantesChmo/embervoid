package com.embervoid.mixin;

import com.embervoid.reach.gear.GearDamage;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Realm gear damage hooks (track reach-gear). */
@Mixin(LivingEntity.class)
abstract class GearLivingEntityMixin {
	// Armor piece and set effects on incoming damage, before armor, enchantments and resistance are applied.
	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float embervoid$gearIncomingDamage(float amount, @Local(argsOnly = true) DamageSource source) {
		return GearDamage.modifyIncoming((LivingEntity) (Object) this, source, amount);
	}
}
