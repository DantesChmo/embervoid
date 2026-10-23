package com.embervoid.client.prop;

import com.embervoid.Embervoid;
import com.embervoid.block.prop.LongPropBlock;
import com.embervoid.block.prop.Prop;
import com.embervoid.block.prop.PropBlock;
import com.embervoid.block.prop.PropCandleBlock;
import com.embervoid.block.prop.PropShape;
import com.embervoid.block.prop.Props;
import com.embervoid.block.prop.StackedPropBlock;
import com.embervoid.block.prop.TallPropBlock;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.BlockStateSupplier;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Property;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Datagen for the props (PLAN 20.3): blockstates from each state's model and facing, item models, loot, tags. */
public final class PropDataGen {
	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(Models::new);
		pack.addProvider(Loot::new);
	}

	/** The variant key of a state: every property but waterlogging, which the model does not show. */
	static String variant(BlockState state) {
		return state.getProperties().stream().filter(p -> p != PropBlock.WATERLOGGED)
				.map(p -> p.getName() + "=" + name(state, p)).collect(Collectors.joining(","));
	}

	private static <T extends Comparable<T>> String name(BlockState state, Property<T> property) {
		return property.name(state.get(property));
	}

	/** Every state's model, turned by its facing (north unturned, y 90 per step clockwise). */
	static BlockStateSupplier blockState(Block block) {
		Prop prop = (Prop) block;
		return new BlockStateSupplier() {
			@Override
			public Block getBlock() {
				return block;
			}

			@Override
			public JsonElement get() {
				JsonObject variants = new JsonObject();
				for (BlockState state : block.getStateManager().getStates()) {
					JsonObject variant = new JsonObject();
					variant.addProperty("model", Embervoid.id("block/prop/" + prop.model(state)).toString());
					int turns = PropShape.quarterTurns(state.get(PropBlock.FACING));
					if (turns > 0) {
						variant.addProperty("y", turns * 90);
					}
					variants.add(variant(state), variant);
				}
				JsonObject root = new JsonObject();
				root.add("variants", variants);
				return root;
			}
		};
	}

	private static class Models extends FabricModelProvider {
		Models(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Prop Models";
		}

		@Override
		public void generateBlockStateModels(BlockStateModelGenerator gen) {
			// The models themselves are hand-made in assets (models/block/prop, tools/prop_art.py).
			for (Block block : Props.ALL) {
				gen.blockStateCollector.accept(blockState(block));
				gen.registerParentedItemModel(block, Embervoid.id("block/prop/" + ((Prop) block).itemModel()));
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
			return "Prop Loot";
		}

		@Override
		public void generate() {
			for (Block block : Props.ALL) {
				if (block instanceof PropCandleBlock) {
					addDrop(block, candleDrops(block));
				} else if (block instanceof StackedPropBlock) {
					addDrop(block, stackDrops(block));
				} else if (block instanceof TallPropBlock) {
					addDrop(block, dropsWithProperty(block, TallPropBlock.HALF, DoubleBlockHalf.LOWER));
				} else if (block instanceof LongPropBlock) {
					addDrop(block, dropsWithProperty(block, LongPropBlock.PART, BedPart.FOOT));
				} else {
					addDrop(block);
				}
			}
		}

		/** One item per sack or urn in the pile, like candles. */
		private LootTable.Builder stackDrops(Block block) {
			List<Integer> counts = IntStream.rangeClosed(2, StackedPropBlock.MAX).boxed().toList();
			return LootTable.builder().pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
					.with(applyExplosionDecay(block, ItemEntry.builder(block).apply(counts, count ->
							SetCountLootFunction.builder(ConstantLootNumberProvider.create(count))
									.conditionally(BlockStatePropertyLootCondition.builder(block)
											.properties(StatePredicate.Builder.create().exactMatch(StackedPropBlock.COUNT, count)))))));
		}
	}

	/** Tool tags. The vanilla tag files are written by the main block tag provider, which calls this. */
	public static void vanillaBlockTags(Function<TagKey<Block>, FabricTagProvider<Block>.FabricTagBuilder> tags) {
		tags.apply(BlockTags.AXE_MINEABLE).add(Props.BURNT_BARREL, Props.BROKEN_CART, Props.SCARECROW, Props.WEAPON_RACK,
				Props.TATTERED_STANDARD, Props.COFFIN);
		tags.apply(BlockTags.PICKAXE_MINEABLE).add(Props.IRON_CAGE, Props.GRAVE_CROSS, Props.LEANING_GRAVESTONE,
				Props.BONE_PILE, Props.ASH_URNS, Props.RUBBLE_PILE);
		tags.apply(BlockTags.HOE_MINEABLE).add(Props.GRAIN_SACKS);
	}

	private PropDataGen() {}
}
