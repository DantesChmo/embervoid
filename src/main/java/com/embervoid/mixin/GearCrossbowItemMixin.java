package com.embervoid.mixin;

import com.embervoid.reach.gear.HuntsmansCrossbowItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Huntsman's Crossbow (track reach-gear): a longer charge and heavier bolts. */
@Mixin(CrossbowItem.class)
abstract class GearCrossbowItemMixin {
	@Inject(method = "getPullTime", at = @At("RETURN"), cancellable = true)
	private static void embervoid$huntsmanPullTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (stack.getItem() instanceof HuntsmansCrossbowItem) {
			cir.setReturnValue(cir.getReturnValue() + HuntsmansCrossbowItem.extraPullTicks());
		}
	}

	@Inject(method = "createArrow", at = @At("RETURN"))
	private static void embervoid$huntsmanBolt(World world, LivingEntity entity, ItemStack crossbow, ItemStack arrow,
			CallbackInfoReturnable<PersistentProjectileEntity> cir) {
		if (crossbow.getItem() instanceof HuntsmansCrossbowItem) {
			HuntsmansCrossbowItem.strengthen(cir.getReturnValue());
		}
	}
}
