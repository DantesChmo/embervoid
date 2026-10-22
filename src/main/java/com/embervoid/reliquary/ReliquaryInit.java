package com.embervoid.reliquary;

import java.util.Objects;

/** Registers the Reliquary (PLAN 11.9–11.13); the one call from {@code Embervoid.onInitialize}. */
public final class ReliquaryInit {
	public static void register() {
		Objects.requireNonNull(ReliquaryConfig.SECTION);
		Objects.requireNonNull(ReliquaryScreenHandler.TYPE);
		ReliquaryManager.register();
		ReliquaryCommands.register();
	}

	private ReliquaryInit() {}
}
