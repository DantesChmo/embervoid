package com.embervoid.reach.gear;

import com.embervoid.registry.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Rarity;

import java.util.List;

/**
 * Ashen Reach armor (DESIGN §4.5 "Armor"): the crafted Smoldering Plate, one mob-drop set per zone and four unique
 * pieces. Set bonuses and piece effects live in {@link GearDamage} and {@link GearEffects}. A unique piece has its own
 * material, so it never counts towards a set.
 */
public final class GearArmor {
	/** Equipment slots of the four armor pieces, helmet first (the order of {@link ArmorItem.Type}). */
	public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	/** Diamond protection, toughness 2.5, ~1.15× diamond durability; fire reduction and the set bonus in {@link GearDamage}. */
	public static final GearArmorMaterial SMOLDERING = new GearArmorMaterial("smoldering", 38, new int[]{3, 8, 6, 3}, 12,
			SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 2.5f, 0, () -> Ingredient.ofItems(GearItems.SMOLDERING_INGOT));
	/** Fields: armor 15, toughness 0; set +10 % move speed. */
	public static final GearArmorMaterial REAPER = new GearArmorMaterial("reaper", 15, new int[]{2, 6, 5, 2}, 12,
			SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0, 0, () -> Ingredient.ofItems(GearItems.CINDER_SHARD));
	/** Woods: armor 17, toughness 1 per piece; set +15 % projectile damage. */
	public static final GearArmorMaterial HUNTSMAN = new GearArmorMaterial("huntsman", 20, new int[]{2, 7, 5, 3}, 15,
			SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 1, 0, () -> Ingredient.ofItems(GearItems.ROYAL_AMBER));
	/** Necropolis: armor 20, toughness 2 per piece, −5 % speed; set +0.3 knockback resistance, −15 % while blocking. */
	public static final GearArmorMaterial WARDEN = new GearArmorMaterial("warden", 30, new int[]{3, 8, 6, 3}, 10,
			SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, 2, 0, () -> Ingredient.ofItems(GearItems.GRAVE_IRON_INGOT));

	// Unique pieces: one slot each, numbers like their zone's set piece.
	public static final GearArmorMaterial SCARECROW = new GearArmorMaterial("scarecrow_mask", 15, new int[]{2, 0, 0, 0}, 12,
			SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0, 0, () -> Ingredient.ofItems(GearItems.CINDER_SHARD));
	public static final GearArmorMaterial HUNTSMAN_GREAVES = new GearArmorMaterial("huntsman_greaves", 20, new int[]{0, 0, 5, 0}, 15,
			SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 1, 0, () -> Ingredient.ofItems(GearItems.ROYAL_AMBER));
	public static final GearArmorMaterial DROWNED = new GearArmorMaterial("drowned_sabatons", 30, new int[]{0, 0, 0, 3}, 10,
			SoundEvents.ITEM_ARMOR_EQUIP_IRON, 2, 0, () -> Ingredient.ofItems(GearItems.GRAVE_IRON_INGOT));
	public static final GearArmorMaterial FURNACE_HEART = new GearArmorMaterial("furnace_heart", 38, new int[]{0, 8, 0, 0}, 12,
			SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 2.5f, 0, () -> Ingredient.ofItems(GearItems.SMOLDERING_INGOT));

	public static final List<Item> SMOLDERING_PLATE = set(SMOLDERING, "smoldering",
			new Item.Settings().fireproof().rarity(Rarity.UNCOMMON), true, true);
	public static final List<Item> REAPERS_GARB = set(REAPER, "reaper", new Item.Settings(), false, true);
	public static final List<Item> HUNTSMANS_LEATHERS = set(HUNTSMAN, "huntsman", new Item.Settings(), false, true);
	public static final List<Item> NECROPOLIS_WARDEN = set(WARDEN, "warden", new Item.Settings(), false, true,
			new GearArmorItem.Extra("warden_weight", EntityAttributes.GENERIC_MOVEMENT_SPEED, -0.0125,
					EntityAttributeModifier.Operation.MULTIPLY_BASE));

	public static final Item SCARECROW_MASK = unique("scarecrow_mask", SCARECROW, ArmorItem.Type.HELMET);
	public static final Item HUNTSMAN_GREAVES_ITEM = unique("huntsman_greaves", HUNTSMAN_GREAVES, ArmorItem.Type.LEGGINGS);
	public static final Item DROWNED_SABATONS = unique("drowned_sabatons", DROWNED, ArmorItem.Type.BOOTS);
	public static final Item FURNACE_HEART_CUIRASS = unique("furnace_heart_cuirass", FURNACE_HEART, ArmorItem.Type.CHESTPLATE);
	public static final List<Item> UNIQUES = List.of(SCARECROW_MASK, HUNTSMAN_GREAVES_ITEM, DROWNED_SABATONS, FURNACE_HEART_CUIRASS);

	/** Registers the four pieces {@code <name>_helmet|chestplate|leggings|boots}. */
	private static List<Item> set(GearArmorMaterial material, String name, Item.Settings settings, boolean pieceEffect,
			boolean setBonus, GearArmorItem.Extra... extra) {
		return List.of(ArmorItem.Type.values()).stream()
				.map(type -> (Item) ModItems.register(name + "_" + type.getName(),
						new GearArmorItem(material, type, settings, name, pieceEffect, setBonus, extra)))
				.toList();
	}

	private static Item unique(String name, GearArmorMaterial material, ArmorItem.Type type) {
		return ModItems.register(name, new GearArmorItem(material, type, new Item.Settings().rarity(Rarity.RARE).fireproof(),
				name, true, false));
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

	/** Whether the entity wears this (unique) armor piece in its slot. */
	public static boolean wears(LivingEntity entity, Item piece) {
		return piece instanceof ArmorItem armor && entity.getEquippedStack(armor.getSlotType()).isOf(piece);
	}

	/** Scarecrow's Mask: Ash Stalkers do not ambush its wearer (DESIGN §4.5); checked by track reach-mobs. */
	public static boolean hiddenFromAmbush(LivingEntity entity) {
		return wears(entity, SCARECROW_MASK);
	}

	static void register() {
	}

	private GearArmor() {}
}
