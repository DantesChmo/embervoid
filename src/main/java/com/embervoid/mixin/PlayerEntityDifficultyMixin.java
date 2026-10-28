package com.embervoid.mixin;

import com.embervoid.mob.MobConfig;
import com.embervoid.mob.MobLevelRules;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Difficulty factors (DESIGN §3.1): difficulty-scaled damage to players (mob attacks, their projectiles
 * and explosions) uses our factors instead of vanilla's Easy ×0.5+1 / Hard ×1.5. Ours is applied at the start of
 * {@code PlayerEntity.damage}, and vanilla's branch is switched off so the two never stack.
 */
@Mixin(PlayerEntity.class)
abstract class PlayerEntityDifficultyMixin {
	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float embervoid$applyDifficulty(float amount, DamageSource source) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (self.getWorld().isClient || !source.isScaledWithDifficulty()) {
			return amount;
		}
		return MobLevelRules.difficultyDamage(MobConfig.get().difficulty(), self.getWorld().getDifficulty(), amount);
	}

	@ModifyExpressionValue(method = "damage", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/damage/DamageSource;isScaledWithDifficulty()Z"))
	private boolean embervoid$skipVanillaDifficulty(boolean scaled) {
		return scaled && ((PlayerEntity) (Object) this).getWorld().isClient;
	}
}
