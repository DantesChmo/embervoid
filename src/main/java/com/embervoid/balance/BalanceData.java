package com.embervoid.balance;

import com.embervoid.reach.boss.AshColossusEntity;
import com.embervoid.reach.boss.CryptKeeperEntity;
import com.embervoid.reach.boss.GrievingTreeEntity;
import com.embervoid.reach.boss.SmolderingLordEntity;
import com.embervoid.reach.mob.FireFlaskEntity;
import com.embervoid.reach.mob.FurnaceGolemEntity;
import com.embervoid.reach.mob.HollowSentinelEntity;
import com.embervoid.reach.mob.RestlessBonesEntity;
import com.embervoid.reach.mob.SparkingMarksmanEntity;
import com.embervoid.sanctum.boss.CryingSoulEntity;
import com.embervoid.sanctum.boss.FallenSisterEntity;
import com.embervoid.sanctum.boss.SanctumBossArena;
import com.embervoid.vampire.VampireBossEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The base numbers of Realm 1 (and the moon nights) that live in entity code, as data for {@link BalanceModel}
 * (R40.1a). Public constants of the entity classes are read directly (compile-time constants, so nothing is loaded);
 * the rest (attributes in {@code createAttributes}, private move damages, XP) is written here and checked against
 * the live entities by the {@code BalanceFightTests} GameTest, so a change on either side fails the build.
 *
 * <p>Everything else the model needs (stat curve, level cost, mob levels, difficulty, boss scaling, armor) comes from
 * the real formula classes in {@link BalanceModel}.
 *
 * <p>For the fight-length model ({@link FightLength}, PLAN 18.1) each boss also has a {@link Cadence}: its moves'
 * wind-ups and the brain's cooldowns, read from the entity classes' public constants (the {@code BalanceFightTests}
 * GameTest runs the moves and checks the wind-ups), its phases and its downtime.
 */
public final class BalanceData {
	/** A realm zone with its mob level band (DESIGN §4.5; {@code realm.Realms}, checked by the GameTest). */
	public enum Zone {
		FIELDS("Ashen Fields", 15, 18),
		WOODS("Royal Woods", 18, 21),
		NECROPOLIS("Sunken Necropolis", 21, 25),
		CITADEL("Smoldering Citadel", 25, 30);

		public final String title;
		public final int minLevel;
		public final int maxLevel;

		Zone(String title, int minLevel, int maxLevel) {
			this.title = title;
			this.minLevel = minLevel;
			this.maxLevel = maxLevel;
		}

		/** Mean level of a mob in this zone (uniform over the band, {@code RealmMobLevels}). */
		public double meanLevel() {
			return (minLevel + maxLevel) / 2.0;
		}

		public static final List<Zone> OPEN = List.of(FIELDS, WOODS, NECROPOLIS);
	}

	/** How a hit reaches the player. */
	public enum HitKind {
		/** Melee or a thrown thing: difficulty, mob level, armor. */
		PHYSICAL,
		/** Fire (IN_FIRE): also fire reductions (RES, Smoldering Plate). */
		FIRE,
		/** Breaks a raised shield; otherwise physical. */
		UNBLOCKABLE,
		/** Magic with no attacker (the ash storm): no difficulty, no level, no armor. */
		FLAT,
		/** Magic from a mob (a witch's harming potion): difficulty and mob level, no armor. */
		MAGIC
	}

	/**
	 * One kind of hit a mob or boss deals.
	 *
	 * @param name     what the move is called in the write-up
	 * @param base     damage before difficulty and level (what the code passes to {@code damage})
	 * @param kind     how it reaches the player
	 * @param interval ticks between two of these hits when the mob keeps attacking (0 = not repeated, e.g. a phase move)
	 * @param hazard   damage over time or from the arena (a pool, a trail, the storm): outside the hit classes
	 */
	public record Hit(String name, double base, HitKind kind, int interval, boolean hazard) {
		public Hit(String name, double base, HitKind kind, int interval) {
			this(name, base, kind, interval, false);
		}
	}

	public enum Role { FODDER, AMBUSHER, RANGED, FLYER, TANK, ELITE }

	/**
	 * A realm mob at level 1 (DESIGN §4.5 Bestiary).
	 *
	 * @param id           entity id path
	 * @param health       max health at level 1; extra lives (a bone pile) are in {@code extraHealth}
	 * @param extraHealth  health to beat after the first death at level 1 (Restless Bones' pile: a share of its max
	 *                     health, so it scales with level like it)
	 * @param armor        armor points
	 * @param attack       {@code generic.attack_damage} (checked against the entity)
	 * @param xp           base XP
	 * @param placedOnly   only placed in structures (tanks, elites, the Marksman)
	 */
	public record Mob(String id, String name, Zone zone, Role role, double health, double extraHealth, double armor,
			double attack, int xp, boolean placedOnly, List<Hit> hits) {
		public Hit mainHit() {
			return hits.get(0);
		}
	}

	private static Hit melee(String name, double base, int interval) {
		return new Hit(name, base, HitKind.PHYSICAL, interval);
	}

	private static Hit hazard(String name, double base, HitKind kind) {
		return new Hit(name, base, kind, 0, true);
	}

