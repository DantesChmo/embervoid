package com.embervoid.sanctum;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;

/**
 * Outer-hall guards. The templates place a guard on every spot; spots tagged {@link #ROLL_HALL} or
 * {@link #ROLL_ARCHER} keep their guard only with the configured chance ({@code sanctum.hallGuardChance},
 * {@code sanctum.hallArcherChance}). The roll happens once, when the guard first loads after worldgen.
 */
public final class SanctumGuards {
	/** On every Sanctum guard. */
	public static final String GUARD = "embervoid.sanctum_guard";
	/** A side-hall melee spot (Hollow), not yet rolled. */
	public static final String ROLL_HALL = "embervoid.sanctum_guard.roll_hall";
	/** A side-hall archer spot (skeleton), not yet rolled. */
	public static final String ROLL_ARCHER = "embervoid.sanctum_guard.roll_archer";

	static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> roll(entity));
	}

	private static void roll(Entity entity) {
		boolean hall = entity.getCommandTags().contains(ROLL_HALL);
		boolean archer = entity.getCommandTags().contains(ROLL_ARCHER);
		if (!hall && !archer) {
			return;
		}
		entity.removeScoreboardTag(ROLL_HALL);
		entity.removeScoreboardTag(ROLL_ARCHER);
		SanctumConfig config = SanctumConfig.get();
		float chance = hall ? config.hallGuardChance() : config.hallArcherChance();
		if (entity.getWorld().getRandom().nextFloat() >= chance) {
			entity.discard();
		}
	}

	private SanctumGuards() {}
}
