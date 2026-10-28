package com.embervoid.reach.gear;

import com.embervoid.Embervoid;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Chest loot table ids of Ashen Reach (fixed ids). Structures in other packages point their chests at
 * these: {@code embervoid:chests/ashen_reach/<zone>_common|_treasure|_arena} and the Citadel's two.
 */
public final class GearLootTables {
	public static final List<String> ZONES = List.of("fields", "woods", "necropolis");

	public static final Identifier CITADEL_COMMON = chest("citadel_common");
	public static final Identifier CITADEL_TREASURE = chest("citadel_treasure");

	public static Identifier common(String zone) {
		return chest(zone + "_common");
	}

	public static Identifier treasure(String zone) {
		return chest(zone + "_treasure");
	}

	/** The chest by a zone's boss arena. */
	public static Identifier arena(String zone) {
		return chest(zone + "_arena");
	}

	/** Every chest table of the realm, zones first. */
	public static List<Identifier> all() {
		List<Identifier> ids = new ArrayList<>();
		for (String zone : ZONES) {
			ids.add(common(zone));
			ids.add(treasure(zone));
			ids.add(arena(zone));
		}
		ids.add(CITADEL_COMMON);
		ids.add(CITADEL_TREASURE);
		return ids;
	}

	private static Identifier chest(String name) {
		return Embervoid.id("chests/ashen_reach/" + name);
	}

	private GearLootTables() {}
}