	public static final List<Mob> MOBS = List.of(
			new Mob("cinder_husk", "Cinder Husk", Zone.FIELDS, Role.FODDER, 10, 0, 0, 3, 6, false,
					List.of(melee("swing", 3, 30))),
			new Mob("ash_stalker", "Ash Stalker", Zone.FIELDS, Role.AMBUSHER, 14, 0, 0, 4, 8, false,
					List.of(melee("swing / leap", 4, 25))),
			new Mob("ashen_carrion", "Ashen Carrion", Zone.FIELDS, Role.FLYER, 14, 0, 0, 4, 15, false,
					List.of(melee("dive", 4, 200))),
			new Mob("withered_thrall", "Withered Thrall", Zone.WOODS, Role.FODDER, 9, 0, 0, 3.5, 8, false,
					List.of(melee("swing", 3.5, 30))),
			new Mob("lantern_hag", "Lantern Hag", Zone.WOODS, Role.RANGED, 13, 0, 0, 2, 12, false,
					List.of(new Hit("fire flask", FireFlaskEntity.DAMAGE, HitKind.PHYSICAL, 50))),
			new Mob("bark_hollow", "Bark Hollow", Zone.WOODS, Role.TANK, 32, 0, 4, 7, 22, true,
					List.of(melee("sweep", 7, 50))),
			new Mob("restless_bones", "Restless Bones", Zone.NECROPOLIS, Role.FODDER, RestlessBonesEntity.BASE_HEALTH,
					RestlessBonesEntity.PILE_HEALTH, 0, 4, 8, false, List.of(melee("swing", 4, 30))),
			new Mob("crypt_moth", "Crypt Moth", Zone.NECROPOLIS, Role.FLYER, 12, 0, 0, 2, 8, false,
					List.of(melee("bite", 2, 25))),
			new Mob("hollow_sentinel", "Hollow Sentinel", Zone.NECROPOLIS, Role.TANK, 28, 0, 5, HollowSentinelEntity.THRUST_DAMAGE,
					22, true, List.of(melee("spear thrust", HollowSentinelEntity.THRUST_DAMAGE, 40),
							melee("shield bash", HollowSentinelEntity.BASH_DAMAGE, 50))),
			new Mob("grave_warden", "Grave Warden", Zone.NECROPOLIS, Role.ELITE, 29, 0, 4, 8, 40, true,
					List.of(melee("swing", 8, 40))),
			new Mob("cinder_hound", "Cinder Hound", Zone.CITADEL, Role.FODDER, 14, 0, 0, 3, 7, false,
					List.of(melee("bite (+2 s fire)", 3, 20))),
			new Mob("sparking_marksman", "Sparking Marksman", Zone.CITADEL, Role.RANGED, 15, 0, 0, 2, 15, true,
					List.of(new Hit("bolt (+fire)", SparkingMarksmanEntity.BOLT_DAMAGE, HitKind.PHYSICAL, 60))),
			new Mob("furnace_golem", "Furnace Golem", Zone.CITADEL, Role.TANK, 30, 0, 12, 7.5, 30, true,
					List.of(melee("slam", 7.5, 50), new Hit("fire wave", 7.5 * FurnaceGolemEntity.WAVE_DAMAGE_FACTOR, HitKind.FIRE, 0))),
			new Mob("smoldering_knight", "Smoldering Knight", Zone.CITADEL, Role.ELITE, 32, 0, 10, 8, 50, true,
					// Three combo hits per 6 + 5 + 8 + 40 ticks; the lunge is ×1.25.
					List.of(melee("combo hit", 8, 20), melee("lunge", 8 * 1.25, 80))));

	/**
	 * A boss (DESIGN §4.5 Bosses), its moves by base damage.
	 *
	 * @param level       fixed level (arena JSON; the Vampire's is computed)
	 * @param sideFactor  damage the boss takes from an ordinary hit, before armor (the Tree's trunk: 0.3)
	 * @param bestFactor  damage it takes at the best opening (back, core, stun)
	 * @param phase2At    health fraction where the second phase starts (its numbers below), 0 for none
	 * @param scalingAdd  entity id of its multiplayer add
	 */
	public record Boss(String id, String name, Zone zone, int level, double health, double armor, double attack, int xp,
			double sideFactor, double bestFactor, String bestWhen, double phase2At, double phase2Armor, double phase2Side,
			double phase2Best, String scalingAdd, List<Hit> hits) {}

	public static final Boss ASH_COLOSSUS = new Boss("ash_colossus", "Ash Colossus", Zone.FIELDS, 18, 105, 10, 8, 150,
			1, AshColossusEntity.BACK_DAMAGE, "back (P2: the bare core)", 0.5, 3, 1, AshColossusEntity.CORE_DAMAGE, "cinder_husk", List.of(
					melee("fist cone", 8, 60),
					melee("leap ring", 9, 0),
					melee("boulder", 7, 0),
					melee("arm drag", 6, 0),
					new Hit("falling cinder (P2)", 5, HitKind.FIRE, 0),
					hazard("ash storm, per s (magic)", AshColossusEntity.STORM_DAMAGE, HitKind.FLAT)));
	public static final Boss GRIEVING_TREE = new Boss("grieving_tree", "Grieving Tree", Zone.WOODS, 21, 165, 6, 8, 170,
			GrievingTreeEntity.TRUNK_DAMAGE, GrievingTreeEntity.STUNNED_DAMAGE, "stunned (lanterns shot)", 0.5, 6,
			GrievingTreeEntity.TRUNK_DAMAGE, GrievingTreeEntity.STUNNED_DAMAGE, "withered_thrall", List.of(
					melee("branch sweep", 8, 60),
					melee("root line", 7, 0),
					melee("root cage (P2)", 4, 0)));
	public static final Boss CRYPT_KEEPER = new Boss("crypt_keeper", "Crypt Keeper", Zone.NECROPOLIS, 24, 102, 4, 5, 190,
			1, 1, "in brazier light (dark: ×" + CryptKeeperEntity.DARK_DAMAGE + ")", 0.6, 4, 1, 1, "restless_bones", List.of(
					melee("halberd swing", 5, 40),
					new Hit("overhead (3rd hit)", 10, HitKind.UNBLOCKABLE, 0),
					melee("sweep ring", 7, 0),
					melee("thrust", 7, 0),
					melee("lantern throw", 5, 0),
					hazard("fire pool, per 0.5 s", 2, HitKind.FIRE)));
	public static final Boss SMOLDERING_LORD = new Boss("smoldering_lord", "Smoldering Lord", Zone.CITADEL, 30, 100, 12, 5, 300,
			1, 1, "-", 0.5, 4, SmolderingLordEntity.SHATTERED_DAMAGE, SmolderingLordEntity.SHATTERED_DAMAGE, "cinder_hound", List.of(
					melee("greatsword swing", 5, 50),
					new Hit("overhead slam", 10, HitKind.UNBLOCKABLE, 0),
					melee("lunge", 8, 0),
					melee("landing ring", 8, 0),
					new Hit("mortar (P0)", 8, HitKind.FIRE, 0),
					new Hit("flame line (P0)", 8, HitKind.FIRE, 0),
					hazard("fire trail, per 0.5 s", 2, HitKind.FIRE)));

