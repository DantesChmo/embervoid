package com.embervoid.client.reach.gear;

import com.embervoid.reach.gear.GearLootTables;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.embervoid.reach.gear.GearItems.*;

/** Datagen for the reach-gear track: item models, recipes, Ashen Reach chest loot tables. */
public final class GearDataGen {
	/** Flat items drawn by {@code tools/gear_placeholders.py}. */
	static final List<Item> FLAT_ITEMS = List.of(CINDER_SHARD, ROYAL_AMBER, RAW_GRAVE_IRON, GRAVE_IRON_INGOT,
			SMOLDERING_INGOT, SMOLDERING_TEMPLATE, ASH_COLOSSUS_SOUL, GRIEVING_TREE_SOUL, CRYPT_KEEPER_SOUL,
			SMOLDERING_LORD_SOUL, ASH_OF_REMATCH, FLASK_EMBER);

	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(ItemModels::new);
		pack.addProvider(Recipes::new);
		pack.addProvider(ChestLoot::new);
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

			// DESIGN §10.11, built as recommended.
			ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ASH_OF_REMATCH)
					.input(CINDER_SHARD, 4)
					.input(ROYAL_AMBER)
					.input(Items.BONE)
					.criterion(hasItem(CINDER_SHARD), conditionsFromItem(CINDER_SHARD))
					.offerTo(exporter);
		}
	}

	private static class ChestLoot extends SimpleFabricLootTableProvider {
		ChestLoot(FabricDataOutput output) {
			super(output, LootContextTypes.CHEST);
		}

		@Override
		public String getName() {
			return "Embervoid Ashen Reach chest loot tables";
		}

		@Override
		public void accept(BiConsumer<Identifier, LootTable.Builder> exporter) {
			for (Identifier id : GearLootTables.all()) {
				exporter.accept(id, LootTable.builder());
			}
		}
	}

	private GearDataGen() {}
}
