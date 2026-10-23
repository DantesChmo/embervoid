package com.embervoid.sanctum.gear;

import com.embervoid.item.ability.SpecialAbility;
import com.embervoid.reach.gear.GearWeaponItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * A Sanctum weapon (DESIGN §10.10): a {@link GearWeaponItem} of the overworld tier, so it gets its STR/DEX scaling, the
 * realm weapons' sword enchantments and their right-click flow, but is repaired with iron ingots and enchants like
 * iron.
 */
public class SanctumWeaponItem extends GearWeaponItem {
	public SanctumWeaponItem(Settings settings, Spec spec, SpecialAbility ability) {
		super(settings, spec, ability);
	}

	@Override
	public int getEnchantability() {
		return 14;
	}

	@Override
	public boolean canRepair(ItemStack stack, ItemStack ingredient) {
		return ingredient.isOf(Items.IRON_INGOT);
	}
}
