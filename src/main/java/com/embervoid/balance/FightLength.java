package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Aim;
import com.embervoid.balance.BalanceData.Beat;
import com.embervoid.balance.BalanceData.Boss;
import com.embervoid.balance.BalanceData.Cadence;
import com.embervoid.balance.BalanceData.Downtime;
import com.embervoid.balance.BalanceData.Hit;
import com.embervoid.balance.BalanceData.Mob;
import com.embervoid.balance.BalanceData.Phase;
import com.embervoid.balance.BalanceData.Range;
import com.embervoid.balance.BalanceData.Size;
import com.embervoid.balance.BalanceData.Tell;
import com.embervoid.balance.BalanceData.Timing;
import com.embervoid.balance.BalanceData.Zone;
import com.embervoid.balance.BalanceModel.Fight;
import com.embervoid.balance.BalanceModel.Player;
import com.embervoid.balance.BalanceModel.Playthrough;
import com.embervoid.balance.BalanceModel.Stage;
import com.embervoid.balance.BalanceModel.Weapon;
import com.embervoid.balance.OverworldData.OverworldMob;
import com.embervoid.boss.BossScaling;
import com.embervoid.nightmob.NightMobConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * The fight-length model of 0.5.0 (PLAN 18.1, DESIGN §5.5): turns {@link BalanceModel}'s hits to kill into seconds of
 * a fight with dodging, from each boss's move cadence in {@link BalanceData} (wind-ups, the brain's cooldowns, phases,
 * hazards, adds), and puts the 0.5.0 targets' stages in one place: the expected player and reference weapon of every
 * boss and mob stage of the game, and every boss move as a share of that player's health. Pure, like the model.
 *
 * <p>A fight is a loop of {@link Beat}s per phase. A fighter swings at full charge whenever the boss is open: during a
 * melee move's windup until {@value #ESCAPE_TICKS} ticks before its hit, and after it from {@value #REENGAGE_TICKS}
 * ticks into its recovery; a fighter the move is not aimed at swings through it. Downtime (a storm behind a pillar,
 * adds to kill) stops all swinging. The share of the fight spent swinging is the boss's uptime.
 */
public final class FightLength {
	/** Ticks before a move's hit at which the player stops swinging to leave its shape (about 2.5 blocks at a sprint). */
	public static final int ESCAPE_TICKS = 10;
	/** Ticks after a dodged hit until the player swings again: back into reach (about 9) and a reaction (about 7). */
	public static final int REENGAGE_TICKS = 16;

	/** DESIGN §5.5: a boss fight solo with dodging, and its clean hits with the stage's reference weapon. */
	public static final double MIN_SECONDS = 90;
	public static final double MAX_SECONDS = 150;
	public static final int MIN_HITS = 20;
	public static final int MAX_HITS = 35;

	// --- Openings ------------------------------------------------------------------------------------------------

	/** Ticks of one {@code beat} in which a fighter can swing at the boss ({@code targeted}: the move is aimed at them). */
	public static double openTicks(Beat beat, boolean targeted) {
		if (beat.range() == Range.RANGED) {
			return 0;
		}
		if (beat.range() == Range.CLOSING) {
			// It comes from range: nothing before it lands; after it, the dodge and the way back.
			return Math.max(0, beat.recovery() - REENGAGE_TICKS);
		}
		if (beat.aim() == Aim.NONE || (beat.aim() == Aim.TARGET && !targeted)) {
			return beat.ticks();
		}
		return Math.max(0, Math.max(0, beat.lead() - ESCAPE_TICKS) + beat.recovery() - REENGAGE_TICKS);
	}

	// --- Fight length --------------------------------------------------------------------------------------------

	/**
	 * One phase of a fight.
	 *
	 * @param health       boss health to take in it (all fighters together)
	 * @param loops        loops of its beats it takes
	 * @param hitsEach     clean hits each fighter lands in it
	 * @param hittingTicks ticks each fighter spends swinging in it
	 */
	public record PhaseLength(Phase phase, double health, double hit, double loops, double ticks, double hitsEach, double hittingTicks) {}

