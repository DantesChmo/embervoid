package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Hit;
import com.embervoid.balance.BalanceData.HitKind;
import com.embervoid.balance.BalanceData.Mob;
import com.embervoid.balance.BalanceData.Role;

import java.util.Arrays;
import java.util.List;

/**
 * The overworld's hostiles as data for the overworld stage of the balance model (PLAN L2): vanilla numbers (1.20.1)
 * and the Embervoid overworld mobs (DESIGN §3.2). Base levels are not here: they come from the {@code mobs.levels}
 * config defaults. The {@code BalanceFightTests} GameTest checks the Embervoid mobs and the vanilla zombie, skeleton
 * and creeper against the live entities.
 */
public final class OverworldData {
	/** Vanilla's arrow: base damage 2 × the skeleton's launch speed 1.6, rounded up (Normal, no crit, mean roll). */
	public static final double SKELETON_ARROW = 4;
	/** Vanilla's creeper: explosion power 3. */
	public static final float CREEPER_POWER = 3;

	/**
	 * One overworld hostile.
	 *
	 * @param mob          its numbers at level 1, as a realm mob ({@link BalanceData.Mob}, no zone)
	 * @param surface      spawns on the surface at night
	 * @param underground  spawns in caves (and so also below y=0, +3 levels)
	 * @param spawnWeight  natural spawn weight in an ordinary overworld biome (0: not natural, e.g. raids)
	 * @param groupSize    mean group size of one spawn
	 * @param burnPerHit   fire damage per landed hit that the burning it leaves deals until the next one (per s: 1)
	 */
	public record OverworldMob(Mob mob, boolean surface, boolean underground, int spawnWeight, double groupSize,
			double burnPerHit) {
		public String id() {
			return mob.id();
		}

		public String name() {
			return mob.name();
		}

		/** Base level from the {@code mobs.levels} config defaults (DESIGN §5.4). */
		public int baseLevel() {
			return BalanceModel.LEVELS.baseLevels().getOrDefault(mob.id(), BalanceModel.LEVELS.defaultBaseLevel());
		}
	}

	private static Hit melee(String name, double base, int interval) {
		return new Hit(name, base, HitKind.PHYSICAL, interval);
	}

	/** Vanilla's numbers, times the type's {@code healthScale} / {@code damageScale} from the config defaults (PLAN 18.3). */
	private static Mob mob(String id, String name, Role role, double health, double armor, double attack, int xp, Hit... hits) {
		double hp = BalanceModel.LEVELS.healthScale(id);
		double dmg = BalanceModel.LEVELS.damageScale(id);
		List<Hit> scaled = Arrays.stream(hits).map(h -> new Hit(h.name(), h.base() * dmg, h.kind(), h.interval(), h.hazard())).toList();
		return new Mob(id, name, null, role, health * hp, 0, armor, attack * dmg, xp, false, scaled);
	}

	/** Vanilla's explosion damage to an entity at {@code distance} from the centre with {@code exposure} (0–1). */
	public static double explosionDamage(float power, double distance, double exposure) {
		double q = power * 2;
		double w = distance / q;
		if (w > 1) {
			return 0;
		}
		double impact = (1 - w) * exposure;
		return (int) ((impact * impact + impact) / 2 * 7 * q + 1);
	}

	public static final OverworldMob ZOMBIE = new OverworldMob(mob("minecraft:zombie", "Zombie", Role.FODDER, 20, 2, 3, 5,
			melee("swing", 3, 20)), true, true, 95, 4, 0);
	public static final OverworldMob SPIDER = new OverworldMob(mob("minecraft:spider", "Spider", Role.FODDER, 16, 0, 2, 5,
			melee("bite", 2, 20)), true, true, 100, 4, 0);
	/** Draws for 20 ticks, then waits 40 (20 on Hard). */
	public static final OverworldMob SKELETON = new OverworldMob(mob("minecraft:skeleton", "Skeleton", Role.RANGED, 20, 0, 2, 5,
			new Hit("arrow", SKELETON_ARROW, HitKind.PHYSICAL, 60)), true, true, 100, 4, 0);
	/** Explodes once: the main hit at 2 blocks (a player backing off during the fuse), the second at 4. */
	public static final OverworldMob CREEPER = new OverworldMob(mob("minecraft:creeper", "Creeper", Role.AMBUSHER, 20, 0, 2, 5,
			new Hit("explosion at 2 blocks", explosionDamage(CREEPER_POWER, 2, 1), HitKind.PHYSICAL, 0),
			new Hit("explosion at 4 blocks", explosionDamage(CREEPER_POWER, 4, 1), HitKind.PHYSICAL, 0)), true, true, 100, 4, 0);
	/** Its worst throw, Harming (6 magic, ignores armor), once per 3 s; it opens with Slowness or Poison. */
	public static final OverworldMob WITCH = new OverworldMob(mob("minecraft:witch", "Witch", Role.RANGED, 26, 0, 2, 5,
			new Hit("harming potion", 6, HitKind.MAGIC, 60)), true, true, 5, 1, 0);
	public static final OverworldMob ENDERMAN = new OverworldMob(mob("minecraft:enderman", "Enderman", Role.ELITE, 40, 0, 7, 5,
			melee("hit", 7, 20)), true, true, 10, 2.5, 0);
	/** Raids and woodland mansions, no natural spawn: the strongest common melee hit (5 + an iron axe's 8). */
	public static final OverworldMob VINDICATOR = new OverworldMob(mob("minecraft:vindicator", "Vindicator", Role.ELITE, 24, 0, 5, 5,
			melee("iron axe", 13, 20)), false, false, 0, 1, 0);
	/** Packs of 3–5 (the entity spawns the pack). */
	public static final OverworldMob ASH_GHOUL = new OverworldMob(mob("embervoid:ash_ghoul", "Ash Ghoul", Role.FODDER, 12, 0, 2.5, 5,
			melee("claw", 2.5, 20)), true, true, 12, 4, 0);
	/** Iron sword 70 % (5), stone sword 30 % (4); its own attack is 0. One swing per 40 ticks after an 8-tick wind-up. */
	public static final OverworldMob HOLLOW = new OverworldMob(mob("embervoid:hollow", "Hollow", Role.TANK, 25, 6, 0, 20,
			melee("iron sword", 5, 40), melee("stone sword", 4, 40)), true, false, 5, 1, 0);
	/** Touch 1 + 4 s of fire, one dive per 20–40 ticks; the fire burns on between dives (1 per s, ignores armor). */
	public static final OverworldMob EMBER_WISP = new OverworldMob(mob("embervoid:ember_wisp", "Ember Wisp", Role.FLYER, 8, 0, 1, 6,
			melee("touch (+4 s fire)", 1, 30)), false, true, 15, 1.5, 1.5);

	public static final List<OverworldMob> MOBS = List.of(ZOMBIE, SPIDER, SKELETON, CREEPER, WITCH, ENDERMAN, VINDICATOR,
			ASH_GHOUL, HOLLOW, EMBER_WISP);

	public static OverworldMob mob(String id) {
		for (OverworldMob mob : MOBS) {
			if (mob.id().equals(id)) {
				return mob;
			}
		}
		throw new IllegalArgumentException("no overworld mob " + id);
	}

	private OverworldData() {}
}
