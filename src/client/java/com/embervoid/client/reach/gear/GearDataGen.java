package com.embervoid.client.reach.gear;

import com.embervoid.Embervoid;
import com.embervoid.reach.gear.Bleeding;
import com.embervoid.reach.gear.GearArmor;
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
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.embervoid.reach.gear.GearItems.*;

/** Datagen for the reach-gear track: item models, recipes, Ashen Reach chest loot tables, tags. */
public final class GearDataGen {
	/** Flat items drawn by {@code tools/gear_placeholders.py}. */
	static final List<Item> FLAT_ITEMS = List.of(CINDER_SHARD, ROYAL_AMBER, RAW_GRAVE_IRON, GRAVE_IRON_INGOT,
			SMOLDERING_INGOT, SMOLDERING_TEMPLATE, ASH_COLOSSUS_SOUL, GRIEVING_TREE_SOUL, CRYPT_KEEPER_SOUL,
			SMOLDERING_LORD_SOUL, ASH_OF_REMATCH, FLASK_EMBER);

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
			// Bloodless: bones, a furnace, bark, ash and stone. Optional entries: the mobs and bosses are other tracks'.
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

			// Weapons: the boss's soul + 2 Smoldering Ingot + a part from its zone (a reaper's hoe, a crypt chain, a
			// courtier's gilded blade, a hunter's crossbow). The crossbow's models are hand-written resources.
			weapon(exporter, GearWeapons.ASHEN_SCYTHE, ASH_COLOSSUS_SOUL, Items.IRON_HOE);
			weapon(exporter, GearWeapons.WARDENS_SCOURGE, CRYPT_KEEPER_SOUL, Items.CHAIN);
			weapon(exporter, GearWeapons.ROYAL_ASH_RAPIER, SMOLDERING_LORD_SOUL, Items.GOLDEN_SWORD);
			weapon(exporter, GearWeapons.HUNTSMANS_CROSSBOW, GRIEVING_TREE_SOUL, Items.CROSSBOW);

			// DESIGN §10.11, built as recommended.
			ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ASH_OF_REMATCH)
					.input(CINDER_SHARD, 4)
					.input(ROYAL_AMBER)
					.input(Items.BONE)
					.criterion(hasItem(CINDER_SHARD), conditionsFromItem(CINDER_SHARD))
					.offerTo(exporter);
		}
	}

	static void weapon(Consumer<RecipeJsonProvider> exporter, Item weapon, Item soul, Item part) {
		ShapelessRecipeJsonBuilder.create(RecipeCategory.COMBAT, weapon)
				.input(soul)
				.input(SMOLDERING_INGOT, 2)
				.input(part)
				.criterion(FabricRecipeProvider.hasItem(soul), FabricRecipeProvider.conditionsFromItem(soul))
				.offerTo(exporter);
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
