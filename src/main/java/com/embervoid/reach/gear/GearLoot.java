package com.embervoid.reach.gear;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.KilledByPlayerLootCondition;
import net.minecraft.loot.condition.RandomChanceWithLootingLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.LootingEnchantLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Realm loot on the realm's mobs (DESIGN §4.5), injected with {@link LootTableEvents#MODIFY} into the mob
 * loot tables by their fixed ids: the zone's ore material, a piece of the zone's armor set (player kills) and the
 * zone's unique piece (player kills). Fortune's extra roll covers these pools too. Chances in {@code gear.loot}.
 *
 * <p>Roles: natural mobs drop at the "fodder" rates. Placed-only mobs (tanks and elites, which never respawn) drop
 * materials and set pieces at the "elite" rates; the unique piece drops at the elite rate from the zone's elite only
 * (Ashen Carrion, Grave Warden; the Woods have none, so the Bark Hollow stands in), elsewhere at the fodder rate.
 * The Citadel has no ore and no set: only its golem and knight drop the Furnace Heart Cuirass. A mob table that
 * does not exist yet is simply not modified.
 */
public final class GearLoot {
	/**
	 * What one mob drops.
	 *
	 * @param material   the zone's ore material, or null
	 * @param placed     placed-only (tank or elite): elite rates for materials and set pieces
	 * @param set        the zone's armor set (one piece, equally likely), or null
	 * @param unique     the zone's unique piece, or null
	 * @param eliteUnique the unique drops at the elite rate
	 */
	public record MobLoot(@Nullable Item material, boolean placed, @Nullable List<Item> set, @Nullable Item unique,
			boolean eliteUnique) {
	}

	/** Mob id (path in {@code embervoid}) → its realm loot. */
	public static Map<String, MobLoot> mobs() {
		return Map.ofEntries(
				Map.entry("cinder_husk", new MobLoot(GearItems.CINDER_SHARD, false, GearArmor.REAPERS_GARB, GearArmor.SCARECROW_MASK, false)),
				Map.entry("ash_stalker", new MobLoot(GearItems.CINDER_SHARD, false, null, GearArmor.SCARECROW_MASK, false)),
				Map.entry("ashen_carrion", new MobLoot(GearItems.CINDER_SHARD, true, GearArmor.REAPERS_GARB, GearArmor.SCARECROW_MASK, true)),
				Map.entry("withered_thrall", new MobLoot(GearItems.ROYAL_AMBER, false, GearArmor.HUNTSMANS_LEATHERS, GearArmor.HUNTSMAN_GREAVES_ITEM, false)),
				Map.entry("lantern_hag", new MobLoot(GearItems.ROYAL_AMBER, false, GearArmor.HUNTSMANS_LEATHERS, GearArmor.HUNTSMAN_GREAVES_ITEM, false)),
				Map.entry("bark_hollow", new MobLoot(GearItems.ROYAL_AMBER, true, GearArmor.HUNTSMANS_LEATHERS, GearArmor.HUNTSMAN_GREAVES_ITEM, true)),
				Map.entry("restless_bones", new MobLoot(GearItems.RAW_GRAVE_IRON, false, GearArmor.NECROPOLIS_WARDEN, GearArmor.DROWNED_SABATONS, false)),
				Map.entry("crypt_moth", new MobLoot(GearItems.RAW_GRAVE_IRON, false, null, GearArmor.DROWNED_SABATONS, false)),
				Map.entry("hollow_sentinel", new MobLoot(GearItems.RAW_GRAVE_IRON, true, GearArmor.NECROPOLIS_WARDEN, GearArmor.DROWNED_SABATONS, false)),
				Map.entry("grave_warden", new MobLoot(GearItems.RAW_GRAVE_IRON, true, GearArmor.NECROPOLIS_WARDEN, GearArmor.DROWNED_SABATONS, true)),
				Map.entry("furnace_golem", new MobLoot(null, true, null, GearArmor.FURNACE_HEART_CUIRASS, true)),
				Map.entry("smoldering_knight", new MobLoot(null, true, null, GearArmor.FURNACE_HEART_CUIRASS, true)));
	}

	/** Adds the realm loot pools for table {@code id} to {@code table}, if it belongs to a realm mob. */
	public static boolean inject(Identifier id, LootTable.Builder table) {
		if (!id.getNamespace().equals(Embervoid.MOD_ID) || !id.getPath().startsWith("entities/")) {
			return false;
		}
		MobLoot loot = mobs().get(id.getPath().substring("entities/".length()));
		if (loot == null) {
			return false;
		}
		GearConfig.Loot config = GearConfig.get().loot();
		if (loot.material() != null) {
			int max = loot.placed() ? 2 : 1;
			table.pool(LootPool.builder()
					.conditionally(RandomChanceWithLootingLootCondition.builder(loot.placed() ? config.eliteMaterial() : config.fodderMaterial(),
							config.lootingBonus()))
					.with(ItemEntry.builder(loot.material())
							.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, max)))
							.apply(LootingEnchantLootFunction.builder(UniformLootNumberProvider.create(0, 1)))));
		}
		if (loot.set() != null) {
			table.pool(playerKillPool(loot.set(), loot.placed() ? config.eliteSet() : config.fodderSet(), config.lootingBonus()));
		}
		if (loot.unique() != null) {
			table.pool(playerKillPool(List.of(loot.unique()), loot.eliteUnique() ? config.eliteUnique() : config.fodderUnique(),
					config.lootingBonus() / 4));
		}
		return true;
	}

	/** One roll at {@code chance} (+ {@code lootingBonus} per Looting level), player kills only: one of {@code items}. */
	private static LootPool.Builder playerKillPool(List<Item> items, float chance, float lootingBonus) {
		LootPool.Builder pool = LootPool.builder()
				.conditionally(KilledByPlayerLootCondition.builder())
				.conditionally(RandomChanceWithLootingLootCondition.builder(chance, lootingBonus));
		for (Item item : items) {
			pool.with(ItemEntry.builder(item));
		}
		return pool;
	}

	static void register() {
		LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			// Not into tables a datapack replaced: those decide their loot themselves.
			if (source.isBuiltin()) {
				inject(id, tableBuilder);
			}
		});
	}

	private GearLoot() {}
}
