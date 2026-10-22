package com.embervoid.reach.gear;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.config.SectionReader;
import com.google.gson.JsonObject;

/**
 * {@code gear.*} in {@code config/embervoid.json}: Ashen Reach gear (DESIGN §4.5 "Loot", PLAN 11.1–11.8). Numbers are
 * starting values for the balance pass.
 *
 * @param bleeding   the Bleeding status effect
 * @param smoldering the Smoldering Plate
 * @param sets       the mob-drop set bonuses
 * @param uniques    the unique armor pieces
 */
public record GearConfig(Bleeding bleeding, Smoldering smoldering, Sets sets, Uniques uniques) {
	private static final GearConfig DEFAULTS = new GearConfig(Bleeding.DEFAULTS, Smoldering.DEFAULTS, Sets.DEFAULTS,
			Uniques.DEFAULTS);

	public static final ConfigSection<GearConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"gear", DEFAULTS,
			in -> new GearConfig(Bleeding.read(in.section("bleeding")), Smoldering.read(in.section("smoldering")),
					Sets.read(in.section("sets")), Uniques.read(in.section("uniques"))),
			(value, out) -> {
				out.add("bleeding", value.bleeding.write());
				out.add("smoldering", value.smoldering.write());
				out.add("sets", value.sets.write());
				out.add("uniques", value.uniques.write());
			}));

	public static GearConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}

	/**
	 * @param durationTicks       how long Bleeding lasts; every new stack refreshes it
	 * @param maxLevel            highest Bleeding level (I–III)
	 * @param damagePerLevel      damage per second per level, ignoring armor
	 */
	public record Bleeding(int durationTicks, int maxLevel, float damagePerLevel) {
		static final Bleeding DEFAULTS = new Bleeding(120, 3, 0.5f);

		static Bleeding read(SectionReader in) {
			return new Bleeding(
					in.readInt("durationTicks", DEFAULTS.durationTicks, 20, 6000),
					in.readInt("maxLevel", DEFAULTS.maxLevel, 1, 10),
					in.readFloat("damagePerLevel", DEFAULTS.damagePerLevel, 0, 20));
		}

		JsonObject write() {
			JsonObject out = new JsonObject();
			out.addProperty("durationTicks", durationTicks);
			out.addProperty("maxLevel", maxLevel);
			out.addProperty("damagePerLevel", damagePerLevel);
			return out;
		}
	}

	/** @param fireReductionPerPiece share of fire damage each worn piece takes away */
	public record Smoldering(float fireReductionPerPiece) {
		static final Smoldering DEFAULTS = new Smoldering(0.08f);

		static Smoldering read(SectionReader in) {
			return new Smoldering(in.readFloat("fireReductionPerPiece", DEFAULTS.fireReductionPerPiece, 0, 0.25f));
		}

		JsonObject write() {
			JsonObject out = new JsonObject();
			out.addProperty("fireReductionPerPiece", fireReductionPerPiece);
			return out;
		}
	}

	/**
	 * @param reaperSpeed            Reaper's Garb: move speed bonus (share of base)
	 * @param huntsmanProjectile     Huntsman's Leathers: projectile damage bonus
	 * @param wardenKnockbackResist  Necropolis Warden: knockback resistance added
	 * @param wardenBlockingReduction Necropolis Warden: share of damage taken away while blocking with a shield
	 */
	public record Sets(float reaperSpeed, float huntsmanProjectile, float wardenKnockbackResist, float wardenBlockingReduction) {
		static final Sets DEFAULTS = new Sets(0.10f, 0.15f, 0.3f, 0.15f);

		static Sets read(SectionReader in) {
			return new Sets(
					in.readFloat("reaperSpeed", DEFAULTS.reaperSpeed, 0, 1),
					in.readFloat("huntsmanProjectile", DEFAULTS.huntsmanProjectile, 0, 2),
					in.readFloat("wardenKnockbackResist", DEFAULTS.wardenKnockbackResist, 0, 1),
					in.readFloat("wardenBlockingReduction", DEFAULTS.wardenBlockingReduction, 0, 1));
		}

		JsonObject write() {
			JsonObject out = new JsonObject();
			out.addProperty("reaperSpeed", reaperSpeed);
			out.addProperty("huntsmanProjectile", huntsmanProjectile);
			out.addProperty("wardenKnockbackResist", wardenKnockbackResist);
			out.addProperty("wardenBlockingReduction", wardenBlockingReduction);
			return out;
		}
	}

	/**
	 * @param scarecrowDetection    Scarecrow's Mask: share of the usual distance at which mobs notice the wearer
	 * @param greavesSpeedTicks     Huntsman's Greaves: Speed I after a kill, ticks
	 * @param greavesSafeFallDamage Huntsman's Greaves: fall damage taken away (4 = no damage from up to 7 blocks)
	 * @param novaHealthFraction    Furnace Heart Cuirass: the nova goes off below this share of max health
	 * @param novaRadius            Furnace Heart Cuirass: nova radius
	 * @param novaDamage            Furnace Heart Cuirass: ember damage to each mob in the nova
	 * @param novaCooldownTicks     Furnace Heart Cuirass: ticks between two novas
	 */
	public record Uniques(float scarecrowDetection, int greavesSpeedTicks, float greavesSafeFallDamage,
			float novaHealthFraction, float novaRadius, float novaDamage, int novaCooldownTicks) {
		static final Uniques DEFAULTS = new Uniques(0.5f, 80, 4, 0.3f, 4, 6, 1200);

		static Uniques read(SectionReader in) {
			return new Uniques(
					in.readFloat("scarecrowDetection", DEFAULTS.scarecrowDetection, 0, 1),
					in.readInt("greavesSpeedTicks", DEFAULTS.greavesSpeedTicks, 0, 1200),
					in.readFloat("greavesSafeFallDamage", DEFAULTS.greavesSafeFallDamage, 0, 100),
					in.readFloat("novaHealthFraction", DEFAULTS.novaHealthFraction, 0, 1),
					in.readFloat("novaRadius", DEFAULTS.novaRadius, 1, 16),
					in.readFloat("novaDamage", DEFAULTS.novaDamage, 0, 100),
					in.readInt("novaCooldownTicks", DEFAULTS.novaCooldownTicks, 20, 72000));
		}

		JsonObject write() {
			JsonObject out = new JsonObject();
			out.addProperty("scarecrowDetection", scarecrowDetection);
			out.addProperty("greavesSpeedTicks", greavesSpeedTicks);
			out.addProperty("greavesSafeFallDamage", greavesSafeFallDamage);
			out.addProperty("novaHealthFraction", novaHealthFraction);
			out.addProperty("novaRadius", novaRadius);
			out.addProperty("novaDamage", novaDamage);
			out.addProperty("novaCooldownTicks", novaCooldownTicks);
			return out;
		}
	}
}
