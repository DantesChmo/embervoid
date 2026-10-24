package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Hit;
import com.embervoid.balance.BalanceModel.Fight;
import com.embervoid.balance.BalanceModel.Player;
import com.embervoid.balance.OverworldData.OverworldMob;
import com.embervoid.moon.MoonConfig;
import net.minecraft.world.Difficulty;

import java.util.List;
import java.util.function.Predicate;

/**
 * The overworld stage of the balance model (PLAN L2): a fresh player from level 1 to the Sanctum against every
 * overworld hostile at its level band, on Easy, Normal and Hard, and the XP of a plausible route to the Sanctum
 * against the Realm 1 model's assumption (level 10, {@link BalanceModel.Playthrough#NORMAL}). Same formulas and the
 * same worst honest case as {@link BalanceModel}: trading hits, no crits, no shield, no enchantments.
 */
public final class OverworldModel {
	public static final int FULL_MOON_BONUS = MoonConfig.DEFAULT.fullMoon().levelBonus();

	// --- The players ---------------------------------------------------------------------------------------------

	/** Level 1, all stats 10, the first armor and a stone sword. */
	public static final Player FRESH_LEATHER = Player.of(1, BalanceModel.LEATHER, BalanceModel.STONE_SWORD, BalanceModel.FLASK.baseCharges());
	/** Level 1 in full iron with an iron sword: the kit a player has before finding a Sanctum. */
	public static final Player FRESH_IRON = Player.of(1, BalanceModel.IRON, BalanceModel.IRON_SWORD, BalanceModel.FLASK.baseCharges());
	/** The Sanctum-level player of the Realm 1 model: level 10, iron, iron sword. */
	public static final Player SANCTUM_IRON = Player.of(BalanceModel.Playthrough.NORMAL.sanctumLevel(), BalanceModel.IRON,
			BalanceModel.IRON_SWORD, BalanceModel.FLASK.baseCharges());
	public static final List<Player> PLAYERS = List.of(FRESH_LEATHER, FRESH_IRON, SANCTUM_IRON);
	public static final List<Difficulty> DIFFICULTIES = List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD);

	// --- Levels --------------------------------------------------------------------------------------------------

	/** Lowest level a natural spawn of {@code mob} has: its base, +3 if it only spawns below y=0 (DESIGN §5.4). */
	public static int minLevel(OverworldMob mob) {
		return mob.baseLevel() + (mob.surface() || !mob.underground() ? 0 : BalanceModel.LEVELS.belowZeroBonus());
	}

	/** Highest level outside a full moon: base + the distance cap, +3 if it spawns below y=0. */
	public static int maxLevel(OverworldMob mob) {
		return mob.baseLevel() + BalanceModel.LEVELS.maxDistanceBonus()
				+ (mob.underground() ? BalanceModel.LEVELS.belowZeroBonus() : 0);
	}

	// --- Fights --------------------------------------------------------------------------------------------------

	/** {@link Fight} plus the fire a burning mob leaves (the Ember Wisp's), ignoring armor, reduced by RES. */
	public record OverworldFight(OverworldMob mob, Fight fight, Difficulty difficulty, double burn) {
		public double damageTaken() {
			return fight.damageTaken() + burn;
		}

		public double tradeShare() {
			return damageTaken() / fight.player().maxHealth();
		}
	}

	public static OverworldFight fight(OverworldMob mob, int level, Player player, Difficulty difficulty) {
		Fight fight = BalanceModel.fight(mob.mob(), level, player, difficulty);
		Hit main = mob.mob().mainHit();
		double landed = main.interval() > 0 ? Math.floor(fight.secondsToKill() * 20 / main.interval()) + 1 : 1;
		double burn = landed * mob.burnPerHit() * (1 - player.derived().fireDamageReduction());
		return new OverworldFight(mob, fight, difficulty, burn);
	}

	/**
	 * Damage a player takes clearing a pack of {@code count} that all attack at once, killing them one after the other
	 * (as {@link BalanceModel#waveDamage}, on {@code difficulty}).
	 */
	public static double packDamage(OverworldMob mob, int level, int count, Player player, Difficulty difficulty) {
		int hits = BalanceModel.hitsToKill(BalanceModel.mobHealth(mob.mob().health(), level),
				BalanceModel.playerHit(player, mob.mob().armor(), 1));
		double seconds = hits * player.attackTicks() / 20;
		Hit hit = mob.mob().mainHit();
		double perSecond = hit(hit, level, player, difficulty) * 20 / hit.interval();
		return count * (count + 1) / 2.0 * seconds * perSecond;
	}

	/** Damage of one {@code hit} of {@code mob} at {@code level} on {@code player}. */
	public static double hit(Hit hit, int level, Player player, Difficulty difficulty) {
		return BalanceModel.hitOnPlayer(hit, level, player, difficulty);
	}

	/**
	 * The Hollow's swing through full iron on Normal at its base level (DESIGN §3.2 target ~6), weighted by its weapons
	 * (iron sword 70 %, stone sword 30 %).
	 */
	public static double hollowHitThroughIron(int level, Difficulty difficulty) {
		List<Hit> hits = OverworldData.HOLLOW.mob().hits();
		Player iron = Player.of(1, BalanceModel.IRON, BalanceModel.IRON_SWORD, 0);
		return 0.7 * hit(hits.get(0), level, iron, difficulty) + 0.3 * hit(hits.get(1), level, iron, difficulty);
	}

	/** Damage per second of {@code player}'s weapon against {@code armor}, swinging at full charge. */
	public static double perSecond(Player player, double armor) {
		return BalanceModel.playerHit(player, armor, 1) * 20 / player.attackTicks();
	}

	// --- XP on the way to the Sanctum ----------------------------------------------------------------------------

	/** Vanilla ore XP, mean of each range: coal 0–2, redstone 1–5, lapis 2–5, diamond 3–7. */
	public static final double COAL_XP = 1;
	public static final double REDSTONE_XP = 3;
	public static final double LAPIS_XP = 3.5;
	public static final double DIAMOND_XP = 5;
	/** Smelting XP per ingot: iron 0.7, gold 1.0. */
	public static final double IRON_SMELT_XP = 0.7;
	public static final double GOLD_SMELT_XP = 1.0;
	/**
	 * Mean XP of one Sanctum outer-hall chest: Bottles o' Enchanting at weight 6 of 36 in its third pool, 1–4 of them,
	 * 3–11 XP each ({@code chests/sanctum_outer_hall}, checked by {@code OverworldDataTest}).
	 */
	public static final double SANCTUM_CHEST_XP = 6.0 / 36 * 2.5 * 7;

	/**
	 * A plausible first trip to the Sanctum, every value an assumption written up in {@code docs/balance-overworld.md}.
	 *
	 * @param surfaceKills   hostiles killed on the surface at night near spawn (mix by spawn weight × group size)
	 * @param caveKills      hostiles killed in caves below y=0 (+3 levels)
	 * @param coalOre        coal ore mined
	 * @param ironSmelted    iron ingots smelted (full iron armor 24, sword 2, tools)
	 * @param goldSmelted    gold ingots smelted
	 * @param redstoneOre    redstone ore mined
	 * @param lapisOre       lapis ore mined
	 * @param diamondOre     diamond ore mined
	 * @param sanctumHollows Hollows guarding the dungeon, killed on the way in (PLAN 16.4: placed dungeons hold 4–5)
	 * @param sanctumArchers skeletons in its side halls
	 * @param sanctumChests  chests opened (PLAN 16.4: placed dungeons hold 5–7, the storeroom included)
	 */
	public record Route(int surfaceKills, int caveKills, int coalOre, int ironSmelted, int goldSmelted, int redstoneOre,
			int lapisOre, int diamondOre, double sanctumHollows, double sanctumArchers, int sanctumChests) {
		public static final Route TYPICAL = new Route(30, 20, 40, 30, 4, 6, 4, 2, 4.5, 1, 6);
	}

	/** XP of one route by where it comes from. */
	public record RouteXp(double surface, double caves, double mining, double smelting, double sanctum) {
		public double total() {
			return surface + caves + mining + smelting + sanctum;
		}
	}

	/** Mean XP of one natural kill: mobs by spawn weight × group size, each at its lowest level there. */
	public static double meanKillXp(Predicate<OverworldMob> where, int levelBonus) {
		double weights = 0;
		double xp = 0;
		for (OverworldMob mob : OverworldData.MOBS) {
			if (mob.spawnWeight() == 0 || !where.test(mob)) {
				continue;
			}
			double weight = mob.spawnWeight() * mob.groupSize();
			weights += weight;
			xp += weight * BalanceModel.mobXp(mob.mob().xp(), mob.baseLevel() + levelBonus);
		}
		return xp / weights;
	}

	public static RouteXp routeXp(Route route) {
		double surface = route.surfaceKills() * meanKillXp(OverworldMob::surface, 0);
		double caves = route.caveKills() * meanKillXp(OverworldMob::underground, BalanceModel.LEVELS.belowZeroBonus());
		double mining = route.coalOre() * COAL_XP + route.redstoneOre() * REDSTONE_XP + route.lapisOre() * LAPIS_XP
				+ route.diamondOre() * DIAMOND_XP;
		double smelting = route.ironSmelted() * IRON_SMELT_XP + route.goldSmelted() * GOLD_SMELT_XP;
		OverworldMob hollow = OverworldData.HOLLOW;
		OverworldMob skeleton = OverworldData.SKELETON;
		double sanctum = route.sanctumHollows() * BalanceModel.mobXp(hollow.mob().xp(), hollow.baseLevel())
				+ route.sanctumArchers() * BalanceModel.mobXp(skeleton.mob().xp(), skeleton.baseLevel())
				+ route.sanctumChests() * SANCTUM_CHEST_XP;
		return new RouteXp(surface, caves, mining, smelting, sanctum);
	}

	private OverworldModel() {}
}
