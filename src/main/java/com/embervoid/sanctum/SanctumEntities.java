package com.embervoid.sanctum;

import com.embervoid.registry.ModEntities;
import com.embervoid.registry.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;

/** Sanctum NPCs. The Ashen Maiden is placed by the core template; the egg is for creative and testing. */
public final class SanctumEntities {
	public static final EntityType<AshenMaidenEntity> ASHEN_MAIDEN = ModEntities.register("ashen_maiden",
			FabricEntityTypeBuilder.<AshenMaidenEntity>create(SpawnGroup.MISC, AshenMaidenEntity::new)
					.dimensions(EntityDimensions.fixed(0.7f, 2.1f))
					.trackRangeBlocks(48)
					.fireImmune()
					.build());

	public static final Item ASHEN_MAIDEN_SPAWN_EGG = ModItems.register("ashen_maiden_spawn_egg",
			new SpawnEggItem(ASHEN_MAIDEN, 0x5c5654, 0xff9632, new Item.Settings()));

	static void register() {
		FabricDefaultAttributeRegistry.register(ASHEN_MAIDEN, AshenMaidenEntity.createAttributes());
	}

	private SanctumEntities() {}
}
