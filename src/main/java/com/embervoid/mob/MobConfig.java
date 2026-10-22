package com.embervoid.mob;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.config.SectionReader;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The {@code mobs} config section: mob levels (DESIGN §5.4, §10.9) and difficulty factors (§3.1).
 *
 * <p>Lives in {@code config/embervoid.json} (synced to clients like the rest of the file). {@code baseLevels} and
 * {@code dimensionBonus} read the keys listed in the defaults plus any the user adds (other mods' entity ids,
 * other dimensions); any hostile not listed uses {@code defaultBaseLevel}.
 *
 * @param levels     how mob levels are assigned and what they scale
 * @param difficulty damage factors that replace vanilla's Easy/Normal/Hard scaling for mob damage to players
 */
public record MobConfig(Levels levels, Difficulty difficulty) {
	public static final MobConfig DEFAULT = new MobConfig(
			new Levels(true, 4,
					Map.ofEntries(
							Map.entry("minecraft:zombie", 3),
							Map.entry("minecraft:zombie_villager", 3),
							Map.entry("minecraft:husk", 3),
							Map.entry("minecraft:drowned", 3),
							Map.entry("minecraft:spider", 3),
							Map.entry("minecraft:skeleton", 4),
							Map.entry("minecraft:stray", 4),
							Map.entry("minecraft:slime", 4),
							Map.entry("minecraft:creeper", 6),
							Map.entry("minecraft:witch", 6),
							Map.entry("minecraft:enderman", 12),
							Map.entry("embervoid:ash_ghoul", 5),
							Map.entry("embervoid:ember_wisp", 8),
							Map.entry("embervoid:hollow", 15),
							// Realm 1 mobs: the bottom of their zone band (DESIGN §4.5); inside the realm the band decides.
							Map.entry("embervoid:cinder_husk", 15),
							Map.entry("embervoid:ash_stalker", 15),
							Map.entry("embervoid:ashen_carrion", 15),
							Map.entry("embervoid:withered_thrall", 18),
							Map.entry("embervoid:lantern_hag", 18),
							Map.entry("embervoid:bark_hollow", 18),
							Map.entry("embervoid:restless_bones", 21),
							Map.entry("embervoid:crypt_moth", 21),
							Map.entry("embervoid:hollow_sentinel", 21),
							Map.entry("embervoid:grave_warden", 21),
							Map.entry("embervoid:cinder_hound", 25),
							Map.entry("embervoid:sparking_marksman", 25),
							Map.entry("embervoid:furnace_golem", 25),
							Map.entry("embervoid:smoldering_knight", 25)),
					1000, 5, 3,
					Map.of("minecraft:the_nether", 5, "minecraft:the_end", 8),
					0.06f, 0.05f, 0.10f),
			new Difficulty(1.0f, 1.25f, 1.75f));

	/**
	 * @param enabled             false: no new levels are assigned and existing ones do nothing
	 * @param defaultBaseLevel    base level of any hostile not in {@code baseLevels} (vanilla or modded)
	 * @param baseLevels          base level by entity type id
	 * @param blocksPerLevel      horizontal distance from world spawn per +1 level (overworld only)
	 * @param maxDistanceBonus    cap for the distance bonus
	 * @param belowZeroBonus      bonus below y=0 (overworld only)
	 * @param dimensionBonus      flat bonus by dimension id
	 * @param healthPerLevel      HP = base × (1 + healthPerLevel·(L − 1))
	 * @param damagePerLevel      damage = base × (1 + damagePerLevel·(L − 1))
	 * @param experiencePerLevel  XP = base × (1 + experiencePerLevel·(L − 1))
	 */
	public record Levels(boolean enabled, int defaultBaseLevel, Map<String, Integer> baseLevels, int blocksPerLevel,
			int maxDistanceBonus, int belowZeroBonus, Map<String, Integer> dimensionBonus, float healthPerLevel,
			float damagePerLevel, float experiencePerLevel) {}

	/** Factors on difficulty-scaled damage to players, per difficulty (vanilla: Easy ×0.5+1, Normal ×1, Hard ×1.5). */
	public record Difficulty(float easy, float normal, float hard) {}