	/**
	 * A whole fight of {@code fighters} players like {@code player}.
	 *
	 * @param addTicks ticks spent on the adds summoned with the boss for the extra fighters (shared)
	 */
	public record Length(Cadence cadence, Player player, int fighters, double health, List<PhaseLength> phases, double addTicks) {
		public double ticks() {
			return phases.stream().mapToDouble(PhaseLength::ticks).sum() + addTicks;
		}

		public double seconds() {
			return ticks() / 20;
		}

		/** Clean hits each fighter lands on the boss. */
		public double hitsEach() {
			return phases.stream().mapToDouble(PhaseLength::hitsEach).sum();
		}

		/** Seconds of each fighter's own swinging: the 0.4.0 model's "seconds". */
		public double hittingSeconds() {
			return phases.stream().mapToDouble(PhaseLength::hittingTicks).sum() / 20;
		}

		/** Share of the fight each fighter spends swinging at the boss. */
		public double uptime() {
			return hittingSeconds() / seconds();
		}
	}

	/** Level of a boss's adds (fight adds and multiplayer adds, {@code BossFight.spawnAdd}). */
	public static int addLevel(Boss boss) {
		return Math.max(1, boss.level() - BalanceModel.BOSS.addLevelOffset());
	}

	/** Ticks {@code player} swings to kill {@code count} of {@code id} at {@code level} (adds stand and trade). */
	public static double killTicks(String id, double count, int level, Player player) {
		Fight fight = BalanceModel.fight(BalanceData.mob(id), level, player);
		return count * fight.hitsToKillMob() * player.attackTicks();
	}

	private static double downtimeTicks(Downtime downtime, Boss boss, Player player, int fighters) {
		double adds = downtime.add() == null ? 0 : killTicks(downtime.add(), downtime.count(), addLevel(boss), player) / fighters;
		return downtime.ticks() + adds;
	}

	/**
	 * {@code fighters} players like {@code player} against the boss of {@code cadence} at its level. The boss aims at
	 * each of them for an equal share of the fight (it switches target every 8–10 s).
	 */
	public static Length length(Cadence cadence, Player player, int fighters) {
		Boss boss = cadence.boss();
		double scale = cadence.scaled() ? BossScaling.healthMultiplier(fighters, BalanceModel.BOSS.healthPerExtraFighter()) : 1;
		double total = BalanceModel.mobHealth(boss.health(), boss.level()) * scale;
		double swing = player.attackTicks();
		double aimedAt = 1.0 / fighters;
		List<PhaseLength> phases = new ArrayList<>();
		List<Phase> list = cadence.phases();
		for (int i = 0; i < list.size(); i++) {
			Phase phase = list.get(i);
			double to = i + 1 < list.size() ? list.get(i + 1).from() : 0;
			double health = total * (phase.from() - to);
			double share = 0;
			double once = phase.pause();
			for (Downtime downtime : phase.downtime()) {
				double ticks = downtimeTicks(downtime, boss, player, fighters);
				if (downtime.every() > 0) {
					share += ticks / downtime.every();
				} else {
					once += ticks;
				}
			}
			double loopTicks = 0;
			double open = 0;
			double damage = 0;
			for (Beat beat : phase.beats()) {
				double beatOpen = aimedAt * openTicks(beat, true) + (1 - aimedAt) * openTicks(beat, false);
				double factor = Double.isNaN(beat.factor()) ? phase.factor() : beat.factor();
				loopTicks += beat.weight() * beat.ticks();
				open += beat.weight() * beatOpen;
				damage += beat.weight() * beatOpen / swing * BalanceModel.playerHit(player, phase.armor(), factor);
			}
			double hit = BalanceModel.playerHit(player, phase.armor(), phase.factor());
			if (health <= 0 || damage <= 0) {
				phases.add(new PhaseLength(phase, health, hit, 0, once, 0, 0));
				continue;
			}
			double loops = health / (fighters * damage);
			double ticks = loops * loopTicks / (1 - Math.min(share, 0.9)) + once;
			phases.add(new PhaseLength(phase, health, hit, loops, ticks, loops * open / swing, loops * open));
		}
		int adds = cadence.scaled() ? BossScaling.adds(fighters, BalanceModel.BOSS.addsPerExtraFighter()) : 0;
		double addTicks = adds == 0 || boss.scalingAdd().equals("-") ? 0
				: killTicks(boss.scalingAdd(), adds, addLevel(boss), player) / fighters;
		return new Length(cadence, player, fighters, total, phases, addTicks);
	}

