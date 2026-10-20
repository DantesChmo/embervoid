package com.embervoid.registry;

import com.embervoid.Embervoid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Sounds and boss music. Files live in assets/embervoid/sounds + sounds.json. */
public final class ModSounds {

	public static SoundEvent register(String name) {
		Identifier id = Embervoid.id(name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void register() {
		Embervoid.LOGGER.debug("Registering sounds");
	}

	private ModSounds() {}
}
