package com.embervoid.reach.gear;

import com.embervoid.registry.ModItems;
import com.embervoid.stats.WeaponScaling;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

import java.util.List;

/** The realm's unique weapons, one per boss soul (DESIGN §4.5 "Weapons"). */
public final class GearWeapons {
	private static final int DURABILITY = 1800;

	/** 8 damage, speed 1.1; sweep, ash wave on a full charge; Harvest. STR 0.6 / DEX 0.4. */
	public static final Item ASHEN_SCYTHE = ModItems.register("ashen_scythe", new AshenScytheItem(settings(),
			new GearWeaponItem.Spec(8, 1.1f, new WeaponScaling.Scaling(0.6f, 0.4f), 0, 0, 0, 0)));
	/** 8 damage, speed 0.9, reach 6, single target, Bleeding +1; Lash. DEX 1. */
	public static final Item WARDENS_SCOURGE = ModItems.register("wardens_scourge", new GearWeaponItem(settings(),
			new GearWeaponItem.Spec(8, 0.9f, WeaponScaling.Scaling.DEXTERITY, 0, 6, 1, 0), GearAbilities.LASH));
	/** 6 damage, speed 2.0, reach 2.5, Bleeding +1, fire 3 s; Lunge. DEX 0.7, the ember part (0.3) KIN. */
	public static final Item ROYAL_ASH_RAPIER = ModItems.register("royal_ash_rapier", new GearWeaponItem(settings(),
			new GearWeaponItem.Spec(6, 2.0f, new WeaponScaling.Scaling(0, 0.7f), 0.3f, 2.5, 1, 3), GearAbilities.LUNGE));

	public static final List<Item> MELEE = List.of(ASHEN_SCYTHE, WARDENS_SCOURGE, ROYAL_ASH_RAPIER);

	/** Charge 2.0 s, bolts ≈ 1.5×, Punch III knockback, pierce 1. DEX 0.5 / STR 0.5. */
	public static final Item HUNTSMANS_CROSSBOW = ModItems.register("huntsmans_crossbow",
			new HuntsmansCrossbowItem(new Item.Settings().maxDamage(1200).rarity(Rarity.EPIC).fireproof()));

	private static Item.Settings settings() {
		return new Item.Settings().maxDamage(DURABILITY).rarity(Rarity.EPIC).fireproof();
	}

	static void register() {
		WeaponScaling.addMeleeRule(stack -> stack.getItem() instanceof GearWeaponItem weapon ? weapon.spec().scaling() : null);
		WeaponScaling.addRangedRule(stack -> stack.getItem() instanceof HuntsmansCrossbowItem ? WeaponScaling.Scaling.HYBRID : null);
	}

	private GearWeapons() {}
}
