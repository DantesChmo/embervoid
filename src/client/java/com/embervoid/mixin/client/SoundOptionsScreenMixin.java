package com.embervoid.mixin.client;

import com.embervoid.client.realm.BossMusicOption;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(SoundOptionsScreen.class)
abstract class SoundOptionsScreenMixin {
	// After the per-category sliders, so Boss music shares their two-column grid (it fills the odd ninth's row).
	@Inject(method = "getVolumeOptions", at = @At("RETURN"), cancellable = true)
	private void embervoid$addBossMusic(CallbackInfoReturnable<SimpleOption<?>[]> cir) {
		SimpleOption<?>[] options = cir.getReturnValue();
		SimpleOption<?>[] withBossMusic = Arrays.copyOf(options, options.length + 1);
		withBossMusic[options.length] = BossMusicOption.OPTION;
		cir.setReturnValue(withBossMusic);
	}
}
