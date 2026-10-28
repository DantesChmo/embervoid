package com.embervoid.sanctum.boss;

import com.embervoid.registry.ModEntities;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

/** The Ruined Sanctum's boss and her crying souls. Only her fight summons them: no spawn eggs. */
public final class SanctumBossEntities {
	public static final EntityType<FallenSisterEntity> FALLEN_SISTER = ModEntities.register("fallen_sister",
			FabricEntityTypeBuilder.<FallenSisterEntity>create(SpawnGroup.MONSTER, FallenSisterEntity::new)
					.dimensions(EntityDimensions.fixed(0.9f, 3.0f))
					.trackRangeBlocks(64)
					.fireImmune()
					.build());
	/** Moved by the Sister every tick, so every tick is sent; not saved (a fight never outlives a restart). */
	public static final EntityType<CryingSoulEntity> CRYING_SOUL = ModEntities.register("crying_soul",
			FabricEntityTypeBuilder.<CryingSoulEntity>create(SpawnGroup.MONSTER, CryingSoulEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 0.6f))
					.trackRangeBlocks(64)
					.trackedUpdateRate(1)
					.fireImmune()
					.disableSaving()
					.build());

	static void register() {
		FabricDefaultAttributeRegistry.register(FALLEN_SISTER, FallenSisterEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CRYING_SOUL, CryingSoulEntity.createAttributes());
	}

	private SanctumBossEntities() {}
}
