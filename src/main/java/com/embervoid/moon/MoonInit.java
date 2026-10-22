package com.embervoid.moon;

import com.embervoid.Embervoid;

/** Registers the moon nights (DESIGN §3.3); the one call from {@code Embervoid.onInitialize}. */
public final class MoonInit {
	public static void register() {
		Embervoid.LOGGER.debug("Moon config section: {}", MoonConfig.SECTION.name());
		MoonNightManager.register();
		MoonSleep.register();
		MoonSync.register();
		FullMoonMobs.register();
		CurseManager.register();
		MoonCommands.register();
	}

	private MoonInit() {}
}