	// --- Stages (DESIGN §5.5 reference players and weapons) -------------------------------------------------------

	/** A boss fight of the 0.5.0 targets: who fights it, with what. */
	public record BossStage(String name, Cadence cadence, Player player) {}

	/**
	 * The melee unique a zone's boss leaves, crafted from its soul and the zone's material during the realm (DESIGN
	 * §6.1), or null (the Woods: the Huntsman's Crossbow, not modelled).
	 */
	public static Weapon uniqueOf(Zone zone) {
		return switch (zone) {
			case FIELDS -> BalanceModel.ASHEN_SCYTHE;
			case NECROPOLIS -> BalanceModel.WARDENS_SCOURGE;
			default -> null;
		};
	}

	/** The melee uniques of the bosses beaten before {@code stage}. */
	public static List<Weapon> uniquesBefore(Stage stage) {
		List<Weapon> weapons = new ArrayList<>();
		for (Zone zone : stage.before()) {
			Weapon weapon = uniqueOf(zone);
			if (weapon != null) {
				weapons.add(weapon);
			}
		}
		return weapons;
	}

	private static Player holding(Player player, Weapon weapon) {
		return new Player(player.level(), player.stats(), player.armor(), weapon, player.flaskCharges());
	}

	/** The Ruined Sanctum player: level 1, full iron, an iron sword (DESIGN §2, §5.5). */
	public static final Player DUNGEON_PLAYER = Player.of(1, BalanceModel.IRON, BalanceModel.IRON_SWORD, BalanceModel.FLASK.baseCharges());
	/** The blood new moon player: the Sanctum-level player, level 10 in iron with an iron sword (the boss is at least 15). */
	public static final Player MOON_PLAYER = Player.of(BalanceModel.Playthrough.NORMAL.sanctumLevel(), BalanceModel.IRON,
			BalanceModel.IRON_SWORD, BalanceModel.FLASK.baseCharges());
	/** A full moon wave's player near spawn: level 5 in iron with an iron sword (the 0.4.0 moon target's). */
	public static final Player FULL_MOON_PLAYER = Player.of(5, BalanceModel.IRON, BalanceModel.IRON_SWORD, BalanceModel.FLASK.baseCharges());

	/**
	 * Every boss fight of the targets, in the order of the game: the Fallen Sister at level 1 in iron, the Vampire boss,
	 * each zone boss entered first (level 10, diamond, diamond sword) and later (diamond and each melee unique of a boss
	 * beaten before, or the diamond sword when there is none), the Smoldering Lord with each melee unique.
	 */
	public static List<BossStage> bossStages(Playthrough run) {
		List<BossStage> stages = new ArrayList<>();
		stages.add(new BossStage("Ruined Sanctum", BalanceData.FALLEN_SISTER_CADENCE, DUNGEON_PLAYER));
		stages.add(new BossStage("blood new moon", BalanceData.VAMPIRE_CADENCE, MOON_PLAYER));
		List<Stage> all = BalanceModel.stages(run);
		for (Boss boss : BalanceData.BOSSES) {
			Cadence cadence = BalanceData.cadenceOf(boss);
			for (Stage stage : all) {
				if (stage.zone() != boss.zone()) {
					continue;
				}
				Player player = BalanceModel.expectedPlayer(stage, false);
				if (stage.order() == 1) {
					stages.add(new BossStage(stage.name(), cadence, player));
					continue;
				}
				List<Weapon> uniques = uniquesBefore(stage);
				if (uniques.isEmpty()) {
					stages.add(new BossStage(stage.name() + ", no melee unique yet", cadence, holding(player, BalanceModel.DIAMOND_SWORD)));
				}
				for (Weapon unique : uniques) {
					stages.add(new BossStage(stage.name(), cadence, holding(player, unique)));
				}
			}
		}
		return stages;
	}

