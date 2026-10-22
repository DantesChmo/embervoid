package com.embervoid.gametest.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test players are never ticked, so their attack cooldown never fills on its own. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Accessor("lastAttackedTicks")
	void embervoid$setLastAttackedTicks(int ticks);
}
