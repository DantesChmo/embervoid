package com.embervoid.boss;

/**
 * An entity that can be summoned as an arena boss. The framework ({@link BossFight}) owns the fight around it:
 * boss bar, fog wall, reset, phases, credit and loot. The entity owns its moves.
 *
 * <p>Implementations must call {@link BossFight#tickBoss} from their server tick and keep the default persistence
 * (bosses never despawn; {@link BossFight} discards them on reset).
 */
public interface ArenaBoss {
	/** Health fractions at which the next phase starts, descending, e.g. {@code {0.5f}} for two phases. */
	default float[] phaseThresholds() {
		return new float[0];
	}

	/** Called once when the boss's health first drops to a threshold; phase 0 is the start of the fight. */
	default void onPhaseChange(int phase) {
	}

	/** Plays a named animation (GeckoLib bosses); used by {@link Telegraph}. */
	default void playAnimation(String name) {
	}

	/** Music played to fighters while the boss is alive, or null for none. */
	@org.jetbrains.annotations.Nullable
	default net.minecraft.util.Identifier music() {
		return null;
	}
}
