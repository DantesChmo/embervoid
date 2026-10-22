package com.embervoid.boss;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * An entity that can be summoned as an arena boss. The framework ({@link BossFight}) owns the fight around it:
 * boss bar, fog wall, reset, phases, multiplayer scaling, target switching, adds, hazards, credit and loot. The entity
 * owns its moves and reaches its fight with {@link BossFight#of}.
 *
 * <p>Implementations must be mob entities, call {@link ArenaManager#tickBoss} from their server tick, and must not
 * despawn on their own ({@link BossFight} makes them persistent and discards them on reset).
 */
public interface ArenaBoss {
	/** Health fractions at which the next phase starts, descending, e.g. {@code {0.5f}} for two phases. */
	default float[] phaseThresholds() {
		return new float[0];
	}

	/**
	 * Phases at the start of the fight that health does not end (the Smoldering Lord on the wall): phases 0 to
	 * {@code n − 1} end only by {@link BossFight#advancePhase}; {@link #phaseThresholds} count from phase {@code n}.
	 */
	default int scriptedPhases() {
		return 0;
	}

	/** Called once when a phase starts; phase 0 is the start of the fight. */
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

	/** The add summoned for each fighter beyond the first ({@link BossConfig#addsPerExtraFighter}), or null for none. */
	@Nullable
	default EntityType<? extends MobEntity> scalingAdd() {
		return null;
	}

	/** Whether the fight turns the boss to another fighter now and then ({@link BossConfig} target switch). */
	default boolean switchesTargets() {
		return true;
	}
}
