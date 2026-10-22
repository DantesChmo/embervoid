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
		FLAT
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
	 * @param extraHealth  health to beat after the first death (Restless Bones' pile), not scaled by level
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
			new Mob("cinder_husk", "Cinder Husk", Zone.FIELDS, Role.FODDER, 16, 0, 0, 3, 6, false,
					List.of(melee("swing", 3, 30))),
			new Mob("ash_stalker", "Ash Stalker", Zone.FIELDS, Role.AMBUSHER, 14, 0, 0, 4, 8, false,
					List.of(melee("swing / leap", 4, 25))),
			new Mob("ashen_carrion", "Ashen Carrion", Zone.FIELDS, Role.FLYER, 18, 0, 0, 4, 15, false,
					List.of(melee("dive", 4, 200))),
			new Mob("withered_thrall", "Withered Thrall", Zone.WOODS, Role.FODDER, 18, 0, 0, 3.5, 8, false,
					List.of(melee("swing", 3.5, 30))),
			new Mob("lantern_hag", "Lantern Hag", Zone.WOODS, Role.RANGED, 16, 0, 0, 2, 12, false,
					List.of(new Hit("fire flask", FireFlaskEntity.DAMAGE, HitKind.PHYSICAL, 50))),
			new Mob("bark_hollow", "Bark Hollow", Zone.WOODS, Role.TANK, 50, 0, 8, 7, 22, true,
					List.of(melee("sweep", 7, 50))),
			new Mob("restless_bones", "Restless Bones", Zone.NECROPOLIS, Role.FODDER, RestlessBonesEntity.BASE_HEALTH,
					RestlessBonesEntity.PILE_HEALTH, 0, 4, 8, false, List.of(melee("swing", 4, 30))),
			new Mob("crypt_moth", "Crypt Moth", Zone.NECROPOLIS, Role.FLYER, 10, 0, 0, 2, 8, false,
					List.of(melee("bite", 2, 25))),
			new Mob("hollow_sentinel", "Hollow Sentinel", Zone.NECROPOLIS, Role.TANK, 45, 0, 10, HollowSentinelEntity.THRUST_DAMAGE,
					22, true, List.of(melee("spear thrust", HollowSentinelEntity.THRUST_DAMAGE, 40),
							melee("shield bash", HollowSentinelEntity.BASH_DAMAGE, 50))),
			new Mob("grave_warden", "Grave Warden", Zone.NECROPOLIS, Role.ELITE, 80, 0, 8, 8, 40, true,
					List.of(melee("swing", 8, 40))),
			new Mob("cinder_hound", "Cinder Hound", Zone.CITADEL, Role.FODDER, 14, 0, 0, 3, 7, false,
					List.of(melee("bite (+2 s fire)", 3, 20))),
			new Mob("sparking_marksman", "Sparking Marksman", Zone.CITADEL, Role.RANGED, 24, 0, 0, 2, 15, true,
					List.of(new Hit("bolt (+fire)", SparkingMarksmanEntity.BOLT_DAMAGE, HitKind.PHYSICAL, 60))),
			new Mob("furnace_golem", "Furnace Golem", Zone.CITADEL, Role.TANK, 90, 0, 12, 9, 30, true,
					List.of(melee("slam", 9, 50), new Hit("fire wave", 9 * FurnaceGolemEntity.WAVE_DAMAGE_FACTOR, HitKind.FIRE, 0))),
			new Mob("smoldering_knight", "Smoldering Knight", Zone.CITADEL, Role.ELITE, 100, 0, 10, 8, 50, true,
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

	public static final Boss ASH_COLOSSUS = new Boss("ash_colossus", "Ash Colossus", Zone.FIELDS, 18, 180, 10, 8, 150,
			1, AshColossusEntity.BACK_DAMAGE, "back (P2: the bare core)", 0.5, 3, 1, AshColossusEntity.CORE_DAMAGE, "cinder_husk", List.of(
					melee("fist cone", 8, 60),
					melee("leap ring", 9, 0),
					melee("boulder", 7, 0),
					melee("arm drag", 6, 0),
					new Hit("falling cinder (P2)", 5, HitKind.FIRE, 0),
					hazard("ash storm, per s (magic)", AshColossusEntity.STORM_DAMAGE, HitKind.FLAT)));
	public static final Boss GRIEVING_TREE = new Boss("grieving_tree", "Grieving Tree", Zone.WOODS, 21, 170, 6, 8, 170,
			GrievingTreeEntity.TRUNK_DAMAGE, GrievingTreeEntity.STUNNED_DAMAGE, "stunned (lanterns shot)", 0.5, 6,
			GrievingTreeEntity.TRUNK_DAMAGE, GrievingTreeEntity.STUNNED_DAMAGE, "withered_thrall", List.of(
					melee("branch sweep", 8, 60),
					melee("root line", 7, 0),
					melee("root cage (P2)", 4, 0)));
	public static final Boss CRYPT_KEEPER = new Boss("crypt_keeper", "Crypt Keeper", Zone.NECROPOLIS, 24, 180, 8, 5, 190,
			1, 1, "in brazier light (dark: ×" + CryptKeeperEntity.DARK_DAMAGE + ")", 0.6, 8, 1, 1, "restless_bones", List.of(
					melee("halberd swing", 5, 40),
					new Hit("overhead (3rd hit)", 10, HitKind.UNBLOCKABLE, 0),
					melee("sweep ring", 7, 0),
					melee("thrust", 7, 0),
					melee("lantern throw", 5, 0),
					hazard("fire pool, per 0.5 s", 2, HitKind.FIRE)));
	public static final Boss SMOLDERING_LORD = new Boss("smoldering_lord", "Smoldering Lord", Zone.CITADEL, 30, 260, 12, 8, 300,
			1, 1, "-", 0.5, 4, SmolderingLordEntity.SHATTERED_DAMAGE, SmolderingLordEntity.SHATTERED_DAMAGE, "cinder_hound", List.of(
					melee("greatsword swing", 8, 50),
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
			List.of(melee("touch", 8, 20)));
	public static final Boss VAMPIRE = new Boss("vampire_boss", "Vampire boss", null, 15, 220, 4, 5, 200,
			1, 1, "-", 0, 4, 1, 1, "-", List.of(
					melee("claw", 5, 30),
					melee("leap", 7, 0),
					melee("blood nova (heals 25 %)", 7.5, 0)));

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
