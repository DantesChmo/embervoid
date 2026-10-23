package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Boss;
import com.embervoid.balance.BalanceData.Hit;
import com.embervoid.balance.BalanceData.Mob;
import com.embervoid.balance.BalanceData.Zone;
import com.embervoid.boss.BossConfig;
import com.embervoid.boss.BossScaling;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.mob.MobConfig;
import com.embervoid.mob.MobLevelRules;
import com.embervoid.reach.gear.GearConfig;
import com.embervoid.reach.gear.WeaponRules;
import com.embervoid.stats.DerivedStats;
import com.embervoid.stats.Stat;
import com.embervoid.stats.StatCurve;
import com.embervoid.stats.StatsConfig;
import com.embervoid.stats.WeaponScaling;
import net.minecraft.entity.DamageUtil;
import net.minecraft.world.Difficulty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Realm 1 balance model (R40.1a): the player expected at each stage of a playthrough and what Realm 1's mobs and
 * bosses do to them, on the real formulas — {@link StatCurve} and {@link DerivedStats} for stats,
 * {@link StatsConfig.Leveling} for level cost, {@link MobLevelRules} for mob levels and difficulty,
 * {@link BossScaling} for multiplayer, vanilla's {@link DamageUtil} for armor, {@link WeaponRules} for Kindling —
 * with the config defaults. Pure: no world, no entities; the {@code BalanceFightTests} GameTest measures the same
 * numbers in the engine.
 *
 * <p>Not modelled: crits (×1.5), enchantments (Protection, Sharpness), blocking, dodging, potions. They all favour
 * the player, so the model is the worst honest case of a fight traded hit for hit.
 */
public final class BalanceModel {
	public static final MobConfig.Levels LEVELS = MobConfig.DEFAULT.levels();
	public static final MobConfig.Difficulty DIFFICULTY = MobConfig.DEFAULT.difficulty();
	public static final StatsConfig.Effects EFFECTS = StatsConfig.Effects.DEFAULT;
	public static final StatsConfig.Leveling LEVELING = StatsConfig.Leveling.DEFAULT;
	public static final EmbervoidConfig.Flask FLASK = EmbervoidConfig.DEFAULT.flask();
	public static final EmbervoidConfig.Stamina STAMINA = EmbervoidConfig.DEFAULT.stamina();
	public static final BossConfig BOSS = BossConfig.SECTION.defaults();
	public static final GearConfig GEAR = GearConfig.SECTION.defaults();

	// --- Gear ----------------------------------------------------------------------------------------------------

	/**
	 * A full armor set.
	 *
	 * @param smolderingPieces Smoldering Plate pieces worn (fire reduction)
	 */
	public record Armor(String name, double armor, double toughness, int smolderingPieces) {}

	public static final Armor NO_ARMOR = new Armor("no armor", 0, 0, 0);
	/** Leather (1/3/2/1): the early overworld kit (PLAN L2). */
	public static final Armor LEATHER = new Armor("leather", 7, 0, 0);
	public static final Armor IRON = new Armor("iron", 15, 0, 0);
	public static final Armor DIAMOND = new Armor("diamond", 20, 8, 0);
	/** Necropolis Warden (3/8/6/3, toughness 2 per piece): diamond's numbers. */
	public static final Armor WARDEN = new Armor("Necropolis Warden", 20, 8, 0);
	/** Smoldering Plate (3/8/6/3, toughness 2.5 per piece, −8 % fire per piece). */
	public static final Armor SMOLDERING = new Armor("Smoldering Plate", 20, 10, 4);

	/**
	 * A melee weapon: damage as the attribute total (player base 1 included), attacks per second.
	 *
	 * @param kindlingShare share of each hit that scales with Kindling (the Rapier's ember part)
	 */
	public record Weapon(String name, double damage, double speed, WeaponScaling.Scaling scaling, float kindlingShare) {}

