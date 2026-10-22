package com.embervoid.client.reliquary;

import com.embervoid.reliquary.ReliquaryItems;
import com.embervoid.reliquary.SlotKinds;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

/** Datagen for the Reliquary track: accessory item models and the slot kind tags. */
public final class ReliquaryDataGen {
	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(ItemModels::new);
		pack.addProvider(ItemTags::new);
	}

	private static class ItemModels extends FabricModelProvider {
		ItemModels(FabricDataOutput output) {
			super(output);
		}

		@Override
		public String getName() {
			return "Embervoid reliquary item models";
		}

		@Override
		public void generateBlockStateModels(BlockStateModelGenerator gen) {
		}

		@Override
		public void generateItemModels(ItemModelGenerator gen) {
			for (Item item : ReliquaryItems.ARTIFACTS) {
				gen.register(item, Models.GENERATED);
			}
			for (Item item : ReliquaryItems.RINGS) {
				gen.register(item, Models.GENERATED);
			}
		}
	}

	private static class ItemTags extends FabricTagProvider.ItemTagProvider {
		ItemTags(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
			super(output, registries);
		}

		@Override
		public String getName() {
			return "Embervoid reliquary item tags";
		}

		@Override
		protected void configure(RegistryWrapper.WrapperLookup registries) {
			var artifacts = getOrCreateTagBuilder(SlotKinds.ARTIFACT.tag());
			ReliquaryItems.ARTIFACTS.forEach(artifacts::add);
			var rings = getOrCreateTagBuilder(SlotKinds.RING.tag());
			ReliquaryItems.RINGS.forEach(rings::add);
		}
	}

	private ReliquaryDataGen() {}
}
