package com.embervoid.block.setpiece;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;

/**
 * {@code setpieces.*} in {@code config/embervoid.json}: the moving set pieces.
 *
 * @param maxPerChunk how many set pieces one chunk may hold; the extra ones become air when the chunk comes alive
 * @param animate     whether set pieces move at all (off: rest pose, no smoke); synced to clients
 */
public record SetPieceConfig(int maxPerChunk, boolean animate) {
	private static final SetPieceConfig DEFAULTS = new SetPieceConfig(8, true);

	public static final ConfigSection<SetPieceConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"setpieces", DEFAULTS,
			in -> new SetPieceConfig(
					in.readInt("maxPerChunk", DEFAULTS.maxPerChunk, 0, 4096),
					in.readBoolean("animate", DEFAULTS.animate)),
			(value, out) -> {
				out.addProperty("maxPerChunk", value.maxPerChunk);
				out.addProperty("animate", value.animate);
			}));

	public static SetPieceConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}
}
