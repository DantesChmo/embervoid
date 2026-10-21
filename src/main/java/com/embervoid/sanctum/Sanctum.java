package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import com.embervoid.progress.ProgressManager;
import com.embervoid.sanctum.leveling.LevelUpNetworking;
import net.minecraft.util.Identifier;

import java.util.Objects;

/** Entry point of the Ember Sanctum (milestone 5): one call from {@code Embervoid.onInitialize}. */
public final class Sanctum {
	public static void register() {
		// Registers the sanctum.* config section before clients join.
		Objects.requireNonNull(SanctumConfig.SECTION);
		ProgressManager.register();
		SanctumBlocks.register();
		SanctumStructures.register();
		SanctumItems.register();
		SanctumEntities.register();
		SanctumLoot.register();
		Attunement.register();
		LevelUpNetworking.register();
	}

	/** Whether a structure template is one of the Sanctum's jigsaw pieces ({@code embervoid:sanctum/...}). */
	public static boolean isSanctumPiece(Identifier template) {
		return template.getNamespace().equals(Embervoid.MOD_ID) && template.getPath().startsWith("sanctum/");
	}

	private Sanctum() {}
}