	public static final List<Boss> ZONE_BOSSES = List.of(ASH_COLOSSUS, GRIEVING_TREE, CRYPT_KEEPER);
	public static final List<Boss> BOSSES = List.of(ASH_COLOSSUS, GRIEVING_TREE, CRYPT_KEEPER, SMOLDERING_LORD);

	// --- Moon nights (DESIGN §3.3) -------------------------------------------------------------------------------

	/** Ashen Zombie: 24 HP, 4 dmg, armor 2, 20 XP. Ghost: 48 HP, 8 dmg, 40 XP (twice the zombie). */
	public static final Mob ASHEN_ZOMBIE = new Mob("ashen_zombie", "Ashen Zombie", null, Role.FODDER, 24, 0, 2, 4, 20, false,
			List.of(melee("swing", 4, 20)));
	public static final Mob GHOST = new Mob("ghost", "Ghost", null, Role.FLYER, 48, 0, 0, 8, 40, false,
			List.of(melee("touch", 8, 40)));
	public static final Boss VAMPIRE = new Boss("vampire_boss", "Vampire boss", null, 15, 220, 4, 5, 200,
			1, 1, "-", 0, 4, 1, 1, "-", List.of(
					melee("claw", 5, 30),
					melee("leap", 7, 0),
					melee("blood nova (heals 25 %)", 7.5, 0)));

	// --- Fight length (PLAN 18.1, DESIGN §5.5) --------------------------------------------------------------------

	/** What the player gets before a move lands (DESIGN §5.5 wind-up rule), weakest first. */
	public enum Tell {
		/** Nothing: a vanilla melee swing. */
		NONE,
		/** The move's animation only. */
		ANIMATION,
		/** Animation and a sound cue. */
		SOUND,
		/** Animation and a marked shape on the ground (ring, line, cone, falling shadow), mostly with a sound. */
		SHAPE
	}

	/**
	 * A boss move's class (DESIGN §5.5): light moves (combo hits, quick swings) at most 20 % of the expected player's
	 * max health, heavy moves (telegraphed areas, lunges, throws) at most 35 %, the boss's biggest at most two thirds.
	 */
	public enum Size { LIGHT, HEAVY, BIGGEST }

	/**
	 * When the boss may be hit around a move: {@link #MELEE} next to the player (its windup until the player has to
	 * leave and its recovery after they are back are openings), {@link #CLOSING} from range onto the player (a leap, a
	 * lunge: only the recovery after it lands), {@link #RANGED} from range and staying there (a throw: nothing).
	 */
	public enum Range { MELEE, CLOSING, RANGED }

	/**
	 * Whom a move makes dodge: its target only (a cone, a line, a shadow at the target), every fighter (a ring around
	 * the boss, root lines at everyone), or nobody (a stun, claws traded hit for hit).
	 */
	public enum Aim { TARGET, ALL, NONE }

	/**
	 * Timing of one boss {@link Hit}, for the hit-size and wind-up checks.
	 *
	 * @param hit    the hit's name in its boss's {@link Boss#hits()}
	 * @param windup ticks from its tell to the hit (a combo's follow-up: from the hit before)
	 */
	public record Timing(String hit, int windup, Size size, Tell tell) {}

	/**
	 * One beat of a boss's loop in a phase: a move (or an opening, a stun) and the brain's cooldown after it.
	 *
	 * @param name     what it is called in the write-up
	 * @param lead     ticks from its tell to its first hit (the part of the windup the player can still swing in)
	 * @param busy     ticks from its tell to its last hit (a combo: all its hits; a leap: windup and flight)
	 * @param recovery ticks after its last hit until the next move starts (the brain's cooldown)
	 * @param weight   how many of it one loop of the phase holds
	 * @param factor   damage the boss takes from a clean hit during it (NaN: the phase's)
	 */
	public record Beat(String name, int lead, int busy, int recovery, double weight, Aim aim, Range range, double factor) {
		public int ticks() {
			return busy + recovery;
		}
	}

	/**
	 * Fight time with no hitting: {@code ticks} every {@code every} ticks of the phase (0: once, when it starts), plus
	 * the time to kill {@code count} of mob {@code add} (null for none) at the boss's add level.
	 */
	public record Downtime(String name, double ticks, double every, String add, double count) {
		public static Downtime every(String name, double ticks, double every) {
			return new Downtime(name, ticks, every, null, 0);
		}

		public static Downtime adds(String name, String add, double count, double every) {
			return new Downtime(name, 0, every, add, count);
		}
	}

	/**
	 * One phase of a boss fight for the fight-length model.
	 *
	 * @param from   boss health fraction where it starts (the next phase's is where it ends; a phase that starts where
	 *               the next does, the Lord on the wall, costs no health)
	 * @param armor  the boss's armor in it
	 * @param factor damage the boss takes from a clean hit in it (a flank: neither the Colossus's front nor its back)
	 * @param pause  fixed ticks at its start in which the boss cannot be hit (a phase animation, the Lord on the wall)
	 */
	public record Phase(String name, double from, double armor, double factor, int pause, List<Beat> beats, List<Downtime> downtime) {}

