package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Boss;
import com.embervoid.balance.BalanceData.Hit;
import com.embervoid.balance.BalanceData.HitKind;
import com.embervoid.balance.BalanceData.Mob;
import com.embervoid.balance.BalanceData.Zone;
import com.embervoid.balance.BalanceModel.Fight;
import com.embervoid.balance.BalanceModel.Player;
import com.embervoid.balance.BalanceModel.Playthrough;
import com.embervoid.balance.BalanceModel.Stage;
import com.embervoid.nightmob.NightMobConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The balance targets of R40.1 (docs/balance-0.4.0.md "Targets") on the model: a number change that breaks one fails
 * the build. Every check collects all its misses before failing, so one run shows everything that is off.
 */
class BalanceTargetsTest {
	private static final Playthrough RUN = Playthrough.NORMAL;
	private static final List<Stage> STAGES = BalanceModel.stages(RUN);

	private static Stage stage(Zone zone, int order) {
		return STAGES.stream().filter(s -> s.zone() == zone && s.order() == order).findFirst().orElseThrow();
	}

	private static Stage depths() {
		return stage(Zone.CITADEL, 4);
	}

	private static void assertNone(List<String> misses) {
		assertTrue(misses.isEmpty(), String.join("\n", misses));
	}

	/** All XP into levels: depths entered at 28–32, Realm 1 left at 32–37; the first zone is worth 4+ levels. */
	@Test
	void xpPaceMatchesTheLevelBands() {
		List<String> misses = new ArrayList<>();
		int depths = depths().level();
		int done = BalanceModel.levelAfter(depths, BalanceModel.zoneXp(Zone.CITADEL, RUN).total() + leftover(depths));
		if (depths < 28 || depths > 32) {
			misses.add("depths entered at level " + depths + ", not 28–32");
		}
		if (done < 32 || done > 37) {
			misses.add("Realm 1 left at level " + done + ", not 32–37");
		}
		for (Zone zone : Zone.OPEN) {
			int after = BalanceModel.levelAfter(RUN.sanctumLevel(), BalanceModel.zoneXp(zone, RUN).total());
			if (after - RUN.sanctumLevel() < 4) {
				misses.add(zone.title + " first is worth only " + (after - RUN.sanctumLevel()) + " levels");
			}
		}
		assertNone(misses);
	}

	/** XP left over after buying the depths level (so the end level is not off by a partial level). */
	private static double leftover(int depthsLevel) {
		double xp = Zone.OPEN.stream().mapToDouble(z -> BalanceModel.zoneXp(z, RUN).total()).sum();
		return xp - (BalanceModel.LEVELING.totalCost(depthsLevel) - BalanceModel.LEVELING.totalCost(RUN.sanctumLevel()));
	}

	/**
	 * Any zone first, at its band's bottom, against the Sanctum-level player in diamond: a natural mob costs at most
	 * 40 % of max health traded hit for hit; a tank or elite needs at least 3 hits to kill the player and at most 30
	 * of the player's. The Fields (the nearest zone) also in iron with an iron sword: at most 50 %.
	 */
	@Test
	void everyZoneIsSurvivableFirst() {
		List<String> misses = new ArrayList<>();
		for (Zone zone : Zone.OPEN) {
			Stage first = stage(zone, 1);
			Player reference = BalanceModel.expectedPlayer(first, false);
			for (Mob mob : BalanceData.MOBS) {
				if (mob.zone() != zone) {
					continue;
				}
				Fight fight = BalanceModel.fight(mob, zone.minLevel, reference);
				if (!mob.placedOnly() && fight.tradeShare() > 0.40) {
					misses.add(mob.name() + " costs " + pct(fight.tradeShare()) + " of " + reference.describe());
				}
				if (mob.placedOnly() && (fight.hitsToKillPlayer() < 3 || fight.hitsToKillMob() > 30)) {
					misses.add(mob.name() + ": kills in " + fight.hitsToKillPlayer() + ", dies in " + fight.hitsToKillMob() + " vs "
							+ reference.describe());
				}
				if (zone == Zone.FIELDS && !mob.placedOnly()) {
					Fight floor = BalanceModel.fight(mob, zone.minLevel, BalanceModel.expectedPlayer(first, true));
					if (floor.tradeShare() > 0.50) {
						misses.add(mob.name() + " costs " + pct(floor.tradeShare()) + " of the iron floor player");
					}
				}
			}
		}
		assertNone(misses);
	}

