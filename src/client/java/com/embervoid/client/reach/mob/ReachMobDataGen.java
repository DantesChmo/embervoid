package com.embervoid.client.reach.mob;

import com.embervoid.reach.mob.ReachMobs;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.KilledByPlayerLootCondition;
import net.minecraft.loot.condition.RandomChanceWithLootingLootCondition;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.LootingEnchantLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.function.BiConsumer;

/**
 * Datagen for the Realm 1 mobs: entity loot tables with the vanilla-flavoured basics (DESIGN §4.5 "Drops"). Realm
 * loot (materials, set and unique pieces, artifacts, rings) is added to these tables by tracks reach-gear and
 * reliquary with {@code LootTableEvents.MODIFY}. Spawn egg item models are hand-written resources.
 */
public final class ReachMobDataGen {
	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(Loot::new);
	}

	private static class Loot extends SimpleFabricLootTableProvider {
		Loot(FabricDataOutput output) {
			super(output, LootContextTypes.ENTITY);
		}

		@Override
		public String getName() {
			return "Embervoid Realm 1 mob loot tables";
		}

		@Override
		public void accept(BiConsumer<Identifier, LootTable.Builder> exporter) {
			exporter.accept(ReachMobs.CINDER_HUSK.getLootTableId(), LootTable.builder()
					.pool(drop(Items.ROTTEN_FLESH, 0, 2))
					.pool(drop(Items.CHARCOAL, 0, 1)));
			exporter.accept(ReachMobs.ASH_STALKER.getLootTableId(), LootTable.builder()
					.pool(drop(Items.BONE, 0, 2))
					.pool(drop(Items.CHARCOAL, 0, 1)));
			exporter.accept(ReachMobs.ASHEN_CARRION.getLootTableId(), LootTable.builder()
					.pool(drop(Items.FEATHER, 1, 3))
					.pool(drop(Items.ROTTEN_FLESH, 0, 1)));
			exporter.accept(ReachMobs.WITHERED_THRALL.getLootTableId(), LootTable.builder()
					.pool(drop(Items.ROTTEN_FLESH, 0, 2))
					.pool(drop(Items.STRING, 0, 2)));
			exporter.accept(ReachMobs.LANTERN_HAG.getLootTableId(), LootTable.builder()
					.pool(drop(Items.GLASS_BOTTLE, 0, 1))
					.pool(drop(Items.CHARCOAL, 0, 2))
					.pool(rare(Items.LANTERN, 0.05f)));
			exporter.accept(ReachMobs.BARK_HOLLOW.getLootTableId(), LootTable.builder()
					.pool(drop(Items.STICK, 1, 4))
					.pool(drop(Items.CHARCOAL, 0, 2)));
			exporter.accept(ReachMobs.RESTLESS_BONES.getLootTableId(), LootTable.builder()
					.pool(drop(Items.BONE, 0, 2)));
			exporter.accept(ReachMobs.CRYPT_MOTH.getLootTableId(), LootTable.builder()
					.pool(drop(Items.STRING, 0, 1))
					.pool(drop(Items.GLOWSTONE_DUST, 0, 1)));
			exporter.accept(ReachMobs.HOLLOW_SENTINEL.getLootTableId(), LootTable.builder()
					.pool(drop(Items.IRON_NUGGET, 1, 4))
					.pool(drop(Items.BONE, 0, 2)));
			exporter.accept(ReachMobs.GRAVE_WARDEN.getLootTableId(), LootTable.builder()
					.pool(drop(Items.IRON_NUGGET, 2, 5))
					.pool(drop(Items.BONE, 1, 3))
					.pool(rare(Items.CANDLE, 0.25f)));
		}

		/** min–max of an item, +0–1 per Looting level. */
		static LootPool.Builder drop(Item item, float min, float max) {
			return LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1))
					.with(ItemEntry.builder(item)
							.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max)))
							.apply(LootingEnchantLootFunction.builder(UniformLootNumberProvider.create(0, 1))));
		}

		/** One item at {@code chance} (+1 % per Looting level), player kills only. */
		static LootPool.Builder rare(Item item, float chance) {
			return LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1))
					.with(ItemEntry.builder(item))
					.conditionally(KilledByPlayerLootCondition.builder())
					.conditionally(RandomChanceWithLootingLootCondition.builder(chance, 0.01f));
		}
	}

	private ReachMobDataGen() {}
}
