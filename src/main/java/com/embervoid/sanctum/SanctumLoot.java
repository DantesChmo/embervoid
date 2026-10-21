package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ExplorationMapLootFunction;
import net.minecraft.loot.function.SetNameLootFunction;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

import java.util.Map;

/**
 * Puts the Sanctum Map (an explorer map to the nearest Ember Sanctum) into vanilla chest loot. The Sanctum's own
 * chests use the data-driven table {@code embervoid:chests/sanctum_outer_hall}.
 */
public final class SanctumLoot {
	public static final TagKey<Structure> ON_SANCTUM_MAPS = TagKey.of(RegistryKeys.STRUCTURE, Embervoid.id("on_sanctum_maps"));

	/** Vanilla chest → chance of one Sanctum Map. */
	private static final Map<Identifier, Float> MAP_CHANCES = Map.of(
			new Identifier("chests/village/village_cartographer"), 0.25f,
			new Identifier("chests/village/village_temple"), 0.1f,
			new Identifier("chests/ruined_portal"), 0.08f,
			new Identifier("chests/shipwreck_map"), 0.25f);

	static void register() {
		LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			Float chance = MAP_CHANCES.get(id);
			if (chance == null || !source.isBuiltin()) {
				return;
			}
			tableBuilder.pool(LootPool.builder()
					.conditionally(RandomChanceLootCondition.builder(chance))
					.with(ItemEntry.builder(Items.MAP)
							.apply(ExplorationMapLootFunction.builder()
									.withDestination(ON_SANCTUM_MAPS)
									.withDecoration(MapIcon.Type.RED_X)
									.withZoom((byte) 2)
									.searchRadius(64))
							.apply(SetNameLootFunction.builder(Text.translatable("item.embervoid.sanctum_map")))));
		});
	}

	private SanctumLoot() {}
}
