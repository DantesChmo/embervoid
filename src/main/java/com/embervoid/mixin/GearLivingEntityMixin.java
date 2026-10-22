package com.embervoid.mixin;

import com.embervoid.reach.gear.GearArmor;
import com.embervoid.reach.gear.GearConfig;
import com.embervoid.reach.gear.GearDamage;
import com.embervoid.reach.gear.GearEffects;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Realm gear hooks (track reach-gear). */
@Mixin(LivingEntity.class)
abstract class GearLivingEntityMixin {
	// Armor piece and set effects on incoming damage, before armor, enchantments and resistance are applied.
	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float embervoid$gearIncomingDamage(float amount, @Local(argsOnly = true) DamageSource source) {
		return GearDamage.modifyIncoming((LivingEntity) (Object) this, source, amount);
	}

	// Effects that answer a hit landing (Smoldering Resin, Furnace Heart Cuirass).
	@Inject(method = "damage", at = @At("RETURN"))
	private void embervoid$gearAfterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			GearEffects.afterDamage((LivingEntity) (Object) this, source);
		}
	}

	// Scarecrow's Mask: mobs notice the wearer at half the distance, the way vanilla mob heads do.
	@Inject(method = "getAttackDistanceScalingFactor", at = @At("RETURN"), cancellable = true)
	private void embervoid$scarecrowMask(@Nullable Entity entity, CallbackInfoReturnable<Double> cir) {
		if (GearArmor.wears((LivingEntity) (Object) this, GearArmor.SCARECROW_MASK)) {
			cir.setReturnValue(cir.getReturnValue() * GearConfig.get().uniques().scarecrowDetection());
		}
	}
}
