package com.embervoid.boss;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * An entity that can be summoned as an arena boss. The framework ({@link BossFight}) owns the fight around it:
 * boss bar, fog wall, reset, phases, credit and loot. The entity owns its moves.
 *
 * <p>Implementations must be mob entities, call {@link ArenaManager#tickBoss} from their server tick, and must not
 * despawn on their own ({@link BossFight} makes them persistent and discards them on reset).
 */
public interface ArenaBoss {
	/** Health fractions at which the next phase starts, descending, e.g. {@code {0.5f}} for two phases. */
	default float[] phaseThresholds() {
		return new float[0];
	}

	/** Called once when the boss's health first drops to a threshold; phase 0 is the start of the fight. */
	default void onPhaseChange(int phase) {
	}

	/** Plays a named animation (GeckoLib bosses); {@link Telegraph} calls it at the start of a windup. */
	default void playAnimation(String name) {
	}

	/** Music played to the fighters while the fight lasts, or null for none. */
	@Nullable
	default Identifier music() {
		return null;
	}
}
