package com.embervoid.reach.gear;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.tag.DamageTypeTags;

/** Damage changes from realm gear: armor piece and set effects. */
public final class GearDamage {
	/**
	 * Called from {@code GearLivingEntityMixin} at the start of {@code LivingEntity.damage}, before armor: scales the
	 * incoming damage by what the target wears.
	 */
	public static float modifyIncoming(LivingEntity target, DamageSource source, float amount) {
		if (target.getWorld().isClient()) {
			return amount;
		}
		if (source.isIn(DamageTypeTags.IS_FIRE)) {
			int pieces = GearArmor.pieces(target, GearArmor.SMOLDERING);
			if (pieces > 0) {
				amount *= Math.max(0, 1 - pieces * GearConfig.get().smoldering().fireReductionPerPiece());
			}
		}
		return amount;
	}

	/** Whether the damage happens at all: the full Smoldering Plate does not burn (lava and fire blocks still hurt). */
	static boolean allow(LivingEntity target, DamageSource source) {
		return !(source.isOf(DamageTypes.ON_FIRE) && GearArmor.fullSet(target, GearArmor.SMOLDERING));
	}

	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> allow(entity, source));
	}

	private GearDamage() {}
}