	/** The first (weakest) expected player who fights each boss: the one its hit sizes are judged against. */
	public static Player weakestPlayer(Cadence cadence, Playthrough run) {
		return bossStages(run).stream().filter(s -> s.cadence() == cadence).findFirst().orElseThrow().player();
	}

	// --- Hit sizes -----------------------------------------------------------------------------------------------

	/** §5.5's most a move of each class may take of the expected player's max health. */
	public static double limit(Size size) {
		return switch (size) {
			case LIGHT -> 0.20;
			case HEAVY -> 0.35;
			case BIGGEST -> 2 / 3.0;
		};
	}

	/** One boss move on one player: damage after everything, and as a share of max health. */
	public record HitShare(Cadence cadence, Hit hit, Timing timing, Player player, double damage, double share) {
		/** Every way it breaks DESIGN §5.5 (empty: none). */
		public List<String> breaks() {
			List<String> breaks = new ArrayList<>();
			if (share > limit(timing.size())) {
				breaks.add(timing.size().name().toLowerCase(java.util.Locale.ROOT) + " above " + Math.round(limit(timing.size()) * 100) + " %");
			}
			if (share > 2 / 3.0 && timing.size() != Size.BIGGEST) {
				breaks.add("above two thirds");
			}
			if (share >= 1 / 3.0 && (timing.windup() < 12 || timing.tell().compareTo(Tell.ANIMATION) < 0)) {
				breaks.add("≥ ⅓ with a " + timing.windup() + "-tick windup (needs ≥ 12 and a tell)");
			}
			if (share >= 0.5 && (timing.windup() < 20 || timing.tell().compareTo(Tell.SOUND) < 0)) {
				breaks.add("≥ ½ with a " + timing.windup() + "-tick windup (needs ≥ 20 and a shape or sound)");
			}
			return breaks;
		}
	}

	/** Every non-hazard move of the boss on {@code player} on Normal. */
	public static List<HitShare> hitShares(Cadence cadence, Player player) {
		List<HitShare> shares = new ArrayList<>();
		for (Hit hit : cadence.boss().hits()) {
			if (hit.hazard()) {
				continue;
			}
			double damage = BalanceModel.hitOnPlayer(hit, cadence.boss().level(), player);
			shares.add(new HitShare(cadence, hit, cadence.timing(hit.name()), player, damage, damage / player.maxHealth()));
		}
		return shares;
	}

	// --- Mobs ----------------------------------------------------------------------------------------------------

	/** DESIGN §5.5's hits-to-kill bands, and the fewest hits a mob of the band may need to kill the player. */
	public enum Band {
		FODDER(2, 3, 6),
		/** Ambushers, ranged and flyers ("ambushers and ranged 3–4"); against the player between fodder and elites. */
		SKIRMISHER(3, 4, 5),
		/** Tanks and elites. */
		ELITE(6, 10, 4);

		public final int minHits;
		public final int maxHits;
		public final int minHitsOnPlayer;

		Band(int minHits, int maxHits, int minHitsOnPlayer) {
			this.minHits = minHits;
			this.maxHits = maxHits;
			this.minHitsOnPlayer = minHitsOnPlayer;
		}
	}

	/** A mob's band by role; the Ghost (twice an Ashen Zombie, the full moon's punishment) counts as an elite. */
	public static Band bandOf(Mob mob) {
		if (mob == BalanceData.GHOST) {
			return Band.ELITE;
		}
		return switch (mob.role()) {
			case FODDER -> Band.FODDER;
			case AMBUSHER, RANGED, FLYER -> Band.SKIRMISHER;
			case TANK, ELITE -> Band.ELITE;
		};
	}

	/** One mob at one level against the stage's expected player. */
	public record MobStage(String stage, Mob mob, int level, Player player) {
		public Fight fight() {
			return BalanceModel.fight(mob, level, player);
		}
	}

