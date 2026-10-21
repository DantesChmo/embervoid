package com.embervoid.stats;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.XpUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Public API for character stats and leveling. Reads work on both sides (on a client, for the local player, from
 * the synced data); changes are server-only and apply their effects and sync at once.
 *
 * <p>The level-up screen previews a point with {@link #derivedWith} and spends it with {@link #levelUp} (after its
 * own checks, such as standing near the Maiden).
 */
public final class PlayerStats {
	/** Sum of the starting stats: 7 stats at 10. Level 1 = this sum. */
	private static final int BASE_SUM = Stat.COUNT * Stat.BASE;

	/** Outcome of {@link #levelUp} / {@link #checkLevelUp}. */
	public enum LevelUpResult {
		SUCCESS,
		/** The stat is at {@code leveling.statCap}. */
		STAT_CAP,
		/** The character is at {@code leveling.levelCap}. */
		LEVEL_CAP,
		/** Fewer XP points than {@link #levelUpCost}. */
		NOT_ENOUGH_XP;

		/** {@code embervoid.levelup.<result>}, for feedback in chat or on a screen. */
		public Text message() {
			return Text.translatable("embervoid.levelup." + name().toLowerCase(java.util.Locale.ROOT));
		}
	}

	public static int get(PlayerEntity player, Stat stat) {
		return PlayerDataManager.get(player).stats[stat.ordinal()];
	}

	/** A copy of all stat values, by {@link Stat#ordinal()}. */
	public static int[] getAll(PlayerEntity player) {
		return PlayerDataManager.get(player).stats.clone();
	}

	/** Character level: {@code 1 + (sum of stats − 70)} (DESIGN §5.3). */
	public static int level(PlayerEntity player) {
		return levelOf(PlayerDataManager.get(player).stats);
	}

	public static int levelOf(int[] stats) {
		int sum = 0;
		for (int value : stats) {
			sum += value;
		}
		return 1 + sum - BASE_SUM;
	}

	/** What the player's current stats give. */
	public static DerivedStats derived(PlayerEntity player) {
		return derivedOf(PlayerDataManager.get(player).stats);
	}

	/** What the player's stats would give with {@code stat} at {@code value} (level-up preview). */
	public static DerivedStats derivedWith(PlayerEntity player, Stat stat, int value) {
		int[] stats = getAll(player);
		stats[stat.ordinal()] = value;
		return derivedOf(stats);
	}

	public static DerivedStats derivedOf(int[] stats) {
		EmbervoidConfig config = EmbervoidConfig.get();
		return DerivedStats.of(stats, config.section(StatsConfig.Effects.SECTION),
				config.stamina().max(), config.stamina().regenPerTick());
	}

	/** XP points the next level costs. */
	public static int levelUpCost(PlayerEntity player) {
		return leveling().cost(level(player));
	}

	/** Whether {@link #levelUp} would succeed right now (usable on the client to grey out buttons). */
	public static LevelUpResult checkLevelUp(PlayerEntity player, Stat stat) {
		StatsConfig.Leveling leveling = leveling();
		if (get(player, stat) >= leveling.statCap()) {
			return LevelUpResult.STAT_CAP;
		}
		if (leveling.levelCap() > 0 && level(player) >= leveling.levelCap()) {
			return LevelUpResult.LEVEL_CAP;
		}
		if (XpUtil.getPoints(player) < levelUpCost(player)) {
			return LevelUpResult.NOT_ENOUGH_XP;
		}
		return LevelUpResult.SUCCESS;
	}

	/**
	 * Raises {@code stat} by one, paying {@link #levelUpCost} in XP points. Checks the stat cap, the level cap and
	 * the player's XP; on failure nothing changes. Effects apply and the client is synced at once.
	 */
	public static LevelUpResult levelUp(ServerPlayerEntity player, Stat stat) {
		LevelUpResult result = checkLevelUp(player, stat);
		if (result != LevelUpResult.SUCCESS) {
			return result;
		}
		if (!XpUtil.trySpend(player, levelUpCost(player))) {
			return LevelUpResult.NOT_ENOUGH_XP;
		}
		setUnchecked(player, stat, get(player, stat) + 1);
		return LevelUpResult.SUCCESS;
	}

	/** Sets a stat directly, without cost or caps beyond {@link Stat#MIN} and 999 (debug, admin tools). */
	public static void set(ServerPlayerEntity player, Stat stat, int value) {
		setUnchecked(player, stat, Math.max(Stat.MIN, Math.min(999, value)));
	}

	private static void setUnchecked(ServerPlayerEntity player, Stat stat, int value) {
		PlayerData data = PlayerDataManager.get(player);
		data.stats[stat.ordinal()] = value;
		StatEffects.apply(player);
		PlayerDataManager.sync(player);
	}

	private static StatsConfig.Leveling leveling() {
		return EmbervoidConfig.get().section(StatsConfig.Leveling.SECTION);
	}

	private PlayerStats() {}
}