	/**
	 * A boss's fight as the fight-length model sees it.
	 *
	 * @param scaled  max health scales with the fighters (arena bosses; the Vampire is one shared boss)
	 * @param timings one per {@link Hit} that is not a hazard
	 */
	public record Cadence(Boss boss, boolean scaled, List<Timing> timings, List<Phase> phases) {
		public Timing timing(String hit) {
			return timings.stream().filter(t -> t.hit().equals(hit)).findFirst().orElseThrow(() -> new IllegalArgumentException(hit));
		}
	}

	/** Vanilla's {@code MeleeAttackGoal}: one swing per 20 ticks, no windup (the Vampire's claws). */
	public static final int VANILLA_MELEE_COOLDOWN = 20;
	/**
	 * Share of a boss's picks made with the player out of melee range (pushed off by a hit, backing off to drink the
	 * flask, a dodge that went too far): the boss's moves at range get this weight in a loop.
	 */
	public static final double OUT_OF_MELEE = 0.2;

	private static Beat melee(String name, int windup, int recovery, double weight, Aim aim) {
		return new Beat(name, windup, windup, recovery, weight, aim, Range.MELEE, Double.NaN);
	}

	private static Beat combo(String name, int lead, int busy, int recovery, double weight) {
		return new Beat(name, lead, busy, recovery, weight, Aim.TARGET, Range.MELEE, Double.NaN);
	}

	private static Beat closing(String name, int busy, int recovery, double weight, Aim aim) {
		return new Beat(name, busy, busy, recovery, weight, aim, Range.CLOSING, Double.NaN);
	}

	private static Beat ranged(String name, int windup, int recovery, double weight) {
		return new Beat(name, windup, windup, recovery, weight, Aim.TARGET, Range.RANGED, Double.NaN);
	}

	/** An opening with no move (a stun): the player walks up to the boss first, like after a dodge. */
	private static Beat opening(String name, int ticks, double weight, double factor) {
		return new Beat(name, 0, 0, ticks, weight, Aim.ALL, Range.MELEE, factor);
	}

	/**
	 * The Fallen Sister, the Ruined Sanctum's boss (DESIGN §2, PLAN 16.3), built to the §5.5 targets for a level 1
	 * player in iron with an iron sword: level 10 in the band 8–12, HP for about 30 clean hits, damage by move class.
	 */
	public static final Boss FALLEN_SISTER = new Boss("fallen_sister", "Fallen Sister", null, SanctumBossArena.LEVEL,
			FallenSisterEntity.BASE_HEALTH, 4, 4, 60, 1, 1, "-", 0.5, 4, 1, 1, "-", List.of(
					melee("censer arc", 4, 0),
					melee("censer slam", 6, 0),
					melee("crying soul dive (P2)", 3, 0),
					melee("censer spin (P2)", 8, 0),
					hazard("ember puddle, per 0.5 s", 1, HitKind.FIRE)));

	/**
	 * Ash Colossus. The player stays at its flank: the front takes ×{@value AshColossusEntity#FRONT_DAMAGE}, the back
	 * ×{@value AshColossusEntity#BACK_DAMAGE}, and it turns to its target on every move, so a clean hit is a side hit.
	 */
	public static final Cadence ASH_COLOSSUS_CADENCE = new Cadence(ASH_COLOSSUS, true, List.of(
			new Timing("fist cone", AshColossusEntity.FIST_WINDUP, Size.HEAVY, Tell.SHAPE),
			new Timing("leap ring", AshColossusEntity.LEAP_WINDUP, Size.BIGGEST, Tell.SHAPE),
			new Timing("boulder", AshColossusEntity.BOULDER_WINDUP, Size.HEAVY, Tell.SHAPE),
			new Timing("arm drag", AshColossusEntity.DRAG_WINDUP, Size.LIGHT, Tell.SHAPE),
			new Timing("falling cinder (P2)", AshColossusEntity.CINDER_WINDUP, Size.LIGHT, Tell.SHAPE)), List.of(
			new Phase("P1", 1, ASH_COLOSSUS.armor(), ASH_COLOSSUS.sideFactor(), 0, List.of(
					melee("fist cone", AshColossusEntity.FIST_WINDUP, AshColossusEntity.MELEE_COOLDOWN, (1 - OUT_OF_MELEE) * 2 / 3, Aim.TARGET),
					melee("arm drag", AshColossusEntity.DRAG_WINDUP, AshColossusEntity.MELEE_COOLDOWN, (1 - OUT_OF_MELEE) / 3, Aim.TARGET),
					closing("leap ring", AshColossusEntity.LEAP_WINDUP + AshColossusEntity.LEAP_FLIGHT_TICKS,
							AshColossusEntity.RANGED_COOLDOWN, OUT_OF_MELEE / 2, Aim.TARGET),
					ranged("boulder", AshColossusEntity.BOULDER_WINDUP, AshColossusEntity.RANGED_COOLDOWN, OUT_OF_MELEE / 2)), List.of(
					Downtime.every("ash storm: roar, then behind a pillar", AshColossusEntity.STORM_WINDUP + AshColossusEntity.STORM_TICKS,
							AshColossusEntity.STORM_INTERVAL))),
			new Phase("P2 (stone shed)", ASH_COLOSSUS.phase2At(), ASH_COLOSSUS.phase2Armor(), ASH_COLOSSUS.phase2Side(),
					AshColossusEntity.SHED_PAUSE, List.of(
					melee("fist cone", AshColossusEntity.FIST_WINDUP, AshColossusEntity.MELEE_COOLDOWN_P2, (1 - OUT_OF_MELEE) * 2 / 3, Aim.TARGET),
					melee("arm drag", AshColossusEntity.DRAG_WINDUP, AshColossusEntity.MELEE_COOLDOWN_P2, (1 - OUT_OF_MELEE) / 3, Aim.TARGET),
					closing("leap ring", AshColossusEntity.LEAP_WINDUP + AshColossusEntity.LEAP_FLIGHT_TICKS,
							AshColossusEntity.RANGED_COOLDOWN_P2, OUT_OF_MELEE / 2, Aim.TARGET),
					ranged("boulder", AshColossusEntity.BOULDER_WINDUP, AshColossusEntity.RANGED_COOLDOWN_P2, OUT_OF_MELEE / 2)), List.of(
					Downtime.every("ash storm: roar, then behind a pillar", AshColossusEntity.STORM_WINDUP + AshColossusEntity.STORM_TICKS,
							AshColossusEntity.STORM_INTERVAL),
					// A cinder falls near each fighter every interval; about a third of them on the player's spot.
					Downtime.every("falling cinders: a step out of the shadow", FightLength.REENGAGE_TICKS / 3.0,
							AshColossusEntity.CINDER_INTERVAL)))));

