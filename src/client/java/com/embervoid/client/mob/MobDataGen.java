package com.embervoid.client.mob;

import com.embervoid.entity.mob.MobEntities;
import com.embervoid.mob.MobLevels;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.entity.EntityType;
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
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Datagen for the mobs: entity loot tables and entity type tags. Spawn egg item models are hand-written
 * resources.
 */
public final class MobDataGen {
	public static void addProviders(FabricDataGenerator.Pack pack) {
		pack.addProvider(Loot::new);
		pack.addProvider(EntityTags::new);
	}

	/** Vanilla bosses keep vanilla stats (DESIGN §5.4). */
	private static class EntityTags extends FabricTagProvider.EntityTypeTagProvider {
		EntityTags(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
			super(output, registries);
		}

		@Override
		protected void configure(RegistryWrapper.WrapperLookup registries) {
			getOrCreateTagBuilder(MobLevels.UNLEVELED)
					.add(EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.WARDEN, EntityType.ELDER_GUARDIAN);
		}
	}

	private static class Loot extends SimpleFabricLootTableProvider {
		Loot(FabricDataOutput output) {
			super(output, LootContextTypes.ENTITY);
		}

		@Override
		public String getName() {
			return "Embervoid mob loot tables";
		}

		@Override
		public void accept(BiConsumer<Identifier, LootTable.Builder> exporter) {
			exporter.accept(table(MobEntities.ASH_GHOUL), LootTable.builder()
					.pool(drop(Items.BONE, 0, 2))
					.pool(drop(Items.CHARCOAL, 0, 1)));
			exporter.accept(table(MobEntities.HOLLOW), LootTable.builder()
					.pool(drop(Items.IRON_NUGGET, 1, 4))
					.pool(drop(Items.BONE, 0, 2))
					.pool(rare(Items.IRON_INGOT, 0.1f, 0.02f)));
			exporter.accept(table(MobEntities.EMBER_WISP), LootTable.builder()
					.pool(drop(Items.GLOWSTONE_DUST, 0, 1))
					.pool(rare(Items.BLAZE_POWDER, 0.05f, 0.01f)));
		}

		private static Identifier table(EntityType<?> type) {
			return type.getLootTableId();
		}

		/** min–max of an item, +0–1 per Looting level. */
		static LootPool.Builder drop(Item item, float min, float max) {
			return LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1))
					.with(ItemEntry.builder(item)
							.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max)))
							.apply(LootingEnchantLootFunction.builder(UniformLootNumberProvider.create(0, 1))));
		}

		/** A rare player-kill drop: {@code chance}, +{@code lootingBonus} per Looting level. */
		static LootPool.Builder rare(Item item, float chance, float lootingBonus) {
			return LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1))
					.with(ItemEntry.builder(item))
					.conditionally(KilledByPlayerLootCondition.builder())
					.conditionally(RandomChanceWithLootingLootCondition.builder(chance, lootingBonus));
		}
	}

	private MobDataGen() {}
}
