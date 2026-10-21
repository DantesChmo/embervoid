package com.embervoid.sanctum;

import com.embervoid.progress.ProgressManager;

/** Entry point of the Ember Sanctum (milestone 5): one call from {@code Embervoid.onInitialize}. */
public final class Sanctum {
	public static void register() {
		ProgressManager.register();
		SanctumBlocks.register();
	}

	private Sanctum() {}
}
