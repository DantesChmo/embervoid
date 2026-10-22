package com.embervoid.client;

import com.embervoid.Embervoid;
import com.embervoid.client.mob.MobDataGen;
import com.embervoid.client.nightmob.NightMobDataGen;
import com.embervoid.sanctum.datagen.SanctumTemplateProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.embervoid.registry.ModBlocks.*;

/** Generates models, recipes, loot tables and tags: ./gradlew runDatagen (output: src/main/generated). */
public class EmbervoidDataGenerator implements DataGeneratorEntrypoint {
	static final TagKey<Block> DEAD_LOGS = TagKey.of(RegistryKeys.BLOCK, Embervoid.id("dead_logs"));
	static final TagKey<Item> DEAD_LOGS_ITEM = TagKey.of(RegistryKeys.ITEM, Embervoid.id("dead_logs"));

	/** Ash stone families: base block, stairs, slab, wall. */
	static final List<Block[]> STONE_FAMILIES = List.of(
			new Block[]{ASH_STONE, ASH_STONE_STAIRS, ASH_STONE_SLAB, ASH_STONE_WALL},
			new Block[]{COBBLED_ASH_STONE, COBBLED_ASH_STONE_STAIRS, COBBLED_ASH_STONE_SLAB, COBBLED_ASH_STONE_WALL},
			new Block[]{POLISHED_ASH_STONE, POLISHED_ASH_STONE_STAIRS, POLISHED_ASH_STONE_SLAB, POLISHED_ASH_STONE_WALL},
			new Block[]{ASH_BRICKS, ASH_BRICK_STAIRS, ASH_BRICK_SLAB, ASH_BRICK_WALL});

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(Models::new);
		pack.addProvider(BlockLoot::new);
		BlockTagGen blockTags = pack.addProvider(BlockTagGen::new);
		pack.addProvider((output, registries) -> new ItemTagGen(output, registries, blockTags));
		pack.addProvider(Recipes::new);
		MobDataGen.addProviders(pack);
		NightMobDataGen.addProviders(pack);
		pack.addProvider(SanctumTemplateProvider::new);
	}

	private static class Models extends FabricModelProvider {
		Models(FabricDataOutput output) {
			super(output);
		}

		@Override
		public void generateBlockStateModels(BlockStateModelGenerator gen) {
			for (Block[] f : STONE_FAMILIES) {
				gen.registerCubeAllModelTexturePool(f[0]).stairs(f[1]).slab(f[2]).wall(f[3]);
			}
			gen.registerSimpleCubeAll(CRACKED_ASH_BRICKS);
			gen.registerSimpleCubeAll(CHISELED_ASH_STONE);

			gen.registerLog(DEAD_LOG).log(DEAD_LOG);
			gen.registerLog(STRIPPED_DEAD_LOG).log(STRIPPED_DEAD_LOG);
			gen.registerCubeAllModelTexturePool(DEAD_PLANKS)
					.stairs(DEAD_STAIRS).slab(DEAD_SLAB).fence(DEAD_FENCE).fenceGate(DEAD_FENCE_GATE);
			gen.registerDoor(DEAD_DOOR);
			gen.registerOrientableTrapdoor(DEAD_TRAPDOOR);

			gen.registerSimpleCubeAll(ASH_SOIL);
			gen.registerLantern(EMBER_LANTERN);
		}

		@Override
		public void generateItemModels(ItemModelGenerator gen) {
		}
	}

	private static class BlockLoot extends FabricBlockLootTableProvider {
		BlockLoot(FabricDataOutput output) {
			super(output);
		}

		@Override
		public void generate() {
			for (Block[] f : STONE_FAMILIES) {
				addDrop(f[1]);
				addDrop(f[2], slabDrops(f[2]));
				addDrop(f[3]);
			}
			// Like stone: breaks into cobble unless mined with Silk Touch.
			addDrop(ASH_STONE, drops(ASH_STONE, COBBLED_ASH_STONE));
			addDrop(COBBLED_ASH_STONE);
			addDrop(POLISHED_ASH_STONE);
			addDrop(ASH_BRICKS);
			addDrop(CRACKED_ASH_BRICKS);
			addDrop(CHISELED_ASH_STONE);

			addDrop(DEAD_LOG);
			addDrop(STRIPPED_DEAD_LOG);
			addDrop(DEAD_PLANKS);
			addDrop(DEAD_STAIRS);
			addDrop(DEAD_SLAB, slabDrops(DEAD_SLAB));
			addDrop(DEAD_FENCE);
			addDrop(DEAD_FENCE_GATE);
			addDrop(DEAD_DOOR, doorDrops(DEAD_DOOR));
			addDrop(DEAD_TRAPDOOR);

			addDrop(ASH_SOIL);
			addDrop(EMBER_LANTERN);
		}
	}

	private static class BlockTagGen extends FabricTagProvider.BlockTagProvider {
		BlockTagGen(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
			super(output, registries);
		}

		@Override
		protected void configure(RegistryWrapper.WrapperLookup registries) {
			var pickaxe = getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE);
			for (Block[] f : STONE_FAMILIES) {
				pickaxe.add(f);
				getOrCreateTagBuilder(BlockTags.STAIRS).add(f[1]);
				getOrCreateTagBuilder(BlockTags.SLABS).add(f[2]);
				getOrCreateTagBuilder(BlockTags.WALLS).add(f[3]);
			}
			pickaxe.add(CRACKED_ASH_BRICKS, CHISELED_ASH_STONE, EMBER_LANTERN);

			getOrCreateTagBuilder(DEAD_LOGS).add(DEAD_LOG, STRIPPED_DEAD_LOG);
			getOrCreateTagBuilder(BlockTags.LOGS_THAT_BURN).addTag(DEAD_LOGS);
			getOrCreateTagBuilder(BlockTags.PLANKS).add(DEAD_PLANKS);
			getOrCreateTagBuilder(BlockTags.WOODEN_STAIRS).add(DEAD_STAIRS);
			getOrCreateTagBuilder(BlockTags.WOODEN_SLABS).add(DEAD_SLAB);
			getOrCreateTagBuilder(BlockTags.WOODEN_FENCES).add(DEAD_FENCE);
			getOrCreateTagBuilder(BlockTags.FENCE_GATES).add(DEAD_FENCE_GATE);
			getOrCreateTagBuilder(BlockTags.WOODEN_DOORS).add(DEAD_DOOR);
			getOrCreateTagBuilder(BlockTags.WOODEN_TRAPDOORS).add(DEAD_TRAPDOOR);
			getOrCreateTagBuilder(BlockTags.AXE_MINEABLE).addTag(DEAD_LOGS)
					.add(DEAD_PLANKS, DEAD_STAIRS, DEAD_SLAB, DEAD_FENCE, DEAD_FENCE_GATE, DEAD_DOOR, DEAD_TRAPDOOR);

			getOrCreateTagBuilder(BlockTags.SHOVEL_MINEABLE).add(ASH_SOIL);
		}
	}

	private static class ItemTagGen extends FabricTagProvider.ItemTagProvider {
		ItemTagGen(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries, BlockTagGen blockTags) {
			super(output, registries, blockTags);
		}

		@Override
		protected void configure(RegistryWrapper.WrapperLookup registries) {
			copy(BlockTags.STAIRS, ItemTags.STAIRS);
			copy(BlockTags.SLABS, ItemTags.SLABS);
			copy(BlockTags.WALLS, ItemTags.WALLS);
			copy(DEAD_LOGS, DEAD_LOGS_ITEM);
			copy(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN);
			copy(BlockTags.PLANKS, ItemTags.PLANKS);
			copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
			copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
			copy(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES);
			copy(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES);
			copy(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS);
			copy(BlockTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_TRAPDOORS);
		}
	}

	private static class Recipes extends FabricRecipeProvider {
		Recipes(FabricDataOutput output) {
			super(output);
		}

		@Override
		public void generate(Consumer<RecipeJsonProvider> exporter) {
			RecipeCategory building = RecipeCategory.BUILDING_BLOCKS;

			// Each family: stairs/slab/wall by crafting and by stonecutter from its base.
			for (Block[] f : STONE_FAMILIES) {
				createStairsRecipe(f[1], Ingredient.ofItems(f[0]))
						.criterion(hasItem(f[0]), conditionsFromItem(f[0])).offerTo(exporter);
				offerSlabRecipe(exporter, building, f[2], f[0]);
				offerWallRecipe(exporter, RecipeCategory.DECORATIONS, f[3], f[0]);
				offerStonecuttingRecipe(exporter, building, f[1], f[0]);
				offerStonecuttingRecipe(exporter, building, f[2], f[0], 2);
				offerStonecuttingRecipe(exporter, RecipeCategory.DECORATIONS, f[3], f[0]);
			}

			offerSmelting(exporter, List.of(COBBLED_ASH_STONE), building, ASH_STONE, 0.1f, 200, "ash_stone");
			offerPolishedStoneRecipe(exporter, building, POLISHED_ASH_STONE, ASH_STONE);
			offerPolishedStoneRecipe(exporter, building, ASH_BRICKS, POLISHED_ASH_STONE);
			offerCrackingRecipe(exporter, CRACKED_ASH_BRICKS, ASH_BRICKS);
			offerChiseledBlockRecipe(exporter, building, CHISELED_ASH_STONE, POLISHED_ASH_STONE_SLAB);

			// Stonecutter shortcuts down the chain: ash stone -> polished -> bricks.
			for (Block input : List.of(ASH_STONE, POLISHED_ASH_STONE)) {
				if (input == ASH_STONE) {
					offerStonecuttingRecipe(exporter, building, POLISHED_ASH_STONE, input);
					offerStonecuttingRecipe(exporter, building, POLISHED_ASH_STONE_STAIRS, input);
					offerStonecuttingRecipe(exporter, building, POLISHED_ASH_STONE_SLAB, input, 2);
					offerStonecuttingRecipe(exporter, RecipeCategory.DECORATIONS, POLISHED_ASH_STONE_WALL, input);
				}
				offerStonecuttingRecipe(exporter, building, ASH_BRICKS, input);
				offerStonecuttingRecipe(exporter, building, ASH_BRICK_STAIRS, input);
				offerStonecuttingRecipe(exporter, building, ASH_BRICK_SLAB, input, 2);
				offerStonecuttingRecipe(exporter, RecipeCategory.DECORATIONS, ASH_BRICK_WALL, input);
				offerStonecuttingRecipe(exporter, building, CHISELED_ASH_STONE, input);
			}

			offerPlanksRecipe(exporter, DEAD_PLANKS, DEAD_LOGS_ITEM, 4);
			createStairsRecipe(DEAD_STAIRS, Ingredient.ofItems(DEAD_PLANKS))
					.criterion(hasItem(DEAD_PLANKS), conditionsFromItem(DEAD_PLANKS)).offerTo(exporter);
			offerSlabRecipe(exporter, building, DEAD_SLAB, DEAD_PLANKS);
			createFenceRecipe(DEAD_FENCE, Ingredient.ofItems(DEAD_PLANKS))
					.criterion(hasItem(DEAD_PLANKS), conditionsFromItem(DEAD_PLANKS)).offerTo(exporter);
			createFenceGateRecipe(DEAD_FENCE_GATE, Ingredient.ofItems(DEAD_PLANKS))
					.criterion(hasItem(DEAD_PLANKS), conditionsFromItem(DEAD_PLANKS)).offerTo(exporter);
			createDoorRecipe(DEAD_DOOR, Ingredient.ofItems(DEAD_PLANKS))
					.criterion(hasItem(DEAD_PLANKS), conditionsFromItem(DEAD_PLANKS)).offerTo(exporter);
			createTrapdoorRecipe(DEAD_TRAPDOOR, Ingredient.ofItems(DEAD_PLANKS))
					.criterion(hasItem(DEAD_PLANKS), conditionsFromItem(DEAD_PLANKS)).offerTo(exporter);

			// Lantern ring with a torch and a lump of coal smouldering beneath it.
			ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, EMBER_LANTERN)
					.pattern("NNN")
					.pattern("NTN")
					.pattern("NCN")
					.input('N', Items.IRON_NUGGET)
					.input('T', Items.TORCH)
					.input('C', ItemTags.COALS)
					.criterion(hasItem(Items.IRON_NUGGET), conditionsFromItem(Items.IRON_NUGGET))
					.offerTo(exporter);
		}
	}
}
