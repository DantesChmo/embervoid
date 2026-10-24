package com.embervoid.client.setpiece;

import com.embervoid.Embervoid;
import com.embervoid.block.setpiece.SetPieceBlock;
import com.embervoid.block.setpiece.SetPieces;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.BlockStateVariant;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.ModelIds;
import net.minecraft.data.client.Models;
import net.minecraft.data.client.TextureMap;
import net.minecraft.data.client.VariantSettings;
import net.minecraft.data.client.VariantsBlockStateSupplier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

import java.util.function.Function;

/**
 * Datagen for the set pieces: one blockstate per piece pointing at its particle-only block model
 * ({@code models/block/setpiece/<name>.json}, written by {@code tools/setpiece_art.py}; the look is the GeckoLib
 * model), a flat item model from the script's icon, loot (the block itself) and tool tags.
 */
public final class SetPieceDataGen {
	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(ModelGen::new);
		pack.addProvider(Loot::new);
	}

	private static String name(Block block) {
		return Registries.BLOCK.getId(block).getPath();
	}

	private static class ModelGen extends FabricModelProvider {
		ModelGen(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Set Piece Models";
		}

		@Override
		public void generateBlockStateModels(BlockStateModelGenerator gen) {
			for (SetPieceBlock block : SetPieces.ALL) {
				gen.blockStateCollector.accept(VariantsBlockStateSupplier.create(block,
						BlockStateVariant.create().put(VariantSettings.MODEL, Embervoid.id("block/setpiece/" + name(block)))));
			}
		}

		@Override
		public void generateItemModels(ItemModelGenerator gen) {
			for (SetPieceBlock block : SetPieces.ALL) {
				Models.GENERATED.upload(ModelIds.getItemModelId(block.asItem()),
						TextureMap.layer0(Embervoid.id("item/setpiece/" + name(block))), gen.writer);
			}
		}
	}

	private static class Loot extends FabricBlockLootTableProvider {
		Loot(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Set Piece Loot";
		}

		@Override
		public void generate() {
			for (SetPieceBlock block : SetPieces.ALL) {
				addDrop(block);
			}
		}
	}

	/** Tool tags. The vanilla tag files are written by the main block tag provider, which calls this. */
	public static void vanillaBlockTags(Function<TagKey<Block>, FabricTagProvider<Block>.FabricTagBuilder> tags) {
		tags.apply(BlockTags.AXE_MINEABLE).add(SetPieces.WINDMILL_SAILS, SetPieces.WINDMILL_SAILS_TORN, SetPieces.GRANARY_HOIST,
				SetPieces.GREAT_GEAR, SetPieces.FORGE_BELLOWS, SetPieces.CHAIN_WINCH);
		tags.apply(BlockTags.PICKAXE_MINEABLE).add(SetPieces.HANGING_SCALES, SetPieces.ASH_MILL, SetPieces.HANGING_CAGE,
				SetPieces.GREAT_BELL, SetPieces.SLUICE_GRATE, SetPieces.GREAT_BRAZIER, SetPieces.PORTCULLIS, SetPieces.CENSER);
	}

	private SetPieceDataGen() {}
}
