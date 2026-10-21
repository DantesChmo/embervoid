package com.embervoid.sanctum;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;

/**
 * {@code sanctum.*} in {@code config/embervoid.json}. Chances apply when loot tables load (world start or
 * {@code /reload}). Structure spacing is not here: it is the {@code structure_set} datapack file.
 *
 * @param mapChanceCartographer chance of a Sanctum Map in a village cartographer chest
 * @param mapChanceTemple       ... in a village temple chest
 * @param mapChanceRuinedPortal ... in a ruined portal chest
 * @param mapChanceShipwreck    ... in a shipwreck map chest
 * @param mapSearchRadius       how far (in chunks) a new Sanctum Map looks for a Sanctum
 */
public record SanctumConfig(float mapChanceCartographer, float mapChanceTemple, float mapChanceRuinedPortal,
		float mapChanceShipwreck, int mapSearchRadius) {
	private static final SanctumConfig DEFAULTS = new SanctumConfig(0.25f, 0.1f, 0.08f, 0.25f, 64);

	public static final ConfigSection<SanctumConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"sanctum", DEFAULTS,
			in -> new SanctumConfig(
					in.readFloat("mapChanceCartographer", DEFAULTS.mapChanceCartographer, 0, 1),
					in.readFloat("mapChanceTemple", DEFAULTS.mapChanceTemple, 0, 1),
					in.readFloat("mapChanceRuinedPortal", DEFAULTS.mapChanceRuinedPortal, 0, 1),
					in.readFloat("mapChanceShipwreck", DEFAULTS.mapChanceShipwreck, 0, 1),
					in.readInt("mapSearchRadius", DEFAULTS.mapSearchRadius, 8, 200)),
			(value, out) -> {
				out.addProperty("mapChanceCartographer", value.mapChanceCartographer);
				out.addProperty("mapChanceTemple", value.mapChanceTemple);
				out.addProperty("mapChanceRuinedPortal", value.mapChanceRuinedPortal);
				out.addProperty("mapChanceShipwreck", value.mapChanceShipwreck);
				out.addProperty("mapSearchRadius", value.mapSearchRadius);
			}));

	public static SanctumConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}
}
