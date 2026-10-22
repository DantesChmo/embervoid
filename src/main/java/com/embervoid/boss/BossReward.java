package com.embervoid.boss;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A boss's personal loot (DESIGN §4.3, §4.5), handed straight into each credited fighter's inventory by its
 * {@link BossFight}. A boss without one gives the framework's Ember Heart on a first kill.
 *
 * @param soul      the boss's soul: every first kill, and a repeat kill with {@link BossConfig#repeatSoulChance}
 *                  (helping a friend), never in a rematch ({@link BossFight#isRematch})
 * @param firstKill what else a first kill gives (Flask Shard, Flask Ember)
 */
public record BossReward(Item soul, List<ItemStack> firstKill) {
	private static final Map<Identifier, BossReward> REWARDS = new ConcurrentHashMap<>();

	/** Registers the personal loot of the boss with progress id {@code bossId}. */
	public static void register(Identifier bossId, BossReward reward) {
		REWARDS.put(bossId, reward);
	}

	@Nullable
	public static BossReward of(Identifier bossId) {
		return REWARDS.get(bossId);
	}

	/** The soul and the first-kill extras, as fresh stacks. */
	public List<ItemStack> firstKillItems() {
		List<ItemStack> items = new ArrayList<>();
		items.add(new ItemStack(soul));
		firstKill.forEach(stack -> items.add(stack.copy()));
		return items;
	}
}