	/**
	 * Grieving Tree. Rooted (P1) it never walks; the branch it lowers after each sweep holds a lantern a melee hit can
	 * break, so each sweep buys a stun ({@value GrievingTreeEntity#STUN_TICKS} ticks at ×{@value
	 * GrievingTreeEntity#STUNNED_DAMAGE}). Otherwise the bark turns every hit (×{@value GrievingTreeEntity#TRUNK_DAMAGE},
	 * PLAN 18.4), so the stuns are its only openings.
	 */
	public static final Cadence GRIEVING_TREE_CADENCE = new Cadence(GRIEVING_TREE, true, List.of(
			new Timing("branch sweep", GrievingTreeEntity.SWEEP_WINDUP, Size.BIGGEST, Tell.SHAPE),
			new Timing("root line", GrievingTreeEntity.ROOT_WINDUP, Size.HEAVY, Tell.SHAPE),
			new Timing("root cage (P2)", GrievingTreeEntity.CAGE_WINDUP, Size.LIGHT, Tell.SHAPE)), List.of(
			new Phase("P1 (rooted)", 1, GRIEVING_TREE.armor(), GRIEVING_TREE.sideFactor(), 0, List.of(
					melee("branch sweep", GrievingTreeEntity.SWEEP_WINDUP, GrievingTreeEntity.SWEEP_COOLDOWN, 2 / 3.0, Aim.TARGET),
					melee("root lines", GrievingTreeEntity.ROOT_WINDUP, GrievingTreeEntity.ROOT_COOLDOWN, 1 / 3.0, Aim.ALL),
					opening("stun (lowered lantern broken)", GrievingTreeEntity.STUN_TICKS, 2 / 3.0, GRIEVING_TREE.bestFactor())), List.of(
					Downtime.adds("Withered Thralls dropping (2)", "withered_thrall", 2, GrievingTreeEntity.THRALL_INTERVAL))),
			new Phase("P2 (walking)", GRIEVING_TREE.phase2At(), GRIEVING_TREE.phase2Armor(), GRIEVING_TREE.phase2Side(),
					GrievingTreeEntity.UPROOT_PAUSE, List.of(
					melee("branch sweep", GrievingTreeEntity.SWEEP_WINDUP, GrievingTreeEntity.SWEEP_COOLDOWN, 2 / 3.0, Aim.TARGET),
					melee("root cage", GrievingTreeEntity.CAGE_WINDUP, GrievingTreeEntity.CAGE_COOLDOWN, 1 / 9.0, Aim.TARGET),
					melee("root lines", GrievingTreeEntity.ROOT_WINDUP, GrievingTreeEntity.ROOT_COOLDOWN, 2 / 9.0, Aim.ALL),
					opening("stun (lowered lantern broken)", GrievingTreeEntity.STUN_TICKS, 2 / 3.0, GRIEVING_TREE.phase2Best())), List.of(
					Downtime.adds("Withered Thralls dropping (2)", "withered_thrall", 2, GrievingTreeEntity.THRALL_INTERVAL)))));

