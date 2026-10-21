package com.embervoid.entity.mob;

import com.embervoid.registry.ModEntities;
import com.embervoid.registry.ModItems;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

import java.util.function.Predicate;

/** The new overworld hostiles (DESIGN §3.2): entity types, attributes, spawn eggs and natural spawns. */
public final class MobEntities {
	public static final EntityType<AshGhoulEntity> ASH_GHOUL = ModEntities.register("ash_ghoul",
			FabricEntityTypeBuilder.<AshGhoulEntity>create(SpawnGroup.MONSTER, AshGhoulEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 1.7f))
					.trackRangeBlocks(64)
					.build());

	public static final Item ASH_GHOUL_SPAWN_EGG = ModItems.register("ash_ghoul_spawn_egg",
			new SpawnEggItem(ASH_GHOUL, 0x5a5550, 0xe0702a, new Item.Settings()));

	/** Overworld biomes where monsters spawn at all (mushroom fields and the deep dark have none). */
	private static final Predicate<BiomeSelectionContext> OVERWORLD_HOSTILE = BiomeSelectors.foundInOverworld()
			.and(BiomeSelectors.excludeByKey(BiomeKeys.MUSHROOM_FIELDS, BiomeKeys.DEEP_DARK));

	public static void register() {
		FabricDefaultAttributeRegistry.register(ASH_GHOUL, AshGhoulEntity.createAttributes());

		// Night on the surface or dark underground, like zombies; packs of 3-5.
		SpawnRestriction.register(ASH_GHOUL, SpawnRestriction.Location.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
		BiomeModifications.addSpawn(OVERWORLD_HOSTILE, SpawnGroup.MONSTER, ASH_GHOUL, 30, 3, 5);
	}

	private MobEntities() {}
}
