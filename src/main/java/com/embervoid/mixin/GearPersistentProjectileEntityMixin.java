package com.embervoid.mixin;

import com.embervoid.reach.gear.HuntsmansCrossbowItem;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Huntsman's Crossbow: its bolts deal their configured damage. */
@Mixin(PersistentProjectileEntity.class)
abstract class GearPersistentProjectileEntityMixin {
	// The hit's damage, vanilla's ceil(speed × base), before the crit roll and the damage call.
	@ModifyExpressionValue(method = "onEntityHit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/math/MathHelper;ceil(D)I"))
	private int embervoid$huntsmanBoltDamage(int damage) {
		return HuntsmansCrossbowItem.hitDamage((PersistentProjectileEntity) (Object) this, damage);
	}
}