	/**
	 * Crypt Keeper. In P2 and P3 the player fights it in brazier light (the lesson; in the dark it takes ×{@value
	 * CryptKeeperEntity#DARK_DAMAGE}); moths come for a lit brazier and are swatted.
	 */
	public static final Cadence CRYPT_KEEPER_CADENCE = new Cadence(CRYPT_KEEPER, true, List.of(
			// The two swings share a hit; the second follows the first after its shorter windup.
			new Timing("halberd swing", CryptKeeperEntity.SWING_2_WINDUP, Size.LIGHT, Tell.SHAPE),
			new Timing("overhead (3rd hit)", CryptKeeperEntity.OVERHEAD_WINDUP, Size.BIGGEST, Tell.SHAPE),
			new Timing("sweep ring", CryptKeeperEntity.SWEEP_WINDUP, Size.HEAVY, Tell.SHAPE),
			new Timing("thrust", CryptKeeperEntity.THRUST_WINDUP, Size.HEAVY, Tell.SHAPE),
			new Timing("lantern throw", CryptKeeperEntity.LANTERN_WINDUP, Size.LIGHT, Tell.SHAPE)), List.of(
			new Phase("P1", 1, CRYPT_KEEPER.armor(), 1, 0, keeperBeats(CryptKeeperEntity.CLOSE_COOLDOWN, ranged("lantern throw",
					CryptKeeperEntity.LANTERN_WINDUP, CryptKeeperEntity.RANGED_COOLDOWN, OUT_OF_MELEE)), List.of()),
			new Phase("P2 (dark, braziers)", CRYPT_KEEPER.phase2At(), CRYPT_KEEPER.phase2Armor(), 1, CryptKeeperEntity.PHASE_PAUSE,
					keeperBeats(CryptKeeperEntity.CLOSE_COOLDOWN_LATE, closing("thrust", CryptKeeperEntity.THRUST_WINDUP,
							CryptKeeperEntity.RANGED_COOLDOWN_LATE, OUT_OF_MELEE, Aim.TARGET)), List.of(
					Downtime.adds("Crypt Moths at a lit brazier (1.5)", "crypt_moth", 1.5, CryptKeeperEntity.MOTH_INTERVAL))),
			new Phase("P3 (flooded)", 0.3, CRYPT_KEEPER.phase2Armor(), 1, CryptKeeperEntity.PHASE_PAUSE,
					keeperBeats(CryptKeeperEntity.CLOSE_COOLDOWN_LATE, closing("thrust", CryptKeeperEntity.THRUST_WINDUP,
							CryptKeeperEntity.RANGED_COOLDOWN_LATE, OUT_OF_MELEE, Aim.TARGET)), List.of(
					Downtime.adds("Crypt Moths at a lit brazier (1.5)", "crypt_moth", 1.5, CryptKeeperEntity.MOTH_INTERVAL),
					Downtime.adds("Restless Bones rising", "restless_bones", CryptKeeperEntity.RISEN_BONES, 0)))));

	/** Close: the halberd combo two times in three, the sweep ring otherwise (always with two players close). */
	private static List<Beat> keeperBeats(int closeCooldown, Beat atRange) {
		return List.of(
				combo("halberd combo (2 swings + overhead)", CryptKeeperEntity.SWING_1_WINDUP, CryptKeeperEntity.SWING_1_WINDUP
						+ CryptKeeperEntity.SWING_2_WINDUP + CryptKeeperEntity.OVERHEAD_WINDUP, closeCooldown, (1 - OUT_OF_MELEE) * 2 / 3),
				melee("sweep ring", CryptKeeperEntity.SWEEP_WINDUP, closeCooldown, (1 - OUT_OF_MELEE) / 3, Aim.ALL),
				atRange);
	}

	/**
	 * Smoldering Lord. On the wall (P0) he cannot be touched: the player lights the three signal braziers, each a run
	 * of about 8 blocks (30 ticks), a light (10) and a wall attack dodged on the way (40), then he lands.
	 */
	public static final int LORD_WALL_TICKS = 3 * (30 + 10 + 40) + SmolderingLordEntity.DESCEND_WINDUP;

	public static final Cadence SMOLDERING_LORD_CADENCE = new Cadence(SMOLDERING_LORD, true, List.of(
			new Timing("greatsword swing", SmolderingLordEntity.SWING_2_WINDUP, Size.LIGHT, Tell.SHAPE),
			new Timing("overhead slam", SmolderingLordEntity.SLAM_WINDUP, Size.BIGGEST, Tell.SHAPE),
			new Timing("lunge", SmolderingLordEntity.LUNGE_WINDUP, Size.HEAVY, Tell.SHAPE),
			new Timing("landing ring", SmolderingLordEntity.DESCEND_WINDUP, Size.HEAVY, Tell.SHAPE),
			new Timing("mortar (P0)", SmolderingLordEntity.MORTAR_WINDUP, Size.HEAVY, Tell.SHAPE),
			new Timing("flame line (P0)", SmolderingLordEntity.FLAME_LINE_WINDUP, Size.HEAVY, Tell.SHAPE)), List.of(
			new Phase("P0 (on the wall)", 1, SMOLDERING_LORD.armor(), 0, LORD_WALL_TICKS, List.of(), List.of()),
			new Phase("P1", 1, SMOLDERING_LORD.armor(), 1, SmolderingLordEntity.PHASE_PAUSE,
					lordBeats(SmolderingLordEntity.COMBO_COOLDOWN, SmolderingLordEntity.LUNGE_COOLDOWN), List.of()),
			new Phase("P2 (armor shattered)", SMOLDERING_LORD.phase2At(), SMOLDERING_LORD.phase2Armor(), SMOLDERING_LORD.phase2Side(),
					SmolderingLordEntity.PHASE_PAUSE, lordBeats(SmolderingLordEntity.COMBO_COOLDOWN_P2, SmolderingLordEntity.LUNGE_COOLDOWN_P2),
					List.of(
							// One zone hazard per interval, in turn.
							Downtime.every("ash storm: roar, then behind cover", SmolderingLordEntity.STORM_WINDUP + SmolderingLordEntity.STORM_TICKS,
									3 * SmolderingLordEntity.HAZARD_INTERVAL),
							Downtime.every("fog: a quarter of it spent finding him", SmolderingLordEntity.FOG_TICKS / 4.0,
									3 * SmolderingLordEntity.HAZARD_INTERVAL)))));

	private static List<Beat> lordBeats(int comboCooldown, int lungeCooldown) {
		return List.of(
				combo("greatsword combo (2 swings + slam)", SmolderingLordEntity.SWING_1_WINDUP, SmolderingLordEntity.SWING_1_WINDUP
						+ SmolderingLordEntity.SWING_2_WINDUP + SmolderingLordEntity.SLAM_WINDUP, comboCooldown, 1 - OUT_OF_MELEE),
				closing("lunge", SmolderingLordEntity.LUNGE_WINDUP, lungeCooldown, OUT_OF_MELEE, Aim.TARGET));
	}

