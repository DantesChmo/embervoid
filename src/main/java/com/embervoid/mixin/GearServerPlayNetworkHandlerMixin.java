package com.embervoid.mixin;

import com.embervoid.reach.gear.GearReach;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Long weapon reach (track reach-gear): a swing with a whip attacks what it points at, up to 6 blocks. */
@Mixin(ServerPlayNetworkHandler.class)
abstract class GearServerPlayNetworkHandlerMixin {
	@Shadow
	public ServerPlayerEntity player;

	@Inject(method = "onHandSwing", at = @At("TAIL"))
	private void embervoid$longReachSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
		GearReach.onSwing(player, packet.getHand());
	}
}
