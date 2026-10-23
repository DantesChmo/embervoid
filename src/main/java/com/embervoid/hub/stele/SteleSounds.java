package com.embervoid.hub.stele;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the Cleft Stele: vanilla files remapped in assets/embervoid/sounds.json. */
public final class SteleSounds {
	public static final SoundEvent KINDLE = ModSounds.register("block.cleft_stele.kindle");
	public static final SoundEvent PUT_OUT = ModSounds.register("block.cleft_stele.put_out");
	public static final SoundEvent AMBIENT = ModSounds.register("block.stele_crack.ambient");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private SteleSounds() {}
}
