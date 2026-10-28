package com.embervoid.reliquary;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.KilledByPlayerLootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accessory drops: an artifact pool and a ring pool, injected with
 * {@link LootTableEvents#MODIFY} into each zone's treasure chest table (artifact 8 %, ring 5 %) and its elite's loot
 * table (2 % / 1 %, player kills only). Each zone drops only its own accessories. Chances in config
 * {@code reliquary.drops}.
 *
 * <p>Table ids are a fixed contract with the reach chest and mob loot tables. A table that does not exist yet is simply not modified. Elites are the bestiary's elite
 * role: Ashen Carrion (Fields), Grave Warden (Necropolis), Smoldering Knight (Citadel); the Woods have none, so its
 * accessories come from the hunting lodge chest only.
 */
public final class ReliquaryLoot {
	public record Zone(String name, List<Item> artifacts, List<Item> rings, @Nullable Identifier elite) {
		public Identifier treasureChest() {
			return Embervoid.id("chests/ashen_reach/" + name + "_treasure");
		}

		@Nullable
		public Identifier eliteTable() {
			return elite == null ? null : elite.withPrefixedPath("entities/");
		}
	}

	public static final List<Zone> ZONES = List.of(
			new Zone("fields", List.of(ReliquaryItems.LAST_HARVEST_GRAIN, ReliquaryItems.PLOUGHMANS_HORSESHOE),
					List.of(ReliquaryItems.COURIERS_RING, ReliquaryItems.RING_OF_ASH_FEATHERS), Embervoid.id("ashen_carrion")),
			new Zone("woods", List.of(ReliquaryItems.ROYAL_HOUND_FANG, ReliquaryItems.AMBER_TEAR),
					List.of(ReliquaryItems.RING_OF_THE_FOG_STALKER, ReliquaryItems.DELVERS_RING), null),
			new Zone("necropolis", List.of(ReliquaryItems.GRAVESTONE_MEDALLION, ReliquaryItems.FERRYMANS_COIN,
					ReliquaryItems.WARDENS_CHAIN), List.of(ReliquaryItems.DROWNED_MANS_RING), Embervoid.id("grave_warden")),
			new Zone("citadel", List.of(ReliquaryItems.SEAL_OF_THE_ASHEN_KING), List.of(ReliquaryItems.RING_OF_EMBERS),
					Embervoid.id("smoldering_knight")));

	/** Where the pools go: table id → zone, and whether it is a chest (else an elite). */
	private record Target(Zone zone, boolean chest) {}

	private static Map<Identifier, Target> targets() {
		Map<Identifier, Target> targets = new HashMap<>();
		for (Zone zone : ZONES) {
			targets.put(zone.treasureChest(), new Target(zone, true));
			if (zone.eliteTable() != null) {
				targets.put(zone.eliteTable(), new Target(zone, false));
			}
		}
		return targets;
	}

	/** Adds the accessory pools for table {@code id} to {@code table}, if it is one of the targets. */
	public static boolean inject(Identifier id, LootTable.Builder table) {
		Target target = targets().get(id);
		if (target == null) {
			return false;
		}
		ReliquaryConfig.Drops drops = ReliquaryConfig.get().drops();
		float artifact = target.chest ? drops.chestArtifact() : drops.eliteArtifact();
		float ring = target.chest ? drops.chestRing() : drops.eliteRing();
		table.pool(pool(target.zone.artifacts(), artifact, !target.chest));
		table.pool(pool(target.zone.rings(), ring, !target.chest));
		return true;
	}

	/** One roll at {@code chance}: one of {@code items}, equally likely. */
	private static LootPool.Builder pool(List<Item> items, float chance, boolean playerKill) {
		LootPool.Builder pool = LootPool.builder().conditionally(RandomChanceLootCondition.builder(chance));
		if (playerKill) {
			pool.conditionally(KilledByPlayerLootCondition.builder());
		}
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

	private ReliquaryLoot() {}
}
