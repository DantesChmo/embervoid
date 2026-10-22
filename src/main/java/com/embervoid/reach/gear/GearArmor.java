package com.embervoid.reach.gear;

import com.embervoid.registry.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Rarity;

import java.util.List;

/** Ashen Reach armor (DESIGN §4.5 "Armor"). */
public final class GearArmor {
	/** Equipment slots of the four armor pieces, helmet first (the order of {@link ArmorItem.Type}). */
	public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	/** Diamond protection, toughness 2.5, ~1.15× diamond durability; fire reduction and the set bonus in {@link GearDamage}. */
	public static final GearArmorMaterial SMOLDERING = new GearArmorMaterial("smoldering", 38, new int[]{3, 8, 6, 3}, 12,
			SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 2.5f, 0, () -> Ingredient.ofItems(GearItems.SMOLDERING_INGOT));

	public static final List<Item> SMOLDERING_PLATE = set(SMOLDERING, "smoldering",
			new Item.Settings().fireproof().rarity(Rarity.UNCOMMON), true, true);

	/** Registers the four pieces {@code <name>_helmet|chestplate|leggings|boots}. */
	private static List<Item> set(GearArmorMaterial material, String name, Item.Settings settings, boolean pieceEffect,
			boolean setBonus, GearArmorItem.Extra... extra) {
		return List.of(ArmorItem.Type.values()).stream()
				.map(type -> (Item) ModItems.register(name + "_" + type.getName(),
						new GearArmorItem(material, type, settings, name, pieceEffect, setBonus, extra)))
				.toList();
	}

	/** How many pieces of {@code material} the entity wears. */
	public static int pieces(LivingEntity entity, GearArmorMaterial material) {
		int count = 0;
		for (EquipmentSlot slot : SLOTS) {
			if (entity.getEquippedStack(slot).getItem() instanceof ArmorItem armor && armor.getMaterial() == material) {
				count++;
			}
		}
		return count;
	}

	public static boolean fullSet(LivingEntity entity, GearArmorMaterial material) {
		return pieces(entity, material) == SLOTS.size();
	}

	static void register() {
	}

	private GearArmor() {}
}
