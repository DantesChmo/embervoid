package com.embervoid.sanctum;

import com.embervoid.progress.ProgressManager;

import java.util.Objects;

/** Entry point of the Ember Sanctum (milestone 5): one call from {@code Embervoid.onInitialize}. */
public final class Sanctum {
	public static void register() {
		// Registers the sanctum.* config section before clients join.
		Objects.requireNonNull(SanctumConfig.SECTION);
		ProgressManager.register();
		SanctumBlocks.register();
		SanctumItems.register();
		SanctumEntities.register();
		SanctumLoot.register();
		Attunement.register();
	}

	private Sanctum() {}
}
