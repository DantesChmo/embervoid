package com.embervoid.reach.mob;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModEntities;
import com.embervoid.registry.ModItems;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.Biome;

import java.util.function.Supplier;

/**
 * Realm 1 mobs (DESIGN §4.5, PLAN 10): entity types under the fixed ids, attributes, spawn eggs and natural spawns.
 *
 * <p>Only fodder, ranged mobs and the rare Ashen Carrion spawn naturally, and only in their own zone biome
 * ({@link ReachMobConfig#spawns}); tanks and elites come from structures (placed, {@link ReachMob#PLACED_TAG}).
 * Natural spawns ignore sky light (the realm is in eternal dusk) and need block light ≤ {@link #MAX_BLOCK_LIGHT}, so
 * torches still keep a camp clear.
 */
public final class ReachMobs {
	public static final RegistryKey<Biome> FIELDS = zoneBiome("ashen_reach_zone_a");
	public static final RegistryKey<Biome> WOODS = zoneBiome("ashen_reach_zone_b");
	public static final RegistryKey<Biome> NECROPOLIS = zoneBiome("ashen_reach_zone_c");
	/** Natural spawns need block light at or below this. */
	public static final int MAX_BLOCK_LIGHT = 7;

	public static final EntityType<CinderHuskEntity> CINDER_HUSK = register("cinder_husk", CinderHuskEntity::new, 0.6f, 1.95f);
	public static final EntityType<AshStalkerEntity> ASH_STALKER = register("ash_stalker", AshStalkerEntity::new, 0.6f, 1.6f);
	public static final EntityType<AshenCarrionEntity> ASHEN_CARRION = register("ashen_carrion", AshenCarrionEntity::new, 1.2f, 0.9f);
	public static final EntityType<WitheredThrallEntity> WITHERED_THRALL = register("withered_thrall", WitheredThrallEntity::new, 0.6f, 1.95f);
	public static final EntityType<LanternHagEntity> LANTERN_HAG = register("lantern_hag", LanternHagEntity::new, 0.6f, 1.8f);
	public static final EntityType<BarkHollowEntity> BARK_HOLLOW = register("bark_hollow", BarkHollowEntity::new, 1.4f, 3.2f);
	public static final EntityType<RestlessBonesEntity> RESTLESS_BONES = register("restless_bones", RestlessBonesEntity::new, 0.6f, 1.99f);
	public static final EntityType<CryptMothEntity> CRYPT_MOTH = register("crypt_moth", CryptMothEntity::new, 0.7f, 0.6f);
	public static final EntityType<HollowSentinelEntity> HOLLOW_SENTINEL = register("hollow_sentinel", HollowSentinelEntity::new, 0.8f, 2.3f);
	public static final EntityType<GraveWardenEntity> GRAVE_WARDEN = register("grave_warden", GraveWardenEntity::new, 0.9f, 2.4f);

	static {
		egg(CINDER_HUSK, 0x403833, 0xe06a28);
		egg(ASH_STALKER, 0x6e6862, 0xff7828);
		egg(ASHEN_CARRION, 0x28262a, 0x968c6e);
		egg(WITHERED_THRALL, 0x646856, 0x967850);
		egg(LANTERN_HAG, 0x3a2e28, 0xffaa3c);
		egg(BARK_HOLLOW, 0x42362c, 0xffbe50);
		egg(RESTLESS_BONES, 0xcdc6af, 0x6e645a);
		egg(CRYPT_MOTH, 0x786e60, 0xdcffa0);
		egg(HOLLOW_SENTINEL, 0x464a52, 0x96c8ff);
		egg(GRAVE_WARDEN, 0x302e36, 0x966e32);
	}

