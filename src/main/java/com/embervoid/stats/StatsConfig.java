package com.embervoid.stats;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;

/**
 * Config sections {@code stats} (what one full stat point gives, see {@link StatCurve}) and {@code leveling}
 * (level cost curve and caps).
 */
public final class StatsConfig {
	/**
	 * Per full point above 10.
	 *
	 * @param vitalityHealth               max health (half-hearts)
	 * @param enduranceStamina             max stamina
	 * @param enduranceStaminaRegen        stamina regen, as a fraction of the configured regen
	 * @param enduranceKnockbackResistance knockback resistance (vanilla 0–1 scale)
	 * @param strengthDamage               damage with STR weapons, as a fraction
	 * @param dexterityDamage              damage with DEX weapons, as a fraction
	 * @param dexterityAttackSpeed         attack speed, as a fraction
	 * @param kindlingFlaskHeal            Healing Flask heal, as a fraction
	 * @param resilienceToughness          armor toughness
	 * @param resilienceFireReduction      fire damage taken, fraction removed
	 * @param resilienceFireReductionCap   most fire damage RES can remove (0–1)
	 * @param fortuneLuck                  luck
	 */
	public record Effects(float vitalityHealth, float enduranceStamina, float enduranceStaminaRegen,
			float enduranceKnockbackResistance, float strengthDamage, float dexterityDamage, float dexterityAttackSpeed,
			float kindlingFlaskHeal, float resilienceToughness, float resilienceFireReduction,
			float resilienceFireReductionCap, float fortuneLuck) {
		public static final Effects DEFAULT = new Effects(1.0f, 2.0f, 0.02f, 0.005f, 0.02f, 0.02f, 0.005f, 0.02f,
				0.1f, 0.01f, 0.5f, 0.1f);

		public static final ConfigSection<Effects> SECTION = EmbervoidConfig.register(ConfigSection.of(
				"stats", DEFAULT,
				in -> new Effects(
						in.readFloat("vitalityHealth", DEFAULT.vitalityHealth, 0, 100),
						in.readFloat("enduranceStamina", DEFAULT.enduranceStamina, 0, 1_000),
						in.readFloat("enduranceStaminaRegen", DEFAULT.enduranceStaminaRegen, 0, 10),
						in.readFloat("enduranceKnockbackResistance", DEFAULT.enduranceKnockbackResistance, 0, 1),
						in.readFloat("strengthDamage", DEFAULT.strengthDamage, 0, 10),
						in.readFloat("dexterityDamage", DEFAULT.dexterityDamage, 0, 10),
						in.readFloat("dexterityAttackSpeed", DEFAULT.dexterityAttackSpeed, 0, 10),
						in.readFloat("kindlingFlaskHeal", DEFAULT.kindlingFlaskHeal, 0, 10),
						in.readFloat("resilienceToughness", DEFAULT.resilienceToughness, 0, 100),
						in.readFloat("resilienceFireReduction", DEFAULT.resilienceFireReduction, 0, 1),
						in.readFloat("resilienceFireReductionCap", DEFAULT.resilienceFireReductionCap, 0, 1),
						in.readFloat("fortuneLuck", DEFAULT.fortuneLuck, 0, 100)),
				(value, out) -> {
					out.addProperty("vitalityHealth", value.vitalityHealth);
					out.addProperty("enduranceStamina", value.enduranceStamina);
					out.addProperty("enduranceStaminaRegen", value.enduranceStaminaRegen);
					out.addProperty("enduranceKnockbackResistance", value.enduranceKnockbackResistance);
					out.addProperty("strengthDamage", value.strengthDamage);
					out.addProperty("dexterityDamage", value.dexterityDamage);
					out.addProperty("dexterityAttackSpeed", value.dexterityAttackSpeed);
					out.addProperty("kindlingFlaskHeal", value.kindlingFlaskHeal);
					out.addProperty("resilienceToughness", value.resilienceToughness);
					out.addProperty("resilienceFireReduction", value.resilienceFireReduction);
					out.addProperty("resilienceFireReductionCap", value.resilienceFireReductionCap);
					out.addProperty("fortuneLuck", value.fortuneLuck);
				}));
	}

	/**
	 * Cost to go from level L to L+1 is {@code costBase + costLinear·L + costQuadratic·L²} XP points (DESIGN §5.3).
	 *
	 * @param statCap  highest value a stat can be levelled to
	 * @param levelCap highest character level reachable by leveling; 0 for none (DESIGN §10.2 is open)
	 */
	public record Leveling(float costBase, float costLinear, float costQuadratic, int statCap, int levelCap) {
		public static final Leveling DEFAULT = new Leveling(20, 6, 0.15f, 99, 0);

		public static final ConfigSection<Leveling> SECTION = EmbervoidConfig.register(ConfigSection.of(
				"leveling", DEFAULT,
				in -> new Leveling(
						in.readFloat("costBase", DEFAULT.costBase, 0, 1_000_000),
						in.readFloat("costLinear", DEFAULT.costLinear, 0, 100_000),
						in.readFloat("costQuadratic", DEFAULT.costQuadratic, 0, 1_000),
						in.readInt("statCap", DEFAULT.statCap, Stat.BASE, 999),
						in.readInt("levelCap", DEFAULT.levelCap, 0, 100_000)),
				(value, out) -> {
					out.addProperty("costBase", value.costBase);
					out.addProperty("costLinear", value.costLinear);
					out.addProperty("costQuadratic", value.costQuadratic);
					out.addProperty("statCap", value.statCap);
					out.addProperty("levelCap", value.levelCap);
				}));

		/** XP points to go from {@code level} to {@code level + 1}, rounded to whole points, capped to an int. */
		public int cost(int level) {
			double l = Math.max(1, level);
			double cost = costBase + costLinear * l + costQuadratic * l * l;
			return (int) Math.min(Integer.MAX_VALUE, Math.round(cost));
		}

		/** XP points spent in total to reach {@code level} from level 1. */
		public long totalCost(int level) {
			long total = 0;
			for (int l = 1; l < level; l++) {
				total += cost(l);
			}
			return total;
		}
	}

	/** Makes sure both sections are registered (call during mod initialization). */
	static void register() {
		java.util.Objects.requireNonNull(Effects.SECTION);
		java.util.Objects.requireNonNull(Leveling.SECTION);
	}

	private StatsConfig() {}
}
