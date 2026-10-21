package com.embervoid.sanctum;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.random.Random;

/**
 * Outer-hall guards. The templates hold a guard on every spot; spots tagged {@link #ROLL_HALL} or
 * {@link #ROLL_ARCHER} keep their guard only with the configured chance ({@code sanctum.hallGuardChance},
 * {@code sanctum.hallArcherChance}). The roll happens while the piece is placed during worldgen, before the entity
 * exists (see {@code StructureTemplateMixin}), so a dropped guard is never created at all.
 */
public final class SanctumGuards {
	/** On every Sanctum guard. */
	public static final String GUARD = "embervoid.sanctum_guard";
	/** A side-hall melee spot (Hollow), not yet rolled. */
	public static final String ROLL_HALL = "embervoid.sanctum_guard.roll_hall";
	/** A side-hall archer spot (skeleton), not yet rolled. */
	public static final String ROLL_ARCHER = "embervoid.sanctum_guard.roll_archer";

	/**
	 * Called for each entity a structure template is about to spawn, with a copy of its NBT. Returns false to drop
	 * it. A kept guard loses its roll tag.
	 */
	public static boolean keep(NbtCompound entityNbt, Random random) {
		SanctumConfig config = SanctumConfig.get();
		return keep(entityNbt, random.nextFloat(), config.hallGuardChance(), config.hallArcherChance());
	}

	/** {@link #keep(NbtCompound, Random)} with the roll and chances given (pure, for tests). */
	static boolean keep(NbtCompound entityNbt, float roll, float hallChance, float archerChance) {
		if (!entityNbt.contains("Tags", NbtElement.LIST_TYPE)) {
			return true;
		}
		NbtList tags = entityNbt.getList("Tags", NbtElement.STRING_TYPE);
		float chance = 1;
		for (int i = tags.size() - 1; i >= 0; i--) {
			String tag = tags.getString(i);
			if (tag.equals(ROLL_HALL) || tag.equals(ROLL_ARCHER)) {
				chance = tag.equals(ROLL_HALL) ? hallChance : archerChance;
				tags.remove(i);
			}
		}
		return roll < chance;
	}

	private SanctumGuards() {}
}
