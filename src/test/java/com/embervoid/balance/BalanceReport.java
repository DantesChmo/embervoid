package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Boss;
import com.embervoid.balance.BalanceData.Hit;
import com.embervoid.balance.BalanceData.Mob;
import com.embervoid.balance.BalanceData.Zone;
import com.embervoid.balance.BalanceModel.BossFight;
import com.embervoid.balance.BalanceModel.Fight;
import com.embervoid.balance.BalanceModel.Player;
import com.embervoid.balance.BalanceModel.Playthrough;
import com.embervoid.balance.BalanceModel.Stage;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders the model's tables as Markdown for {@code docs/balance-0.4.0.md} ({@link BalanceReportTest} writes them to
 * {@code build/balance/tables.md}).
 */
final class BalanceReport {
	private final StringBuilder out = new StringBuilder();
	private final Playthrough run;

	BalanceReport(Playthrough run) {
		this.run = run;
	}

	static String f(double value) {
		return String.format(Locale.ROOT, value == Math.rint(value) ? "%.0f" : "%.1f", value);
	}

	/** Hits to kill, or "-" for a target those hits never hurt. */
	static String hits(int hits) {
		return hits == Integer.MAX_VALUE ? "-" : String.valueOf(hits);
	}

	private void line(String text) {
		out.append(text).append('\n');
	}

	private void row(Object... cells) {
		StringBuilder row = new StringBuilder("|");
		for (Object cell : cells) {
			row.append(' ').append(cell).append(" |");
		}
		line(row.toString());
	}

	private void header(String... cells) {
		row((Object[]) cells);
		row((Object[]) java.util.Collections.nCopies(cells.length, "---").toArray(new String[0]));
	}

	String render() {
		xp();
		stages();
		mobs();
		bosses();
		bossTargets();
		multiplayer();
		moon();
		loot();
		weapons();
		return out.toString();
	}

	private void xp() {
		line("### XP by zone (" + run + ")");
		line("");
		header("Zone", "Placed", "Natural", "Boss", "Total");
		for (Zone zone : Zone.values()) {
			BalanceModel.ZoneXp xp = BalanceModel.zoneXp(zone, run);
			row(zone.title, f(xp.placed()), f(xp.natural()), f(xp.boss()), f(xp.total()));
		}
		line("");
		header("Level", "Cost of the next", "Total to reach");
		for (int level : new int[]{10, 15, 20, 25, 30, 35, 40}) {
			row(level, BalanceModel.LEVELING.cost(level), BalanceModel.LEVELING.totalCost(level));
		}
		line("");
	}

	private void stages() {
		line("### Expected player per stage");
		line("");
		header("Stage", "Level", "HP", "Flask HP", "Floor kit", "Reference kit");
		for (Stage stage : BalanceModel.stages(run)) {
			Player floor = BalanceModel.expectedPlayer(stage, true);
			Player ref = BalanceModel.expectedPlayer(stage, false);
			row(stage.name(), stage.level(), f(ref.maxHealth()), f(ref.flaskHealing()), floor.armor().name() + " + " + floor.weapon().name(),
					ref.armor().name() + " + " + ref.weapon().name());
		}
		line("");
	}

	/** Each zone's mobs at the band's bottom (entered first, floor kit) and top (entered third, reference kit). */
	private void mobs() {
		line("### Mobs: hits both ways");
		line("");
		line("Per mob: band bottom against the player entering the zone first, in the floor kit and in the reference kit;");
		line("band top against the player entering it third in the reference kit. Citadel: the depths player, bottom and top.");
		line("");
		header("Mob", "Lv", "Player", "Mob HP", "Player hit", "Hits to kill", "Mob hit", "Hits to kill player", "Trade (% HP)");
		List<Stage> stages = BalanceModel.stages(run);
		for (Mob mob : BalanceData.MOBS) {
			Stage first = stages.stream().filter(s -> s.zone() == mob.zone() && s.order() == (mob.zone() == Zone.CITADEL ? 4 : 1)).findFirst().orElseThrow();
			Stage last = stages.stream().filter(s -> s.zone() == mob.zone() && s.order() == (mob.zone() == Zone.CITADEL ? 4 : 3)).findFirst().orElseThrow();
			if (mob.zone() != Zone.CITADEL) {
				fightRow(BalanceModel.fight(mob, mob.zone().minLevel, BalanceModel.expectedPlayer(first, true)));
			}
			fightRow(BalanceModel.fight(mob, mob.zone().minLevel, BalanceModel.expectedPlayer(first, false)));
			fightRow(BalanceModel.fight(mob, mob.zone().maxLevel, BalanceModel.expectedPlayer(last, false)));
		}
		line("");
	}

