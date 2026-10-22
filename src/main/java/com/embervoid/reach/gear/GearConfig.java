package com.embervoid.reach.gear;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.config.SectionReader;
import com.google.gson.JsonObject;

/**
 * {@code gear.*} in {@code config/embervoid.json}: Ashen Reach gear (DESIGN §4.5 "Loot", PLAN 11.1–11.8). Numbers are
 * starting values for the balance pass.
 *
 * @param bleeding the Bleeding status effect
 */
public record GearConfig(Bleeding bleeding) {
	private static final GearConfig DEFAULTS = new GearConfig(Bleeding.DEFAULTS);

	public static final ConfigSection<GearConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"gear", DEFAULTS,
			in -> new GearConfig(Bleeding.read(in.section("bleeding"))),
			(value, out) -> out.add("bleeding", value.bleeding.write())));

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
}
