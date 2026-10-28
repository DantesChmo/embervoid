package com.embervoid.reach.boss;

import com.embervoid.registry.ModEntities;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

/** The Ashen Reach bosses under their fixed ids. Hitboxes are their final sizes. */
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

	public static final EntityType<GrievingTreeEntity> GRIEVING_TREE = ModEntities.register("grieving_tree",
			FabricEntityTypeBuilder.<GrievingTreeEntity>create(SpawnGroup.MONSTER, GrievingTreeEntity::new)
					.dimensions(EntityDimensions.fixed(3.0f, 10.0f))
					.trackRangeBlocks(128)
					.build());
	public static final EntityType<SmolderingLordEntity> SMOLDERING_LORD = ModEntities.register("smoldering_lord",
			FabricEntityTypeBuilder.<SmolderingLordEntity>create(SpawnGroup.MONSTER, SmolderingLordEntity::new)
					.dimensions(EntityDimensions.fixed(1.4f, 3.5f))
					.trackRangeBlocks(96)
					.fireImmune()
					.build());
	/** The Grieving Tree's hanged lanterns (not a fixed id; only the tree makes them). */
	public static final EntityType<TreeLanternEntity> TREE_LANTERN = ModEntities.register("tree_lantern",
			FabricEntityTypeBuilder.<TreeLanternEntity>create(SpawnGroup.MISC, TreeLanternEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 0.8f))
					.trackRangeBlocks(96)
					.disableSaving()
					.build());

	static void register() {
		FabricDefaultAttributeRegistry.register(CRYPT_KEEPER, CryptKeeperEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ASH_COLOSSUS, AshColossusEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GRIEVING_TREE, GrievingTreeEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SMOLDERING_LORD, SmolderingLordEntity.createAttributes());
	}

	private ReachBossEntities() {}
}
