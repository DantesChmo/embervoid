package com.embervoid.mixin.client;

import com.embervoid.reach.gear.HuntsmansCrossbowItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Huntsman's Crossbow (track reach-gear): vanilla gives the "hold a loaded crossbow" arm pose only to
 * {@code Items.CROSSBOW}; the realm crossbow answers that check too.
 */
@Mixin(PlayerEntityRenderer.class)
abstract class GearPlayerEntityRendererMixin {
	@WrapOperation(method = "getArmPose", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
	private static boolean embervoid$huntsmansCrossbowPose(ItemStack stack, Item item, Operation<Boolean> original) {
		return HuntsmansCrossbowItem.countsAsCrossbow(stack, item) || original.call(stack, item);
	}
}
