package com.embervoid.reliquary;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.config.SectionReader;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The {@code reliquary} config section (DESIGN §4.5).
 *
 * @param slots     slots per {@link SlotKind#name()}; 0 hides a kind. Lowering a count hands the items in the lost
 *                  slots back to the player on their next join
 * @param artifacts stat points the artifacts give
 * @param rings     ring numbers
 * @param drops     accessory drop chances
 */
public record ReliquaryConfig(Map<String, Integer> slots, Artifacts artifacts, Rings rings, Drops drops) {
	/**
	 * @param points     points of a one-stat artifact (DESIGN §4.5: +3)
	 * @param sealPoints points per stat of the Seal of the Ashen King (+1 to every stat)
	 */
	public record Artifacts(int points, int sealPoints) {}

	/**
	 * @param courierSprintBonus  Courier's Ring: extra sprint speed (0.15 = +15 %)
	 * @param featherSafeFall     Ring of Ash Feathers: falls up to this many blocks do no damage (vanilla 3)
	 * @param featherMaxFallSpeed Ring of Ash Feathers: fastest fall, blocks per tick (vanilla ~3.9)
	 * @param fogStalkerClearing  Ring of the Fog Stalker: realm fog starts and ends this many times further out
	 * @param delverHasteLevel    Delver's Ring: Haste level while mining realm ores
	 * @param embersFireSeconds   Ring of Embers: seconds a melee hit sets the target on fire
	 */
	public record Rings(float courierSprintBonus, float featherSafeFall, float featherMaxFallSpeed,
			float fogStalkerClearing, int delverHasteLevel, int embersFireSeconds) {}

	/**
	 * Chances of one accessory of the zone (DESIGN §4.5), per treasure chest and per elite killed by a player.
	 *
	 * @param chestArtifact artifact per treasure chest (8 %)
	 * @param chestRing     ring per treasure chest (5 %)
	 * @param eliteArtifact artifact per elite (2 %)
	 * @param eliteRing     ring per elite (1 %)
	 */
	public record Drops(float chestArtifact, float chestRing, float eliteArtifact, float eliteRing) {}

	/** Most slots of one kind: the screen has room for three columns of three on each side of the player model. */
	public static final int MAX_SLOTS_PER_KIND = 8;

	public static final ReliquaryConfig DEFAULT = new ReliquaryConfig(defaultSlots(), new Artifacts(3, 1),
			new Rings(0.15f, 8, 0.5f, 2, 1, 2),
			new Drops(0.08f, 0.05f, 0.02f, 0.01f));

	public static final ConfigSection<ReliquaryConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"reliquary", DEFAULT, ReliquaryConfig::read, ReliquaryConfig::write));

	public static ReliquaryConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}

	public int slots(SlotKind kind) {
		return slots.getOrDefault(kind.name(), kind.defaultCount());
	}

	private static Map<String, Integer> defaultSlots() {
		Map<String, Integer> slots = new LinkedHashMap<>();
		for (SlotKind kind : SlotKinds.ALL) {
			slots.put(kind.name(), kind.defaultCount());
		}
		return Map.copyOf(slots);
	}

	private static ReliquaryConfig read(SectionReader in) {
		SectionReader slotsIn = in.section("slots");
		Map<String, Integer> slots = new LinkedHashMap<>();
		for (SlotKind kind : SlotKinds.ALL) {
			slots.put(kind.name(), slotsIn.readInt(kind.name(), kind.defaultCount(), 0, MAX_SLOTS_PER_KIND));
		}
		SectionReader artifacts = in.section("artifacts");
		return new ReliquaryConfig(Map.copyOf(slots), new Artifacts(
				artifacts.readInt("points", DEFAULT.artifacts.points, 0, 99),
				artifacts.readInt("sealPoints", DEFAULT.artifacts.sealPoints, 0, 99)),
				readRings(in.section("rings")),
				readDrops(in.section("drops")));
	}

	private static Drops readDrops(SectionReader in) {
		Drops def = DEFAULT.drops;
		return new Drops(
				in.readFloat("chestArtifact", def.chestArtifact, 0, 1),
				in.readFloat("chestRing", def.chestRing, 0, 1),
				in.readFloat("eliteArtifact", def.eliteArtifact, 0, 1),
				in.readFloat("eliteRing", def.eliteRing, 0, 1));
	}

	private static Rings readRings(SectionReader in) {
		Rings def = DEFAULT.rings;
		return new Rings(
				in.readFloat("courierSprintBonus", def.courierSprintBonus, 0, 2),
				in.readFloat("featherSafeFall", def.featherSafeFall, 3, 64),
				in.readFloat("featherMaxFallSpeed", def.featherMaxFallSpeed, 0.05f, 4),
				in.readFloat("fogStalkerClearing", def.fogStalkerClearing, 1, 8),
				in.readInt("delverHasteLevel", def.delverHasteLevel, 0, 5),
				in.readInt("embersFireSeconds", def.embersFireSeconds, 0, 60));
	}

	private static void write(ReliquaryConfig value, JsonObject out) {
		JsonObject slots = new JsonObject();
		for (SlotKind kind : SlotKinds.ALL) {
			slots.addProperty(kind.name(), value.slots(kind));
		}
		out.add("slots", slots);
		JsonObject artifacts = new JsonObject();
		artifacts.addProperty("points", value.artifacts.points);
		artifacts.addProperty("sealPoints", value.artifacts.sealPoints);
		out.add("artifacts", artifacts);
		JsonObject rings = new JsonObject();
		rings.addProperty("courierSprintBonus", value.rings.courierSprintBonus);
		rings.addProperty("featherSafeFall", value.rings.featherSafeFall);
		rings.addProperty("featherMaxFallSpeed", value.rings.featherMaxFallSpeed);
		rings.addProperty("fogStalkerClearing", value.rings.fogStalkerClearing);
		rings.addProperty("delverHasteLevel", value.rings.delverHasteLevel);
		rings.addProperty("embersFireSeconds", value.rings.embersFireSeconds);
		out.add("rings", rings);
		JsonObject drops = new JsonObject();
		drops.addProperty("chestArtifact", value.drops.chestArtifact);
		drops.addProperty("chestRing", value.drops.chestRing);
		drops.addProperty("eliteArtifact", value.drops.eliteArtifact);
		drops.addProperty("eliteRing", value.drops.eliteRing);
		out.add("drops", drops);
	}
}
