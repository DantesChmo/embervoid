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

	public static final EntityType<HollowEntity> HOLLOW = ModEntities.register("hollow",
			FabricEntityTypeBuilder.<HollowEntity>create(SpawnGroup.MONSTER, HollowEntity::new)
					.dimensions(EntityDimensions.fixed(0.66f, 2.15f))
					.trackRangeBlocks(64)
					.build());

	public static final EntityType<EmberWispEntity> EMBER_WISP = ModEntities.register("ember_wisp",
			FabricEntityTypeBuilder.<EmberWispEntity>create(SpawnGroup.MONSTER, EmberWispEntity::new)
					.dimensions(EntityDimensions.fixed(0.5f, 0.5f))
					.trackRangeBlocks(48)
					.fireImmune()
					.build());

	public static final Item ASH_GHOUL_SPAWN_EGG = ModItems.register("ash_ghoul_spawn_egg",
			new SpawnEggItem(ASH_GHOUL, 0x5a5550, 0xe0702a, new Item.Settings()));
	public static final Item HOLLOW_SPAWN_EGG = ModItems.register("hollow_spawn_egg",
			new SpawnEggItem(HOLLOW, 0x4a4e55, 0xc9c1a8, new Item.Settings()));
	public static final Item EMBER_WISP_SPAWN_EGG = ModItems.register("ember_wisp_spawn_egg",
			new SpawnEggItem(EMBER_WISP, 0xff7a1a, 0xffd36e, new Item.Settings()));

	/** Overworld biomes where monsters spawn at all (mushroom fields and the deep dark have none). */
	private static final Predicate<BiomeSelectionContext> OVERWORLD_HOSTILE = BiomeSelectors.foundInOverworld()
			.and(BiomeSelectors.excludeByKey(BiomeKeys.MUSHROOM_FIELDS, BiomeKeys.DEEP_DARK));

	public static void register() {
		FabricDefaultAttributeRegistry.register(ASH_GHOUL, AshGhoulEntity.createAttributes());

		// Night on the surface or dark underground, like zombies; packs of 3-5.
		SpawnRestriction.register(ASH_GHOUL, SpawnRestriction.Location.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
		BiomeModifications.addSpawn(OVERWORLD_HOSTILE, SpawnGroup.MONSTER, ASH_GHOUL, 30, 3, 5);

		// Rare and alone, on the surface at night (open sky). The Sanctum places its own.
		FabricDefaultAttributeRegistry.register(HOLLOW, HollowEntity.createAttributes());
		SpawnRestriction.register(HOLLOW, SpawnRestriction.Location.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HollowEntity::canSpawn);
		BiomeModifications.addSpawn(OVERWORLD_HOSTILE, SpawnGroup.MONSTER, HOLLOW, 5, 1, 1);

		// Dark caves below y=0.
		FabricDefaultAttributeRegistry.register(EMBER_WISP, EmberWispEntity.createAttributes());
		SpawnRestriction.register(EMBER_WISP, SpawnRestriction.Location.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EmberWispEntity::canSpawn);
		BiomeModifications.addSpawn(OVERWORLD_HOSTILE, SpawnGroup.MONSTER, EMBER_WISP, 15, 1, 2);
	}

	private MobEntities() {}
}
