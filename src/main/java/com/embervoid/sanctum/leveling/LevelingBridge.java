package com.embervoid.sanctum.leveling;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.player.XpUtil;
import com.embervoid.stats.DerivedStats;
import com.embervoid.stats.PlayerStats;
import com.embervoid.stats.Stat;
import com.embervoid.stats.StatsConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Builds the level-up screen's {@link StatSheet} from player-core's {@link PlayerStats} and spends points through it. */
final class LevelingBridge {
	/** One derived value a stat drives: its name and how to format it from {@link DerivedStats}. */
	private record Line(String key, Function<DerivedStats, String> format) {}

	static StatSheet sheet(ServerPlayerEntity player) {
		int statCap = EmbervoidConfig.get().section(StatsConfig.Leveling.SECTION).statCap();
		DerivedStats now = PlayerStats.derived(player);
		List<StatSheet.Stat> stats = new ArrayList<>();
		for (Stat stat : Stat.values()) {
			int value = PlayerStats.get(player, stat);
			// At the level cap no stat can rise: show each as capped where it stands.
			boolean levelCapped = PlayerStats.checkLevelUp(player, stat) == PlayerStats.LevelUpResult.LEVEL_CAP;
			int max = levelCapped ? value : statCap;
			List<StatSheet.Derived> derived = new ArrayList<>();
			if (value < max) {
				DerivedStats next = PlayerStats.derivedWith(player, stat, value + 1);
				for (Line line : lines(stat)) {
					derived.add(new StatSheet.Derived(line.key, line.format.apply(now), line.format.apply(next)));
				}
			}
			stats.add(new StatSheet.Stat(stat.id(), value, max, derived));
		}
		return new StatSheet(PlayerStats.level(player), PlayerStats.levelUpCost(player), XpUtil.getPoints(player), stats);
	}

	/** Raises {@code statId} by one point. Returns null on success, or why it failed (shown to the player). */
	@Nullable
	static Text levelUp(ServerPlayerEntity player, String statId) {
		Stat stat = Stat.byId(statId);
		if (stat == null) {
			return PlayerStats.LevelUpResult.STAT_CAP.message();
		}
		PlayerStats.LevelUpResult result = PlayerStats.levelUp(player, stat);
		return result == PlayerStats.LevelUpResult.SUCCESS ? null : result.message();
	}

	private static List<Line> lines(Stat stat) {
		return switch (stat) {
			case VITALITY -> List.of(new Line("embervoid.derived.max_health", d -> number(d.maxHealth())));
			case ENDURANCE -> List.of(
					new Line("embervoid.derived.max_stamina", d -> number(d.maxStamina())),
					new Line("embervoid.derived.stamina_regen", d -> number(d.staminaRegenPerTick() * 20)),
					new Line("embervoid.derived.knockback_resistance", d -> percent(d.knockbackResistance())));
			case STRENGTH -> List.of(new Line("embervoid.derived.strength_damage", d -> bonus(d.strengthDamageBonus())));
			case DEXTERITY -> List.of(
					new Line("embervoid.derived.dexterity_damage", d -> bonus(d.dexterityDamageBonus())),
					new Line("embervoid.derived.attack_speed", d -> bonus(d.attackSpeedBonus())));
			case KINDLING -> List.of(new Line("embervoid.derived.flask_heal", d -> percent(d.flaskHealMultiplier())));
			case RESILIENCE -> List.of(
					new Line("embervoid.derived.armor_toughness", d -> number(d.armorToughness())),
					new Line("embervoid.derived.fire_reduction", d -> percent(d.fireDamageReduction())));
			case FORTUNE -> List.of(new Line("embervoid.derived.luck", d -> number(d.luck())));
		};
	}

	/** One decimal, without a trailing ".0". */
	private static String number(double value) {
		String s = String.format(Locale.ROOT, "%.1f", value);
		return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
	}

	private static String percent(double fraction) {
		return number(fraction * 100) + "%";
	}

	private static String bonus(double fraction) {
		return "+" + percent(fraction);
	}

	private LevelingBridge() {}
}