	/**
	 * Every mob stage of the targets: the overworld at each mob's lowest level and the Ruined Sanctum's guards (a level
	 * 1 player in iron with an iron sword), full moon waves near spawn, each zone's mobs at the band's bottom against
	 * the player entering it first (level 10, diamond sword) and at its top against the player entering it third (with
	 * a unique), the Citadel's at both ends against the depths player.
	 */
	public static List<MobStage> mobStages(Playthrough run) {
		List<MobStage> stages = new ArrayList<>();
		for (OverworldMob mob : OverworldData.MOBS) {
			if (mob != OverworldData.HOLLOW) {
				stages.add(new MobStage("overworld", mob.mob(), OverworldModel.minLevel(mob), DUNGEON_PLAYER));
			}
		}
		for (OverworldMob guard : List.of(OverworldData.HOLLOW, OverworldData.SKELETON)) {
			stages.add(new MobStage("Ruined Sanctum", guard.mob(), guard.baseLevel(), DUNGEON_PLAYER));
		}
		int moonLevel = 4 + NightMobConfig.DEFAULT.zombieLevelBonus();
		stages.add(new MobStage("full moon", BalanceData.ASHEN_ZOMBIE, moonLevel, FULL_MOON_PLAYER));
		stages.add(new MobStage("full moon", BalanceData.GHOST, moonLevel, FULL_MOON_PLAYER));
		List<Stage> all = BalanceModel.stages(run);
		for (Zone zone : Zone.values()) {
			Stage first = all.stream().filter(s -> s.zone() == zone).findFirst().orElseThrow();
			Stage last = all.stream().filter(s -> s.zone() == zone).reduce((a, b) -> b).orElseThrow();
			List<Weapon> uniques = uniquesBefore(last);
			Player late = uniques.isEmpty() ? BalanceModel.expectedPlayer(last, false) : holding(BalanceModel.expectedPlayer(last, false), uniques.get(0));
			// The depths are one stage: the player there already holds a unique.
			Player early = zone == Zone.CITADEL ? late : BalanceModel.expectedPlayer(first, false);
			for (Mob mob : BalanceData.MOBS) {
				if (mob.zone() == zone) {
					stages.add(new MobStage(zone == Zone.CITADEL ? "depths" : first.name(), mob, zone.minLevel, early));
					stages.add(new MobStage(zone == Zone.CITADEL ? "depths" : last.name(), mob, zone.maxLevel, late));
				}
			}
		}
		return stages;
	}

	// --- Weapons -------------------------------------------------------------------------------------------------

	/** An unenchanted netherite sword per second (DESIGN §6.1): 8 at 1.6. */
	public static final double NETHERITE_PER_SECOND = BalanceModel.NETHERITE_SWORD.damage() * BalanceModel.NETHERITE_SWORD.speed();

	/**
	 * A unique's damage factor in its own use (DESIGN §6.1): the Scythe on two enemies (its sweep hits the second at
	 * {@code gear.weapons.sweepShare}), the Scourge at its tip (×1.6, built in PLAN 17.4), the Rapier fully ramped
	 * ({@code gear.weapons.rampMax}).
	 */
	public static double ownUse(Weapon weapon) {
		if (weapon == BalanceModel.ASHEN_SCYTHE) {
			return 1 + BalanceModel.GEAR.weapons().sweepShare();
		}
		if (weapon == BalanceModel.WARDENS_SCOURGE) {
			return SCOURGE_TIP;
		}
		if (weapon == BalanceModel.ROYAL_ASH_RAPIER) {
			return 1 + BalanceModel.GEAR.weapons().rampMax();
		}
		return 1;
	}

	/** The Scourge's tip crack (DESIGN §6.1); PLAN 17.4 builds it, until then the Scourge hits one target plainly. */
	public static final double SCOURGE_TIP = 1.6;

	/** {@link #ownUse} on a single target: the same but for the Scythe, whose sweep needs a second enemy. */
	public static double oneTarget(Weapon weapon) {
		return weapon == BalanceModel.ASHEN_SCYTHE ? 1 : ownUse(weapon);
	}

	/** Attribute damage per second at full charge, no stats (DESIGN §6.1 compares weapons this way). */
	public static double perSecond(Weapon weapon) {
		return weapon.damage() * weapon.speed();
	}

	private FightLength() {}
}
