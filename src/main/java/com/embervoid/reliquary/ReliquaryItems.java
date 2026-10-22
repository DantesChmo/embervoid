package com.embervoid.reliquary;

import com.embervoid.registry.ModItems;
import com.embervoid.stats.Stat;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

import java.util.List;

/**
 * The Realm 1 artifacts (PLAN 11.11) and rings (PLAN 11.12), DESIGN §4.5. Artifact points come from config
 * ({@code reliquary.artifacts}); rings come with their effects.
 */
public final class ReliquaryItems {
	// Fields
	public static final ArtifactItem LAST_HARVEST_GRAIN = artifact("last_harvest_grain", Stat.VITALITY);
	public static final ArtifactItem PLOUGHMANS_HORSESHOE = artifact("ploughmans_horseshoe", Stat.ENDURANCE);
	// Woods
	public static final ArtifactItem ROYAL_HOUND_FANG = artifact("royal_hound_fang", Stat.DEXTERITY);
	public static final ArtifactItem AMBER_TEAR = artifact("amber_tear", Stat.KINDLING);
	// Necropolis
	public static final ArtifactItem GRAVESTONE_MEDALLION = artifact("gravestone_medallion", Stat.RESILIENCE);
	public static final ArtifactItem FERRYMANS_COIN = artifact("ferrymans_coin", Stat.FORTUNE);
	public static final ArtifactItem WARDENS_CHAIN = artifact("wardens_chain", Stat.STRENGTH);
	// Citadel: every stat
	public static final ArtifactItem SEAL_OF_THE_ASHEN_KING = ModItems.register("seal_of_the_ashen_king",
			new ArtifactItem(new Item.Settings().rarity(Rarity.EPIC),
					() -> ReliquaryConfig.get().artifacts().sealPoints(), Stat.values()));

	public static final List<ArtifactItem> ARTIFACTS = List.of(LAST_HARVEST_GRAIN, PLOUGHMANS_HORSESHOE,
			ROYAL_HOUND_FANG, AMBER_TEAR, GRAVESTONE_MEDALLION, FERRYMANS_COIN, WARDENS_CHAIN, SEAL_OF_THE_ASHEN_KING);

	private static ArtifactItem artifact(String name, Stat stat) {
		return ModItems.register(name, new ArtifactItem(new Item.Settings().rarity(Rarity.UNCOMMON),
				() -> ReliquaryConfig.get().artifacts().points(), stat));
	}

	static void register() {
	}

	private ReliquaryItems() {}
}
