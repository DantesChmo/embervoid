package com.embervoid.client.sanctum.gear;

import com.embervoid.entity.mob.HollowEntity;
import com.embervoid.sanctum.gear.SanctumWeapons;
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

/** Datagen for the Sanctum weapons (PLAN L1): handheld item models and the guard-breaking tag. */
public final class SanctumGearDataGen {
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
			return "Embervoid Sanctum gear item models";
		}

		@Override
		public void generateBlockStateModels(BlockStateModelGenerator gen) {
		}

		@Override
		public void generateItemModels(ItemModelGenerator gen) {
			for (Item item : SanctumWeapons.ALL) {
				gen.register(item, Models.HANDHELD);
			}
		}
	}

	private static class ItemTags extends FabricTagProvider.ItemTagProvider {
		ItemTags(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
			super(output, registries);
		}

		@Override
		public String getName() {
			return "Embervoid Sanctum gear item tags";
		}

		@Override
		protected void configure(RegistryWrapper.WrapperLookup registries) {
			getOrCreateTagBuilder(HollowEntity.BREAKS_GUARD).add(SanctumWeapons.ASHBEARERS_MACE);
		}
	}

	private SanctumGearDataGen() {}
}
