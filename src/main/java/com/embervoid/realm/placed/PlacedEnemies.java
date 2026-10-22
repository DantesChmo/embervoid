package com.embervoid.realm.placed;

import com.embervoid.Embervoid;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Placed enemies (PLAN 9.2, DESIGN §4.5 Bestiary): mobs set into structures at worldgen. Persistent, never respawned,
 * not reset by shrine rest; their level comes from the zone band like any realm mob ({@code RealmMobLevels}, when the
 * chunk loads).
 *
 * <p>A template holds an {@link #MARKER} block whose NBT is {@link #markerNbt}: {@code entity} (any entity id;
 * unknown ids log one warning and spawn nothing, so templates use final mob ids before the mobs exist), optional
 * {@code tags} added as command tags besides {@link #TAG} (e.g. {@code embervoid.placed.cage} for a caged Withered
 * Thrall) and optional {@code nbt} merged into the entity. The {@link PlacedEnemyProcessor} spawns it. A mob knows it
 * was placed by {@link #isPlaced} (the {@link #TAG} command tag).
 */
public final class PlacedEnemies {
	/** Command tag on every placed enemy. */
	public static final String TAG = "embervoid.placed";

	public static final Block MARKER = Registry.register(Registries.BLOCK, Embervoid.id("placed_enemy"),
			new PlacedEnemyMarkerBlock(AbstractBlock.Settings.create().mapColor(MapColor.CLEAR).noCollision()
					.strength(-1, 3600000).dropsNothing().nonOpaque().pistonBehavior(PistonBehavior.BLOCK)));
	public static final StructureProcessorType<PlacedEnemyProcessor> PROCESSOR = Registry.register(
			Registries.STRUCTURE_PROCESSOR, Embervoid.id("placed_enemies"), () -> PlacedEnemyProcessor.CODEC);

	private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

	public static void register() {
		// Static fields register the marker and the processor.
	}

	/** True for a mob spawned by a placed-enemy marker. */
	public static boolean isPlaced(Entity entity) {
		return entity.getCommandTags().contains(TAG);
	}

	/** Marker NBT for a template: the entity id and optional NBT merged into it. */
	public static NbtCompound markerNbt(Identifier entity, @Nullable NbtCompound entityNbt) {
		return markerNbt(entity, entityNbt, List.of());
	}

	/** Marker NBT for a template: the entity id, optional NBT merged into it and extra command tags. */
	public static NbtCompound markerNbt(Identifier entity, @Nullable NbtCompound entityNbt, List<String> tags) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("entity", entity.toString());
		if (entityNbt != null) {
			nbt.put("nbt", entityNbt.copy());
		}
		if (!tags.isEmpty()) {
			NbtList list = new NbtList();
			tags.forEach(tag -> list.add(NbtString.of(tag)));
			nbt.put("tags", list);
		}
		return nbt;
	}

	/**
	 * Spawns the entity a marker names at {@code pos} (standing in the block's centre), facing {@code yaw}.
	 *
	 * @return the entity, or null for an unknown id
	 */
	@Nullable
	public static Entity spawn(ServerWorldAccess world, BlockPos pos, float yaw, NbtCompound marker) {
		String name = marker.getString("entity");
		Identifier id = Identifier.tryParse(name);
		Optional<EntityType<?>> type = id == null ? Optional.empty() : Registries.ENTITY_TYPE.getOrEmpty(id);
		if (type.isEmpty()) {
			if (WARNED.add(name)) {
				Embervoid.LOGGER.warn("Placed enemy: unknown entity id '{}' at {}, nothing spawned", name, pos);
			}
			return null;
		}
		Entity entity = type.get().create(world.toServerWorld());
		if (entity == null) {
			return null;
		}
		entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
		entity.setHeadYaw(yaw);
		entity.setBodyYaw(yaw);
		if (entity instanceof MobEntity mob) {
			mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
		}
		if (marker.contains("nbt", NbtElement.COMPOUND_TYPE)) {
			NbtCompound merged = entity.writeNbt(new NbtCompound());
			merged.copyFrom(marker.getCompound("nbt"));
			entity.readNbt(merged);
		}
		entity.addCommandTag(TAG);
		NbtList tags = marker.getList("tags", NbtElement.STRING_TYPE);
		for (int i = 0; i < tags.size(); i++) {
			entity.addCommandTag(tags.getString(i));
		}
		if (entity instanceof MobEntity mob) {
			mob.setPersistent();
		}
		world.spawnEntityAndPassengers(entity);
		return entity;
	}

	private PlacedEnemies() {}
}