	public static final Weapon STONE_SWORD = new Weapon("stone sword", 5, 1.6, WeaponScaling.Scaling.DEXTERITY, 0);
	public static final Weapon IRON_SWORD = new Weapon("iron sword", 6, 1.6, WeaponScaling.Scaling.DEXTERITY, 0);
	public static final Weapon IRON_AXE = new Weapon("iron axe", 9, 0.9, WeaponScaling.Scaling.STRENGTH, 0);
	public static final Weapon DIAMOND_SWORD = new Weapon("diamond sword", 7, 1.6, WeaponScaling.Scaling.DEXTERITY, 0);
	public static final Weapon NETHERITE_SWORD = new Weapon("netherite sword", 8, 1.6, WeaponScaling.Scaling.DEXTERITY, 0);
	/** Sharpness V adds 3 (vanilla 0.5 + 0.5 per level). */
	public static final Weapon NETHERITE_SWORD_SHARP_V = new Weapon("netherite sword, Sharpness V", 11, 1.6, WeaponScaling.Scaling.DEXTERITY, 0);
	public static final Weapon DIAMOND_AXE = new Weapon("diamond axe", 9, 1.0, WeaponScaling.Scaling.STRENGTH, 0);
	/** {@code reach.gear.GearWeapons}: 10 damage, speed 1.1, STR 0.6 / DEX 0.4. */
	public static final Weapon ASHEN_SCYTHE = new Weapon("Ashen Scythe", 10, 1.1, new WeaponScaling.Scaling(0.6f, 0.4f), 0);
	public static final Weapon WARDENS_SCOURGE = new Weapon("Warden's Scourge", 9, 0.9, WeaponScaling.Scaling.DEXTERITY, 0);
	public static final Weapon ROYAL_ASH_RAPIER = new Weapon("Royal Ash Rapier", 6, 2.0, new WeaponScaling.Scaling(0, 0.7f), 0.3f);
	/** {@code sanctum.gear.SanctumWeapons} (PLAN L1): 6.5 damage, speed 1.6, DEX 1; Wide Cleave. */
	public static final Weapon SANCTUM_LONGSWORD = new Weapon("Sanctum Longsword", 6.5, 1.6, WeaponScaling.Scaling.DEXTERITY, 0);
	/** 8 damage, speed 0.9, STR 1; Ground Slam. */
	public static final Weapon ASHBEARERS_MACE = new Weapon("Ashbearer's Mace", 8, 0.9, WeaponScaling.Scaling.STRENGTH, 0);
	public static final List<Weapon> REALM_MELEE = List.of(ASHEN_SCYTHE, WARDENS_SCOURGE, ROYAL_ASH_RAPIER);
	/**
	 * DESIGN §6.1 starting numbers, "0.5.0 proposed" (PLAN 17.3 builds them): Scythe 12 at 1.1, Scourge 10 at 1.0,
	 * Rapier 7 at 2.2, scaling unchanged. The fight-length model shows them next to today's.
	 */
	public static final Weapon ASHEN_SCYTHE_PROPOSED = new Weapon("Ashen Scythe (0.5.0 proposed)", 12, 1.1, ASHEN_SCYTHE.scaling(), 0);
	public static final Weapon WARDENS_SCOURGE_PROPOSED = new Weapon("Warden's Scourge (0.5.0 proposed)", 10, 1.0, WARDENS_SCOURGE.scaling(), 0);
	public static final Weapon ROYAL_ASH_RAPIER_PROPOSED = new Weapon("Royal Ash Rapier (0.5.0 proposed)", 7, 2.2, ROYAL_ASH_RAPIER.scaling(),
			ROYAL_ASH_RAPIER.kindlingShare());
	public static final List<Weapon> REALM_MELEE_PROPOSED = List.of(ASHEN_SCYTHE_PROPOSED, WARDENS_SCOURGE_PROPOSED, ROYAL_ASH_RAPIER_PROPOSED);
	public static final List<Weapon> SANCTUM_MELEE = List.of(SANCTUM_LONGSWORD, ASHBEARERS_MACE);

	// --- The player ----------------------------------------------------------------------------------------------

	/**
	 * Where a typical melee character puts its points, repeating: VIT, DEX, VIT, DEX, END, VIT, DEX, RES (a sword
	 * build that also wants health; 3 of every 8 points into VIT and DEX each).
	 */
	private static final Stat[] ALLOCATION = {Stat.VITALITY, Stat.DEXTERITY, Stat.VITALITY, Stat.DEXTERITY, Stat.ENDURANCE,
			Stat.VITALITY, Stat.DEXTERITY, Stat.RESILIENCE};

