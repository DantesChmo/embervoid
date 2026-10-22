package com.embervoid.vampire;

import com.embervoid.Embervoid;
import com.embervoid.vampire.moonstub.MoonNights;

/** Registers the blood new moon's Vampires; the one call from {@code Embervoid.onInitialize}. */
public final class VampireInit {
	public static void register() {
		Embervoid.LOGGER.debug("Vampire config section: {}", VampireConfig.SECTION.name());
		MoonNights.register();
		VampireEntities.register();
		VampireNight.register();
		VampireCommands.register();
	}

	private VampireInit() {}
}
