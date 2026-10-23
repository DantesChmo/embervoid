package com.embervoid.reach.gear;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** Royal Ash Rapier: a thrust whose full-charge hits on one target ramp ({@link RapierRamp}); Lunge. */
public class RoyalAshRapierItem extends GearWeaponItem {
	public RoyalAshRapierItem(Settings settings, Spec spec) {
		super(settings, spec, GearAbilities.LUNGE);
	}

	@Override
	protected void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker instanceof PlayerEntity player) {
			RapierRamp.onHit(player, target, GearReach.lastCharge(player));
		}
	}
}
