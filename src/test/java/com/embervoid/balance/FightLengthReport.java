package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Beat;
import com.embervoid.balance.BalanceData.Cadence;
import com.embervoid.balance.BalanceData.Downtime;
import com.embervoid.balance.BalanceData.Phase;
import com.embervoid.balance.BalanceModel.Fight;
import com.embervoid.balance.BalanceModel.Player;
import com.embervoid.balance.BalanceModel.Playthrough;
import com.embervoid.balance.BalanceModel.Weapon;
import com.embervoid.balance.FightLength.Band;
import com.embervoid.balance.FightLength.BossStage;
import com.embervoid.balance.FightLength.HitShare;
import com.embervoid.balance.FightLength.Length;
import com.embervoid.balance.FightLength.MobStage;
import com.embervoid.balance.FightLength.PhaseLength;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.embervoid.balance.FightTargets.f;
import static com.embervoid.balance.FightTargets.pct;
import static com.embervoid.balance.FightTargets.who;

/**
 * Renders the fight-length model's tables as Markdown for {@code docs/balance-0.5.0.md} ({@link BalanceReportTest}
 * writes them to {@code build/balance/fight-length.md}), ending with every miss of the 0.5.0 targets.
 */
final class FightLengthReport {
	private final StringBuilder out = new StringBuilder();
	private final Playthrough run;

	FightLengthReport(Playthrough run) {
		this.run = run;
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
		row(Collections.nCopies(cells.length, "---").toArray());
	}

	private static String min(double seconds) {
		return String.format(Locale.ROOT, "%.1f", seconds / 60);
	}

	String render() {
		cadences();
		bosses();
		multiplayer();
		hitsForTheTime();
		hitSizes();
		mobs();
		weapons();
		misses();
		return out.toString();
	}

	private void cadences() {
		line("### Boss cadence: openings per beat (escape " + FightLength.ESCAPE_TICKS + " ticks, re-engage " + FightLength.REENGAGE_TICKS
				+ ", out of melee " + pct(BalanceData.OUT_OF_MELEE) + " of picks)");
		line("");
		header("Boss", "Phase", "Beat", "Lead / busy / recovery", "Weight", "Open (aimed at you / not)", "Range, aim");
		for (Cadence cadence : BalanceData.CADENCES) {
			for (Phase phase : cadence.phases()) {
				for (Beat beat : phase.beats()) {
					row(cadence.boss().name(), phase.name(), beat.name(), beat.lead() + " / " + beat.busy() + " / " + beat.recovery(),
							f(beat.weight()), f(FightLength.openTicks(beat, true)) + " / " + f(FightLength.openTicks(beat, false)),
							beat.range().name().toLowerCase(Locale.ROOT) + ", " + beat.aim().name().toLowerCase(Locale.ROOT));
				}
			}
		}
		line("");
		header("Boss", "Phase", "From", "Pause", "Loop ticks", "Open in the loop", "Downtime (each)");
		for (Cadence cadence : BalanceData.CADENCES) {
			Player player = FightLength.weakestPlayer(cadence, run);
			for (Phase phase : cadence.phases()) {
				double loop = phase.beats().stream().mapToDouble(b -> b.weight() * b.ticks()).sum();
				double open = phase.beats().stream().mapToDouble(b -> b.weight() * FightLength.openTicks(b, true)).sum();
				List<String> downtime = new ArrayList<>();
				for (Downtime d : phase.downtime()) {
					double ticks = d.ticks() + (d.add() == null ? 0 : FightLength.killTicks(d.add(), d.count(), FightLength.addLevel(cadence.boss()), player));
					downtime.add(d.name() + ": " + f(ticks) + (d.every() > 0 ? " per " + f(d.every()) : " once"));
				}
				row(cadence.boss().name(), phase.name(), pct(phase.from()), phase.pause(), f(loop), loop > 0 ? pct(open / loop) : "-",
						downtime.isEmpty() ? "-" : String.join("; ", downtime));
			}
		}
		line("");
	}

