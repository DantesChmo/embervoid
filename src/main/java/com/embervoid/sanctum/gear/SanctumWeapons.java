package com.embervoid.sanctum.gear;

import com.embervoid.registry.ModItems;
import com.embervoid.reach.gear.GearWeaponItem;
import com.embervoid.stats.WeaponScaling;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

import java.util.List;

/**
 * The two Sanctum weapons (DESIGN §10.10), found in the outer-hall chests: below every realm weapon, so the
 * special attacks (and END) matter before Realm 1.
 */
public final class SanctumWeapons {
	private static final int DURABILITY = 600;

	/** 6.5 damage, speed 1.6 (between the iron and the diamond sword); Wide Cleave. DEX 1. */
	public static final Item SANCTUM_LONGSWORD = ModItems.register("sanctum_longsword", new SanctumWeaponItem(settings(),
			new GearWeaponItem.Spec(6.5f, 1.6f, WeaponScaling.Scaling.DEXTERITY, 0, 0, 0, 0), SanctumAbilities.WIDE_CLEAVE));
	/** 8 damage, speed 0.9 (below the iron axe's 9); breaks a Hollow's guard like an axe; Ground Slam. STR 1. */
	public static final Item ASHBEARERS_MACE = ModItems.register("ashbearers_mace", new SanctumWeaponItem(settings(),
			new GearWeaponItem.Spec(8, 0.9f, WeaponScaling.Scaling.STRENGTH, 0, 0, 0, 0), SanctumAbilities.GROUND_SLAM));

	public static final List<Item> ALL = List.of(SANCTUM_LONGSWORD, ASHBEARERS_MACE);

	private static Item.Settings settings() {
		return new Item.Settings().maxDamage(DURABILITY).rarity(Rarity.UNCOMMON);
	}

	private SanctumWeapons() {}
}
