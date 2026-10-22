package com.embervoid.reliquary;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the Reliquary screen (R40.3a): vanilla files remapped in assets/embervoid/sounds.json. */
public final class ReliquarySounds {
	public static final SoundEvent OPEN = ModSounds.register("ui.reliquary.open");
	public static final SoundEvent EQUIP = ModSounds.register("ui.reliquary.equip");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private ReliquarySounds() {}
}
