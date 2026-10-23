package com.embervoid.hub.stele;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the Cleft Stele: vanilla files remapped in assets/embervoid/sounds.json. */
public final class SteleSounds {
	public static final SoundEvent KINDLE = ModSounds.register("block.cleft_stele.kindle");
	public static final SoundEvent PUT_OUT = ModSounds.register("block.cleft_stele.put_out");
	public static final SoundEvent AMBIENT = ModSounds.register("block.stele_crack.ambient");
	/** Through a lit stele into the hub; heard at the stele left and where the player lands. */
	public static final SoundEvent ENTER = ModSounds.register("block.cleft_stele.enter");
	/** Through the hub's stele back out. */
	public static final SoundEvent LEAVE = ModSounds.register("block.cleft_stele.leave");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private SteleSounds() {}
}
