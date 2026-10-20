package com.embervoid.registry;

import com.embervoid.Embervoid;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.ArrayList;
import java.util.List;

/** Weapons, armor, materials, consumables. */
public final class ModItems {
	/** All mod items in registration order; shown in the creative tab. */
	public static final List<Item> ALL = new ArrayList<>();

	// public static final Item EMBER_STEEL_INGOT = register("ember_steel_ingot", new Item(new Item.Settings()));

	public static <T extends Item> T register(String name, T item) {
		ALL.add(item);
		return Registry.register(Registries.ITEM, Embervoid.id(name), item);
	}

	public static void register() {
		Embervoid.LOGGER.debug("Registering items");
	}

	private ModItems() {}
}
