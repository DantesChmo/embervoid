package com.embervoid.registry;

import com.embervoid.Embervoid;
import com.embervoid.entity.AshRemnantEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Attributes go through FabricDefaultAttributeRegistry. */
public final class ModEntities {
	public static final EntityType<AshRemnantEntity> ASH_REMNANT = register("ash_remnant",
			FabricEntityTypeBuilder.<AshRemnantEntity>create(SpawnGroup.MISC, AshRemnantEntity::new)
					.dimensions(EntityDimensions.fixed(0.6f, 1.2f))
					.trackRangeBlocks(64)
					.fireImmune()
					.build());

	public static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
		return Registry.register(Registries.ENTITY_TYPE, Embervoid.id(name), type);
	}

	public static void register() {
		Embervoid.LOGGER.debug("Registering entities");
	}

	private ModEntities() {}
}