	public static void register() {
		Embervoid.LOGGER.debug("Reach mob config section: {}", ReachMobConfig.SECTION.name());
		attributes(CINDER_HUSK, CinderHuskEntity::createAttributes);
		attributes(ASH_STALKER, AshStalkerEntity::createAttributes);
		attributes(ASHEN_CARRION, AshenCarrionEntity::createAttributes);
		attributes(WITHERED_THRALL, WitheredThrallEntity::createAttributes);
		attributes(LANTERN_HAG, LanternHagEntity::createAttributes);
		attributes(BARK_HOLLOW, BarkHollowEntity::createAttributes);
		attributes(RESTLESS_BONES, RestlessBonesEntity::createAttributes);
		attributes(CRYPT_MOTH, CryptMothEntity::createAttributes);
		attributes(HOLLOW_SENTINEL, HollowSentinelEntity::createAttributes);
		attributes(GRAVE_WARDEN, GraveWardenEntity::createAttributes);

		naturalSpawn(CINDER_HUSK, FIELDS, ReachMobs::canSpawnInRealm);
		naturalSpawn(ASH_STALKER, FIELDS, ReachMobs::canSpawnInRealm);
		naturalSpawn(ASHEN_CARRION, FIELDS, ReachMobs::canSpawnInRealm);
		naturalSpawn(WITHERED_THRALL, WOODS, ReachMobs::canSpawnInRealm);
		naturalSpawn(LANTERN_HAG, WOODS, ReachMobs::canSpawnInRealm);
		naturalSpawn(RESTLESS_BONES, NECROPOLIS, ReachMobs::canSpawnInRealm);
		naturalSpawn(CRYPT_MOTH, NECROPOLIS, ReachMobs::canSpawnUnderCover);
		// Placed only, but spawners and commands still check a rule.
		for (EntityType<? extends HostileEntity> placedOnly : java.util.List.of(BARK_HOLLOW, HOLLOW_SENTINEL, GRAVE_WARDEN)) {
			SpawnRestriction.register(placedOnly, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
					ReachMobs::canSpawnInRealm);
		}
	}

	/** Not peaceful, a valid floor, no torch light. Sky light does not matter (eternal dusk). */
	public static boolean canSpawnInRealm(EntityType<? extends HostileEntity> type, ServerWorldAccess world, SpawnReason reason,
			BlockPos pos, Random random) {
		return HostileEntity.canSpawnIgnoreLightLevel(type, world, reason, pos, random)
				&& world.getLightLevel(LightType.BLOCK, pos) <= MAX_BLOCK_LIGHT;
	}

	/** {@link #canSpawnInRealm} under a roof (crypts, sinkhole overhangs). */
	public static boolean canSpawnUnderCover(EntityType<? extends HostileEntity> type, ServerWorldAccess world,
			SpawnReason reason, BlockPos pos, Random random) {
		return canSpawnInRealm(type, world, reason, pos, random) && (reason != SpawnReason.NATURAL || !world.isSkyVisible(pos));
	}

	private static <T extends HostileEntity> void naturalSpawn(EntityType<T> type, RegistryKey<Biome> biome,
			SpawnRestriction.SpawnPredicate<T> rule) {
		SpawnRestriction.register(type, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, rule);
		ReachMobConfig.Spawn spawn = ReachMobConfig.get().spawns().get(Registries.ENTITY_TYPE.getId(type).getPath());
		if (spawn != null && spawn.weight() > 0) {
			BiomeModifications.addSpawn(BiomeSelectors.includeByKey(biome), SpawnGroup.MONSTER, type, spawn.weight(),
					spawn.minGroup(), spawn.maxGroup());
		}
	}

	private static <T extends ReachMob> EntityType<T> register(String name, EntityType.EntityFactory<T> factory,
			float width, float height) {
		return ModEntities.register(name, FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, factory)
				.dimensions(EntityDimensions.fixed(width, height))
				.trackRangeBlocks(64)
				.build());
	}

	private static void egg(EntityType<? extends ReachMob> type, int primary, int secondary) {
		ModItems.register(Registries.ENTITY_TYPE.getId(type).getPath() + "_spawn_egg",
				new SpawnEggItem(type, primary, secondary, new Item.Settings()));
	}

	private static void attributes(EntityType<? extends ReachMob> type, Supplier<DefaultAttributeContainer.Builder> builder) {
		FabricDefaultAttributeRegistry.register(type, builder.get());
	}

	private static RegistryKey<Biome> zoneBiome(String path) {
		return RegistryKey.of(RegistryKeys.BIOME, Embervoid.id(path));
	}

	private ReachMobs() {}
}
