package com.embervoid.registry;

import com.embervoid.Embervoid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Enemies and bosses. Attributes go through FabricDefaultAttributeRegistry. */
public final class ModEntities {

	public static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
		return Registry.register(Registries.ENTITY_TYPE, Embervoid.id(name), type);
	}

	public static void register() {
		Embervoid.LOGGER.debug("Registering entities");
	}

	private ModEntities() {}
}
