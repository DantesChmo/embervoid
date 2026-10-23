package com.embervoid.sanctum.gear;

import com.embervoid.Embervoid;

/** The Sanctum weapons (PLAN L1): one call from {@code Sanctum.register}. */
public final class SanctumGear {
	public static void register() {
		Embervoid.LOGGER.debug("Sanctum gear: {} weapons, config section {}", SanctumWeapons.ALL.size(),
				SanctumGearConfig.SECTION.name());
	}

	private SanctumGear() {}
}
