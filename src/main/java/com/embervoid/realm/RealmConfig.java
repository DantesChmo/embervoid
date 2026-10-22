package com.embervoid.realm;

import com.embervoid.config.ConfigSection;
import com.embervoid.config.EmbervoidConfig;

/**
 * {@code realm.*} in {@code config/embervoid.json}.
 *
 * @param depthsOpen whether an unsealed Sealed Gate leads into the final zone; off in 0.3.0, where the depths are a
 *                   placeholder ("the depths are still forming"). {@code /embervoid realm} commands reach them anyway
 */
public record RealmConfig(boolean depthsOpen) {
	private static final RealmConfig DEFAULTS = new RealmConfig(false);

	public static final ConfigSection<RealmConfig> SECTION = EmbervoidConfig.register(ConfigSection.of(
			"realm", DEFAULTS,
			in -> new RealmConfig(in.readBoolean("depthsOpen", DEFAULTS.depthsOpen)),
			(value, out) -> out.addProperty("depthsOpen", value.depthsOpen)));

	public static RealmConfig get() {
		return EmbervoidConfig.get().section(SECTION);
	}
}
