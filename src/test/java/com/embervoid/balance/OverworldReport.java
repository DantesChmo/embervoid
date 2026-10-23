package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Hit;
import com.embervoid.balance.BalanceModel.Player;
import com.embervoid.balance.OverworldData.OverworldMob;
import com.embervoid.balance.OverworldModel.OverworldFight;
import com.embervoid.balance.OverworldModel.RouteXp;
import com.embervoid.sanctum.gear.SanctumGearConfig;
import net.minecraft.world.Difficulty;

import java.util.List;
import java.util.Locale;

import static com.embervoid.balance.BalanceReport.f;

/**
 * Renders the overworld stage's tables as Markdown for {@code docs/balance-overworld.md} ({@link BalanceReportTest}
 * writes them to {@code build/balance/overworld.md}).
 */
final class OverworldReport {
	private final StringBuilder out = new StringBuilder();

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
		bands();
		fights();
		hollow();
		route();
		weapons();
		return out.toString();
	}

	private void bands() {
		line("### Level bands (DESIGN §5.4)");
		line("");
		header("Mob", "Base", "Lowest", "Highest", "Full moon highest", "HP at lowest / highest", "XP at lowest / highest");
		for (OverworldMob mob : OverworldData.MOBS) {
			int min = OverworldModel.minLevel(mob);
			int max = OverworldModel.maxLevel(mob);
			row(mob.name(), mob.baseLevel(), min, max, max + OverworldModel.FULL_MOON_BONUS,
					f(BalanceModel.mobHealth(mob.mob().health(), min)) + " / " + f(BalanceModel.mobHealth(mob.mob().health(), max)),
					BalanceModel.mobXp(mob.mob().xp(), min) + " / " + BalanceModel.mobXp(mob.mob().xp(), max));
		}
		line("");
	}

	private void fights() {
		line("### Fights, traded hit for hit (cells: its hit / its hits to kill you / your hits to kill it / % of your health it costs)");
		line("");
		header("Mob", "Lv", "Player", "Easy", "Normal", "Hard");
		for (OverworldMob mob : OverworldData.MOBS) {
			for (int level : new int[]{OverworldModel.minLevel(mob), OverworldModel.maxLevel(mob)}) {
				for (Player player : OverworldModel.PLAYERS) {
					Object[] cells = new Object[6];
					cells[0] = mob.name();
					cells[1] = level;
					cells[2] = "Lv " + player.level() + " " + player.armor().name() + ", " + player.weapon().name();
					for (int i = 0; i < 3; i++) {
						OverworldFight fight = OverworldModel.fight(mob, level, player, OverworldModel.DIFFICULTIES.get(i));
						cells[3 + i] = f(fight.fight().mobHit()) + " / " + fight.fight().hitsToKillPlayer() + " / "
								+ fight.fight().hitsToKillMob() + " / " + Math.round(100 * fight.tradeShare()) + " %";
					}
					row(cells);
				}
			}
		}
		line("");
		header("Ash Ghoul pack of 4, lowest level, all attacking", "Easy", "Normal", "Hard", "Budget (health + flask)");
		for (Player player : List.of(OverworldModel.FRESH_LEATHER, OverworldModel.FRESH_IRON)) {
			Object[] cells = {"Lv 1 " + player.armor().name() + ", " + player.weapon().name(), "", "", "", f(player.maxHealth() + player.flaskHealing())};
			for (int i = 0; i < 3; i++) {
				cells[1 + i] = f(OverworldModel.packDamage(OverworldData.ASH_GHOUL, OverworldData.ASH_GHOUL.baseLevel(), 4, player,
						OverworldModel.DIFFICULTIES.get(i)));
			}
			row(cells);
		}
		line("");
		line("Second hits (one each, lowest level, level 1 in iron):");
		line("");
		header("Mob", "Hit", "Easy", "Normal", "Hard");
		for (OverworldMob mob : OverworldData.MOBS) {
			for (Hit hit : mob.mob().hits().subList(1, mob.mob().hits().size())) {
				Object[] cells = {mob.name(), hit.name(), "", "", ""};
				for (int i = 0; i < 3; i++) {
					cells[2 + i] = f(OverworldModel.hit(hit, OverworldModel.minLevel(mob), OverworldModel.FRESH_IRON,
							OverworldModel.DIFFICULTIES.get(i)));
				}
				row(cells);
			}
		}
		line("");
	}

	private void hollow() {
		line("### The Hollow's swing through full iron (DESIGN §3.2: ~6 on Normal), weapons 70 % iron / 30 % stone sword");
		line("");
		header("Level", "Easy", "Normal", "Hard");
		OverworldMob hollow = OverworldData.HOLLOW;
		for (int level : new int[]{OverworldModel.minLevel(hollow), OverworldModel.maxLevel(hollow),
				OverworldModel.maxLevel(hollow) + OverworldModel.FULL_MOON_BONUS}) {
			row(level, f(OverworldModel.hollowHitThroughIron(level, Difficulty.EASY)),
					f(OverworldModel.hollowHitThroughIron(level, Difficulty.NORMAL)), f(OverworldModel.hollowHitThroughIron(level, Difficulty.HARD)));
		}
		line("");
	}

	private void route() {
		OverworldModel.Route route = OverworldModel.Route.TYPICAL;
		RouteXp xp = OverworldModel.routeXp(route);
		line("### XP on the way to the Sanctum (" + route + ")");
		line("");
		header("Surface kills", "Cave kills", "Mining", "Smelting", "Sanctum guards + chests", "Total", "Level bought from 1");
		row(f(xp.surface()), f(xp.caves()), f(xp.mining()), f(xp.smelting()), f(xp.sanctum()), f(xp.total()),
				BalanceModel.levelAfter(1, xp.total()));
		line("");
		line("Mean XP per kill: surface " + f(OverworldModel.meanKillXp(OverworldMob::surface, 0)) + ", below y=0 "
				+ f(OverworldModel.meanKillXp(OverworldMob::underground, BalanceModel.LEVELS.belowZeroBonus())) + ".");
		line("");
		header("Level", "Cost of the next", "Total to reach");
		for (int level = 1; level <= 12; level++) {
			row(level, BalanceModel.LEVELING.cost(level), BalanceModel.LEVELING.totalCost(level));
		}
		line("");
	}

	private void weapons() {
		line("### Weapons for the Sanctum-level player (Lv " + OverworldModel.SANCTUM_IRON.level() + ") against armor 10 and armor 0");
		line("");
		header("Weapon", "Damage", "Speed", "Hit (armor 10)", "Per second (armor 10)", "Hit (armor 0)", "Per second (armor 0)");
		List<BalanceModel.Weapon> weapons = List.of(BalanceModel.IRON_SWORD, BalanceModel.DIAMOND_SWORD, BalanceModel.IRON_AXE,
				BalanceModel.DIAMOND_AXE, BalanceModel.SANCTUM_LONGSWORD, BalanceModel.ASHBEARERS_MACE, BalanceModel.ASHEN_SCYTHE,
				BalanceModel.WARDENS_SCOURGE, BalanceModel.ROYAL_ASH_RAPIER);
		for (BalanceModel.Weapon weapon : weapons) {
			Player player = withWeapon(weapon);
			row(weapon.name(), f(weapon.damage()), f(weapon.speed()), f(BalanceModel.playerHit(player, 10, 1)),
					f(OverworldModel.perSecond(player, 10)), f(BalanceModel.playerHit(player, 0, 1)), f(OverworldModel.perSecond(player, 0)));
		}
		line("");
		SanctumGearConfig gear = SanctumGearConfig.SECTION.defaults();
		line(String.format(Locale.ROOT, "Specials (armor 0): Wide Cleave %s to every mob in the arc for %s stamina / %s s; "
						+ "Ground Slam %s to every mob around for %s stamina / %s s.",
				f(BalanceModel.playerHit(withWeapon(BalanceModel.SANCTUM_LONGSWORD), 0, gear.cleaveMultiplier())), f(gear.cleaveStamina()),
				f(gear.cleaveCooldown() / 20.0), f(BalanceModel.playerHit(withWeapon(BalanceModel.ASHBEARERS_MACE), 0, gear.slamMultiplier())),
				f(gear.slamStamina()), f(gear.slamCooldown() / 20.0)));
		line("");
	}

	static Player withWeapon(BalanceModel.Weapon weapon) {
		Player sanctum = OverworldModel.SANCTUM_IRON;
		return new Player(sanctum.level(), sanctum.stats(), sanctum.armor(), weapon, sanctum.flaskCharges());
	}
}
