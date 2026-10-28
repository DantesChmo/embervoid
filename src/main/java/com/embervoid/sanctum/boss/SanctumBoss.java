package com.embervoid.sanctum.boss;

import com.embervoid.Embervoid;
import com.embervoid.boss.BossReward;
import com.embervoid.hub.stele.SteleItems;
import com.embervoid.sanctum.SanctumItems;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

/** Registers the Ruined Sanctum's boss; the one call from {@code Embervoid.onInitialize}. */
public final class SanctumBoss {
	/** What a player's first clear rolls besides the soul and a Flask Shard: Sanctum-tier loot. */
	public static final Identifier FIRST_CLEAR_LOOT = Embervoid.id("gameplay/fallen_sister_first_clear");

	public static void register() {
		SanctumBossSounds.register();
		SanctumBossEntities.register();
		SanctumBossArena.register();
		SanctumBossCommands.register();
		// DESIGN §2: a Crying Soul for every credited player on every clear; the first clear also gives a Flask Shard
		// and Sanctum-tier loot.
		BossReward.register(SanctumBossArena.BOSS_ID, new BossReward(SteleItems.CRYING_SOUL, List.of(new ItemStack(SanctumItems.FLASK_SHARD)),
				true, FIRST_CLEAR_LOOT));
	}

	private SanctumBoss() {}
}