	/** Stat values of a character of {@code level} that spent its points by {@link #ALLOCATION}. */
	public static int[] statsAt(int level) {
		int[] stats = new int[Stat.COUNT];
		Arrays.fill(stats, Stat.BASE);
		for (int point = 0; point < level - 1; point++) {
			stats[ALLOCATION[point % ALLOCATION.length].ordinal()]++;
		}
		return stats;
	}

	/** A character: level, stats, gear and flask charges. */
	public record Player(int level, int[] stats, Armor armor, Weapon weapon, int flaskCharges) {
		public static Player of(int level, Armor armor, Weapon weapon, int flaskCharges) {
			return new Player(level, statsAt(level), armor, weapon, flaskCharges);
		}

		public DerivedStats derived() {
			return DerivedStats.of(stats, EFFECTS, STAMINA.max(), STAMINA.regenPerTick());
		}

		public double maxHealth() {
			return derived().maxHealth();
		}

		/** Healing in the flask: charges × heal × Kindling. */
		public double flaskHealing() {
			return flaskCharges * FLASK.healAmount() * derived().flaskHealMultiplier();
		}

		public int stat(Stat stat) {
			return stats[stat.ordinal()];
		}

		/** Ticks between two fully charged attacks (DEX raises attack speed). */
		public double attackTicks() {
			return 20 / (weapon.speed() * (1 + derived().attackSpeedBonus()));
		}

		public String describe() {
			return "Lv " + level + " (VIT " + stat(Stat.VITALITY) + ", DEX " + stat(Stat.DEXTERITY) + ", END " + stat(Stat.ENDURANCE)
					+ ", RES " + stat(Stat.RESILIENCE) + "), " + armor.name() + ", " + weapon.name();
		}
	}

	// --- Damage both ways ----------------------------------------------------------------------------------------

	/**
	 * Damage one hit of a level-{@code mobLevel} mob does to {@code player}: difficulty factor and level multiplier
	 * (PlayerEntityDifficultyMixin, LivingEntityMobLevelMixin), fire reductions (RES, Smoldering Plate), then armor
	 * and toughness (vanilla). Flat hits (the ash storm's magic) ignore all of it; a mob's magic (a witch's potion)
	 * ignores armor.
	 */
	public static double hitOnPlayer(Hit hit, int mobLevel, Player player, Difficulty difficulty) {
		if (hit.kind() == BalanceData.HitKind.FLAT) {
			return hit.base();
		}
		DerivedStats derived = player.derived();
		double amount = MobLevelRules.difficultyDamage(DIFFICULTY, difficulty, (float) hit.base());
		amount *= MobLevelRules.damageMultiplier(LEVELS, mobLevel);
		if (hit.kind() == BalanceData.HitKind.MAGIC) {
			return amount;
		}
		if (hit.kind() == BalanceData.HitKind.FIRE) {
			amount *= 1 - derived.fireDamageReduction();
			amount *= Math.max(0, 1 - player.armor().smolderingPieces() * GEAR.smoldering().fireReductionPerPiece());
		}
		return DamageUtil.getDamageLeft((float) amount, (float) player.armor().armor(),
				(float) (player.armor().toughness() + derived.armorToughness()));
	}

	public static double hitOnPlayer(Hit hit, int mobLevel, Player player) {
		return hitOnPlayer(hit, mobLevel, player, Difficulty.NORMAL);
	}

	/**
	 * Damage of one fully charged, non-critical melee hit by {@code player} on a target with {@code armor}, after the
	 * target's own factor ({@code factor}: a boss's front, back, trunk...). STR/DEX scaling (WeaponScaling via
	 * StatEffects), Kindling on an ember part (GearDamage), then the target's armor.
	 */
	public static double playerHit(Player player, double armor, double factor) {
		DerivedStats derived = player.derived();
		WeaponScaling.Scaling scaling = player.weapon().scaling();
		double amount = player.weapon().damage();
		amount *= Math.max(0, 1 + scaling.strength() * derived.strengthDamageBonus() + scaling.dexterity() * derived.dexterityDamageBonus());
		if (player.weapon().kindlingShare() > 0) {
			amount *= WeaponRules.kindlingMultiplier(StatCurve.effectivePoints(player.stat(Stat.KINDLING)),
					GEAR.weapons().kindlingPerPoint(), player.weapon().kindlingShare());
		}
		amount *= factor;
		return DamageUtil.getDamageLeft((float) amount, (float) armor, 0);
	}

