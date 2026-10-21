package com.embervoid.boss;

import com.embervoid.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

/** Boss items. */
public final class BossItems {
	public static final Item EMBER_HEART = ModItems.register("ember_heart",
			new EmberHeartItem(new Item.Settings().maxCount(16).rarity(Rarity.EPIC).fireproof()));

	public static void register() {
	}

	private BossItems() {}
}