	/**
	 * Vampire boss: a melee chaser that trades claws (no windup), with the nova on its own cooldown from P1 on and its
	 * leap and mist step only when the player is away. One shared boss, not scaled by the number of players.
	 */
	public static final Cadence VAMPIRE_CADENCE = new Cadence(VAMPIRE, false, List.of(
			new Timing("claw", 0, Size.LIGHT, Tell.NONE),
			new Timing("leap", VampireBossEntity.LEAP_WINDUP, Size.HEAVY, Tell.SHAPE),
			new Timing("blood nova (heals 25 %)", VampireBossEntity.NOVA_WINDUP, Size.BIGGEST, Tell.SHAPE)), List.of(
			new Phase("P0", 1, VAMPIRE.armor(), 1, 0, List.of(
					melee("claws, traded", 0, VANILLA_MELEE_COOLDOWN, 1 - OUT_OF_MELEE, Aim.NONE),
					closing("leap", VampireBossEntity.LEAP_WINDUP + 14, 0, OUT_OF_MELEE, Aim.TARGET)), List.of()),
			new Phase("P1 (nova)", 0.6, VAMPIRE.armor(), 1, 0, vampireBeats(VampireBossEntity.NOVA_COOLDOWN), List.of()),
			new Phase("P2 (mist step)", 0.3, VAMPIRE.armor(), 1, 0, vampireBeats(VampireBossEntity.NOVA_COOLDOWN_P2), List.of())));

	/** One nova per its cooldown; claws traded the rest of it; a leap now and then when the player backs off. */
	private static List<Beat> vampireBeats(int novaCooldown) {
		double claws = (novaCooldown - VampireBossEntity.NOVA_WINDUP) / (double) VANILLA_MELEE_COOLDOWN;
		return List.of(
				melee("claws, traded", 0, VANILLA_MELEE_COOLDOWN, claws * (1 - OUT_OF_MELEE), Aim.NONE),
				melee("blood nova", VampireBossEntity.NOVA_WINDUP, 0, 1, Aim.ALL),
				closing("leap / mist step", VampireBossEntity.LEAP_WINDUP + 14, 0, claws * OUT_OF_MELEE, Aim.TARGET));
	}

	/**
	 * The Fallen Sister's cadence (her brain's picks in reach): in P1 the two censer arcs five times in eight and the
	 * slam otherwise, whose ember puddle costs a step around it; a backstep when the player gets behind her (the
	 * out-of-melee share). In P2, after the veil burns (she cannot be hurt meanwhile), the spin a third of her picks
	 * (always with two players close), the slam a third of the rest; her crying souls come 3, then 4 every 20 s, each a
	 * dodge, the way to it and one swing.
	 */
	public static final Cadence FALLEN_SISTER_CADENCE = new Cadence(FALLEN_SISTER, true, List.of(
			// The two arcs share a hit; the second follows the first after its shorter windup.
			new Timing("censer arc", FallenSisterEntity.ARC_2_WINDUP, Size.LIGHT, Tell.SHAPE),
			new Timing("censer slam", FallenSisterEntity.SLAM_WINDUP, Size.HEAVY, Tell.SHAPE),
			new Timing("crying soul dive (P2)", CryingSoulEntity.DIVE_WINDUP, Size.LIGHT, Tell.SHAPE),
			new Timing("censer spin (P2)", FallenSisterEntity.SPIN_WINDUP, Size.BIGGEST, Tell.SHAPE)), List.of(
			new Phase("P1 (censer)", 1, FALLEN_SISTER.armor(), 1, 0, sisterBeats(false), List.of(
					Downtime.every("ember puddle: a step around it", FightLength.REENGAGE_TICKS / 2.0,
							every(sisterBeats(false), "censer slam")))),
			new Phase("P2 (veil burnt, crying souls)", FALLEN_SISTER.phase2At(), FALLEN_SISTER.phase2Armor(), 1,
					FallenSisterEntity.VEIL_BURN_TICKS, sisterBeats(true), List.of(
					Downtime.every("ember puddle: a step around it", FightLength.REENGAGE_TICKS / 2.0,
							every(sisterBeats(true), "censer slam")),
					Downtime.every("crying souls: 3.5 a flock, each dodged, reached and killed with one swing",
							3.5 * (FightLength.ESCAPE_TICKS + FightLength.REENGAGE_TICKS + 16), FallenSisterEntity.FLOCK_INTERVAL)))));

	private static List<Beat> sisterBeats(boolean late) {
		double close = 1 - OUT_OF_MELEE;
		double spin = late ? close / 3 : 0;
		double slam = late ? (close - spin) / 3 : close * 3 / 8;
		List<Beat> beats = new ArrayList<>(List.of(
				combo("two censer arcs", FallenSisterEntity.ARC_1_WINDUP, FallenSisterEntity.ARC_1_WINDUP + FallenSisterEntity.ARC_2_WINDUP,
						FallenSisterEntity.ARC_COOLDOWN, close - spin - slam),
				melee("censer slam", FallenSisterEntity.SLAM_WINDUP, FallenSisterEntity.SLAM_COOLDOWN, slam, Aim.TARGET),
				ranged("backstep", FallenSisterEntity.BACKSTEP_WINDUP, FallenSisterEntity.BACKSTEP_COOLDOWN, OUT_OF_MELEE)));
		if (late) {
			beats.add(melee("censer spin", FallenSisterEntity.SPIN_WINDUP, FallenSisterEntity.SPIN_COOLDOWN, spin, Aim.ALL));
		}
		return beats;
	}

	/** Ticks of a phase between two of its beat {@code name} (the loop's length over that beat's weight). */
	private static double every(List<Beat> beats, String name) {
		double loop = beats.stream().mapToDouble(beat -> beat.weight() * beat.ticks()).sum();
		return loop / beats.stream().filter(beat -> beat.name().equals(name)).mapToDouble(Beat::weight).sum();
	}

