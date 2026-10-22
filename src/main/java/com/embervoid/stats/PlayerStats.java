package com.embervoid.stats;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.XpUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public API for character stats and leveling. Reads work on both sides (on a client, for the local player, from
 * the synced data); changes are server-only and apply their effects and sync at once.
 *
 * <p>The level-up screen previews a point with {@link #derivedWith} and spends it with {@link #levelUp} (after its
 * own checks, such as standing near the Maiden).
 *
 * <p>Bonus points (artifacts, DESIGN §4.5) come from {@link BonusSource}s: they count in every effect ({@link
 * #derived}, {@link #effective}) but not in the stat values themselves ({@link #get}), the character level, the level
 * cost or the stat cap. A source calls {@link StatEffects#apply} when what it gives changes.
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

	/** Adds stat points that do not count toward level, level cost or the stat cap (e.g. worn artifacts). */
	@FunctionalInterface
	public interface BonusSource {
		/** Adds this source's points to {@code bonus}, indexed by {@link Stat#ordinal()}. Both sides. */
		void addBonus(PlayerEntity player, int[] bonus);
	}

	private static final List<BonusSource> BONUS_SOURCES = new CopyOnWriteArrayList<>();

	/** Registers a source of bonus points (during mod initialization). */
	public static void registerBonusSource(BonusSource source) {
		BONUS_SOURCES.add(source);
	}

	/** The player's bonus points from every {@link BonusSource}, by {@link Stat#ordinal()}. */
	public static int[] bonus(PlayerEntity player) {
		int[] bonus = new int[Stat.COUNT];
		for (BonusSource source : BONUS_SOURCES) {
			source.addBonus(player, bonus);
		}
		return bonus;
	}

	/** The stat value effects use: the player's own value plus bonus points. */
	public static int effective(PlayerEntity player, Stat stat) {
		return get(player, stat) + bonus(player)[stat.ordinal()];
	}

	/** The player's own stat value (what they leveled; no bonus points). */
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

	/** What the player's current stats give, bonus points included. */
	public static DerivedStats derived(PlayerEntity player) {
		return derivedOf(withBonus(player, getAll(player)));
	}

	/**
	 * What the player's stats would give with their own {@code stat} at {@code value} (level-up preview), bonus
	 * points included.
	 */
	public static DerivedStats derivedWith(PlayerEntity player, Stat stat, int value) {
		int[] stats = getAll(player);
		stats[stat.ordinal()] = value;
		return derivedOf(withBonus(player, stats));
	}

	private static int[] withBonus(PlayerEntity player, int[] stats) {
		int[] bonus = bonus(player);
		for (int i = 0; i < stats.length; i++) {
			stats[i] += bonus[i];
		}
		return stats;
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
