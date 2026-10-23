package com.embervoid.client.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.reach.world.ReachBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.BlockStateVariant;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.TexturedModel;
import net.minecraft.data.client.VariantSettings;
import net.minecraft.data.client.VariantsBlockStateSupplier;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Datagen for the Ashen Reach blocks and ores (PLAN 9.5): models, loot, tags. */
public final class ReachWorldDataGen {
	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(Models::new);
		pack.addProvider(Loot::new);
		pack.addProvider(Tags::new);
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
					ReachBlocks.EMBER_CRACK, ReachBlocks.WET_CRYPT_STONE, ReachBlocks.KINGSROAD_FLAGSTONES, ReachBlocks.KINGSROAD_KERB}) {
				gen.registerSimpleCubeAll(block);
			}
			gen.registerAxisRotated(ReachBlocks.BURNT_STRAW, TexturedModel.CUBE_COLUMN);
			gen.registerLog(ReachBlocks.SMOLDERING_LOG).log(ReachBlocks.SMOLDERING_LOG);
			// Milestone and gravestone: hand-made models in assets (models/block/<name>.json), turned by facing.
			for (Block block : new Block[]{ReachBlocks.MILESTONE, ReachBlocks.GRAVESTONE}) {
				Identifier model = Embervoid.id("block/" + Registries.BLOCK.getId(block).getPath());
				gen.blockStateCollector.accept(VariantsBlockStateSupplier.create(block,
								BlockStateVariant.create().put(VariantSettings.MODEL, model))
						.coordinate(BlockStateModelGenerator.createNorthDefaultHorizontalRotationStates()));
				gen.registerParentedItemModel(block, model);
			}
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
					ReachBlocks.KINGSROAD_FLAGSTONES, ReachBlocks.KINGSROAD_KERB}) {
				addDrop(block);
			}
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
				ReachBlocks.CINDER_ORE, ReachBlocks.AMBER_VEIN, ReachBlocks.GRAVE_IRON_ORE);
		tags.apply(BlockTags.AXE_MINEABLE).add(ReachBlocks.SMOLDERING_LOG);
		tags.apply(BlockTags.HOE_MINEABLE).add(ReachBlocks.BURNT_STRAW);
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

	private ReachWorldDataGen() {}
}