	/** Max health of a mob of base {@code health} at {@code level} (an ADD_MULTIPLIED_BASE modifier). */
	public static double mobHealth(double health, int level) {
		return health * (1 + MobLevelRules.healthBonus(LEVELS, level));
	}

	/** XP a mob of base {@code xp} drops at {@code level}. */
	public static int mobXp(int xp, int level) {
		return MobLevelRules.experience(LEVELS, level, xp);
	}

	public static int hitsToKill(double health, double perHit) {
		return perHit <= 0 ? Integer.MAX_VALUE : (int) Math.ceil(health / perHit - 1e-6);
	}

	/** One mob at one level against one player. */
	public record Fight(Mob mob, int level, Player player, double mobHealth, double playerHit, int hitsToKillMob,
			double mobHit, int hitsToKillPlayer, double secondsToKill, double damageTaken) {
		/** Damage taken while trading hits until the mob dies, as a share of the player's max health. */
		public double tradeShare() {
			return damageTaken / player.maxHealth();
		}
	}

	/**
	 * {@code player} against {@code mob} at {@code level}, trading hits: the player swings at full charge, the mob
	 * lands its main hit every {@link Hit#interval()} ticks from the first swing on.
	 */
	public static Fight fight(Mob mob, int level, Player player) {
		return fight(mob, level, player, Difficulty.NORMAL);
	}

	public static Fight fight(Mob mob, int level, Player player, Difficulty difficulty) {
		double health = mobHealth(mob.health(), level) + mob.extraHealth();
		double perHit = playerHit(player, mob.armor(), 1);
		int hits = hitsToKill(health, perHit);
		Hit main = mob.mainHit();
		double taken = hitOnPlayer(main, level, player, difficulty);
		double ticks = (hits - 1) * player.attackTicks();
		double landed = main.interval() > 0 ? Math.floor(ticks / main.interval()) + 1 : 1;
		return new Fight(mob, level, player, health, perHit, hits, taken, hitsToKill(player.maxHealth(), taken),
				ticks / 20, landed * taken);
	}

	// --- Bosses --------------------------------------------------------------------------------------------------

	/**
	 * A boss at its level against {@code fighters} players like {@code player}. Hits to kill count both phases: the
	 * first at the boss's armor and factors, the second (from {@link Boss#phase2At}) at its second-phase ones.
	 */
	public record BossFight(Boss boss, int level, int fighters, Player player, double health, double sideHit, double bestHit,
			int sideHitsEach, int bestHitsEach, int adds, Mob addMob, int addLevel, Map<String, Double> moveDamage) {
		/** Seconds of the player's own swinging to bring the boss down at ordinary openings, in a group. */
		public double sideSeconds() {
			return sideHitsEach * player.attackTicks() / 20;
		}

		public double bestSeconds() {
			return bestHitsEach * player.attackTicks() / 20;
		}
	}

	public static BossFight bossFight(Boss boss, int level, int fighters, Player player) {
		double health = mobHealth(boss.health(), level) * BossScaling.healthMultiplier(fighters, BOSS.healthPerExtraFighter());
		double share = health / fighters;
		double p1 = boss.phase2At() > 0 ? share * (1 - boss.phase2At()) : share;
		double p2 = share - p1;
		double side = playerHit(player, boss.armor(), boss.sideFactor());
		double best = playerHit(player, boss.armor(), boss.bestFactor());
		double side2 = playerHit(player, boss.phase2Armor(), boss.phase2Side());
		double best2 = playerHit(player, boss.phase2Armor(), boss.phase2Best());
		int sideHits = hitsToKill(p1, side) + (p2 > 0 ? hitsToKill(p2, side2) : 0);
		int bestHits = hitsToKill(p1, best) + (p2 > 0 ? hitsToKill(p2, best2) : 0);
		Map<String, Double> moves = new LinkedHashMap<>();
		for (Hit hit : boss.hits()) {
			moves.put(hit.name(), hitOnPlayer(hit, level, player));
		}
		int adds = BossScaling.adds(fighters, BOSS.addsPerExtraFighter());
		Mob add = boss.scalingAdd().equals("-") ? null : BalanceData.mob(boss.scalingAdd());
		return new BossFight(boss, level, fighters, player, health, side, best, sideHits, bestHits, adds, add,
				Math.max(1, level - BOSS.addLevelOffset()), moves);
	}

