package com.embervoid.mixin;

import com.embervoid.stats.StatEffects;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	// Weapon scaling and fire reduction from stats, before armor, enchantments and resistance are applied.
	@Unique
	private static boolean embervoid$droppingExtra;

	@Shadow
	protected abstract void dropLoot(DamageSource source, boolean causedByPlayer);

	// Fortune: a mob killed by a player may roll its loot table once more. Players have no loot table to roll.
	@Inject(method = "dropLoot", at = @At("TAIL"))
	private void embervoid$fortuneExtraDrop(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
		if (embervoid$droppingExtra || !causedByPlayer || (Object) this instanceof PlayerEntity
				|| !(source.getAttacker() instanceof ServerPlayerEntity killer) || !StatEffects.rollExtraDrop(killer)) {
			return;
		}
		embervoid$droppingExtra = true;
		try {
			dropLoot(source, causedByPlayer);
		} finally {
			embervoid$droppingExtra = false;
		}
	}

	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float embervoid$statDamage(float amount, @Local(argsOnly = true) DamageSource source) {
		return StatEffects.modifyDamage((LivingEntity) (Object) this, source, amount);
	}
}