	private void bosses() {
		line("### Boss fights, solo (today's numbers)");
		line("");
		header("Boss", "Stage", "Player", "Boss HP", "Clean hit (P1)", "Clean hits", "Swinging (s)", "Uptime", "Fight (min)", "Hits / time ok");
		for (BossStage stage : FightLength.bossStages(run)) {
			Length length = FightLength.length(stage.cadence(), stage.player(), 1);
			int hits = (int) Math.ceil(length.hitsEach() - 1e-6);
			boolean hitsOk = hits >= FightLength.MIN_HITS && hits <= FightLength.MAX_HITS;
			boolean timeOk = length.seconds() >= FightLength.MIN_SECONDS && length.seconds() <= FightLength.MAX_SECONDS;
			PhaseLength first = length.phases().stream().filter(p -> p.health() > 0).findFirst().orElseThrow();
			row(stage.cadence().boss().name(), stage.name(), who(stage.player()), f(length.health()), f(first.hit()), hits,
					f(length.hittingSeconds()), pct(length.uptime()), min(length.seconds()), (hitsOk ? "yes" : "**no**") + " / " + (timeOk ? "yes" : "**no**"));
		}
		line("");
	}

	private void multiplayer() {
		line("### Boss fights with 2–3 players (each like the stage's player; HP ×1.5 / ×2, one add per extra fighter)");
		line("");
		header("Boss", "Stage", "Players", "Boss HP", "Clean hits each", "Uptime", "Adds (s)", "Fight (min)");
		List<BossStage> stages = FightLength.bossStages(run);
		for (Cadence cadence : BalanceData.CADENCES) {
			List<BossStage> of = stages.stream().filter(s -> s.cadence() == cadence).toList();
			// The first player who fights it and the last (strongest).
			for (BossStage stage : of.size() == 1 ? of : List.of(of.get(0), of.get(of.size() - 1))) {
				for (int n = 1; n <= 3; n++) {
					Length length = FightLength.length(cadence, stage.player(), n);
					row(cadence.boss().name(), stage.name() + ", " + stage.player().weapon().name(), n, f(length.health()),
							f(length.hitsEach()), pct(length.uptime()), f(length.addTicks() / 20), min(length.seconds()));
				}
			}
		}
		line("");
	}

	/** What the boss would need to last 1.5 and 2.5 min at its uptime with the stage's weapon, and the hits it takes then. */
	private void hitsForTheTime() {
		line("### What 1.5–2.5 min means in clean hits (at each fight's own uptime and swing)");
		line("");
		header("Boss", "Stage", "Weapon", "Swing (s)", "Uptime", "Clean hits for 1.5 min", "for 2.5 min", "Boss HP for 1.5–2.5 min (× today)",
				"Hits meeting both bands", "Boss HP for those (× today)");
		for (BossStage stage : FightLength.bossStages(run)) {
			Length length = FightLength.length(stage.cadence(), stage.player(), 1);
			double swing = stage.player().attackTicks() / 20;
			// Fixed time (the Lord's wall, phase pauses) does not scale with HP.
			double fixed = stage.cadence().phases().stream().mapToDouble(Phase::pause).sum() / 20;
			double scalable = length.seconds() - fixed;
			double low = Math.max(0, FightLength.MIN_SECONDS - fixed) / scalable;
			double high = Math.max(0, FightLength.MAX_SECONDS - fixed) / scalable;
			// Clean hits scale with boss HP; the overlap of the time band's hits with 20–35.
			double from = Math.max(FightLength.MIN_HITS, length.hitsEach() * low);
			double to = Math.min(FightLength.MAX_HITS, length.hitsEach() * high);
			boolean both = from <= to;
			row(stage.cadence().boss().name(), stage.name(), stage.player().weapon().name(), f(swing), pct(length.uptime()),
					f(length.hitsEach() * low), f(length.hitsEach() * high), "×" + factor(low) + "–" + factor(high),
					both ? f(from) + "–" + f(to) : "**none**",
					both ? "×" + factor(from / length.hitsEach()) + "–" + factor(to / length.hitsEach()) : "-");
		}
		line("");
	}