	/**
	 * DESIGN §4.5 against full diamond on Normal: no ordinary boss hit above heavy (12), unblockable hits 12–17, never
	 * above 17. Hazards (pools, trails, the storm) are per tick and outside the classes.
	 */
	@Test
	void bossHitsStayInTheirClasses() {
		List<String> misses = new ArrayList<>();
		for (Boss boss : BalanceData.BOSSES) {
			for (Hit hit : boss.hits()) {
				if (hit.hazard()) {
					continue;
				}
				double damage = BalanceModel.hitOnPlayer(hit, boss.level(), BalanceModel.DIAMOND_REFERENCE);
				if (hit.kind() == HitKind.UNBLOCKABLE ? damage < 11.5 || damage > 17.5 : damage > 12.5) {
					misses.add(boss.name() + " " + hit.name() + ": " + BalanceReport.f(damage) + " vs full diamond");
				}
			}
		}
		assertNone(misses);
	}

	/**
	 * A zone boss falls to the Sanctum-level player in diamond in at most 70 hits at its best openings, the Lord to
	 * the depths player in at most 100.
	 */
	@Test
	void bossesFallInReasonableTime() {
		List<String> misses = new ArrayList<>();
		for (Boss boss : BalanceData.BOSSES) {
			Player player = BalanceModel.expectedPlayer(boss.zone() == Zone.CITADEL ? depths() : stage(boss.zone(), 1), false);
			BalanceModel.BossFight fight = BalanceModel.bossFight(boss, boss.level(), 1, player);
			int hits = Math.min(fight.sideHitsEach(), fight.bestHitsEach());
			int limit = boss.zone() == Zone.CITADEL ? 100 : 70;
			if (hits > limit) {
				misses.add(boss.name() + " takes " + hits + " hits from " + player.describe());
			}
		}
		assertNone(misses);
	}

	/** Citadel mobs against the depths player: elites die in at most 30 hits and need at least 3 to kill. */
	@Test
	void citadelIsFairForTheDepthsPlayer() {
		List<String> misses = new ArrayList<>();
		Player player = BalanceModel.expectedPlayer(depths(), false);
		for (Mob mob : BalanceData.MOBS) {
			if (mob.zone() != Zone.CITADEL) {
				continue;
			}
			Fight low = BalanceModel.fight(mob, Zone.CITADEL.minLevel, player);
			Fight high = BalanceModel.fight(mob, Zone.CITADEL.maxLevel, player);
			if (mob.placedOnly() && low.hitsToKillMob() > 30) {
				misses.add(mob.name() + " takes " + low.hitsToKillMob() + " hits at level " + low.level());
			}
			if (high.hitsToKillPlayer() < 3) {
				misses.add(mob.name() + " kills in " + high.hitsToKillPlayer() + " at level " + high.level());
			}
			if (!mob.placedOnly() && low.tradeShare() > 0.40) {
				misses.add(mob.name() + " costs " + pct(low.tradeShare()));
			}
		}
		assertNone(misses);
	}

	/**
	 * Full moon waves near spawn (spot level 4) against a level 5 player in iron with the starting flask, traded hit
	 * for hit with all of them attacking: two Ashen Zombies (two curse stacks) cleared with health + flask to spare, and
	 * one Ghost alone too. The whole Ghost wave is meant for a shield and kiting (DESIGN: dying once is cheaper), and
	 * the player's death ends it, so it is not held to this.
	 */
	@Test
	void fullMoonWavesCanBeCleared() {
		List<String> misses = new ArrayList<>();
		NightMobConfig night = NightMobConfig.DEFAULT;
		Player player = Player.of(5, BalanceModel.IRON, BalanceModel.IRON_SWORD, BalanceModel.FLASK.baseCharges());
		double budget = player.maxHealth() + player.flaskHealing();
		int level = 4 + night.zombieLevelBonus();
		double zombies = BalanceModel.waveDamage(BalanceData.ASHEN_ZOMBIE, level, 2, player, 0);
		double ghost = BalanceModel.waveDamage(BalanceData.GHOST, level, 1, player, night.ghostMissChance());
		if (zombies > budget) {
			misses.add("two Ashen Zombies at level " + level + " deal " + BalanceReport.f(zombies) + " of " + BalanceReport.f(budget));
		}
		if (ghost > budget) {
			misses.add("one Ghost at level " + level + " deals " + BalanceReport.f(ghost) + " of " + BalanceReport.f(budget));
		}
		assertNone(misses);
	}

	/** The Vampire boss at its lowest level against a level 10 player in iron: no hit above heavy (12). */
	@Test
	void vampireBossHitsAnIronPlayerFairly() {
		List<String> misses = new ArrayList<>();
		Player player = Player.of(10, BalanceModel.IRON, BalanceModel.IRON_SWORD, BalanceModel.FLASK.baseCharges());
		for (Hit hit : BalanceData.VAMPIRE.hits()) {
			double damage = BalanceModel.hitOnPlayer(hit, BalanceData.VAMPIRE.level(), player);
			if (damage > 12.5) {
				misses.add(hit.name() + ": " + BalanceReport.f(damage) + " vs iron");
			}
		}
		assertNone(misses);
	}

	private static String pct(double share) {
		return Math.round(share * 100) + " %";
	}
}
