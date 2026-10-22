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
 * @param weapons    the unique weapons' passives and special attacks (base damage, speed and reach are in code)
 */
public record GearConfig(Bleeding bleeding, Smoldering smoldering, Sets sets, Uniques uniques, Weapons weapons) {
	private static final GearConfig DEFAULTS = new GearConfig(Bleeding.DEFAULTS, Smoldering.DEFAULTS, Sets.DEFAULTS,
			Uniques.DEFAULTS, Weapons.DEFAULTS);

	public static final ConfigSection<GearConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"gear", DEFAULTS,
			in -> new GearConfig(Bleeding.read(in.section("bleeding")), Smoldering.read(in.section("smoldering")),
					Sets.read(in.section("sets")), Uniques.read(in.section("uniques")), Weapons.read(in.section("weapons"))),
			(value, out) -> {
				out.add("bleeding", value.bleeding.write());
				out.add("smoldering", value.smoldering.write());
				out.add("sets", value.sets.write());
				out.add("uniques", value.uniques.write());
				out.add("weapons", value.weapons.write());
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

	/**
	 * @param kindlingPerPoint  ember damage bonus per effective KIN point above 10 (like STR/DEX's 2 %)
	 * @param sweepRadius       Ashen Scythe: sweep radius around the target
	 * @param sweepShare        Ashen Scythe: share of the hit the sweep deals
	 * @param waveLength        Ashen Scythe: ash wave length from a fully charged hit
	 * @param waveWidth         Ashen Scythe: ash wave width
	 * @param waveDamage        Ashen Scythe: ash wave ember damage
	 * @param harvestRadius     Harvest: spin radius
	 * @param harvestStamina    Harvest: stamina cost
	 * @param harvestCooldown   Harvest: cooldown, ticks
	 * @param lashPull          Lash: blocks the target is pulled
	 * @param lashStamina       Lash: stamina cost
	 * @param lashCooldown      Lash: cooldown, ticks
	 * @param lungeDash         Lunge: dash length, blocks
	 * @param lungeStamina      Lunge: stamina cost
	 * @param lungeCooldown     Lunge: cooldown, ticks
	 */
	public record Weapons(float kindlingPerPoint, float sweepRadius, float sweepShare, float waveLength, float waveWidth,
			float waveDamage, float harvestRadius, float harvestStamina, int harvestCooldown, float lashPull,
			float lashStamina, int lashCooldown, float lungeDash, float lungeStamina, int lungeCooldown) {
		static final Weapons DEFAULTS = new Weapons(0.02f, 2.5f, 0.75f, 6, 3, 3, 3.5f, 25, 80, 4, 20, 100, 4, 20, 60);

		static Weapons read(SectionReader in) {
			return new Weapons(
					in.readFloat("kindlingPerPoint", DEFAULTS.kindlingPerPoint, 0, 1),
					in.readFloat("sweepRadius", DEFAULTS.sweepRadius, 0, 8),
					in.readFloat("sweepShare", DEFAULTS.sweepShare, 0, 2),
					in.readFloat("waveLength", DEFAULTS.waveLength, 0, 16),
					in.readFloat("waveWidth", DEFAULTS.waveWidth, 0, 8),
					in.readFloat("waveDamage", DEFAULTS.waveDamage, 0, 100),
					in.readFloat("harvestRadius", DEFAULTS.harvestRadius, 1, 8),
					in.readFloat("harvestStamina", DEFAULTS.harvestStamina, 0, 1000),
					in.readInt("harvestCooldown", DEFAULTS.harvestCooldown, 0, 1200),
					in.readFloat("lashPull", DEFAULTS.lashPull, 0, 12),
					in.readFloat("lashStamina", DEFAULTS.lashStamina, 0, 1000),
					in.readInt("lashCooldown", DEFAULTS.lashCooldown, 0, 1200),
					in.readFloat("lungeDash", DEFAULTS.lungeDash, 0, 12),
					in.readFloat("lungeStamina", DEFAULTS.lungeStamina, 0, 1000),
					in.readInt("lungeCooldown", DEFAULTS.lungeCooldown, 0, 1200));
		}

		JsonObject write() {
			JsonObject out = new JsonObject();
			out.addProperty("kindlingPerPoint", kindlingPerPoint);
			out.addProperty("sweepRadius", sweepRadius);
			out.addProperty("sweepShare", sweepShare);
			out.addProperty("waveLength", waveLength);
			out.addProperty("waveWidth", waveWidth);
			out.addProperty("waveDamage", waveDamage);
			out.addProperty("harvestRadius", harvestRadius);
			out.addProperty("harvestStamina", harvestStamina);
			out.addProperty("harvestCooldown", harvestCooldown);
			out.addProperty("lashPull", lashPull);
			out.addProperty("lashStamina", lashStamina);
			out.addProperty("lashCooldown", lashCooldown);
			out.addProperty("lungeDash", lungeDash);
			out.addProperty("lungeStamina", lungeStamina);
			out.addProperty("lungeCooldown", lungeCooldown);
			return out;
		}
	}
}