	private void fightRow(Fight fight) {
		row(fight.mob().name(), fight.level(), "Lv " + fight.player().level() + " " + fight.player().armor().name() + ", " + fight.player().weapon().name(), f(fight.mobHealth()),
				f(fight.playerHit()), fight.hitsToKillMob(), f(fight.mobHit()), fight.hitsToKillPlayer(), f(100 * fight.tradeShare()));
	}

	private void bosses() {
		line("### Bosses: the player's hits (solo)");
		line("");
		header("Boss", "Lv", "Player", "Boss HP", "Hit", "Hits", "Seconds", "Best opening", "Hit", "Hits");
		List<Stage> stages = BalanceModel.stages(run);
		for (Boss boss : BalanceData.BOSSES) {
			for (Stage stage : stages) {
				if (stage.zone() != boss.zone() || (stage.order() == 2)) {
					continue;
				}
				BossFight fight = BalanceModel.bossFight(boss, boss.level(), 1, BalanceModel.expectedPlayer(stage, false));
				row(boss.name(), boss.level(), fight.player().describe(), f(fight.health()), f(fight.sideHit()), hits(fight.sideHitsEach()),
						fight.sideHitsEach() == Integer.MAX_VALUE ? "-" : f(fight.sideSeconds()), boss.bestWhen(), f(fight.bestHit()), fight.bestHitsEach());
			}
		}
		line("");
		line("### Bosses: their hits on the expected player");
		line("");
		header("Boss", "Move", "Base", "vs diamond ref", "Class", "vs expected (1st / 3rd)", "Hits to kill (1st / 3rd)");
		for (Boss boss : BalanceData.BOSSES) {
			Stage first = stages.stream().filter(s -> s.zone() == boss.zone() && s.order() == (boss.zone() == Zone.CITADEL ? 4 : 1)).findFirst().orElseThrow();
			Stage last = stages.stream().filter(s -> s.zone() == boss.zone() && s.order() == (boss.zone() == Zone.CITADEL ? 4 : 3)).findFirst().orElseThrow();
			Player p1 = BalanceModel.expectedPlayer(first, false);
			Player p3 = BalanceModel.expectedPlayer(last, false);
			for (Hit hit : boss.hits()) {
				double ref = BalanceModel.hitOnPlayer(hit, boss.level(), BalanceModel.DIAMOND_REFERENCE);
				double d1 = BalanceModel.hitOnPlayer(hit, boss.level(), p1);
				double d3 = BalanceModel.hitOnPlayer(hit, boss.level(), p3);
				row(boss.name(), hit.name(), f(hit.base()), f(ref), classOf(ref), f(d1) + " / " + f(d3),
						BalanceModel.hitsToKill(p1.maxHealth(), d1) + " / " + BalanceModel.hitsToKill(p3.maxHealth(), d3));
			}
		}
		line("");
	}

	static String classOf(double damage) {
		for (BalanceModel.HitClass c : BalanceModel.HitClass.values()) {
			if (damage >= c.min && damage <= c.max) {
				return c.name().toLowerCase(Locale.ROOT);
			}
		}
		return damage < 4 ? "chip" : damage < 8 ? "light+" : damage > 17 ? "over" : "between";
	}

	private void bossTargets() {
	}

	private void loot() {
		line("### Loot along the route (expected counts)");
		line("");
		String[] items = LootBudget.ITEMS;
		header("Where", "Cinder", "Amber", "Raw Grave Iron", "Grave Iron Ingot", "Smoldering Ingot", "Template", "Set piece");
		for (Zone zone : Zone.values()) {
			Map<String, Double> got = LootBudget.zone(zone, run);
			Object[] cells = new Object[items.length + 1];
			cells[0] = zone.title;
			for (int i = 0; i < items.length; i++) {
				cells[i + 1] = String.format(Locale.ROOT, "%.1f", got.getOrDefault(items[i], 0.0));
			}
			row(cells);
		}
		line("");
		Map<String, Double> found = LootBudget.beforeLord(run);
		header("Goal (before the Lord)", "Grave Iron needed", "Cinder needed", "Amber needed", "Ore blocks to mine (GI / Cinder / Amber)");
		for (boolean zoneWeapons : new boolean[]{false, true}) {
			Map<String, Double> need = LootBudget.need(found, zoneWeapons);
			Map<String, Double> mine = LootBudget.oreToMine(found, need);
			row(zoneWeapons ? "Smoldering Plate + the three zone uniques" : "Smoldering Plate", f(need.get(LootBudget.RAW_GRAVE_IRON)), f(need.get(LootBudget.CINDER)),
					f(need.get(LootBudget.AMBER)), String.format(Locale.ROOT, "%.0f / %.0f / %.0f", mine.get(LootBudget.RAW_GRAVE_IRON),
							mine.get(LootBudget.CINDER), mine.get(LootBudget.AMBER)));
		}
		line("");
	}

