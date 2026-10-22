package com.embervoid.vampire;

import com.embervoid.registry.ModEntities;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

/**
 * Entity types of the blood new moon. No spawn eggs: a Vampire or its boss form outside a
 * blood night leaves at once. */
public final class VampireEntities {
	// MISC: never spawns naturally and does not count towards any mob cap.
	public static final EntityType<VampireEntity> VAMPIRE = ModEntities.register("vampire",
			FabricEntityTypeBuilder.<VampireEntity>create(SpawnGroup.MISC, VampireEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 1.95f))
					.trackRangeBlocks(64)
					.fireImmune()
					.build());

	public static final EntityType<VampireBossEntity> VAMPIRE_BOSS = ModEntities.register("vampire_boss",
			FabricEntityTypeBuilder.<VampireBossEntity>create(SpawnGroup.MONSTER, VampireBossEntity::new)
					.dimensions(EntityDimensions.fixed(0.8f, 2.4f))
					.trackRangeBlocks(96)
					.fireImmune()
					.build());

	static void register() {
		FabricDefaultAttributeRegistry.register(VAMPIRE, VampireEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(VAMPIRE_BOSS, VampireBossEntity.createAttributes());
	}

	private VampireEntities() {}
}
