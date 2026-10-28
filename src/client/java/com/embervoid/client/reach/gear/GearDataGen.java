package com.embervoid.client.reach.gear;

import com.embervoid.Embervoid;
import com.embervoid.reach.gear.Bleeding;
import com.embervoid.reach.gear.GearArmor;
import com.embervoid.reach.gear.GearConsumables;
import com.embervoid.reach.gear.GearLootTables;
import com.embervoid.reach.gear.GearWeapons;
import com.embervoid.registry.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.data.server.recipe.SmithingTransformRecipeJsonBuilder;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.embervoid.reach.gear.GearItems.*;

/** Datagen for the realm gear: item models, recipes, Ashen Reach chest loot tables, tags. */
public final class GearDataGen {
	/** Flat items drawn by {@code tools/gear_placeholders.py}. */
	static final List<Item> FLAT_ITEMS = List.of(CINDER_SHARD, ROYAL_AMBER, RAW_GRAVE_IRON, GRAVE_IRON_INGOT,
			SMOLDERING_INGOT, SMOLDERING_TEMPLATE, ASH_COLOSSUS_SOUL, GRIEVING_TREE_SOUL, CRYPT_KEEPER_SOUL,
			SMOLDERING_LORD_SOUL, ASH_OF_REMATCH, FLASK_EMBER, GearConsumables.ASH_BOMB, GearConsumables.SMOLDERING_RESIN,
			GearConsumables.AMBER_BALM, GearConsumables.AMBER_LANTERN_OIL, GearConsumables.WHETSTONE);

