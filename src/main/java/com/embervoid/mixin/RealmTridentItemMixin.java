package com.embervoid.mixin;

import com.embervoid.realm.FinalZoneLock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The final zone's lock: a riptide throw aimed into the final zone by a locked-out player does nothing,
 * on the server and on their own client alike (the clearance is synced).
 */
@Mixin(TridentItem.class)
abstract class RealmTridentItemMixin {
	@Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
	private void embervoid$finalZoneLock(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
		if (user instanceof PlayerEntity player && FinalZoneLock.refusesRiptide(player, stack)) {
			ci.cancel();
		}
	}
}
