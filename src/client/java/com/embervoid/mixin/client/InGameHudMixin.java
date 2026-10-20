package com.embervoid.mixin.client;

import com.embervoid.client.StaminaHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
	// Right after health/hunger/air, so the stamina bar shares their visibility and layering.
	@Inject(method = "renderStatusBars", at = @At("TAIL"))
	private void embervoid$renderStamina(DrawContext context, CallbackInfo ci) {
		StaminaHud.render(context);
	}
}
