package com.embervoid.mixin;

import com.embervoid.stats.StatEffects;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	// Weapon scaling and fire reduction from stats, before armor, enchantments and resistance are applied.
	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float embervoid$statDamage(float amount, @Local(argsOnly = true) DamageSource source) {
		return StatEffects.modifyDamage((LivingEntity) (Object) this, source, amount);
	}
}