	private void weapons() {
		line("### Weapons against a Citadel elite (armor 10) for the depths player");
		line("");
		Stage depths = BalanceModel.stages(run).stream().filter(s -> s.zone() == Zone.CITADEL).findFirst().orElseThrow();
		header("Weapon", "Damage", "Speed", "Hit (armor 10)", "Per second");
		for (BalanceModel.Weapon weapon : List.of(BalanceModel.DIAMOND_SWORD, BalanceModel.NETHERITE_SWORD, BalanceModel.NETHERITE_SWORD_SHARP_V,
				BalanceModel.DIAMOND_AXE, BalanceModel.ASHEN_SCYTHE, BalanceModel.WARDENS_SCOURGE, BalanceModel.ROYAL_ASH_RAPIER)) {
			Player player = new Player(depths.level(), BalanceModel.statsAt(depths.level()), BalanceModel.SMOLDERING, weapon, 3);
			double hit = BalanceModel.playerHit(player, 10, 1);
			row(weapon.name(), f(weapon.damage()), f(weapon.speed()), f(hit), f(hit * 20 / player.attackTicks()));
		}
		line("");
	}

	private void multiplayer() {
		line("### Bosses with 2–3 players (each player like the solo row's 3rd-zone player)");
		line("");
		header("Boss", "Players", "Boss HP", "Hits each", "Adds", "Add HP", "Add: hits to kill");
		List<Stage> stages = BalanceModel.stages(run);
		for (Boss boss : BalanceData.BOSSES) {
			Stage last = stages.stream().filter(s -> s.zone() == boss.zone() && s.order() == (boss.zone() == Zone.CITADEL ? 4 : 3)).findFirst().orElseThrow();
			Player player = BalanceModel.expectedPlayer(last, false);
			for (int n = 1; n <= 3; n++) {
				BossFight fight = BalanceModel.bossFight(boss, boss.level(), n, player);
				Fight add = BalanceModel.fight(fight.addMob(), fight.addLevel(), player);
				row(boss.name(), n, f(fight.health()), hits(fight.sideHitsEach()), fight.adds() + " × " + fight.addMob().name() + " Lv " + fight.addLevel(),
						f(add.mobHealth()), add.hitsToKillMob());
			}
		}
		line("");
	}

	private void moon() {
		line("### Moon nights");
		line("");
		header("Who", "Lv", "Player", "HP", "Player hit", "Hits to kill", "Its hit", "Hits to kill player", "Trade (% HP)");
		for (int level : new int[]{5, 10, 15}) {
			Player player = Player.of(level, BalanceModel.IRON, BalanceModel.IRON_SWORD, 3);
			for (int mobLevel : new int[]{9, 14}) {
				Fight zombie = BalanceModel.fight(BalanceData.ASHEN_ZOMBIE, mobLevel, player);
				Fight ghost = BalanceModel.fight(BalanceData.GHOST, mobLevel, player);
				moonRow(zombie);
				moonRow(ghost);
			}
		}
		line("");
		header("Vampire boss Lv", "Player", "Boss HP", "Hit", "Hits", "Seconds", "Claw / leap / nova");
		for (int level : new int[]{5, 10, 15, 20, 30}) {
			Player player = Player.of(level, level >= 20 ? BalanceModel.DIAMOND : BalanceModel.IRON,
					level >= 20 ? BalanceModel.DIAMOND_SWORD : BalanceModel.IRON_SWORD, 3);
			int bossLevel = Math.min(Math.max(level, 15), 30);
			BossFight fight = BalanceModel.bossFight(BalanceData.VAMPIRE, bossLevel, 1, player);
			row(bossLevel, player.describe(), f(fight.health()), f(fight.sideHit()), hits(fight.sideHitsEach()),
					fight.sideHitsEach() == Integer.MAX_VALUE ? "-" : f(fight.sideSeconds()),
					String.join(" / ", fight.moveDamage().values().stream().map(BalanceReport::f).toList()));
		}
		line("");
	}

	private void moonRow(Fight fight) {
		row(fight.mob().name(), fight.level(), fight.player().describe(), f(fight.mobHealth()), f(fight.playerHit()), fight.hitsToKillMob(),
				f(fight.mobHit()), fight.hitsToKillPlayer(), f(100 * fight.tradeShare()));
	}
}
