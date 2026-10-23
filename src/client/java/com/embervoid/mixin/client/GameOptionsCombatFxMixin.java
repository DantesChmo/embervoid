package com.embervoid.mixin.client;

import com.embervoid.client.fx.CombatFxOptions;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
abstract class GameOptionsCombatFxMixin {
	// accept() both reads and writes options.txt, so the combat effect options are loaded and saved with the vanilla
	// ones. GameOptions.Visitor is made accessible by embervoid.accesswidener.
	@Inject(method = "accept", at = @At("TAIL"))
	private void embervoid$visitCombatFx(GameOptions.Visitor visitor, CallbackInfo ci) {
		visitor.accept(CombatFxOptions.CAMERA_SHAKE_KEY, CombatFxOptions.CAMERA_SHAKE);
		visitor.accept(CombatFxOptions.HIT_FLASHES_KEY, CombatFxOptions.HIT_FLASHES);
	}
}
