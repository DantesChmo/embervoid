package com.embervoid.mixin.client;

import com.embervoid.client.fx.CombatFxOptions;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(AccessibilityOptionsScreen.class)
abstract class AccessibilityOptionsScreenMixin {
	// After vanilla's own, next to Screen Effects and Distortion Effects, which they are like.
	@Inject(method = "getOptions", at = @At("RETURN"), cancellable = true)
	private static void embervoid$addCombatFx(GameOptions gameOptions, CallbackInfoReturnable<SimpleOption<?>[]> cir) {
		SimpleOption<?>[] options = cir.getReturnValue();
		SimpleOption<?>[] withCombatFx = Arrays.copyOf(options, options.length + 2);
		withCombatFx[options.length] = CombatFxOptions.CAMERA_SHAKE;
		withCombatFx[options.length + 1] = CombatFxOptions.HIT_FLASHES;
		cir.setReturnValue(withCombatFx);
	}
}
