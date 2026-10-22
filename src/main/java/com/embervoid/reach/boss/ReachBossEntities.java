package com.embervoid.reach.boss;

import com.embervoid.registry.ModEntities;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

/** The Ashen Reach bosses under their fixed ids (PLAN "0.4.0"). Hitboxes are their final sizes. */
public final class ReachBossEntities {
	public static final EntityType<CryptKeeperEntity> CRYPT_KEEPER = ModEntities.register("crypt_keeper",
			FabricEntityTypeBuilder.<CryptKeeperEntity>create(SpawnGroup.MONSTER, CryptKeeperEntity::new)
					.dimensions(EntityDimensions.fixed(1.6f, 4.0f))
					.trackRangeBlocks(96)
					.fireImmune()
					.build());

	public static final EntityType<AshColossusEntity> ASH_COLOSSUS = ModEntities.register("ash_colossus",
			FabricEntityTypeBuilder.<AshColossusEntity>create(SpawnGroup.MONSTER, AshColossusEntity::new)
					.dimensions(EntityDimensions.fixed(2.4f, 4.5f))
					.trackRangeBlocks(96)
					.fireImmune()
					.build());

	static void register() {
		FabricDefaultAttributeRegistry.register(CRYPT_KEEPER, CryptKeeperEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ASH_COLOSSUS, AshColossusEntity.createAttributes());
	}

	private ReachBossEntities() {}
}
