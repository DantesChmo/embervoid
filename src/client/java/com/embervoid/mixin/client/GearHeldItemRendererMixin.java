package com.embervoid.mixin.client;

import com.embervoid.reach.gear.HuntsmansCrossbowItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Huntsman's Crossbow: the first-person view holds, charges and aims a crossbow with both hands
 * only for {@code Items.CROSSBOW}; the realm crossbow answers those checks too.
 */
@Mixin(HeldItemRenderer.class)
abstract class GearHeldItemRendererMixin {
	// Which hands render and how (static helpers).
	@WrapOperation(method = {"getHandRenderType", "getUsingItemHandRenderType", "isChargedCrossbow"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
	private static boolean embervoid$huntsmansCrossbowHands(ItemStack stack, Item item, Operation<Boolean> original) {
		return HuntsmansCrossbowItem.countsAsCrossbow(stack, item) || original.call(stack, item);
	}

	// The crossbow's own first-person placement while loaded or charging.
	@WrapOperation(method = "renderFirstPersonItem",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
	private boolean embervoid$huntsmansCrossbowFirstPerson(ItemStack stack, Item item, Operation<Boolean> original) {
		return HuntsmansCrossbowItem.countsAsCrossbow(stack, item) || original.call(stack, item);
	}
}
