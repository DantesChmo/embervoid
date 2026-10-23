package com.embervoid.gametest.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Test players are never ticked, so they never notice the water they stand in on their own. */
@Mixin(Entity.class)
public interface EntityAccessor {
	@Invoker("updateWaterState")
	boolean embervoid$updateWaterState();
}