	// --- Moon nights ---------------------------------------------------------------------------------------------

	/**
	 * Damage a player takes clearing a wave of {@code count} mobs that all attack at once, killing them one after the
	 * other, trading hits ({@code missChance}: share of the player's hits that pass through, the Ghost's).
	 */
	public static double waveDamage(Mob mob, int level, int count, Player player, double missChance) {
		double health = mobHealth(mob.health(), level) / (1 - missChance);
		int hits = hitsToKill(health, playerHit(player, mob.armor(), 1));
		double seconds = hits * player.attackTicks() / 20;
		Hit hit = mob.mainHit();
		double perSecond = hitOnPlayer(hit, level, player) * 20 / hit.interval();
		// While the i-th dies, count − i + 1 of them are still hitting.
		return count * (count + 1) / 2.0 * seconds * perSecond;
	}

	/** Where a boss hit falls against the DESIGN §4.5 targets (vs full diamond on Normal). */
	public enum HitClass {
		LIGHT(4, 6), HEAVY(8, 12), UNBLOCKABLE(12, 17);

		public final double min;
		public final double max;

		HitClass(double min, double max) {
			this.min = min;
			this.max = max;
		}
	}

	/** The reference the boss targets are written against: full unenchanted diamond, no stat effects, Normal. */
	public static final Player DIAMOND_REFERENCE = new Player(1, statsAt(1), DIAMOND, DIAMOND_SWORD, FLASK.baseCharges());

	// --- Progression ---------------------------------------------------------------------------------------------

	/**
	 * What "a normal playthrough" means in numbers (R40.1a). Every value is an assumption of the model, written up in
	 * {@code docs/balance-0.4.0.md}.
	 *
	 * @param sanctumLevel        character level on the first step into the realm (overworld XP spent at the Maiden)
	 * @param structuresPerZone   minor structures cleared on the way to a zone's arena, besides the two road outposts
	 * @param cryptsPerZone       Necropolis crypts cleared
	 * @param naturalKillsPerZone naturally spawned mobs killed in a zone
	 * @param naturalKillsDepths  Cinder Hounds killed in the Burning City
	 */
	public record Playthrough(int sanctumLevel, int structuresPerZone, int cryptsPerZone, int naturalKillsPerZone,
			int naturalKillsDepths) {
		public static final Playthrough NORMAL = new Playthrough(10, 6, 1, 25, 20);
	}

	/** XP of a zone by where it comes from. */
	public record ZoneXp(Zone zone, double placed, double natural, double boss) {
		public double total() {
			return placed + natural + boss;
		}
	}

	/** Mean XP of one of {@code id}s killed at a uniform level of the zone's band. */
	public static double meanXp(String id, Zone zone) {
		Mob mob = BalanceData.mob(id);
		double sum = 0;
		for (int level = zone.minLevel; level <= zone.maxLevel; level++) {
			sum += mobXp(mob.xp(), level);
		}
		return sum / (zone.maxLevel - zone.minLevel + 1);
	}

	private static double xpOf(Map<String, Integer> mobs, Zone zone) {
		double xp = 0;
		for (Map.Entry<String, Integer> entry : mobs.entrySet()) {
			xp += entry.getValue() * meanXp(entry.getKey(), zone);
		}
		return xp;
	}

	/** Mean placed-enemy XP of one minor structure of the zone, by structure-set weight. */
	public static double structureXp(Zone zone) {
		double xp = 0;
		int weights = 0;
		for (BalanceData.Structure structure : BalanceData.STRUCTURES) {
			if (structure.zone() == zone) {
				xp += structure.weight() * xpOf(structure.mobs(), zone);
				weights += structure.weight();
			}
		}
		return weights == 0 ? 0 : xp / weights;
	}