	public static final ConfigSection<MobConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"mobs", DEFAULT, MobConfig::read, MobConfig::write));

	public static MobConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}

	private static MobConfig read(SectionReader in) {
		Levels def = DEFAULT.levels;
		SectionReader levels = in.section("levels");
		boolean enabled = levels.readBoolean("enabled", def.enabled);
		int defaultBaseLevel = levels.readInt("defaultBaseLevel", def.defaultBaseLevel, MobLevelRules.MIN_LEVEL, MobLevelRules.MAX_LEVEL);
		Levels read = new Levels(
				enabled,
				defaultBaseLevel,
				readLevels(levels.section("baseLevels"), def.baseLevels, defaultBaseLevel, MobLevelRules.MIN_LEVEL),
				levels.readInt("blocksPerLevel", def.blocksPerLevel, 0, 1_000_000),
				levels.readInt("maxDistanceBonus", def.maxDistanceBonus, 0, MobLevelRules.MAX_LEVEL),
				levels.readInt("belowZeroBonus", def.belowZeroBonus, 0, MobLevelRules.MAX_LEVEL),
				readLevels(levels.section("dimensionBonus"), def.dimensionBonus, 0, 0),
				levels.readFloat("healthPerLevel", def.healthPerLevel, 0, 10),
				levels.readFloat("damagePerLevel", def.damagePerLevel, 0, 10),
				levels.readFloat("experiencePerLevel", def.experiencePerLevel, 0, 10));
		Difficulty d = DEFAULT.difficulty;
		SectionReader difficulty = in.section("difficulty");
		return new MobConfig(read, new Difficulty(
				difficulty.readFloat("easy", d.easy, 0, 100),
				difficulty.readFloat("normal", d.normal, 0, 100),
				difficulty.readFloat("hard", d.hard, 0, 100)));
	}

	/**
	 * Reads one int per key: every key of {@code defaults} (filled in if missing) plus any key the user added, e.g.
	 * another mod's entity id or dimension. An invalid value of an added key falls back to {@code fallback}.
	 */
	private static Map<String, Integer> readLevels(SectionReader in, Map<String, Integer> defaults, int fallback, int min) {
		Map<String, Integer> result = new LinkedHashMap<>();
		defaults.keySet().stream().sorted().forEach(key ->
				result.put(key, in.readInt(key, defaults.get(key), min, MobLevelRules.MAX_LEVEL)));
		for (String key : in.keys()) {
			if (!result.containsKey(key)) {
				result.put(key, in.readInt(key, fallback, min, MobLevelRules.MAX_LEVEL));
			}
		}
		return Map.copyOf(result);
	}

	private static void write(MobConfig config, JsonObject out) {
		Levels l = config.levels;
		JsonObject levels = new JsonObject();
		levels.addProperty("enabled", l.enabled);
		levels.addProperty("defaultBaseLevel", l.defaultBaseLevel);
		levels.add("baseLevels", writeLevels(l.baseLevels));
		levels.addProperty("blocksPerLevel", l.blocksPerLevel);
		levels.addProperty("maxDistanceBonus", l.maxDistanceBonus);
		levels.addProperty("belowZeroBonus", l.belowZeroBonus);
		levels.add("dimensionBonus", writeLevels(l.dimensionBonus));
		levels.addProperty("healthPerLevel", l.healthPerLevel);
		levels.addProperty("damagePerLevel", l.damagePerLevel);
		levels.addProperty("experiencePerLevel", l.experiencePerLevel);
		JsonObject difficulty = new JsonObject();
		difficulty.addProperty("easy", config.difficulty.easy);
		difficulty.addProperty("normal", config.difficulty.normal);
		difficulty.addProperty("hard", config.difficulty.hard);
		out.add("levels", levels);
		out.add("difficulty", difficulty);
	}

	private static JsonObject writeLevels(Map<String, Integer> levels) {
		JsonObject json = new JsonObject();
		levels.keySet().stream().sorted().forEach(key -> json.addProperty(key, levels.get(key)));
		return json;
	}
}
