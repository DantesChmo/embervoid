package com.embervoid.mixin;

import com.embervoid.mob.MobLevels;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mob levels at damage time: damage dealt by a leveled attacker is scaled when it reaches its target, so
 * melee, projectiles, explosions and potions all count (the damage source's attacker is the projectile's owner, the
 * creeper, the witch). Also scales the XP a leveled entity drops.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMobLevelMixin {
	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float embervoid$scaleByAttackerLevel(float amount, DamageSource source) {
		Entity attacker = source.getAttacker();
		return attacker != null && attacker != (Object) this ? amount * MobLevels.damageMultiplier(attacker) : amount;
	}

	@ModifyArg(method = "dropXp", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"),
			index = 2)
	private int embervoid$scaleExperience(int experience) {
		return MobLevels.scaleExperience((LivingEntity) (Object) this, experience);
	}
}
