package com.embervoid.hub.stele;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.BlockStateVariant;
import net.minecraft.data.client.BlockStateVariantMap;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Model;
import net.minecraft.data.client.Models;
import net.minecraft.data.client.TextureKey;
import net.minecraft.data.client.TextureMap;
import net.minecraft.data.client.VariantSettings;
import net.minecraft.data.client.VariantsBlockStateSupplier;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** Datagen for the Cleft Stele: models, loot, the Cleft Stone recipe and tool tag. */
public final class SteleDataGen {
	private static final TextureKey PORTAL = TextureKey.of("portal");
	/** Vanilla's nether portal sheets, retextured: the crack is the same thin pane. */
	private static final Model CRACK_NS = new Model(Optional.of(new Identifier("block/nether_portal_ns")),
			Optional.of("_ns"), PORTAL, TextureKey.PARTICLE);
	private static final Model CRACK_EW = new Model(Optional.of(new Identifier("block/nether_portal_ew")),
			Optional.of("_ew"), PORTAL, TextureKey.PARTICLE);

	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(ModelGen::new);
		pack.addProvider(Loot::new);
		pack.addProvider(Recipes::new);
	}

	/**
	 * Tool tag of the stone. The vanilla tag files are written by the main block tag provider (two providers cannot
	 * write the same file), which calls this.
	 */
	public static void vanillaBlockTags(Function<TagKey<Block>, FabricTagProvider<Block>.FabricTagBuilder> tags) {
		tags.apply(BlockTags.PICKAXE_MINEABLE).add(SteleBlocks.CLEFT_STONE);
	}

	private static class ModelGen extends FabricModelProvider {
		ModelGen(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Cleft Stele Models";
		}

		@Override
		public void generateBlockStateModels(BlockStateModelGenerator gen) {
			Block stone = SteleBlocks.CLEFT_STONE;
			Identifier cold = Models.CUBE_ALL.upload(stone, TextureMap.all(stone), gen.modelCollector);
			Identifier kindledTexture = TextureMap.getSubId(stone, "_kindled");
			Identifier kindled = Models.CUBE_ALL.upload(kindledTexture, TextureMap.all(kindledTexture), gen.modelCollector);
			gen.blockStateCollector.accept(VariantsBlockStateSupplier.create(stone)
					.coordinate(BlockStateModelGenerator.createBooleanModelMap(CleftStoneBlock.KINDLED, kindled, cold)));
			gen.registerParentedItemModel(stone, cold);

			Block crack = SteleBlocks.STELE_CRACK;
			TextureMap mist = new TextureMap().put(PORTAL, TextureMap.getId(crack)).put(TextureKey.PARTICLE, TextureMap.getId(crack));
			gen.blockStateCollector.accept(VariantsBlockStateSupplier.create(crack)
					.coordinate(BlockStateVariantMap.create(SteleCrackBlock.AXIS)
							.register(Direction.Axis.X, BlockStateVariant.create()
									.put(VariantSettings.MODEL, CRACK_NS.upload(crack, mist, gen.modelCollector)))
							.register(Direction.Axis.Z, BlockStateVariant.create()
									.put(VariantSettings.MODEL, CRACK_EW.upload(crack, mist, gen.modelCollector)))));

			// The ritual circle's rift: the crack's mist on two crossed sheets, seen from every side.
			Block rift = SteleBlocks.STELE_RIFT;
			gen.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(rift,
					Models.CROSS.upload(rift, TextureMap.cross(TextureMap.getId(crack)), gen.modelCollector)));
		}

		@Override
		public void generateItemModels(ItemModelGenerator gen) {
			gen.register(SteleItems.CRYING_SOUL, Models.GENERATED);
		}
	}

	private static class Loot extends FabricBlockLootTableProvider {
		Loot(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Cleft Stele Block Loot";
		}

		@Override
		public void generate() {
			// Kindled or cold, it drops as cold stone; the crack drops nothing.
			addDrop(SteleBlocks.CLEFT_STONE);
		}
	}

	private static class Recipes extends FabricRecipeProvider {
		Recipes(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Cleft Stele Recipes";
		}

		@Override
		public void generate(Consumer<RecipeJsonProvider> exporter) {
			// Cheap on purpose (DESIGN §2): the soul is the gate, not the stone.
			ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, SteleBlocks.CLEFT_STONE, 8)
					.pattern("BBB")
					.pattern("BCB")
					.pattern("BBB")
					.input('B', Items.STONE_BRICKS)
					.input('C', ItemTags.COALS)
					.criterion(hasItem(Items.STONE_BRICKS), conditionsFromItem(Items.STONE_BRICKS))
					.offerTo(exporter);
		}
	}

	private SteleDataGen() {}
}
