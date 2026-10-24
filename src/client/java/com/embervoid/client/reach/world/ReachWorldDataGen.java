package com.embervoid.client.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.reach.world.ReachBlocks;
import com.embervoid.registry.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.BlockStateVariant;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.ModelIds;
import net.minecraft.data.client.MultipartBlockStateSupplier;
import net.minecraft.data.client.TextureKey;
import net.minecraft.data.client.TextureMap;
import net.minecraft.data.client.When;
import net.minecraft.data.client.TexturedModel;
import net.minecraft.data.client.VariantSettings;
import net.minecraft.data.client.VariantsBlockStateSupplier;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.state.property.Properties;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/** Datagen for the Ashen Reach blocks and ores (PLAN 9.5): models, loot, tags. */
public final class ReachWorldDataGen {
	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(Models::new);
		pack.addProvider(Loot::new);
		pack.addProvider(Tags::new);
		pack.addProvider(Recipes::new);
		pack.addProvider(ReachTemplateProvider::new);
	}

	private static class Models extends FabricModelProvider {
		Models(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Reach World Models";
		}

		@Override
		public void generateBlockStateModels(BlockStateModelGenerator gen) {
			for (Block block : new Block[]{ReachBlocks.CINDER_ORE, ReachBlocks.AMBER_VEIN, ReachBlocks.GRAVE_IRON_ORE,
					ReachBlocks.EMBER_CRACK, ReachBlocks.WET_CRYPT_STONE, ReachBlocks.KINGSROAD_FLAGSTONES, ReachBlocks.KINGSROAD_KERB,
					ReachBlocks.GILDED_ASH_STONE, ReachBlocks.EMBER_GLASS, ReachBlocks.GRAVE_IRON_PLATE, ReachBlocks.RUSTED_BRASS_BLOCK,
					ReachBlocks.CUT_BRASS, ReachBlocks.TAR_BLOCK, ReachBlocks.ASH_COBBLE_PATH, ReachBlocks.AMBER_BLOCK,
					ReachBlocks.CRYPT_STONE_TILES}) {
				gen.registerSimpleCubeAll(block);
			}
			gen.registerAxisRotated(ReachBlocks.BURNT_STRAW, TexturedModel.CUBE_COLUMN);
			gen.registerAxisRotated(ReachBlocks.BURNT_THATCH, TexturedModel.CUBE_COLUMN);
			gen.registerAxisRotated(ReachBlocks.BONE_PILLAR, TexturedModel.CUBE_COLUMN);
			gen.registerLog(ReachBlocks.SMOLDERING_LOG).log(ReachBlocks.SMOLDERING_LOG);
			gen.registerLog(ReachBlocks.CHARRED_LOG).log(ReachBlocks.CHARRED_LOG);
			gen.registerCubeAllModelTexturePool(ReachBlocks.CHARRED_PLANKS).stairs(ReachBlocks.CHARRED_STAIRS).slab(ReachBlocks.CHARRED_SLAB);
			gen.registerCubeAllModelTexturePool(ReachBlocks.BONE_BRICKS).wall(ReachBlocks.BONE_BRICK_WALL);
			grate(gen, ReachBlocks.GRAVE_IRON_GRATE);
			// Milestone and gravestone: hand-made models in assets (models/block/<name>.json), turned by facing.
			for (Block block : new Block[]{ReachBlocks.MILESTONE, ReachBlocks.GRAVESTONE}) {
				Identifier model = Embervoid.id("block/" + Registries.BLOCK.getId(block).getPath());
				gen.blockStateCollector.accept(VariantsBlockStateSupplier.create(block,
								BlockStateVariant.create().put(VariantSettings.MODEL, model))
						.coordinate(BlockStateModelGenerator.createNorthDefaultHorizontalRotationStates()));
				gen.registerParentedItemModel(block, model);
			}
		}

		/**
		 * A pane like iron bars from its own textures ({@code <name>} for the bars, {@code <name>_top} for the post and
		 * the ends), the blockstate laid out as vanilla's iron bars.
		 */
		private static void grate(BlockStateModelGenerator gen, Block block) {
			String name = Registries.BLOCK.getId(block).getPath();
			TextureMap textures = new TextureMap().put(TextureKey.PANE, Embervoid.id("block/" + name))
					.put(TextureKey.EDGE, Embervoid.id("block/" + name + "_top"));
			Identifier post = net.minecraft.data.client.Models.TEMPLATE_GLASS_PANE_POST.upload(block, textures, gen.modelCollector);
			Identifier side = net.minecraft.data.client.Models.TEMPLATE_GLASS_PANE_SIDE.upload(block, textures, gen.modelCollector);
			Identifier sideAlt = net.minecraft.data.client.Models.TEMPLATE_GLASS_PANE_SIDE_ALT.upload(block, textures, gen.modelCollector);
			Identifier noSide = net.minecraft.data.client.Models.TEMPLATE_GLASS_PANE_NOSIDE.upload(block, textures, gen.modelCollector);
			Identifier noSideAlt = net.minecraft.data.client.Models.TEMPLATE_GLASS_PANE_NOSIDE_ALT.upload(block, textures, gen.modelCollector);
			net.minecraft.data.client.Models.GENERATED.upload(ModelIds.getItemModelId(block.asItem()), TextureMap.layer0(Embervoid.id("block/" + name)),
					gen.modelCollector);
			gen.blockStateCollector.accept(MultipartBlockStateSupplier.create(block)
					.with(BlockStateVariant.create().put(VariantSettings.MODEL, post))
					.with(When.create().set(Properties.NORTH, true), BlockStateVariant.create().put(VariantSettings.MODEL, side))
					.with(When.create().set(Properties.EAST, true), BlockStateVariant.create().put(VariantSettings.MODEL, side).put(VariantSettings.Y, VariantSettings.Rotation.R90))
					.with(When.create().set(Properties.SOUTH, true), BlockStateVariant.create().put(VariantSettings.MODEL, sideAlt))
					.with(When.create().set(Properties.WEST, true), BlockStateVariant.create().put(VariantSettings.MODEL, sideAlt).put(VariantSettings.Y, VariantSettings.Rotation.R90))
					.with(When.create().set(Properties.NORTH, false), BlockStateVariant.create().put(VariantSettings.MODEL, noSide))
					.with(When.create().set(Properties.EAST, false), BlockStateVariant.create().put(VariantSettings.MODEL, noSideAlt))
					.with(When.create().set(Properties.SOUTH, false), BlockStateVariant.create().put(VariantSettings.MODEL, noSideAlt).put(VariantSettings.Y, VariantSettings.Rotation.R90))
					.with(When.create().set(Properties.WEST, false), BlockStateVariant.create().put(VariantSettings.MODEL, noSide).put(VariantSettings.Y, VariantSettings.Rotation.R270)));
		}

		@Override
		public void generateItemModels(ItemModelGenerator gen) {
		}
	}

	private static class Loot extends FabricBlockLootTableProvider {
		Loot(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Reach World Block Loot";
		}

		@Override
		public void generate() {
			for (Block block : new Block[]{ReachBlocks.BURNT_STRAW, ReachBlocks.MILESTONE, ReachBlocks.GRAVESTONE,
					ReachBlocks.EMBER_CRACK, ReachBlocks.SMOLDERING_LOG, ReachBlocks.WET_CRYPT_STONE,
					ReachBlocks.KINGSROAD_FLAGSTONES, ReachBlocks.KINGSROAD_KERB, ReachBlocks.GILDED_ASH_STONE, ReachBlocks.CHARRED_PLANKS,
					ReachBlocks.CHARRED_STAIRS, ReachBlocks.CHARRED_LOG, ReachBlocks.BURNT_THATCH, ReachBlocks.BONE_BRICKS,
					ReachBlocks.BONE_BRICK_WALL, ReachBlocks.BONE_PILLAR, ReachBlocks.GRAVE_IRON_GRATE, ReachBlocks.GRAVE_IRON_PLATE,
					ReachBlocks.RUSTED_BRASS_BLOCK, ReachBlocks.CUT_BRASS, ReachBlocks.TAR_BLOCK, ReachBlocks.ASH_COBBLE_PATH,
					ReachBlocks.AMBER_BLOCK, ReachBlocks.CRYPT_STONE_TILES}) {
				addDrop(block);
			}
			addDrop(ReachBlocks.CHARRED_SLAB, slabDrops(ReachBlocks.CHARRED_SLAB));
			addDrop(ReachBlocks.EMBER_GLASS, dropsWithSilkTouch(ReachBlocks.EMBER_GLASS));
			addDrop(ReachBlocks.CINDER_ORE, block -> countedOre(block, GearItems.CINDER_SHARD, 1, 3));
			addDrop(ReachBlocks.AMBER_VEIN, block -> countedOre(block, GearItems.ROYAL_AMBER, 1, 2));
			addDrop(ReachBlocks.GRAVE_IRON_ORE, block -> oreDrops(block, GearItems.RAW_GRAVE_IRON));
		}

		/** min–max of an item, Fortune multiplies like vanilla ores; silk touch drops the ore. */
		private LootTable.Builder countedOre(Block block, Item item, int min, int max) {
			return dropsWithSilkTouch(block, applyExplosionDecay(block, ItemEntry.builder(item)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max)))
					.apply(ApplyBonusLootFunction.oreDrops(Enchantments.FORTUNE))));
		}
	}

	/**
	 * Tool tags of the blocks. The vanilla tag files are written by the main block tag provider (two providers cannot
	 * write the same file), which calls this.
	 */
	public static void vanillaBlockTags(Function<TagKey<Block>, FabricTagProvider<Block>.FabricTagBuilder> tags) {
		tags.apply(BlockTags.PICKAXE_MINEABLE).add(ReachBlocks.MILESTONE, ReachBlocks.GRAVESTONE, ReachBlocks.EMBER_CRACK,
				ReachBlocks.WET_CRYPT_STONE, ReachBlocks.KINGSROAD_FLAGSTONES, ReachBlocks.KINGSROAD_KERB,
				ReachBlocks.CINDER_ORE, ReachBlocks.AMBER_VEIN, ReachBlocks.GRAVE_IRON_ORE, ReachBlocks.GILDED_ASH_STONE,
				ReachBlocks.BONE_BRICKS, ReachBlocks.BONE_BRICK_WALL, ReachBlocks.BONE_PILLAR, ReachBlocks.GRAVE_IRON_GRATE,
				ReachBlocks.GRAVE_IRON_PLATE, ReachBlocks.RUSTED_BRASS_BLOCK, ReachBlocks.CUT_BRASS, ReachBlocks.TAR_BLOCK,
				ReachBlocks.AMBER_BLOCK, ReachBlocks.CRYPT_STONE_TILES);
		tags.apply(BlockTags.AXE_MINEABLE).add(ReachBlocks.SMOLDERING_LOG, ReachBlocks.CHARRED_PLANKS, ReachBlocks.CHARRED_STAIRS,
				ReachBlocks.CHARRED_SLAB, ReachBlocks.CHARRED_LOG);
		tags.apply(BlockTags.HOE_MINEABLE).add(ReachBlocks.BURNT_STRAW, ReachBlocks.BURNT_THATCH);
		tags.apply(BlockTags.SHOVEL_MINEABLE).add(ReachBlocks.ASH_COBBLE_PATH);
		tags.apply(BlockTags.STAIRS).add(ReachBlocks.CHARRED_STAIRS);
		tags.apply(BlockTags.SLABS).add(ReachBlocks.CHARRED_SLAB);
		tags.apply(BlockTags.WALLS).add(ReachBlocks.BONE_BRICK_WALL);
		tags.apply(BlockTags.IMPERMEABLE).add(ReachBlocks.EMBER_GLASS);
		tags.apply(BlockTags.NEEDS_IRON_TOOL).add(ReachBlocks.CINDER_ORE, ReachBlocks.AMBER_VEIN);
		tags.apply(BlockTags.NEEDS_DIAMOND_TOOL).add(ReachBlocks.GRAVE_IRON_ORE);
	}

	private static class Tags extends FabricTagProvider.BlockTagProvider {
		Tags(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
			super(output, registries);
		}

		@Override
		public String getName() {
			return "Reach World Block Tags";
		}

		@Override
		protected void configure(RegistryWrapper.WrapperLookup registries) {
			getOrCreateTagBuilder(ReachBlocks.REALM_ORES).add(ReachBlocks.CINDER_ORE, ReachBlocks.AMBER_VEIN,
					ReachBlocks.GRAVE_IRON_ORE);
		}
	}

	/** Recipes of the art pass's zone blocks: from their zone's materials and the ash stone chain. */
	private static class Recipes extends FabricRecipeProvider {
		Recipes(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Reach World Recipes";
		}

		@Override
		public void generate(Consumer<RecipeJsonProvider> exporter) {
			RecipeCategory building = RecipeCategory.BUILDING_BLOCKS;
			ShapelessRecipeJsonBuilder.create(building, ReachBlocks.GILDED_ASH_STONE).input(ModBlocks.POLISHED_ASH_STONE).input(Items.GOLD_NUGGET)
					.criterion(hasItem(Items.GOLD_NUGGET), conditionsFromItem(Items.GOLD_NUGGET)).offerTo(exporter);
			ShapedRecipeJsonBuilder.create(building, ReachBlocks.EMBER_GLASS, 8).pattern("GGG").pattern("GSG").pattern("GGG")
					.input('G', Blocks.GLASS).input('S', GearItems.CINDER_SHARD)
					.criterion(hasItem(GearItems.CINDER_SHARD), conditionsFromItem(GearItems.CINDER_SHARD)).offerTo(exporter);
			offerSmelting(exporter, List.of(ModBlocks.DEAD_PLANKS), building, ReachBlocks.CHARRED_PLANKS, 0.1f, 100, "charred_planks");
			offerSmelting(exporter, List.of(ModBlocks.DEAD_LOG, ModBlocks.STRIPPED_DEAD_LOG), building, ReachBlocks.CHARRED_LOG, 0.1f, 100, "charred_log");
			createStairsRecipe(ReachBlocks.CHARRED_STAIRS, Ingredient.ofItems(ReachBlocks.CHARRED_PLANKS))
					.criterion(hasItem(ReachBlocks.CHARRED_PLANKS), conditionsFromItem(ReachBlocks.CHARRED_PLANKS)).offerTo(exporter);
			offerSlabRecipe(exporter, building, ReachBlocks.CHARRED_SLAB, ReachBlocks.CHARRED_PLANKS);
			offerPolishedStoneRecipe(exporter, building, ReachBlocks.BURNT_THATCH, ReachBlocks.BURNT_STRAW);
			offerPolishedStoneRecipe(exporter, building, ReachBlocks.BONE_BRICKS, Blocks.BONE_BLOCK);
			offerWallRecipe(exporter, RecipeCategory.DECORATIONS, ReachBlocks.BONE_BRICK_WALL, ReachBlocks.BONE_BRICKS);
			ShapedRecipeJsonBuilder.create(building, ReachBlocks.BONE_PILLAR, 2).pattern("B").pattern("B").input('B', ReachBlocks.BONE_BRICKS)
					.criterion(hasItem(ReachBlocks.BONE_BRICKS), conditionsFromItem(ReachBlocks.BONE_BRICKS)).offerTo(exporter);
			offerStonecuttingRecipe(exporter, building, ReachBlocks.BONE_BRICKS, Blocks.BONE_BLOCK);
			offerStonecuttingRecipe(exporter, RecipeCategory.DECORATIONS, ReachBlocks.BONE_BRICK_WALL, Blocks.BONE_BLOCK);
			offerStonecuttingRecipe(exporter, building, ReachBlocks.BONE_PILLAR, Blocks.BONE_BLOCK);
			ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, ReachBlocks.GRAVE_IRON_GRATE, 16).pattern("III").pattern("III")
					.input('I', GearItems.GRAVE_IRON_INGOT)
					.criterion(hasItem(GearItems.GRAVE_IRON_INGOT), conditionsFromItem(GearItems.GRAVE_IRON_INGOT)).offerTo(exporter);
			ShapedRecipeJsonBuilder.create(building, ReachBlocks.GRAVE_IRON_PLATE, 4).pattern("II").pattern("II")
					.input('I', GearItems.GRAVE_IRON_INGOT)
					.criterion(hasItem(GearItems.GRAVE_IRON_INGOT), conditionsFromItem(GearItems.GRAVE_IRON_INGOT)).offerTo(exporter);
			ShapedRecipeJsonBuilder.create(building, ReachBlocks.RUSTED_BRASS_BLOCK).pattern("CG").pattern("GC")
					.input('C', Items.COPPER_INGOT).input('G', Items.GOLD_NUGGET)
					.criterion(hasItem(Items.COPPER_INGOT), conditionsFromItem(Items.COPPER_INGOT)).offerTo(exporter);
			offerPolishedStoneRecipe(exporter, building, ReachBlocks.CUT_BRASS, ReachBlocks.RUSTED_BRASS_BLOCK);
			offerStonecuttingRecipe(exporter, building, ReachBlocks.CUT_BRASS, ReachBlocks.RUSTED_BRASS_BLOCK);
			ShapelessRecipeJsonBuilder.create(building, ReachBlocks.TAR_BLOCK).input(ModBlocks.ASH_SOIL).input(ItemTags.COALS)
					.criterion(hasItem(ModBlocks.ASH_SOIL), conditionsFromItem(ModBlocks.ASH_SOIL)).offerTo(exporter);
			ShapelessRecipeJsonBuilder.create(building, ReachBlocks.ASH_COBBLE_PATH, 2).input(ModBlocks.ASH_SOIL).input(ModBlocks.COBBLED_ASH_STONE)
					.criterion(hasItem(ModBlocks.ASH_SOIL), conditionsFromItem(ModBlocks.ASH_SOIL)).offerTo(exporter);
			offerReversibleCompactingRecipes(exporter, RecipeCategory.MISC, GearItems.ROYAL_AMBER, building, ReachBlocks.AMBER_BLOCK,
					"amber_block_from_royal_amber", null, "royal_amber_from_amber_block", null);
			offerPolishedStoneRecipe(exporter, building, ReachBlocks.CRYPT_STONE_TILES, ReachBlocks.WET_CRYPT_STONE);
			offerStonecuttingRecipe(exporter, building, ReachBlocks.CRYPT_STONE_TILES, ReachBlocks.WET_CRYPT_STONE);
		}
	}

	private ReachWorldDataGen() {}
}
