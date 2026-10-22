package com.embervoid.nightmob;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModEntities;
import com.embervoid.registry.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;

/**
 * The full moon's own mobs (DESIGN §3.3, PLAN 8.7–8.9): entity types, attributes, spawn eggs. They have no natural
 * spawns; the full moon waves bring them.
 */
public final class NightMobs {
	public static final EntityType<AshenZombieEntity> ASHEN_ZOMBIE = ModEntities.register("ashen_zombie",
			FabricEntityTypeBuilder.<AshenZombieEntity>create(SpawnGroup.MONSTER, AshenZombieEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 1.95f))
					.trackRangeBlocks(64)
					.build());

	public static final Item ASHEN_ZOMBIE_SPAWN_EGG = ModItems.register("ashen_zombie_spawn_egg",
			new SpawnEggItem(ASHEN_ZOMBIE, 0x3d3a37, 0xb5462a, new Item.Settings()));

	public static void register() {
		Embervoid.LOGGER.debug("Night mob config section: {}", NightMobConfig.SECTION.name());
		FabricDefaultAttributeRegistry.register(ASHEN_ZOMBIE, AshenZombieEntity.createAttributes());
	}

	private NightMobs() {}
}
