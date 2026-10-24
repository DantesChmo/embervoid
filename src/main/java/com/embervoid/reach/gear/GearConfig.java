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
 * @param crossbow   Huntsman's Crossbow
 * @param consumables bombs, resin, balm, lantern oil, whetstone, fuel and the Flask Ember
 * @param loot       realm loot on the realm's mobs (chest contents are datapack loot tables)
 */
public record GearConfig(Bleeding bleeding, Smoldering smoldering, Sets sets, Uniques uniques, Weapons weapons,
		Crossbow crossbow, Consumables consumables, Loot loot) {
	private static final GearConfig DEFAULTS = new GearConfig(Bleeding.DEFAULTS, Smoldering.DEFAULTS, Sets.DEFAULTS,
			Uniques.DEFAULTS, Weapons.DEFAULTS, Crossbow.DEFAULTS, Consumables.DEFAULTS, Loot.DEFAULTS);

	public static final ConfigSection<GearConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"gear", DEFAULTS,
			in -> new GearConfig(Bleeding.read(in.section("bleeding")), Smoldering.read(in.section("smoldering")),
					Sets.read(in.section("sets")), Uniques.read(in.section("uniques")), Weapons.read(in.section("weapons")),
					Crossbow.read(in.section("crossbow")), Consumables.read(in.section("consumables")),
					Loot.read(in.section("loot"))),
			(value, out) -> {
				out.add("bleeding", value.bleeding.write());
				out.add("smoldering", value.smoldering.write());
				out.add("sets", value.sets.write());
				out.add("uniques", value.uniques.write());
				out.add("weapons", value.weapons.write());
				out.add("crossbow", value.crossbow.write());
				out.add("consumables", value.consumables.write());
				out.add("loot", value.loot.write());
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
	 * @param sweepStamina      Ashen Scythe: stamina given back per enemy a sweep or Harvest catches
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
	 * @param rampPerHit        Royal Ash Rapier: damage added per full-charge hit in a row on the same target
	 * @param rampMax           Royal Ash Rapier: the most the ramp adds
	 * @param rampWindowTicks   Royal Ash Rapier: longest gap between two hits that keeps the ramp
	 * @param whipTipBand       Warden's Scourge: the tip, the last blocks of the lash, that cracks
	 * @param whipTipMultiplier Warden's Scourge: damage of a hit with the tip
	 * @param whipTipStunTicks  Warden's Scourge: a hit with the tip stops the mob (its wind-up broken) for this long
	 */
	public record Weapons(float kindlingPerPoint, float sweepRadius, float sweepShare, float sweepStamina, float waveLength,
			float waveWidth, float waveDamage, float harvestRadius, float harvestStamina, int harvestCooldown, float lashPull,
			float lashStamina, int lashCooldown, float lungeDash, float lungeStamina, int lungeCooldown, float rampPerHit,
			float rampMax, int rampWindowTicks, float whipTipBand, float whipTipMultiplier, int whipTipStunTicks) {
		static final Weapons DEFAULTS = new Weapons(0.02f, 2.5f, 0.75f, 3, 6, 3, 3, 3.5f, 40, 10, 4, 30, 8, 4, 30, 6,
				0.1f, 0.3f, 30, 1.5f, 1.6f, 10);

		static Weapons read(SectionReader in) {
			return new Weapons(
					in.readFloat("kindlingPerPoint", DEFAULTS.kindlingPerPoint, 0, 1),
					in.readFloat("sweepRadius", DEFAULTS.sweepRadius, 0, 8),
					in.readFloat("sweepShare", DEFAULTS.sweepShare, 0, 2),
					in.readFloat("sweepStamina", DEFAULTS.sweepStamina, 0, 1000),
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
					in.readInt("lungeCooldown", DEFAULTS.lungeCooldown, 0, 1200),
					in.readFloat("rampPerHit", DEFAULTS.rampPerHit, 0, 2),
					in.readFloat("rampMax", DEFAULTS.rampMax, 0, 10),
					in.readInt("rampWindowTicks", DEFAULTS.rampWindowTicks, 1, 1200),
					in.readFloat("whipTipBand", DEFAULTS.whipTipBand, 0, 6),
					in.readFloat("whipTipMultiplier", DEFAULTS.whipTipMultiplier, 0, 10),
					in.readInt("whipTipStunTicks", DEFAULTS.whipTipStunTicks, 0, 100));
		}

		JsonObject write() {
			JsonObject out = new JsonObject();
			out.addProperty("kindlingPerPoint", kindlingPerPoint);
			out.addProperty("sweepRadius", sweepRadius);
			out.addProperty("sweepShare", sweepShare);
			out.addProperty("sweepStamina", sweepStamina);
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
			out.addProperty("rampPerHit", rampPerHit);
			out.addProperty("rampMax", rampMax);
			out.addProperty("rampWindowTicks", rampWindowTicks);
			out.addProperty("whipTipBand", whipTipBand);
			out.addProperty("whipTipMultiplier", whipTipMultiplier);
			out.addProperty("whipTipStunTicks", whipTipStunTicks);
			return out;
		}
	}

	/**
	 * @param chargeTicks      charge time without Quick Charge (vanilla 25)
	 * @param boltDamage       damage of a bolt hitting at its full launch speed (a vanilla crossbow's is 7, plus the
	 *                         crit roll ours do not get)
	 * @param punch            knockback level of the bolts (Punch)
	 * @param pierce           extra mobs a bolt passes through
	 * @param openerMultiplier damage of a bolt into a mob that is not targeting the shooter
	 */
	public record Crossbow(int chargeTicks, float boltDamage, int punch, int pierce, float openerMultiplier) {
		static final Crossbow DEFAULTS = new Crossbow(32, 16, 3, 2, 1.5f);

		static Crossbow read(SectionReader in) {
			return new Crossbow(
					in.readInt("chargeTicks", DEFAULTS.chargeTicks, 5, 200),
					in.readFloat("boltDamage", DEFAULTS.boltDamage, 0, 200),
					in.readInt("punch", DEFAULTS.punch, 0, 10),
					in.readInt("pierce", DEFAULTS.pierce, 0, 10),
					in.readFloat("openerMultiplier", DEFAULTS.openerMultiplier, 0, 10));
		}

		JsonObject write() {
			JsonObject out = new JsonObject();
			out.addProperty("chargeTicks", chargeTicks);
			out.addProperty("boltDamage", boltDamage);
			out.addProperty("punch", punch);
			out.addProperty("pierce", pierce);
			out.addProperty("openerMultiplier", openerMultiplier);
			return out;
		}
	}

	/**
	 * @param bombRadius       Ash Bomb: radius of the fire ring
	 * @param bombDamage       Ash Bomb: ember damage
	 * @param bombFireSeconds  Ash Bomb: seconds the mobs it catches burn
	 * @param resinSeconds     Smoldering Resin: how long a coated weapon ignites what it hits
	 * @param resinFireSeconds Smoldering Resin: seconds a target burns
	 * @param balmRegenTicks   Amber Balm: Regeneration I duration
	 * @param oilMinutes       Amber Lantern Oil: burn time of a lantern
	 * @param whetstoneShare   Whetstone: share of max durability repaired
	 * @param cinderFuelTicks  Cinder Shard as furnace fuel (coal is 1600); read at startup
	 * @param flaskEmberBonus  Flask Ember: extra flask healing
	 */
	public record Consumables(float bombRadius, float bombDamage, int bombFireSeconds, int resinSeconds, int resinFireSeconds,
			int balmRegenTicks, int oilMinutes, float whetstoneShare, int cinderFuelTicks, float flaskEmberBonus) {
		static final Consumables DEFAULTS = new Consumables(2, 4, 3, 60, 4, 160, 10, 0.25f, 3200, 0.15f);

		static Consumables read(SectionReader in) {
			return new Consumables(
					in.readFloat("bombRadius", DEFAULTS.bombRadius, 0.5f, 8),
					in.readFloat("bombDamage", DEFAULTS.bombDamage, 0, 100),
					in.readInt("bombFireSeconds", DEFAULTS.bombFireSeconds, 0, 60),
					in.readInt("resinSeconds", DEFAULTS.resinSeconds, 1, 3600),
					in.readInt("resinFireSeconds", DEFAULTS.resinFireSeconds, 1, 60),
					in.readInt("balmRegenTicks", DEFAULTS.balmRegenTicks, 0, 6000),
					in.readInt("oilMinutes", DEFAULTS.oilMinutes, 1, 600),
					in.readFloat("whetstoneShare", DEFAULTS.whetstoneShare, 0, 1),
					in.readInt("cinderFuelTicks", DEFAULTS.cinderFuelTicks, 0, 32000),
					in.readFloat("flaskEmberBonus", DEFAULTS.flaskEmberBonus, 0, 2));
		}

		JsonObject write() {
			JsonObject out = new JsonObject();
			out.addProperty("bombRadius", bombRadius);
			out.addProperty("bombDamage", bombDamage);
			out.addProperty("bombFireSeconds", bombFireSeconds);
			out.addProperty("resinSeconds", resinSeconds);
			out.addProperty("resinFireSeconds", resinFireSeconds);
			out.addProperty("balmRegenTicks", balmRegenTicks);
			out.addProperty("oilMinutes", oilMinutes);
			out.addProperty("whetstoneShare", whetstoneShare);
			out.addProperty("cinderFuelTicks", cinderFuelTicks);
			out.addProperty("flaskEmberBonus", flaskEmberBonus);
			return out;
		}
	}

	/**
	 * Chances are read when loot tables load (server start, {@code /reload}).
	 *
	 * @param fodderMaterial chance of the zone material from a natural mob
	 * @param eliteMaterial  chance of 1–2 of the zone material from a placed tank or elite
	 * @param fodderSet      chance of a set piece from a natural mob, player kills only
	 * @param eliteSet       chance of a set piece from a placed tank or elite
	 * @param fodderUnique   chance of the zone's unique piece from its mobs
	 * @param eliteUnique    chance of the zone's unique piece from its elite
	 * @param lootingBonus   added chance per Looting level (a quarter of it for unique pieces)
	 */
	public record Loot(float fodderMaterial, float eliteMaterial, float fodderSet, float eliteSet, float fodderUnique,
			float eliteUnique, float lootingBonus) {
		static final Loot DEFAULTS = new Loot(0.10f, 0.5f, 0.03f, 0.15f, 0.005f, 0.04f, 0.01f);

		static Loot read(SectionReader in) {
			return new Loot(
					in.readFloat("fodderMaterial", DEFAULTS.fodderMaterial, 0, 1),
					in.readFloat("eliteMaterial", DEFAULTS.eliteMaterial, 0, 1),
					in.readFloat("fodderSet", DEFAULTS.fodderSet, 0, 1),
					in.readFloat("eliteSet", DEFAULTS.eliteSet, 0, 1),
					in.readFloat("fodderUnique", DEFAULTS.fodderUnique, 0, 1),
					in.readFloat("eliteUnique", DEFAULTS.eliteUnique, 0, 1),
					in.readFloat("lootingBonus", DEFAULTS.lootingBonus, 0, 1));
		}

		JsonObject write() {
			JsonObject out = new JsonObject();
			out.addProperty("fodderMaterial", fodderMaterial);
			out.addProperty("eliteMaterial", eliteMaterial);
			out.addProperty("fodderSet", fodderSet);
			out.addProperty("eliteSet", eliteSet);
			out.addProperty("fodderUnique", fodderUnique);
			out.addProperty("eliteUnique", eliteUnique);
			out.addProperty("lootingBonus", lootingBonus);
			return out;
		}
	}
}
