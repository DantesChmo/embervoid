package com.embervoid.mixin.client;

import com.embervoid.client.realm.BossMusicOption;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
abstract class GameOptionsBossMusicMixin {
	// accept() both reads and writes options.txt, so the slider is loaded and saved with the vanilla options.
	// GameOptions.Visitor is made accessible by embervoid.accesswidener.
	@Inject(method = "accept", at = @At("TAIL"))
	private void embervoid$visitBossMusic(GameOptions.Visitor visitor, CallbackInfo ci) {
		visitor.accept(BossMusicOption.KEY, BossMusicOption.OPTION);
	}
}
