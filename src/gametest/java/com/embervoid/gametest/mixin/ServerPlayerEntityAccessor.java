package com.embervoid.gametest.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test players are never ticked, so their join invulnerability never runs out on its own. */
@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerEntityAccessor {
	@Accessor("joinInvulnerabilityTicks")
	void embervoid$setJoinInvulnerabilityTicks(int ticks);
}
