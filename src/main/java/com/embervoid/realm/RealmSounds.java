package com.embervoid.realm;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of realm travel: vanilla files remapped in assets/embervoid/sounds.json. */
public final class RealmSounds {
	public static final SoundEvent TRAVEL = ModSounds.register("block.void_obelisk.travel");
	public static final SoundEvent REFUSE = ModSounds.register("block.void_obelisk.refuse");
	public static final SoundEvent GATE_OPEN = ModSounds.register("block.citadel_gate.open");
	public static final SoundEvent GATE_SEALED = ModSounds.register("block.citadel_gate.sealed");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private RealmSounds() {}
}
