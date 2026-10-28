package com.embervoid.reach.gear;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.SmithingTemplateItem;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.Util;

import java.util.List;

/** Ashen Reach materials and boss items (fixed ids). */
public final class GearItems {
	// Ore drops, one material per zone (DESIGN §4.5 "Ores").
	public static final Item CINDER_SHARD = ModItems.register("cinder_shard", new Item(new Item.Settings()));
	public static final Item ROYAL_AMBER = ModItems.register("royal_amber", new Item(new Item.Settings()));
	public static final Item RAW_GRAVE_IRON = ModItems.register("raw_grave_iron", new Item(new Item.Settings()));
	public static final Item GRAVE_IRON_INGOT = ModItems.register("grave_iron_ingot", new Item(new Item.Settings()));
	/** Needs all three zones: 4 Grave Iron Ingot + 2 Cinder Shard + 2 Royal Amber. */
	public static final Item SMOLDERING_INGOT = ModItems.register("smoldering_ingot",
			new LoreItem(new Item.Settings().rarity(Rarity.UNCOMMON).fireproof()));
	/** Diamond armor + Smoldering Ingot + this on the smithing table → Smoldering Plate. */
	public static final Item SMOLDERING_TEMPLATE = ModItems.register("smoldering_template", smolderingTemplate());

	// First-kill personal drops of the realm's bosses; each is the core of that boss's weapon.
	public static final Item ASH_COLOSSUS_SOUL = ModItems.register("ash_colossus_soul", soul());
	public static final Item GRIEVING_TREE_SOUL = ModItems.register("grieving_tree_soul", soul());
	public static final Item CRYPT_KEEPER_SOUL = ModItems.register("crypt_keeper_soul", soul());
	public static final Item SMOLDERING_LORD_SOUL = ModItems.register("smoldering_lord_soul", soul());

	/** Summons a beaten zone boss again at its arena shrine (DESIGN §10.11); its use is in {@code AshOfRematch}. */
	public static final Item ASH_OF_REMATCH = ModItems.register("ash_of_rematch",
			new LoreItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));
	public static final Item FLASK_EMBER = ModItems.register("flask_ember",
			new FlaskEmberItem(new Item.Settings().maxCount(16).rarity(Rarity.EPIC).fireproof()));

	private static Item soul() {
		return new LoreItem(new Item.Settings().maxCount(16).rarity(Rarity.EPIC).fireproof());
	}

	/** Works like the netherite upgrade, for armor only. */
	private static SmithingTemplateItem smolderingTemplate() {
		return new SmithingTemplateItem(
				text("applies_to", Formatting.BLUE),
				text("ingredients", Formatting.BLUE),
				text("title", Formatting.GRAY),
				text("base_slot", null),
				text("additions_slot", null),
				List.of(new Identifier("item/empty_armor_slot_helmet"), new Identifier("item/empty_armor_slot_chestplate"),
						new Identifier("item/empty_armor_slot_leggings"), new Identifier("item/empty_armor_slot_boots")),
				List.of(new Identifier("item/empty_slot_ingot")));
	}

	private static Text text(String key, Formatting formatting) {
		Text text = Text.translatable(Util.createTranslationKey("item", Embervoid.id("smithing_template.smoldering." + key)));
		return formatting == null ? text : text.copy().formatted(formatting);
	}

	static void register() {
	}

	private GearItems() {}
}
