package com.embervoid.stats;

/**
 * What a set of stat values gives, as absolute numbers or bonuses over vanilla. Pure: computed from the stat values
 * and the config only, so the level-up screen can preview "what if this stat were one higher".
 *
 * @param maxHealth             max health, half-hearts (vanilla 20)
 * @param knockbackResistance   added knockback resistance (0–1 scale)
 * @param attackSpeedBonus      attack speed bonus, as a fraction (0.1 = +10%)
 * @param armorToughness        added armor toughness
 * @param luck                  added luck
 * @param fireDamageReduction   fraction of fire damage removed (0–1)
 * @param flaskHealMultiplier   multiplier on Healing Flask heal (1 = unchanged)
 * @param strengthDamageBonus   damage bonus with a fully STR-scaling weapon, as a fraction
 * @param dexterityDamageBonus  damage bonus with a fully DEX-scaling weapon, as a fraction
 * @param extraDropChance       chance (0–1) that a mob this player kills drops its loot a second time
 */
public record DerivedStats(float maxHealth, float maxStamina, float staminaRegenPerTick, double knockbackResistance,
		double attackSpeedBonus, double armorToughness, double luck, float fireDamageReduction,
		float flaskHealMultiplier, float strengthDamageBonus, float dexterityDamageBonus, float extraDropChance) {
	/** Vanilla player max health. */
	public static final float BASE_HEALTH = 20;

	/**
	 * @param stats        stat values, indexed by {@link Stat#ordinal()}
	 * @param effects      per-point amounts
	 * @param baseStamina  stamina pool at END 10 ({@code stamina.max})
	 * @param baseRegen    regen per tick at END 10 ({@code stamina.regenPerTick})
	 */
	public static DerivedStats of(int[] stats, StatsConfig.Effects effects, int baseStamina, float baseRegen) {
		double vit = StatCurve.effectivePoints(stats[Stat.VITALITY.ordinal()]);
		double end = StatCurve.effectivePoints(stats[Stat.ENDURANCE.ordinal()]);
		double str = StatCurve.effectivePoints(stats[Stat.STRENGTH.ordinal()]);
		double dex = StatCurve.effectivePoints(stats[Stat.DEXTERITY.ordinal()]);
		double kin = StatCurve.effectivePoints(stats[Stat.KINDLING.ordinal()]);
		double res = StatCurve.effectivePoints(stats[Stat.RESILIENCE.ordinal()]);
		double fortune = StatCurve.effectivePoints(stats[Stat.FORTUNE.ordinal()]);
		return new DerivedStats(
				(float) Math.max(1, BASE_HEALTH + vit * effects.vitalityHealth()),
				(float) Math.max(1, baseStamina + end * effects.enduranceStamina()),
				(float) Math.max(0, baseRegen * (1 + end * effects.enduranceStaminaRegen())),
				end * effects.enduranceKnockbackResistance(),
				dex * effects.dexterityAttackSpeed(),
				res * effects.resilienceToughness(),
				fortune * effects.fortuneLuck(),
				(float) Math.max(0, Math.min(effects.resilienceFireReductionCap(), res * effects.resilienceFireReduction())),
				(float) Math.max(0, 1 + kin * effects.kindlingFlaskHeal()),
				(float) (str * effects.strengthDamage()),
				(float) (dex * effects.dexterityDamage()),
				(float) Math.max(0, Math.min(effects.fortuneExtraDropCap(), fortune * effects.fortuneExtraDrop())));
	}
}
