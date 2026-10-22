package com.embervoid.client.reach.boss.datagen;

import com.embervoid.reach.boss.ReachBossEntities;
import com.embervoid.reach.gear.GearItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.function.BiConsumer;

/**
 * Datagen for the Ashen Reach bosses: arena templates and the bosses' own loot tables (every kill: zone materials;
 * the soul and Flask Shard of a first kill are personal loot handed out by the fight, PLAN 12.6).
 */
public final class ReachBossDataGen {
	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(ArenaTemplateProvider::new);
		pack.addProvider(Loot::new);
	}

	private static class Loot extends SimpleFabricLootTableProvider {
		Loot(FabricDataOutput output) {
			super(output, LootContextTypes.ENTITY);
		}

		@Override
		public String getName() {
			return "Embervoid Ashen Reach boss loot tables";
		}

		@Override
		public void accept(BiConsumer<Identifier, LootTable.Builder> exporter) {
			exporter.accept(ReachBossEntities.CRYPT_KEEPER.getLootTableId(), LootTable.builder()
					.pool(drop(GearItems.RAW_GRAVE_IRON, 2, 4))
					.pool(drop(Items.BONE, 3, 6))
					.pool(drop(Items.CANDLE, 1, 2)));
			exporter.accept(ReachBossEntities.ASH_COLOSSUS.getLootTableId(), LootTable.builder()
					.pool(drop(GearItems.CINDER_SHARD, 3, 6))
					.pool(drop(Items.CHARCOAL, 2, 5)));
		}

		static LootPool.Builder drop(Item item, float min, float max) {
			return LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1))
					.with(ItemEntry.builder(item).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max))));
		}
	}

	private ReachBossDataGen() {}
}
