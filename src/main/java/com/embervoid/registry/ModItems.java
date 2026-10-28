package com.embervoid.registry;

import com.embervoid.Embervoid;
import com.embervoid.item.HealingFlaskItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
	/** All mod items in registration order; shown in the creative tab. */
	public static final List<Item> ALL = new ArrayList<>();

	public static final Item HEALING_FLASK = register("healing_flask", new HealingFlaskItem(new Item.Settings().maxCount(1)));

	public static <T extends Item> T register(String name, T item) {
		ALL.add(item);
		return Registry.register(Registries.ITEM, Embervoid.id(name), item);
	}

	public static void register() {
		Embervoid.LOGGER.debug("Registering items");
	}

	private ModItems() {}
}
