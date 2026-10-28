package com.embervoid.realm;

import com.embervoid.registry.ModItems;
import net.minecraft.item.Item;

public final class RealmItems {
	public static final Item EMBER_LODESTAR = ModItems.register("ember_lodestar", new EmberLodestarItem(new Item.Settings().maxCount(1)));

	public static void register() {
	}

	private RealmItems() {}
}
