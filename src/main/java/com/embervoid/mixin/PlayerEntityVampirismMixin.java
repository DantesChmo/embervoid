package com.embervoid.mixin;

import com.embervoid.moon.Vampirism;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blood new moon: measures the damage a player actually took (after armor and absorption) for vampirism (PLAN 8.6). */
@Mixin(PlayerEntity.class)
abstract class PlayerEntityVampirismMixin {
	@Unique
	private float embervoid$healthBeforeDamage;

	@Inject(method = "applyDamage", at = @At("HEAD"))
	private void embervoid$beforeDamage(DamageSource source, float amount, CallbackInfo ci) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		embervoid$healthBeforeDamage = self.getHealth() + self.getAbsorptionAmount();
	}

	@Inject(method = "applyDamage", at = @At("TAIL"))
	private void embervoid$afterDamage(DamageSource source, float amount, CallbackInfo ci) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		float dealt = embervoid$healthBeforeDamage - (self.getHealth() + self.getAbsorptionAmount());
		if (dealt > 0) {
			Vampirism.onPlayerDamaged(self, source, dealt);
		}
	}
}