	/** Armor pieces; icons and armor layers drawn by {@code tools/gear_placeholders.py}. */
	static final List<Item> ARMOR = Stream.of(GearArmor.SMOLDERING_PLATE, GearArmor.REAPERS_GARB,
			GearArmor.HUNTSMANS_LEATHERS, GearArmor.NECROPOLIS_WARDEN, GearArmor.UNIQUES).flatMap(List::stream).toList();

	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(ItemModels::new);
		pack.addProvider(Recipes::new);
		pack.addProvider(ChestLoot::new);
		pack.addProvider(EntityTags::new);
	}

	private static class EntityTags extends FabricTagProvider.EntityTypeTagProvider {
		EntityTags(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
			super(output, registries);
		}

		@Override
		public String getName() {
			return "Embervoid reach gear entity tags";
		}

		@Override
		protected void configure(RegistryWrapper.WrapperLookup registries) {
			// Bloodless: bones, a furnace, bark, ash and stone. Optional entries: the mobs and bosses are registered elsewhere.
			var immune = getOrCreateTagBuilder(Bleeding.IMMUNE);
			for (String id : List.of("restless_bones", "furnace_golem", "bark_hollow", "ash_colossus", "grieving_tree")) {
				immune.addOptional(Embervoid.id(id));
			}
		}
	}

	private static class ItemModels extends FabricModelProvider {
		ItemModels(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Embervoid reach gear item models";
		}

		@Override
		public void generateBlockStateModels(BlockStateModelGenerator gen) {
		}

		@Override
		public void generateItemModels(ItemModelGenerator gen) {
			for (Item item : FLAT_ITEMS) {
				gen.register(item, Models.GENERATED);
			}
			for (Item item : ARMOR) {
				gen.registerArmor((ArmorItem) item);
			}
			for (Item item : GearWeapons.MELEE) {
				gen.register(item, Models.HANDHELD);
			}
		}
	}

	private static class Recipes extends FabricRecipeProvider {
		Recipes(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Embervoid reach gear recipes";
		}

		@Override
		public void generate(Consumer<RecipeJsonProvider> exporter) {
			offerSmelting(exporter, List.of(RAW_GRAVE_IRON), RecipeCategory.MISC, GRAVE_IRON_INGOT, 1.0f, 200, "grave_iron_ingot");
			offerBlasting(exporter, List.of(RAW_GRAVE_IRON), RecipeCategory.MISC, GRAVE_IRON_INGOT, 1.0f, 100, "grave_iron_ingot");

			// All three zones: Necropolis metal, Fields cinder, Woods amber.
			ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SMOLDERING_INGOT)
					.input(GRAVE_IRON_INGOT, 4)
					.input(CINDER_SHARD, 2)
					.input(ROYAL_AMBER, 2)
					.criterion(hasItem(GRAVE_IRON_INGOT), conditionsFromItem(GRAVE_IRON_INGOT))
					.offerTo(exporter);

			// Like the netherite upgrade: diamond piece + Smoldering Ingot + template, enchantments kept.
			ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SMOLDERING_TEMPLATE)
					.pattern("CSC")
					.pattern("CDC")
					.pattern("CSC")
					.input('C', CINDER_SHARD)
					.input('S', ModBlocks.ASH_STONE)
					.input('D', Items.DIAMOND)
					.criterion(hasItem(CINDER_SHARD), conditionsFromItem(CINDER_SHARD))
					.offerTo(exporter);
			List<Item> diamond = List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
			for (int i = 0; i < diamond.size(); i++) {
				Item result = GearArmor.SMOLDERING_PLATE.get(i);
				SmithingTransformRecipeJsonBuilder.create(Ingredient.ofItems(SMOLDERING_TEMPLATE), Ingredient.ofItems(diamond.get(i)),
								Ingredient.ofItems(SMOLDERING_INGOT), RecipeCategory.COMBAT, result)
						.criterion(hasItem(SMOLDERING_INGOT), conditionsFromItem(SMOLDERING_INGOT))
						.offerTo(exporter, getItemPath(result) + "_smithing");
			}

			// Weapons: the boss's soul + a part from its zone (a reaper's hoe, a crypt chain, a hunter's crossbow, a
			// courtier's gilded blade) + 4 of the zone's material, so a zone boss's unique comes during the realm (DESIGN
			// §6.1); the Smoldering Lord's Rapier takes 2 Smoldering Ingots. The crossbow's models are hand-written resources.
			weapon(exporter, GearWeapons.ASHEN_SCYTHE, ASH_COLOSSUS_SOUL, CINDER_SHARD, 4, Items.IRON_HOE);
			weapon(exporter, GearWeapons.WARDENS_SCOURGE, CRYPT_KEEPER_SOUL, GRAVE_IRON_INGOT, 4, Items.CHAIN);
			weapon(exporter, GearWeapons.HUNTSMANS_CROSSBOW, GRIEVING_TREE_SOUL, ROYAL_AMBER, 4, Items.CROSSBOW);
			weapon(exporter, GearWeapons.ROYAL_ASH_RAPIER, SMOLDERING_LORD_SOUL, SMOLDERING_INGOT, 2, Items.GOLDEN_SWORD);

			// Consumables: cinder for fire, amber for healing and light, grave iron for the whetstone.
			ShapelessRecipeJsonBuilder.create(RecipeCategory.COMBAT, GearConsumables.ASH_BOMB, 2)
					.input(CINDER_SHARD, 2)
					.input(Items.GUNPOWDER)
					.criterion(hasItem(CINDER_SHARD), conditionsFromItem(CINDER_SHARD))
					.offerTo(exporter);
			ShapelessRecipeJsonBuilder.create(RecipeCategory.COMBAT, GearConsumables.SMOLDERING_RESIN)
					.input(CINDER_SHARD, 2)
					.input(Items.CHARCOAL)
					.criterion(hasItem(CINDER_SHARD), conditionsFromItem(CINDER_SHARD))
					.offerTo(exporter);
			ShapelessRecipeJsonBuilder.create(RecipeCategory.BREWING, GearConsumables.AMBER_BALM)
					.input(ROYAL_AMBER, 2)
					.input(Items.GLASS_BOTTLE)
					.criterion(hasItem(ROYAL_AMBER), conditionsFromItem(ROYAL_AMBER))
					.offerTo(exporter);
			ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, GearConsumables.AMBER_LANTERN_OIL)
					.input(ROYAL_AMBER)
					.input(Items.GLASS_BOTTLE)
					.input(ItemTags.COALS)
					.criterion(hasItem(ROYAL_AMBER), conditionsFromItem(ROYAL_AMBER))
					.offerTo(exporter);
			ShapelessRecipeJsonBuilder.create(RecipeCategory.TOOLS, GearConsumables.WHETSTONE, 2)
					.input(GRAVE_IRON_INGOT)
					.input(ModBlocks.COBBLED_ASH_STONE, 2)
					.criterion(hasItem(GRAVE_IRON_INGOT), conditionsFromItem(GRAVE_IRON_INGOT))
					.offerTo(exporter);

			// DESIGN §10.11, built as recommended.
			ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ASH_OF_REMATCH)
					.input(CINDER_SHARD, 4)
					.input(ROYAL_AMBER)
					.input(Items.BONE)
					.criterion(hasItem(CINDER_SHARD), conditionsFromItem(CINDER_SHARD))
					.offerTo(exporter);
		}
	}

	static void weapon(Consumer<RecipeJsonProvider> exporter, Item weapon, Item soul, Item material, int count, Item part) {
		ShapelessRecipeJsonBuilder.create(RecipeCategory.COMBAT, weapon)
				.input(soul)
				.input(material, count)
				.input(part)
				.criterion(FabricRecipeProvider.hasItem(soul), FabricRecipeProvider.conditionsFromItem(soul))
				.offerTo(exporter);
	}

	/**
	 * Ashen Reach chests (DESIGN §4.5 "Chests"): common (outposts, farmsteads, barns, kennels, graves), treasure
	 * (granary vault, hunting lodge, lower crypt, Citadel keep: Smoldering Ingot, templates, set pieces, the zone weapon
	 * 4 %; the reliquary injects artifacts and rings) and the chest by each arena (a template, an Ash of Rematch and
	 * the zone's consumables).
	 */
	private static class ChestLoot extends SimpleFabricLootTableProvider {
		ChestLoot(FabricDataOutput output) {
			super(output, LootContextTypes.CHEST);
		}

		@Override
		public String getName() {
			return "Embervoid Ashen Reach chest loot tables";
		}

		/** A zone's ore material, set, weapon and arena consumables. */
		private record Zone(String name, Item material, int materialMax, List<Item> set, Item weapon, Item consumable,
				Item secondConsumable) {}

		@Override
		public void accept(BiConsumer<Identifier, LootTable.Builder> exporter) {
			List<Zone> zones = List.of(
					new Zone("fields", CINDER_SHARD, 3, GearArmor.REAPERS_GARB, GearWeapons.ASHEN_SCYTHE,
							GearConsumables.ASH_BOMB, GearConsumables.SMOLDERING_RESIN),
					new Zone("woods", ROYAL_AMBER, 2, GearArmor.HUNTSMANS_LEATHERS, GearWeapons.HUNTSMANS_CROSSBOW,
							GearConsumables.AMBER_BALM, Items.ARROW),
					new Zone("necropolis", RAW_GRAVE_IRON, 2, GearArmor.NECROPOLIS_WARDEN, GearWeapons.WARDENS_SCOURGE,
							GearConsumables.AMBER_LANTERN_OIL, GearConsumables.AMBER_BALM));
			for (Zone zone : zones) {
				LootPool.Builder common = supplies(3, 5).with(item(zone.material, 20, 1, zone.materialMax));
				if (zone.name.equals("necropolis")) {
					// The Necropolis is dark: its graves keep lantern oil.
					common.with(item(GearConsumables.AMBER_LANTERN_OIL, 4, 1, 1));
				}
				exporter.accept(GearLootTables.common(zone.name), LootTable.builder().pool(common));
				exporter.accept(GearLootTables.treasure(zone.name), treasure(List.of(zone.material, GRAVE_IRON_INGOT),
						zone.set, zone.weapon, 0.12f, 0.25f, 0.3f));
				exporter.accept(GearLootTables.arena(zone.name), LootTable.builder()
						.pool(single(SMOLDERING_TEMPLATE))
						.pool(single(ASH_OF_REMATCH))
						.pool(LootPool.builder().rolls(UniformLootNumberProvider.create(2, 3))
								.with(item(zone.consumable, 10, 1, zone.consumable == GearConsumables.ASH_BOMB ? 4 : 2))
								.with(item(zone.secondConsumable, 8, 1, zone.secondConsumable == Items.ARROW ? 16 : 2))
								.with(item(zone.material, 6, 1, zone.materialMax))));
			}
			// The burning capital: no ore of its own, a little of everything; the keep holds the king's hoard.
			exporter.accept(GearLootTables.CITADEL_COMMON, LootTable.builder()
					.pool(supplies(3, 5)
							.with(item(CINDER_SHARD, 8, 1, 3))
							.with(item(ROYAL_AMBER, 6, 1, 2))
							.with(item(RAW_GRAVE_IRON, 5, 1, 2))
							.with(item(GearConsumables.SMOLDERING_RESIN, 4, 1, 1))
							.with(item(GearConsumables.AMBER_LANTERN_OIL, 3, 1, 1))));
			List<Item> anySet = Stream.of(GearArmor.REAPERS_GARB, GearArmor.HUNTSMANS_LEATHERS, GearArmor.NECROPOLIS_WARDEN)
					.flatMap(List::stream).toList();
			exporter.accept(GearLootTables.CITADEL_TREASURE, treasure(List.of(GRAVE_IRON_INGOT, CINDER_SHARD, ROYAL_AMBER),
					anySet, GearWeapons.ROYAL_ASH_RAPIER, 0.3f, 0.35f, 0.35f));
		}

		/** The everyday pool of a common chest: food, arrows, iron and gold, bombs, light. */
		private static LootPool.Builder supplies(int min, int max) {
			return LootPool.builder().rolls(UniformLootNumberProvider.create(min, max))
					.with(item(Items.BREAD, 15, 1, 3))
					.with(item(Items.BAKED_POTATO, 10, 1, 3))
					.with(item(Items.COOKED_RABBIT, 8, 1, 2))
					.with(item(Items.APPLE, 8, 1, 3))
					.with(item(Items.ARROW, 12, 2, 8))
					.with(item(Items.IRON_NUGGET, 10, 2, 6))
					.with(item(Items.IRON_INGOT, 6, 1, 2))
					.with(item(Items.GOLD_NUGGET, 8, 2, 6))
					.with(item(Items.GOLD_INGOT, 3, 1, 1))
					.with(item(GearConsumables.ASH_BOMB, 6, 1, 2))
					.with(item(GearConsumables.AMBER_BALM, 4, 1, 1))
					.with(item(Items.TORCH, 8, 2, 5))
					.with(item(Items.BONE, 8, 1, 3))
					.with(item(Items.STRING, 6, 1, 3));
		}

		/**
		 * A treasure chest: a rich pool of materials and consumables, then separate rolls for a Smoldering Ingot, a
		 * template, a set piece and the weapon (4 %).
		 */
		private static LootTable.Builder treasure(List<Item> materials, List<Item> set, Item weapon, float ingot,
				float template, float setPiece) {
			LootPool.Builder hoard = LootPool.builder().rolls(UniformLootNumberProvider.create(3, 5))
					.with(item(Items.GOLD_INGOT, 10, 1, 3))
					.with(item(Items.DIAMOND, 4, 1, 2))
					.with(item(Items.EMERALD, 4, 1, 2))
					.with(item(Items.GOLDEN_APPLE, 2, 1, 1))
					.with(item(GearConsumables.ASH_BOMB, 8, 2, 4))
					.with(item(GearConsumables.AMBER_BALM, 8, 1, 2))
					.with(item(GearConsumables.SMOLDERING_RESIN, 6, 1, 2))
					.with(item(GearConsumables.WHETSTONE, 5, 1, 1))
					.with(item(GearConsumables.AMBER_LANTERN_OIL, 5, 1, 1));
			for (Item material : materials) {
				hoard.with(item(material, 12, 2, 4));
			}
			LootPool.Builder setPool = LootPool.builder().conditionally(RandomChanceLootCondition.builder(setPiece));
			for (Item piece : set) {
				setPool.with(ItemEntry.builder(piece));
			}
			return LootTable.builder()
					.pool(hoard)
					.pool(chance(SMOLDERING_INGOT, ingot))
					.pool(chance(SMOLDERING_TEMPLATE, template))
					.pool(setPool)
					.pool(chance(weapon, 0.04f));
		}

		private static LootPoolEntry.Builder<?> item(Item item, int weight, int min, int max) {
			ItemEntry.Builder<?> entry = ItemEntry.builder(item).weight(weight);
			if (max > 1) {
				entry.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max)));
			}
			return entry;
		}

		private static LootPool.Builder single(Item item) {
			return LootPool.builder().with(ItemEntry.builder(item));
		}

		private static LootPool.Builder chance(Item item, float chance) {
			return LootPool.builder().conditionally(RandomChanceLootCondition.builder(chance)).with(ItemEntry.builder(item));
		}
	}

	private GearDataGen() {}
}
