package com.embervoid.reach.gear;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;

import java.util.function.Supplier;

/**
 * An armor material of the realm. The armor layer textures live in the {@code minecraft} namespace as
 * {@code textures/models/armor/embervoid_<name>_layer_1|2.png}: 1.20.1 builds that path from {@link #getName()}
 * without a namespace.
 *
 * @param durabilityMultiplier like vanilla's (diamond 33): a piece's durability is its base × this
 * @param protection           armor points for helmet, chestplate, leggings, boots
 */
public record GearArmorMaterial(String name, int durabilityMultiplier, int[] protection, int enchantability,
		SoundEvent equipSound, float toughness, float knockbackResistance, Supplier<Ingredient> repair)
		implements ArmorMaterial {
	/** Vanilla base durability for helmet, chestplate, leggings, boots. */
	private static final int[] BASE_DURABILITY = {11, 16, 15, 13};

	@Override
	public int getDurability(ArmorItem.Type type) {
		return BASE_DURABILITY[type.ordinal()] * durabilityMultiplier;
	}

	@Override
	public int getProtection(ArmorItem.Type type) {
		return protection[type.ordinal()];
	}

	@Override
	public int getEnchantability() {
		return enchantability;
	}

	@Override
	public SoundEvent getEquipSound() {
		return equipSound;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return repair.get();
	}

	@Override
	public String getName() {
		return "embervoid_" + name;
	}

	@Override
	public float getToughness() {
		return toughness;
	}

	@Override
	public float getKnockbackResistance() {
		return knockbackResistance;
	}
}