	public static final List<Cadence> CADENCES = List.of(FALLEN_SISTER_CADENCE, VAMPIRE_CADENCE, ASH_COLOSSUS_CADENCE,
			GRIEVING_TREE_CADENCE, CRYPT_KEEPER_CADENCE, SMOLDERING_LORD_CADENCE);

	public static Cadence cadenceOf(Boss boss) {
		return CADENCES.stream().filter(c -> c.boss() == boss).findFirst().orElseThrow(() -> new IllegalArgumentException(boss.id()));
	}

	// --- Where the XP comes from (R40.1a "a normal playthrough") ------------------------------------------------

	/**
	 * Placed enemies per minor structure (counted from the datagen templates) and its weight in the zone's structure
	 * set. Outposts are two per road; Necropolis crypts are one jigsaw of several rooms.
	 */
	public record Structure(String name, Zone zone, int weight, Map<String, Integer> mobs, String chest) {}

	public static final List<Structure> STRUCTURES = List.of(
			new Structure("farmstead", Zone.FIELDS, 4, Map.of("cinder_husk", 2, "ash_stalker", 1), "common"),
			new Structure("barn", Zone.FIELDS, 3, Map.of("cinder_husk", 3), "common"),
			new Structure("windmill_skeleton", Zone.FIELDS, 2, Map.of("cinder_husk", 1, "ashen_carrion", 1), "common"),
			new Structure("scarecrow_row", Zone.FIELDS, 3, Map.of("ash_stalker", 2, "ashen_carrion", 1), ""),
			new Structure("granary_vault", Zone.FIELDS, 1, Map.of("cinder_husk", 2, "ashen_carrion", 1), "treasure"),
			new Structure("pilgrim_camp", Zone.FIELDS, 3, Map.of("cinder_husk", 3), "common"),
			new Structure("gallows", Zone.FIELDS, 2, Map.of("cinder_husk", 2), ""),
			new Structure("village_altar", Zone.FIELDS, 2, Map.of("cinder_husk", 1), ""),
			new Structure("park_wall", Zone.WOODS, 3, Map.of("withered_thrall", 1, "bark_hollow", 1), ""),
			new Structure("hunting_lodge", Zone.WOODS, 1, Map.of("withered_thrall", 2, "bark_hollow", 1, "lantern_hag", 1), "treasure"),
			new Structure("kennels", Zone.WOODS, 3, Map.of("withered_thrall", 2), "common"),
			new Structure("hunters_hide", Zone.WOODS, 4, Map.of("lantern_hag", 1), "common"),
			new Structure("hanging_cages", Zone.WOODS, 3, Map.of("withered_thrall", 3), ""),
			new Structure("lantern_row", Zone.WOODS, 3, Map.of("lantern_hag", 1), ""),
			new Structure("glade", Zone.WOODS, 2, Map.of("bark_hollow", 2), ""),
			new Structure("mausoleum_small", Zone.NECROPOLIS, 4, Map.of("hollow_sentinel", 1), "common"),
			new Structure("mausoleum_large", Zone.NECROPOLIS, 2, Map.of("hollow_sentinel", 2, "restless_bones", 2), "common"),
			new Structure("grave_rows", Zone.NECROPOLIS, 4, Map.of("restless_bones", 3), ""),
			new Structure("chapel", Zone.NECROPOLIS, 1, Map.of("restless_bones", 2, "grave_warden", 1), "common"),
			new Structure("ossuary_pit", Zone.NECROPOLIS, 3, Map.of("restless_bones", 2, "crypt_moth", 2), ""),
			new Structure("collapsed_tomb", Zone.NECROPOLIS, 3, Map.of("hollow_sentinel", 1), "common"));

	/** Both road outposts of a zone (toll gate + watchtower) together. */
	public static final Map<Zone, Map<String, Integer>> OUTPOSTS = Map.of(
			Zone.FIELDS, Map.of("cinder_husk", 4, "ashen_carrion", 2),
			Zone.WOODS, Map.of("withered_thrall", 4, "lantern_hag", 2),
			Zone.NECROPOLIS, Map.of("restless_bones", 4, "crypt_moth", 2));

	/**
	 * One crypt along the route: each of its rooms once (entrance, descent, junction, corridor, flooded hall, burial
	 * hall, warden chamber, lower crypt), which a jigsaw of size 7 about strings together.
	 */
	public static final Map<String, Integer> CRYPT = Map.of("restless_bones", 7, "hollow_sentinel", 3, "grave_warden", 2,
			"crypt_moth", 2);

	/** The Citadel: every piece once (fixed plan, {@code reach.citadel.CitadelLayout}). */
	public static final Map<String, Integer> CITADEL = Map.of("sparking_marksman", 7, "furnace_golem", 5, "smoldering_knight", 4);

	/** Natural spawns weights per zone ({@code reachMobs.spawns}). */
	public static final Map<Zone, Map<String, Integer>> NATURAL = Map.of(
			Zone.FIELDS, Map.of("cinder_husk", 100, "ash_stalker", 35, "ashen_carrion", 8),
			Zone.WOODS, Map.of("withered_thrall", 100, "lantern_hag", 40),
			Zone.NECROPOLIS, Map.of("restless_bones", 100, "crypt_moth", 50),
			Zone.CITADEL, Map.of("cinder_hound", 100));

	public static Mob mob(String id) {
		for (Mob mob : MOBS) {
			if (mob.id().equals(id)) {
				return mob;
			}
		}
		if (id.equals(ASHEN_ZOMBIE.id())) {
			return ASHEN_ZOMBIE;
		}
		if (id.equals(GHOST.id())) {
			return GHOST;
		}
		throw new IllegalArgumentException("no mob " + id);
	}

	private BalanceData() {}
}
