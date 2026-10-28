package com.embervoid.hub.stele;

import com.embervoid.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

public final class SteleItems {
	/** Dropped by the Fallen Sister. */
	public static final Item CRYING_SOUL = ModItems.register("crying_soul",
			new CryingSoulItem(new Item.Settings().maxCount(16).rarity(Rarity.RARE)));

	static void register() {
	}

	private SteleItems() {}
}
