package com.embervoid.sanctum;

import com.embervoid.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

/** Items of the Ember Sanctum. */
public final class SanctumItems {
	public static final Item FLASK_SHARD = ModItems.register("flask_shard",
			new FlaskShardItem(new Item.Settings().rarity(Rarity.UNCOMMON)));

	static void register() {
	}

	private SanctumItems() {}
}
