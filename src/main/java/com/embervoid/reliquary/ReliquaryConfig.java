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
 */
public record ReliquaryConfig(Map<String, Integer> slots, Artifacts artifacts) {
	/**
	 * @param points     points of a one-stat artifact (DESIGN §4.5: +3)
	 * @param sealPoints points per stat of the Seal of the Ashen King (+1 to every stat)
	 */
	public record Artifacts(int points, int sealPoints) {}

	/** Most slots of one kind: the screen has room for three columns of three on each side of the player model. */
	public static final int MAX_SLOTS_PER_KIND = 8;

	public static final ReliquaryConfig DEFAULT = new ReliquaryConfig(defaultSlots(), new Artifacts(3, 1));

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
				artifacts.readInt("sealPoints", DEFAULT.artifacts.sealPoints, 0, 99)));
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
	}
}
