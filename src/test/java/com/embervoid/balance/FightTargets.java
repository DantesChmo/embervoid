package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Cadence;
import com.embervoid.balance.BalanceModel.Fight;
import com.embervoid.balance.BalanceModel.Player;
import com.embervoid.balance.BalanceModel.Playthrough;
import com.embervoid.balance.BalanceModel.Weapon;
import com.embervoid.balance.FightLength.Band;
import com.embervoid.balance.FightLength.BossStage;
import com.embervoid.balance.FightLength.HitShare;
import com.embervoid.balance.FightLength.Length;
import com.embervoid.balance.FightLength.MobStage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The fight-length targets (DESIGN §5.5, §6.1) as lists of misses, on the game's numbers as they are: shared by {@link
 * FightLengthTargetsTest}, which fails on any, and {@link FightLengthReport}, which lists them in {@code
 * docs/balance-0.5.0.md}. Each miss says by how much.
 */
final class FightTargets {
	private FightTargets() {}

	static String f(double value) {
		return BalanceReport.f(value);
	}

	static String pct(double share) {
		return Math.round(share * 100) + " %";
	}

	static String who(Player player) {
		return "Lv " + player.level() + " " + player.armor().name() + ", " + player.weapon().name();
	}

	/** Which rows a check looks at: the whole game, or Realm 1 alone (its zones, the Citadel). */
	enum Scope {
		ALL, REALM_ONE;

		boolean has(BalanceData.Boss boss) {
			return this == ALL || boss.zone() != null;
		}

		boolean has(BalanceData.Mob mob) {
			return this == ALL || mob.zone() != null;
		}
	}

	/** Solo boss fights last 1.5–2.5 min with dodging. */
	static List<String> bossFightLength(Playthrough run, Scope scope) {
		List<String> misses = new ArrayList<>();
		for (BossStage stage : FightLength.bossStages(run)) {
			if (!scope.has(stage.cadence().boss())) {
				continue;
			}
			Length length = FightLength.length(stage.cadence(), stage.player(), 1);
			double s = length.seconds();
			if (s < FightLength.MIN_SECONDS || s > FightLength.MAX_SECONDS) {
				misses.add(stage.cadence().boss().name() + " (" + stage.name() + ", " + who(stage.player()) + "): " + f(s / 60)
						+ " min, want 1.5–2.5");
			}
		}
		return misses;
	}

	/** Solo boss fights take 20–35 clean hits with the stage's reference weapon. */
	static List<String> bossCleanHits(Playthrough run, Scope scope) {
		List<String> misses = new ArrayList<>();
		for (BossStage stage : FightLength.bossStages(run)) {
			if (!scope.has(stage.cadence().boss())) {
				continue;
			}
			int hits = (int) Math.ceil(FightLength.length(stage.cadence(), stage.player(), 1).hitsEach() - 1e-6);
			if (hits < FightLength.MIN_HITS || hits > FightLength.MAX_HITS) {
				misses.add(stage.cadence().boss().name() + " (" + stage.name() + ", " + who(stage.player()) + "): " + hits
						+ " clean hits, want 20–35");
			}
		}
		return misses;
	}

	/** Mobs die in their band's hits: fodder 2–3, ambushers, ranged and flyers 3–4, tanks and elites 6–10. */
	static List<String> mobHitsToKill(Playthrough run, Scope scope) {
		List<String> misses = new ArrayList<>();
		for (MobStage stage : FightLength.mobStages(run)) {
			if (!scope.has(stage.mob())) {
				continue;
			}
			Band band = FightLength.bandOf(stage.mob());
			int hits = stage.fight().hitsToKillMob();
			if (hits < band.minHits || hits > band.maxHits) {
				misses.add(stage.mob().name() + " Lv " + stage.level() + " (" + stage.stage() + ", " + who(stage.player()) + "): dies in "
						+ hits + ", want " + band.minHits + "–" + band.maxHits);
			}
		}
		return misses;
	}

	/** The creeper's blast is kept as it is (the harder overworld; a level 10 player survives it). */
	static final String CREEPER_KEPT = "minecraft:creeper";

