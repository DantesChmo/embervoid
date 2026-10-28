package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import com.embervoid.progress.ProgressManager;
import com.embervoid.sanctum.gear.SanctumGear;
import com.embervoid.sanctum.leveling.LevelUpNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * Entry point of the Sanctum, the overworld dungeon (the Ruined Sanctum): one call from
 * {@code Embervoid.onInitialize}.
 */
public final class Sanctum {
	public static void register() {
		// Registers the sanctum.* config section before clients join.
		Objects.requireNonNull(SanctumConfig.SECTION);
		ProgressManager.register();
		SanctumBlocks.register();
		SanctumStructures.register();
		SanctumArena.register();
		SanctumItems.register();
		SanctumGear.register();
		SanctumEntities.register();
		SanctumLoot.register();
		LevelUpNetworking.register();
	}

	/**
	 * Whether the player stands in a Sanctum core: on its {@link SanctumBlocks#SANCTUM_FLAGSTONE} floor (the hub's
	 * core and the cores of 0.4.x worlds). Moon nights leave such players alone.
	 */
	public static boolean isInCore(PlayerEntity player) {
		return player.getWorld().getBlockState(player.getBlockPos().down()).isOf(SanctumBlocks.SANCTUM_FLAGSTONE)
				|| player.getWorld().getBlockState(player.getSteppingPos()).isOf(SanctumBlocks.SANCTUM_FLAGSTONE);
	}

	/** Whether a structure template is one of the Sanctum's jigsaw pieces ({@code embervoid:sanctum/...}). */
	public static boolean isSanctumPiece(Identifier template) {
		return template.getNamespace().equals(Embervoid.MOD_ID) && template.getPath().startsWith("sanctum/");
	}

	private Sanctum() {}
}
