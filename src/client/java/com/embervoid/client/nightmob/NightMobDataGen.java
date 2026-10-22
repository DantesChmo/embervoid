package com.embervoid.client.nightmob;

import com.embervoid.nightmob.NightMobs;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.LootingEnchantLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.function.BiConsumer;

/** Datagen for the night mobs track: entity loot tables. Spawn egg item models are hand-written resources. */
public final class NightMobDataGen {
	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(Loot::new);
	}

	private static class Loot extends SimpleFabricLootTableProvider {
		Loot(FabricDataOutput output) {
			super(output, LootContextTypes.ENTITY);
		}

		@Override
		public String getName() {
			return "Embervoid night mob loot tables";
		}

		@Override
		public void accept(BiConsumer<Identifier, LootTable.Builder> exporter) {
			exporter.accept(NightMobs.ASHEN_ZOMBIE.getLootTableId(), LootTable.builder()
					.pool(drop(Items.ROTTEN_FLESH, 0, 2))
					.pool(drop(Items.CHARCOAL, 0, 1)));
			exporter.accept(NightMobs.GHOST.getLootTableId(), LootTable.builder()
					.pool(drop(Items.PHANTOM_MEMBRANE, 0, 1)));
		}

		/** min–max of an item, +0–1 per Looting level. */
		static LootPool.Builder drop(Item item, float min, float max) {
			return LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1))
					.with(ItemEntry.builder(item)
							.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max)))
							.apply(LootingEnchantLootFunction.builder(UniformLootNumberProvider.create(0, 1))));
		}
	}

	private NightMobDataGen() {}
}