	private static String factor(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private void hitSizes() {
		line("### Boss moves as a share of the expected player's max health (the first, weakest player who fights them)");
		line("");
		header("Boss", "Move", "Class", "Windup", "Tell", "Player", "Damage", "Share", "Limit", "Breaks");
		for (Cadence cadence : BalanceData.CADENCES) {
			Player player = FightLength.weakestPlayer(cadence, run);
			for (HitShare share : FightLength.hitShares(cadence, player)) {
				List<String> breaks = share.breaks();
				row(cadence.boss().name(), share.hit().name(), share.timing().size().name().toLowerCase(Locale.ROOT), share.timing().windup(),
						share.timing().tell().name().toLowerCase(Locale.ROOT), who(player) + " (" + f(player.maxHealth()) + " HP)", f(share.damage()),
						pct(share.share()), pct(FightLength.limit(share.timing().size())), breaks.isEmpty() ? "-" : "**" + String.join("; ", breaks) + "**");
			}
		}
		line("");
		line("### Hazards (per tick, outside the classes) on the same player");
		line("");
		header("Boss", "Hazard", "Damage", "Share");
		for (Cadence cadence : BalanceData.CADENCES) {
			Player player = FightLength.weakestPlayer(cadence, run);
			for (BalanceData.Hit hit : cadence.boss().hits()) {
				if (hit.hazard()) {
					double damage = BalanceModel.hitOnPlayer(hit, cadence.boss().level(), player);
					row(cadence.boss().name(), hit.name(), f(damage), pct(damage / player.maxHealth()));
				}
			}
		}
		line("");
	}

	private void mobs() {
		line("### Mobs: hits both ways against the stage's player");
		line("");
		header("Stage", "Mob", "Lv", "Band", "Player", "Mob HP", "Hits to kill (want)", "Its hit", "Hits to kill the player (want)");
		for (MobStage stage : FightLength.mobStages(run)) {
			Fight fight = stage.fight();
			Band band = FightLength.bandOf(stage.mob());
			boolean hitsOk = fight.hitsToKillMob() >= band.minHits && fight.hitsToKillMob() <= band.maxHits;
			boolean playerOk = fight.hitsToKillPlayer() >= band.minHitsOnPlayer;
			row(stage.stage(), stage.mob().name(), stage.level(), band.name().toLowerCase(Locale.ROOT), who(stage.player()), f(fight.mobHealth()),
					mark(fight.hitsToKillMob(), hitsOk) + " (" + band.minHits + "–" + band.maxHits + ")", f(fight.mobHit()),
					mark(fight.hitsToKillPlayer(), playerOk) + " (≥ " + band.minHitsOnPlayer + ")");
		}
		line("");
	}

	private static String mark(int value, boolean ok) {
		return ok ? String.valueOf(value) : "**" + value + "**";
	}

	private void weapons() {
		line("### Weapons per second (attribute damage × speed, no stats; DESIGN §6.1, netherite " + f(FightLength.NETHERITE_PER_SECOND) + ")");
		line("");
		header("Weapon", "Damage", "Speed", "Per second", "Own use", "Per second in own use", "vs netherite", "One target");
		List<Weapon> weapons = new ArrayList<>(List.of(BalanceModel.IRON_SWORD, BalanceModel.DIAMOND_SWORD, BalanceModel.NETHERITE_SWORD));
		weapons.addAll(BalanceModel.REALM_MELEE);
		weapons.addAll(BalanceModel.SANCTUM_MELEE);
		for (Weapon weapon : weapons) {
			double single = FightLength.perSecond(weapon);
			double own = single * FightLength.ownUse(weapon);
			row(weapon.name(), f(weapon.damage()), f(weapon.speed()), f(single),
					"×" + java.math.BigDecimal.valueOf(Math.round(FightLength.ownUse(weapon) * 100) / 100.0).stripTrailingZeros().toPlainString(), f(own),
					String.format(Locale.ROOT, "%+.0f %%", (own / FightLength.NETHERITE_PER_SECOND - 1) * 100),
					f(single * FightLength.oneTarget(weapon)));
		}
		line("");
		line("The Scourge's ×1.6 is its tip crack, built in PLAN 17.4; until then it hits one target at ×1.");
		line("");
	}

	private void misses() {
		line("### Misses of the 0.5.0 targets (today's numbers; `./gradlew balanceTargets`)");
		line("");
		for (Map.Entry<String, List<String>> target : FightTargets.all(run).entrySet()) {
			line("**" + target.getKey() + "**: " + (target.getValue().isEmpty() ? "none." : target.getValue().size() + " misses."));
			line("");
			for (String miss : target.getValue()) {
				line("- " + miss);
			}
			if (!target.getValue().isEmpty()) {
				line("");
			}
		}
	}
}
