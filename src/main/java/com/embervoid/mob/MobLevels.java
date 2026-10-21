package com.embervoid.mob;

import com.embervoid.Embervoid;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mob levels (DESIGN §5.4, PLAN 3.5). Every hostile mob gets a level once, the first time it is added to a server
 * world; the level is stored on the entity (a persistent attachment, so it survives restarts and chunk reloads)
 * and drives its max health (a fixed-UUID attribute modifier), the damage it deals (applied when damage is dealt,
 * see {@code LivingEntityMobLevelMixin}) and the XP it drops.
 *
 * <p>"Hostile" means {@link Monster} (vanilla and modded alike), minus the {@link #UNLEVELED} entity type tag (the
 * vanilla bosses by default). Other tracks set levels with {@link #setLevel} (any living entity, e.g. a realm boss)
 * or decide them at spawn with a {@link LevelProvider} (e.g. per-zone level bands in a realm dimension).
 *
 * <p>Clients learn levels through {@link #SYNC_PACKET}, sent when a player starts tracking a leveled entity and
 * whenever its level changes.
 */
public final class MobLevels {
	/** S2C: entity network id (varint), level (varint). */
	public static final Identifier SYNC_PACKET = Embervoid.id("mob_level");
	/** Hostile entity types that never get a level and keep vanilla stats (data: tags/entity_types/unleveled.json). */
	public static final TagKey<EntityType<?>> UNLEVELED = TagKey.of(RegistryKeys.ENTITY_TYPE, Embervoid.id("unleveled"));

	@SuppressWarnings("UnstableApiUsage")
	private static final AttachmentType<Integer> LEVEL = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.INT)
			.buildAndRegister(Embervoid.id("mob_level"));
	private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("0f5d6f1e-8c1a-4d55-9a4b-6a1f3e2c7d10");
	private static final List<LevelProvider> PROVIDERS = new CopyOnWriteArrayList<>();

	/** Decides the level of a hostile mob the first time it is added to a world, instead of the overworld rules. */
	@FunctionalInterface
	public interface LevelProvider {
		/** The level for {@code mob}, or empty to leave it to the next provider and finally the default rules. */
		OptionalInt levelFor(MobEntity mob, ServerWorld world);
	}

	/** Adds a provider; providers are asked in registration order and the first non-empty answer wins. */
	public static void registerProvider(LevelProvider provider) {
		PROVIDERS.add(provider);
	}

	/** The entity's level, or 0 if it has none. Works on both sides (the client knows levels of tracked entities). */
	public static int getLevel(Entity entity) {
		if (entity.getWorld().isClient) {
			return ClientLevels.get(entity.getId());
		}
		@SuppressWarnings("UnstableApiUsage")
		Integer level = entity.getAttached(LEVEL);
		return level == null ? 0 : level;
	}

	/** Whether the default rules give this entity a level (hostile and not tagged {@link #UNLEVELED}). */
	public static boolean canHaveLevel(Entity entity) {
		return entity instanceof MobEntity && entity instanceof Monster && !entity.getType().isIn(UNLEVELED);
	}

	/**
	 * Sets or overrides the level of any living entity (server side), even one the default rules skip. Max health
	 * is rescaled keeping the current health fraction; tracking clients are told.
	 */
	@SuppressWarnings("UnstableApiUsage")
	public static void setLevel(LivingEntity entity, int level) {
		if (entity.getWorld().isClient) {
			throw new IllegalStateException("Mob levels are set on the server");
		}
		level = MobLevelRules.clamp(level);
		entity.setAttached(LEVEL, level);
		applyHealth(entity, level);
		PacketByteBuf buf = syncBuf(entity, level);
		for (ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
			ServerPlayNetworking.send(player, SYNC_PACKET, PacketByteBufs.copy(buf));
		}
	}

	/** Multiplier for damage dealt by {@code attacker} (the attacking entity or a projectile's owner). */
	public static float damageMultiplier(Entity attacker) {
		MobConfig.Levels config = MobConfig.get().levels();
		int level = getLevel(attacker);
		return config.enabled() && level > 0 ? MobLevelRules.damageMultiplier(config, level) : 1;
	}

	/** XP a dying entity drops, scaled by its level. */
	public static int scaleExperience(LivingEntity entity, int experience) {
		MobConfig.Levels config = MobConfig.get().levels();
		int level = getLevel(entity);
		return config.enabled() && level > 0 ? MobLevelRules.experience(config, level, experience) : experience;
	}

	/** The level the default (overworld §5.4) rules give a mob at its current position. */
	public static int defaultLevel(MobEntity mob, ServerWorld world) {
		MobConfig.Levels config = MobConfig.get().levels();
		String type = Registries.ENTITY_TYPE.getId(mob.getType()).toString();
		int base = config.baseLevels().getOrDefault(type, config.defaultBaseLevel());
		int dimensionBonus = config.dimensionBonus().getOrDefault(world.getRegistryKey().getValue().toString(), 0);
		boolean overworld = world.getRegistryKey() == World.OVERWORLD;
		double distance = 0;
		if (overworld) {
			BlockPos spawn = world.getSpawnPos();
			distance = Math.hypot(mob.getX() - spawn.getX(), mob.getZ() - spawn.getZ());
		}
		return MobLevelRules.level(config, base, dimensionBonus, distance, overworld && mob.getY() < 0);
	}

	/**
	 * The level a hostile mob gets when first added to a world at its current position: the first registered
	 * {@link LevelProvider} that answers, else {@link #defaultLevel}.
	 */
	public static int spawnLevel(MobEntity mob, ServerWorld world) {
		for (LevelProvider provider : PROVIDERS) {
			OptionalInt level = provider.levelFor(mob, world);
			if (level.isPresent()) {
				return MobLevelRules.clamp(level.getAsInt());
			}
		}
		return defaultLevel(mob, world);
	}

	private static void assign(MobEntity mob, ServerWorld world) {
		setLevel(mob, spawnLevel(mob, world));
	}

	/** Puts the health modifier in line with the level (and the current config), keeping the health fraction. */
	private static void applyHealth(LivingEntity entity, int level) {
		EntityAttributeInstance maxHealth = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		MobConfig.Levels config = MobConfig.get().levels();
		double bonus = config.enabled() ? MobLevelRules.healthBonus(config, level) : 0;
		EntityAttributeModifier current = maxHealth.getModifier(HEALTH_MODIFIER_ID);
		if (current == null ? bonus == 0 : current.getValue() == bonus) {
			return;
		}
		float fraction = entity.getHealth() / entity.getMaxHealth();
		maxHealth.removeModifier(HEALTH_MODIFIER_ID);
		if (bonus != 0) {
			maxHealth.addPersistentModifier(new EntityAttributeModifier(HEALTH_MODIFIER_ID, "Embervoid mob level", bonus,
					EntityAttributeModifier.Operation.MULTIPLY_BASE));
		}
		entity.setHealth(fraction * entity.getMaxHealth());
	}

	private static PacketByteBuf syncBuf(Entity entity, int level) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entity.getId());
		buf.writeVarInt(level);
		return buf;
	}

	public static void register() {
		Embervoid.LOGGER.debug("Mob config section: {}", MobConfig.SECTION.name());
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!(entity instanceof LivingEntity living)) {
				return;
			}
			int level = getLevel(living);
			if (level > 0) {
				applyHealth(living, level);
			} else if (MobConfig.get().levels().enabled() && canHaveLevel(living)) {
				assign((MobEntity) living, world);
			}
		});
		EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
			int level = getLevel(entity);
			if (level > 0) {
				ServerPlayNetworking.send(player, SYNC_PACKET, syncBuf(entity, level));
			}
		});
		MobLevelCommands.register();
	}

	/**
	 * Client-side copy of the levels of tracked entities, filled by the client's {@link #SYNC_PACKET} receiver.
	 * Lives here (common code) so {@link #getLevel} works on both sides; only the client writes to it.
	 */
	public static final class ClientLevels {
		private static final Int2IntOpenHashMap LEVELS = new Int2IntOpenHashMap();

		public static int get(int entityId) {
			return LEVELS.get(entityId);
		}

		public static void put(int entityId, int level) {
			LEVELS.put(entityId, level);
		}

		public static void remove(int entityId) {
			LEVELS.remove(entityId);
		}

		public static void clear() {
			LEVELS.clear();
		}

		private ClientLevels() {}
	}

	private MobLevels() {}
}
