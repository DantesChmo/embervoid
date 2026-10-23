package com.embervoid.reach.gear;

import com.embervoid.anim.AttackAnimations;
import com.embervoid.anim.WeaponKind;
import com.embervoid.registry.ModItems;
import com.embervoid.stats.WeaponScaling;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

import java.util.List;
import java.util.Set;

/** The realm's unique weapons, one per boss soul (DESIGN §4.5 "Weapons"). */
public final class GearWeapons {
	private static final int DURABILITY = 1800;

	/**
	 * 12 damage, speed 1.1, reach 3.5; every hit sweeps, ash wave on a full charge, stamina back per enemy a sweep or
	 * Harvest catches; Harvest. STR 0.6 / DEX 0.4. The numbers are DESIGN §6.1's (0.5.0).
	 */
	public static final Item ASHEN_SCYTHE = ModItems.register("ashen_scythe", new AshenScytheItem(settings(),
			new GearWeaponItem.Spec(12, 1.1f, new WeaponScaling.Scaling(0.6f, 0.4f), 0, 3.5, 0, 0)));
	/** 10 damage, speed 1.0, reach 6, single target (the arc line and the tip are PLAN 17.4), Bleeding +1; Lash. DEX 1. */
	public static final Item WARDENS_SCOURGE = ModItems.register("wardens_scourge", new GearWeaponItem(settings(),
			new GearWeaponItem.Spec(10, 1.0f, WeaponScaling.Scaling.DEXTERITY, 0, 6, 1, 0), GearAbilities.LASH));
	/**
	 * 7 damage, speed 2.2, reach 3.5, a thrust (no sweep) that ramps on the same target ({@link RapierRamp}), Bleeding
	 * +1, fire 3 s; Lunge. DEX 0.7, the ember part (0.3) KIN.
	 */
	public static final Item ROYAL_ASH_RAPIER = ModItems.register("royal_ash_rapier", new RoyalAshRapierItem(settings(),
			new GearWeaponItem.Spec(7, 2.2f, new WeaponScaling.Scaling(0, 0.7f), 0.3f, 3.5, 1, 3)));

	public static final List<Item> MELEE = List.of(ASHEN_SCYTHE, WARDENS_SCOURGE, ROYAL_ASH_RAPIER);

	/**
	 * The sword enchantments the realm melee weapons take on the anvil and the enchanting table (R40.1c): without them
	 * an enchanted netherite sword out-damaged every realm weapon. Not Sweeping Edge: only swords sweep the vanilla
	 * way, and the Scythe has its own sweep.
	 */
	public static final Set<Enchantment> SWORD_ENCHANTMENTS = Set.of(Enchantments.SHARPNESS, Enchantments.SMITE,
			Enchantments.BANE_OF_ARTHROPODS, Enchantments.FIRE_ASPECT, Enchantments.LOOTING, Enchantments.KNOCKBACK);

	/** Charge 1.6 s, bolt 16, Punch III knockback, pierce 2, ×1.5 into a mob not targeting the shooter. DEX 0.5 / STR 0.5. */
	public static final Item HUNTSMANS_CROSSBOW = ModItems.register("huntsmans_crossbow",
			new HuntsmansCrossbowItem(new Item.Settings().maxDamage(1200).rarity(Rarity.EPIC).fireproof()));

	/** Whether {@code item} is a realm melee weapon and {@code enchantment} one of {@link #SWORD_ENCHANTMENTS}. */
	public static boolean takesSwordEnchantment(Enchantment enchantment, Item item) {
		return item instanceof GearWeaponItem && SWORD_ENCHANTMENTS.contains(enchantment);
	}

	private static Item.Settings settings() {
		return new Item.Settings().maxDamage(DURABILITY).rarity(Rarity.EPIC).fireproof();
	}

	static void register() {
		WeaponScaling.addMeleeRule(stack -> stack.getItem() instanceof GearWeaponItem weapon ? weapon.spec().scaling() : null);
		WeaponScaling.addRangedRule(stack -> stack.getItem() instanceof HuntsmansCrossbowItem ? WeaponScaling.Scaling.HYBRID : null);
		AttackAnimations.register(ASHEN_SCYTHE, WeaponKind.SCYTHE);
		AttackAnimations.register(WARDENS_SCOURGE, WeaponKind.WHIP);
		AttackAnimations.register(ROYAL_ASH_RAPIER, WeaponKind.RAPIER);
		AttackAnimations.register(HUNTSMANS_CROSSBOW, WeaponKind.CROSSBOW);
	}

	private GearWeapons() {}
}