	/** Mean XP of one natural spawn of the zone, by spawn weight. */
	public static double naturalXp(Zone zone) {
		Map<String, Integer> weights = BalanceData.NATURAL.get(zone);
		int total = weights.values().stream().mapToInt(Integer::intValue).sum();
		double xp = 0;
		for (Map.Entry<String, Integer> entry : weights.entrySet()) {
			xp += entry.getValue() * meanXp(entry.getKey(), zone);
		}
		return xp / total;
	}

	public static Boss bossOf(Zone zone) {
		for (Boss boss : BalanceData.BOSSES) {
			if (boss.zone() == zone) {
				return boss;
			}
		}
		throw new IllegalArgumentException(zone.name());
	}

	public static ZoneXp zoneXp(Zone zone, Playthrough run) {
		Boss boss = bossOf(zone);
		double bossXp = mobXp(boss.xp(), boss.level());
		if (zone == Zone.CITADEL) {
			return new ZoneXp(zone, xpOf(BalanceData.CITADEL, zone), run.naturalKillsDepths() * naturalXp(zone), bossXp);
		}
		double placed = xpOf(BalanceData.OUTPOSTS.get(zone), zone) + run.structuresPerZone() * structureXp(zone);
		if (zone == Zone.NECROPOLIS) {
			placed += run.cryptsPerZone() * xpOf(BalanceData.CRYPT, zone);
		}
		return new ZoneXp(zone, placed, run.naturalKillsPerZone() * naturalXp(zone), bossXp);
	}

	/** Level reached from {@code level} by spending {@code xp} XP points at the Maiden (whole levels only). */
	public static int levelAfter(int level, double xp) {
		while (xp >= LEVELING.cost(level)) {
			xp -= LEVELING.cost(level);
			level++;
		}
		return level;
	}

	/** One stage of the playthrough: the player entering a place, and how they got there. */
	public record Stage(String name, Zone zone, int order, List<Zone> before, int level) {}

	/**
	 * The player entering each open zone as the first, second or third (every order of the zones before it), and the
	 * depths after all three. Levels assume every XP point earned so far went into levels.
	 */
	public static List<Stage> stages(Playthrough run) {
		List<Stage> stages = new ArrayList<>();
		for (Zone zone : Zone.OPEN) {
			List<Zone> others = Zone.OPEN.stream().filter(z -> z != zone).toList();
			stages.add(stage(zone, List.of(), run));
			for (Zone other : others) {
				stages.add(stage(zone, List.of(other), run));
			}
			stages.add(stage(zone, others, run));
		}
		stages.add(stage(Zone.CITADEL, Zone.OPEN, run));
		return stages;
	}

	private static Stage stage(Zone zone, List<Zone> before, Playthrough run) {
		double xp = before.stream().mapToDouble(z -> zoneXp(z, run).total()).sum();
		String name = zone == Zone.CITADEL ? "depths" : zone.title + " " + ordinal(before.size() + 1)
				+ (before.isEmpty() ? "" : " (after " + String.join(" + ", before.stream().map(z -> z.title).toList()) + ")");
		return new Stage(name, zone, before.size() + 1, before, levelAfter(run.sanctumLevel(), xp));
	}

	private static String ordinal(int n) {
		return switch (n) {
			case 1 -> "1st";
			case 2 -> "2nd";
			default -> "3rd";
		};
	}

	/**
	 * The gear expected when entering a stage: a zone entered first with iron armor and an iron sword (the floor),
	 * later zones in diamond with a diamond sword, the depths in the Smoldering Plate with the first zone boss's
	 * weapon (all three zone materials are needed for a Smoldering Ingot, so neither exists earlier).
	 */
	public static Player expectedPlayer(Stage stage, boolean floor) {
		int flask = FLASK.baseCharges() + stage.before().size();
		if (stage.zone() == Zone.CITADEL) {
			return Player.of(stage.level(), SMOLDERING, floor ? DIAMOND_SWORD : ASHEN_SCYTHE, flask);
		}
		return floor && stage.order() == 1 ? Player.of(stage.level(), IRON, IRON_SWORD, flask)
				: Player.of(stage.level(), DIAMOND, DIAMOND_SWORD, flask);
	}

	private BalanceModel() {}
}
