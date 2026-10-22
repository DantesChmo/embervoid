package com.embervoid.reach.boss;

import com.embervoid.Embervoid;
import com.embervoid.boss.BossReward;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.sanctum.SanctumItems;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

/** Registers the Ashen Reach bosses (PLAN 12, 13.4); the one call from {@code Embervoid.onInitialize}. */
public final class ReachBosses {
	/** Progress ids of the zone bosses (fixed, PLAN "0.4.0"). */
	public static final Identifier FIELDS = Embervoid.id("ashen_reach/zone_a");
	public static final Identifier WOODS = Embervoid.id("ashen_reach/zone_b");
	public static final Identifier NECROPOLIS = Embervoid.id("ashen_reach/zone_c");

	public static void register() {
		ReachBossEntities.register();
		// PLAN 12.6: a zone boss's first kill gives its soul and a Flask Shard (+1 flask charge; three in the realm).
		BossReward.register(FIELDS, new BossReward(GearItems.ASH_COLOSSUS_SOUL, List.of(new ItemStack(SanctumItems.FLASK_SHARD))));
		BossReward.register(WOODS, new BossReward(GearItems.GRIEVING_TREE_SOUL, List.of(new ItemStack(SanctumItems.FLASK_SHARD))));
		BossReward.register(NECROPOLIS, new BossReward(GearItems.CRYPT_KEEPER_SOUL, List.of(new ItemStack(SanctumItems.FLASK_SHARD))));
	}

	private ReachBosses() {}
}
