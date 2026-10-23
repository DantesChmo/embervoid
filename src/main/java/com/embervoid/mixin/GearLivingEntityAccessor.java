package com.embervoid.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The attack cooldown counter (track reach-gear): the Warden's Scourge attacks every mob on its lash with one swing,
 * and each vanilla attack resets the cooldown, so each mob gets the swing's own charge back.
 */
@Mixin(LivingEntity.class)
public interface GearLivingEntityAccessor {
	@Accessor("lastAttackedTicks")
	int embervoid$lastAttackedTicks();

	@Accessor("lastAttackedTicks")
	void embervoid$setLastAttackedTicks(int ticks);
}
