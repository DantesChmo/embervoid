package com.embervoid.mixin;

import com.embervoid.player.RemnantDeathHandler;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerPlayNetworkHandler.class)
abstract class ServerPlayNetworkHandlerMixin {
	@Shadow
	public ServerPlayerEntity player;

	// Every server-side teleport of a player ends here (/tp, ender pearls, chorus fruit, dimension changes), so the
	// remnant's safe spot from before the jump is not used if they die right after it.
	@Inject(method = "requestTeleport(DDDFFLjava/util/Set;)V", at = @At("TAIL"))
	private void embervoid$forgetSafeSpot(double x, double y, double z, float yaw, float pitch, Set<PositionFlag> flags,
			CallbackInfo ci) {
		RemnantDeathHandler.onTeleport(player);
	}
}
