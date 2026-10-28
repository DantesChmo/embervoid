package com.embervoid.boss;

import com.embervoid.registry.ModEntities;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

public final class BossEntities {
	public static final EntityType<TestBossEntity> TEST_BOSS = ModEntities.register("test_boss",
			FabricEntityTypeBuilder.<TestBossEntity>create(SpawnGroup.MONSTER, TestBossEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 1.95f))
					.trackRangeBlocks(96)
					.fireImmune()
					.build());

	static void register() {
		FabricDefaultAttributeRegistry.register(TEST_BOSS, TestBossEntity.createAttributes());
	}

	private BossEntities() {}
}
