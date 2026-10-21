package com.embervoid.sanctum.leveling;

import com.embervoid.player.XpUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The level-up screen's only link to player-core's stats (COORDINATION.md: {@code levelUp(player, stat)} + stat read
 * API). Everything else in this package works on {@link StatSheet}.
 *
 * <p>STUB until player-core merges its leveling: shows the 7 design stats at their starting value 10 with the
 * DESIGN §5.3 cost, and refuses every level-up. Then {@link #sheet} reads the real stats and derived values and
 * {@link #levelUp} calls the real {@code levelUp}.
 */
final class LevelingBridge {
	private static final List<String> STATS = List.of(
			"vitality", "endurance", "strength", "dexterity", "kindling", "resilience", "fortune");
	private static final int START = 10;
	private static final int MAX = 99;

	static StatSheet sheet(ServerPlayerEntity player) {
		List<StatSheet.Stat> stats = new ArrayList<>();
		for (String id : STATS) {
			stats.add(new StatSheet.Stat(id, START, MAX, List.of()));
		}
		int level = 1;
		return new StatSheet(level, cost(level), XpUtil.getPoints(player), stats);
	}

	/** Raises {@code statId} by one point. Returns null on success, or why it failed (shown to the player). */
	@Nullable
	static Text levelUp(ServerPlayerEntity player, String statId) {
		return Text.translatable("embervoid.levelup.unavailable");
	}

	/** DESIGN §5.3: 20 + 6L + 0.15L². */
	private static int cost(int level) {
		return (int) Math.round(20 + 6.0 * level + 0.15 * level * level);
	}

	private LevelingBridge() {}
}
