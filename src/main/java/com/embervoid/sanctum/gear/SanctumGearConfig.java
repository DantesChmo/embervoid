package com.embervoid.sanctum.gear;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;

/**
 * {@code sanctumGear.*} in {@code config/embervoid.json}: the Sanctum weapons' special attacks (DESIGN §10.10).
 * Damage multipliers apply to the weapon's own hit (its attack damage), which then goes through the normal
 * {@code damage()} call, so armor, mob levels and STR/DEX scaling still apply.
 *
 * @param cleaveRange           Wide Cleave: reach of the arc, blocks
 * @param cleaveArc             Wide Cleave: width of the arc in front, degrees
 * @param cleaveMultiplier      Wide Cleave: share of the weapon's hit each target takes
 * @param cleaveStamina         Wide Cleave: stamina cost
 * @param cleaveCooldown        Wide Cleave: cooldown, ticks
 * @param slamRadius            Ground Slam: radius of the ring around the player, blocks
 * @param slamMultiplier        Ground Slam: share of the weapon's hit each target takes
 * @param slamKnockback         Ground Slam: knockback strength (vanilla's sword knockback is 0.4)
 * @param slamSlownessTicks     Ground Slam: Slowness duration, ticks
 * @param slamSlownessAmplifier Ground Slam: Slowness amplifier (0 = Slowness I)
 * @param slamStamina           Ground Slam: stamina cost
 * @param slamCooldown          Ground Slam: cooldown, ticks
 */
public record SanctumGearConfig(float cleaveRange, float cleaveArc, float cleaveMultiplier, float cleaveStamina,
		int cleaveCooldown, float slamRadius, float slamMultiplier, float slamKnockback, int slamSlownessTicks,
		int slamSlownessAmplifier, float slamStamina, int slamCooldown) {
	private static final SanctumGearConfig DEFAULTS = new SanctumGearConfig(3, 90, 1.25f, 35, 8, 3, 0.75f, 0.8f, 40, 1, 45, 10);

	public static final ConfigSection<SanctumGearConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"sanctumGear", DEFAULTS,
			in -> new SanctumGearConfig(
					in.readFloat("cleaveRange", DEFAULTS.cleaveRange, 1, 8),
					in.readFloat("cleaveArc", DEFAULTS.cleaveArc, 10, 360),
					in.readFloat("cleaveMultiplier", DEFAULTS.cleaveMultiplier, 0, 10),
					in.readFloat("cleaveStamina", DEFAULTS.cleaveStamina, 0, 1000),
					in.readInt("cleaveCooldown", DEFAULTS.cleaveCooldown, 0, 1200),
					in.readFloat("slamRadius", DEFAULTS.slamRadius, 1, 8),
					in.readFloat("slamMultiplier", DEFAULTS.slamMultiplier, 0, 10),
					in.readFloat("slamKnockback", DEFAULTS.slamKnockback, 0, 4),
					in.readInt("slamSlownessTicks", DEFAULTS.slamSlownessTicks, 0, 1200),
					in.readInt("slamSlownessAmplifier", DEFAULTS.slamSlownessAmplifier, 0, 9),
					in.readFloat("slamStamina", DEFAULTS.slamStamina, 0, 1000),
					in.readInt("slamCooldown", DEFAULTS.slamCooldown, 0, 1200)),
			(value, out) -> {
				out.addProperty("cleaveRange", value.cleaveRange);
				out.addProperty("cleaveArc", value.cleaveArc);
				out.addProperty("cleaveMultiplier", value.cleaveMultiplier);
				out.addProperty("cleaveStamina", value.cleaveStamina);
				out.addProperty("cleaveCooldown", value.cleaveCooldown);
				out.addProperty("slamRadius", value.slamRadius);
				out.addProperty("slamMultiplier", value.slamMultiplier);
				out.addProperty("slamKnockback", value.slamKnockback);
				out.addProperty("slamSlownessTicks", value.slamSlownessTicks);
				out.addProperty("slamSlownessAmplifier", value.slamSlownessAmplifier);
				out.addProperty("slamStamina", value.slamStamina);
				out.addProperty("slamCooldown", value.slamCooldown);
			}));

	public static SanctumGearConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}
}