	/**
	 * Mobs need at least their band's hits to kill the player from full health: fodder 6, skirmishers 5, elites 4. The
	 * creeper is left out ({@link #CREEPER_KEPT}).
	 */
	static List<String> mobHitsOnPlayer(Playthrough run, Scope scope) {
		List<String> misses = new ArrayList<>();
		for (MobStage stage : FightLength.mobStages(run)) {
			if (!scope.has(stage.mob())) {
				continue;
			}
			if (stage.mob().id().equals(CREEPER_KEPT)) {
				continue;
			}
			Band band = FightLength.bandOf(stage.mob());
			Fight fight = stage.fight();
			if (fight.hitsToKillPlayer() < band.minHitsOnPlayer) {
				misses.add(stage.mob().name() + " Lv " + stage.level() + " (" + stage.stage() + ", " + who(stage.player()) + "): kills in "
						+ fight.hitsToKillPlayer() + " (" + f(fight.mobHit()) + " of " + f(stage.player().maxHealth()) + "), want ≥ "
						+ band.minHitsOnPlayer);
			}
		}
		return misses;
	}

	private static List<HitShare> weakestShares(Playthrough run, Scope scope) {
		List<HitShare> shares = new ArrayList<>();
		for (Cadence cadence : BalanceData.CADENCES) {
			if (!scope.has(cadence.boss())) {
				continue;
			}
			shares.addAll(FightLength.hitShares(cadence, FightLength.weakestPlayer(cadence, run)));
		}
		return shares;
	}

	/**
	 * Boss moves against the first (weakest) expected player who fights them: light ≤ 20 % of max health, heavy ≤ 35 %,
	 * the biggest ≤ two thirds.
	 */
	static List<String> bossHitSizes(Playthrough run, Scope scope) {
		List<String> misses = new ArrayList<>();
		for (HitShare share : weakestShares(run, scope)) {
			double limit = FightLength.limit(share.timing().size());
			if (share.share() > limit) {
				misses.add(share.cadence().boss().name() + " " + share.hit().name() + ": " + pct(share.share()) + " of " + who(share.player())
						+ " (" + f(share.damage()) + " HP), " + share.timing().size().name().toLowerCase(Locale.ROOT) + " wants ≤ " + pct(limit));
			}
		}
		return misses;
	}

	/** A hit of ≥ ⅓ needs a ≥ 12-tick windup with a tell, ≥ ½ a ≥ 20-tick one with a shape or sound. */
	static List<String> windupRule(Playthrough run, Scope scope) {
		List<String> misses = new ArrayList<>();
		for (HitShare share : weakestShares(run, scope)) {
			for (String broken : share.breaks()) {
				if (broken.startsWith("≥")) {
					misses.add(share.cadence().boss().name() + " " + share.hit().name() + ": " + pct(share.share()) + " " + broken);
				}
			}
		}
		return misses;
	}

	/**
	 * DESIGN §6.1: each melee unique beats an unenchanted netherite sword (12.8 per second) by at least 20 % in its own
	 * use (about 30 % meant), and none falls below it on a single target (the Scythe without its sweep, the Scourge
	 * at its tip, the Rapier ramped).
	 */
	static List<String> uniquesAboveNetherite(List<Weapon> uniques) {
		List<String> misses = new ArrayList<>();
		double netherite = FightLength.NETHERITE_PER_SECOND;
		for (Weapon weapon : uniques) {
			double single = FightLength.perSecond(weapon) * FightLength.oneTarget(weapon);
			double own = FightLength.perSecond(weapon) * FightLength.ownUse(weapon);
			if (own < 1.2 * netherite) {
				misses.add(weapon.name() + ": " + f(own) + " per second in its own use, want ≥ " + f(1.2 * netherite) + " (netherite " + f(netherite) + ")");
			}
			if (single < netherite) {
				misses.add(weapon.name() + ": " + f(single) + " per second on one target, below netherite's " + f(netherite));
			}
		}
		return misses;
	}

	/** Every target's misses, by target. */
	static Map<String, List<String>> all(Playthrough run) {
		Map<String, List<String>> all = new LinkedHashMap<>();
		all.put("Boss fight length, solo (1.5–2.5 min)", bossFightLength(run, Scope.ALL));
		all.put("Boss clean hits, solo (20–35)", bossCleanHits(run, Scope.ALL));
		all.put("Mob hits to kill (fodder 2–3, skirmishers 3–4, elites 6–10)", mobHitsToKill(run, Scope.ALL));
		all.put("Mob hits to kill the player (fodder ≥ 6, skirmishers ≥ 5, elites ≥ 4)", mobHitsOnPlayer(run, Scope.ALL));
		all.put("Boss hit sizes (light ≤ 20 %, heavy ≤ 35 %, biggest ≤ ⅔)", bossHitSizes(run, Scope.ALL));
		all.put("Wind-up rule (≥ ⅓: 12 ticks + tell; ≥ ½: 20 ticks + shape or sound)", windupRule(run, Scope.ALL));
		all.put("Uniques above netherite (≥ +20 % in own use, ≥ netherite on one target)", uniquesAboveNetherite(BalanceModel.REALM_MELEE));
		return all;
	}
}
