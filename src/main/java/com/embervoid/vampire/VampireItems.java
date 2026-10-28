package com.embervoid.vampire;

import com.embervoid.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

public final class VampireItems {
	/** First-kill trophy of the Vampire boss (DESIGN §10.12); a material for later gear. */
	public static final Item VAMPIRE_FANG = ModItems.register("vampire_fang",
			new Item(new Item.Settings().maxCount(16).rarity(Rarity.RARE)));

	static void register() {
	}

	private VampireItems() {}
}
